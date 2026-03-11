package com.avc.scheduler.uploader;

import com.avc.common.enums.Platform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Bilibili uploader via Cookie-based authentication.
 *
 * Upload flow:
 * 1. Pre-upload: obtain upload endpoint, auth, and upload-id.
 * 2. Chunked upload: split the file and PUT each chunk to the upos endpoint.
 * 3. Merge chunks: POST to signal all chunks have been uploaded.
 * 4. Submit: publish the video with metadata via the web add API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BilibiliUploader implements PlatformUploader {

    private static final String PREUPLOAD_URL = "https://member.bilibili.com/preupload";
    private static final String SUBMIT_URL = "https://member.bilibili.com/x/vu/web/add/v3";
    private static final String NAV_URL = "https://api.bilibili.com/x/web-interface/nav";

    private static final int DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024; // 4 MB
    private static final int DEFAULT_TID = 122; // Tech channel
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private static final MediaType MEDIA_TYPE_OCTET = MediaType.parse("application/octet-stream");
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    @Override
    public Platform getPlatform() {
        return Platform.BILIBILI;
    }

    @Override
    public String upload(Path videoPath, String title, String description,
                         String tags, Map<String, String> credentials) {
        validateRequiredCredentials(credentials);

        String sessdata = credentials.get("SESSDATA");
        String biliJct = credentials.get("bili_jct");

        log.info("Uploading to Bilibili: title={}, file={}", title, videoPath.getFileName());

        try {
            long fileSize = Files.size(videoPath);
            String fileName = videoPath.getFileName().toString();

            // Step 1: Pre-upload to obtain upload URL and tokens
            JsonNode preUploadResult = preUpload(fileName, fileSize, sessdata);
            String uploadUrl = extractUploadUrl(preUploadResult);
            String auth = preUploadResult.get("auth").asText();
            String biliFilename = preUploadResult.get("bili_filename").asText();
            String uploadId = initUpload(uploadUrl, auth, sessdata);

            log.info("Pre-upload complete: biliFilename={}, uploadId={}", biliFilename, uploadId);

            // Step 2: Upload video in chunks
            int chunkSize = preUploadResult.has("chunk_size")
                    ? preUploadResult.get("chunk_size").asInt(DEFAULT_CHUNK_SIZE)
                    : DEFAULT_CHUNK_SIZE;
            int totalChunks = (int) Math.ceil((double) fileSize / chunkSize);

            uploadChunks(videoPath, uploadUrl, auth, sessdata, uploadId,
                    chunkSize, totalChunks, fileSize);

            log.info("All {} chunks uploaded successfully", totalChunks);

            // Step 3: Merge chunks
            mergeChunks(uploadUrl, auth, sessdata, uploadId, fileName, totalChunks, fileSize);

            log.info("Chunks merged successfully");

            // Step 4: Submit video with metadata
            String bvid = submitVideo(biliFilename, title, description, tags,
                    sessdata, biliJct);

            log.info("Video submitted to Bilibili: bvid={}", bvid);
            return bvid;

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload video to Bilibili: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateCredentials(Map<String, String> credentials) {
        if (credentials == null
                || !credentials.containsKey("SESSDATA")
                || !credentials.containsKey("bili_jct")) {
            return false;
        }

        String sessdata = credentials.get("SESSDATA");
        Request request = new Request.Builder()
                .url(NAV_URL)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .addHeader("User-Agent", buildUserAgent())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("Bilibili credential validation failed: HTTP {}", response.code());
                return false;
            }

            JsonNode body = objectMapper.readTree(response.body().string());
            int code = body.get("code").asInt(-1);
            if (code == 0) {
                String uname = body.path("data").path("uname").asText("unknown");
                log.info("Bilibili credentials valid for user: {}", uname);
                return true;
            }

            log.warn("Bilibili credential validation returned code={}, message={}",
                    code, body.path("message").asText());
            return false;

        } catch (IOException e) {
            log.error("Failed to validate Bilibili credentials", e);
            return false;
        }
    }

    /**
     * Step 1: Pre-upload request to obtain upload endpoint and authorization.
     */
    private JsonNode preUpload(String fileName, long fileSize, String sessdata) throws IOException {
        HttpUrl url = HttpUrl.parse(PREUPLOAD_URL).newBuilder()
                .addQueryParameter("name", fileName)
                .addQueryParameter("size", String.valueOf(fileSize))
                .addQueryParameter("r", "upos")
                .addQueryParameter("profile", "ugcupos/bup")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .addHeader("User-Agent", buildUserAgent())
                .get()
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            validateHttpResponse(response, "Pre-upload");
            JsonNode result = objectMapper.readTree(response.body().string());

            if (!result.has("bili_filename") || !result.has("auth")) {
                throw new IOException("Pre-upload response missing required fields: " + result);
            }

            return result;
        }
    }

    /**
     * Extract the full upload URL from the pre-upload response.
     * The response contains either 'url' directly, or 'endpoint' + 'upos_uri'.
     */
    private String extractUploadUrl(JsonNode preUploadResult) {
        if (preUploadResult.has("url") && !preUploadResult.get("url").isNull()) {
            return preUploadResult.get("url").asText();
        }

        String endpoint = preUploadResult.path("endpoint").asText("");
        String uposUri = preUploadResult.path("upos_uri").asText("");

        if (endpoint.isEmpty() || uposUri.isEmpty()) {
            throw new RuntimeException(
                    "Pre-upload response missing upload URL fields: " + preUploadResult);
        }

        String scheme = endpoint.startsWith("//") ? "https:" : "";
        return scheme + endpoint + uposUri;
    }

    /**
     * Initialize the upload session and obtain the upload_id.
     */
    private String initUpload(String uploadUrl, String auth, String sessdata) throws IOException {
        HttpUrl url = HttpUrl.parse(uploadUrl).newBuilder()
                .addQueryParameter("uploads", "")
                .addQueryParameter("output", "json")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Upos-Auth", auth)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .addHeader("User-Agent", buildUserAgent())
                .post(RequestBody.create(new byte[0], null))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            validateHttpResponse(response, "Init upload");
            JsonNode result = objectMapper.readTree(response.body().string());

            String uploadId = result.path("upload_id").asText(null);
            if (uploadId == null || uploadId.isEmpty()) {
                throw new IOException("Init upload did not return upload_id: " + result);
            }

            return uploadId;
        }
    }

    /**
     * Step 2: Upload file in chunks using PUT requests.
     */
    private void uploadChunks(Path videoPath, String uploadUrl, String auth,
                              String sessdata, String uploadId,
                              int chunkSize, int totalChunks,
                              long fileSize) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(videoPath.toFile(), "r")) {
            for (int chunkIndex = 0; chunkIndex < totalChunks; chunkIndex++) {
                long start = (long) chunkIndex * chunkSize;
                int currentChunkSize = (int) Math.min(chunkSize, fileSize - start);
                byte[] buffer = new byte[currentChunkSize];

                raf.seek(start);
                raf.readFully(buffer);

                uploadSingleChunk(uploadUrl, auth, sessdata, uploadId, buffer,
                        chunkIndex, totalChunks, currentChunkSize, start, fileSize);

                log.debug("Uploaded chunk {}/{} ({} bytes)",
                        chunkIndex + 1, totalChunks, currentChunkSize);
            }
        }
    }

    /**
     * Upload a single chunk with retry logic.
     */
    private void uploadSingleChunk(String uploadUrl, String auth, String sessdata,
                                   String uploadId, byte[] data,
                                   int chunkIndex, int totalChunks,
                                   int chunkSize, long start,
                                   long fileSize) throws IOException {
        HttpUrl url = HttpUrl.parse(uploadUrl).newBuilder()
                .addQueryParameter("partNumber", String.valueOf(chunkIndex + 1))
                .addQueryParameter("uploadId", uploadId)
                .addQueryParameter("chunk", String.valueOf(chunkIndex))
                .addQueryParameter("chunks", String.valueOf(totalChunks))
                .addQueryParameter("size", String.valueOf(chunkSize))
                .addQueryParameter("start", String.valueOf(start))
                .addQueryParameter("end", String.valueOf(start + chunkSize))
                .addQueryParameter("total", String.valueOf(fileSize))
                .build();

        RequestBody body = RequestBody.create(data, MEDIA_TYPE_OCTET);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Upos-Auth", auth)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .addHeader("User-Agent", buildUserAgent())
                .put(body)
                .build();

        executeWithRetry(request, "Upload chunk " + (chunkIndex + 1));
    }

    /**
     * Step 3: Signal the server to merge all uploaded chunks.
     */
    private void mergeChunks(String uploadUrl, String auth, String sessdata,
                             String uploadId, String fileName,
                             int totalChunks, long fileSize) throws IOException {
        HttpUrl url = HttpUrl.parse(uploadUrl).newBuilder()
                .addQueryParameter("output", "json")
                .addQueryParameter("name", fileName)
                .addQueryParameter("profile", "ugcupos/bup")
                .addQueryParameter("uploadId", uploadId)
                .addQueryParameter("biz_id", "0")
                .build();

        ObjectNode mergePayload = objectMapper.createObjectNode();
        ArrayNode partsArray = objectMapper.createArrayNode();
        for (int i = 1; i <= totalChunks; i++) {
            ObjectNode part = objectMapper.createObjectNode();
            part.put("partNumber", i);
            part.put("eTag", "etag");
            partsArray.add(part);
        }
        mergePayload.set("parts", partsArray);

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsBytes(mergePayload), MEDIA_TYPE_JSON);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Upos-Auth", auth)
                .addHeader("Cookie", "SESSDATA=" + sessdata)
                .addHeader("User-Agent", buildUserAgent())
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            validateHttpResponse(response, "Merge chunks");
            String responseBody = response.body().string();
            JsonNode result = objectMapper.readTree(responseBody);

            int ok = result.path("OK").asInt(-1);
            if (ok != 1) {
                throw new IOException("Chunk merge failed: " + responseBody);
            }
        }
    }

    /**
     * Step 4: Submit the video with metadata to publish it.
     */
    private String submitVideo(String biliFilename, String title, String description,
                               String tags, String sessdata,
                               String biliJct) throws IOException {
        ObjectNode videoEntry = objectMapper.createObjectNode();
        videoEntry.put("filename", biliFilename);
        videoEntry.put("title", title);
        videoEntry.put("desc", description);

        ArrayNode videosArray = objectMapper.createArrayNode();
        videosArray.add(videoEntry);

        ObjectNode submitPayload = objectMapper.createObjectNode();
        submitPayload.put("copyright", 1);
        submitPayload.put("source", "");
        submitPayload.put("tid", DEFAULT_TID);
        submitPayload.put("title", title);
        submitPayload.put("desc_format_id", 0);
        submitPayload.put("desc", description);
        submitPayload.put("tag", tags);
        submitPayload.set("videos", videosArray);
        submitPayload.put("csrf", biliJct);

        RequestBody body = RequestBody.create(
                objectMapper.writeValueAsBytes(submitPayload), MEDIA_TYPE_JSON);

        HttpUrl url = HttpUrl.parse(SUBMIT_URL).newBuilder()
                .addQueryParameter("csrf", biliJct)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", buildCookieHeader(sessdata, biliJct))
                .addHeader("User-Agent", buildUserAgent())
                .addHeader("Referer", "https://member.bilibili.com/platform/upload/video/frame")
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            validateHttpResponse(response, "Submit video");
            String responseBody = response.body().string();
            JsonNode result = objectMapper.readTree(responseBody);

            int code = result.path("code").asInt(-1);
            if (code != 0) {
                throw new IOException("Video submit failed: code=" + code
                        + ", message=" + result.path("message").asText());
            }

            JsonNode data = result.path("data");
            String bvid = data.path("bvid").asText(null);
            if (bvid == null || bvid.isEmpty()) {
                throw new IOException("Submit succeeded but no bvid returned: " + responseBody);
            }

            return bvid;
        }
    }

    /**
     * Execute an HTTP request with retry logic for transient failures.
     */
    private void executeWithRetry(Request request, String operationName) throws IOException {
        IOException lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return;
                }

                int code = response.code();
                String body = response.body() != null ? response.body().string() : "no body";

                if (code >= 500 && attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("{} attempt {}/{} failed with HTTP {}: {}",
                            operationName, attempt, MAX_RETRY_ATTEMPTS, code, body);
                    sleep(RETRY_DELAY_MS * attempt);
                    continue;
                }

                throw new IOException(operationName + " failed: HTTP " + code + " - " + body);
            } catch (IOException e) {
                lastException = e;
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("{} attempt {}/{} encountered error: {}",
                            operationName, attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                    sleep(RETRY_DELAY_MS * attempt);
                } else {
                    throw e;
                }
            }
        }

        throw lastException;
    }

    private void validateRequiredCredentials(Map<String, String> credentials) {
        if (credentials == null) {
            throw new IllegalArgumentException("Credentials map must not be null");
        }

        List<String> missing = new ArrayList<>();
        if (!credentials.containsKey("SESSDATA")) {
            missing.add("SESSDATA");
        }
        if (!credentials.containsKey("bili_jct")) {
            missing.add("bili_jct");
        }

        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required Bilibili credentials: " + String.join(", ", missing));
        }
    }

    private void validateHttpResponse(Response response, String operationName) throws IOException {
        if (!response.isSuccessful()) {
            String body = response.body() != null ? response.body().string() : "no body";
            throw new IOException(operationName + " failed: HTTP " + response.code() + " - " + body);
        }
        if (response.body() == null) {
            throw new IOException(operationName + " returned empty body");
        }
    }

    private String buildCookieHeader(String sessdata, String biliJct) {
        return "SESSDATA=" + sessdata + "; bili_jct=" + biliJct;
    }

    private String buildUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Upload interrupted", e);
        }
    }
}
