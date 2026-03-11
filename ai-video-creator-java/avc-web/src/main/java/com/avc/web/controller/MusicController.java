package com.avc.web.controller;

import com.avc.common.dto.response.ApiResponse;
import com.avc.common.enums.MusicCategory;
import com.avc.common.enums.MusicMood;
import com.avc.infra.entity.MusicTrackEntity;
import com.avc.infra.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/music")
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;

    @GetMapping
    public ApiResponse<List<MusicTrackEntity>> list(
            @RequestParam(required = false) MusicCategory category,
            @RequestParam(required = false) MusicMood mood) {
        return ApiResponse.ok(musicService.listTracks(category, mood));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<MusicTrackEntity>> recommend() {
        return ApiResponse.ok(musicService.recommend());
    }

    @PostMapping("/upload")
    public ApiResponse<MusicTrackEntity> upload(
            @RequestParam String name,
            @RequestParam(required = false) String artist,
            @RequestParam(required = false) MusicCategory category,
            @RequestParam(required = false) MusicMood mood,
            @RequestParam(defaultValue = "0") int durationSeconds,
            @RequestParam("file") MultipartFile file) throws IOException {

        MusicTrackEntity track = musicService.upload(
                name, artist, category, mood, durationSeconds,
                file.getInputStream(), file.getOriginalFilename(), file.getSize());
        return ApiResponse.ok(track);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        musicService.delete(id);
        return ApiResponse.ok(null);
    }
}
