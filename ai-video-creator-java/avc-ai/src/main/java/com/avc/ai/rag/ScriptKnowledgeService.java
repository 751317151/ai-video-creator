package com.avc.ai.rag;

import com.avc.infra.entity.ScriptKnowledgeEntity;
import com.avc.infra.mapper.ScriptKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScriptKnowledgeService {

    private final VectorStore vectorStore;
    private final ScriptKnowledgeMapper scriptKnowledgeMapper;

    public void ingest(String title, String content, String videoType, String tags) {
        Document doc = new Document(content, Map.of(
                "title", title,
                "videoType", videoType,
                "tags", tags
        ));
        vectorStore.add(List.of(doc));

        ScriptKnowledgeEntity entity = new ScriptKnowledgeEntity();
        entity.setTitle(title);
        entity.setScriptContent(content);
        entity.setVideoType(videoType);
        entity.setTags(tags);
        scriptKnowledgeMapper.insert(entity);

        log.info("Ingested script knowledge: title={}, type={}", title, videoType);
    }

    public void ingestFromJob(String jobId, String title, String content, String videoType) {
        ingest(title, content, videoType, "");
        Optional.ofNullable(scriptKnowledgeMapper.findByJobId(jobId)).ifPresentOrElse(
                existing -> log.info("Script already exists for job {}", jobId),
                () -> {
                    ScriptKnowledgeEntity entity = new ScriptKnowledgeEntity();
                    entity.setJobId(jobId);
                    entity.setTitle(title);
                    entity.setScriptContent(content);
                    entity.setVideoType(videoType);
                    scriptKnowledgeMapper.insert(entity);
                }
        );
    }

    public List<String> search(String query, int topK) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build());
        return docs.stream()
                .map(Document::getText)
                .toList();
    }
}
