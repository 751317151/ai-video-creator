package com.avc.infra.service;

import com.avc.common.enums.MusicCategory;
import com.avc.common.enums.MusicMood;
import com.avc.common.enums.StorageType;
import com.avc.common.exception.BusinessException;
import com.avc.infra.entity.MusicTrackEntity;
import com.avc.infra.mapper.MusicTrackMapper;
import com.avc.infra.storage.MediaStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicTrackMapper musicTrackMapper;
    private final MediaStorage mediaStorage;

    public List<MusicTrackEntity> listTracks(MusicCategory category, MusicMood mood) {
        if (category != null) {
            return musicTrackMapper.findByCategoryOrderByNameAsc(category.name());
        }
        if (mood != null) {
            return musicTrackMapper.findByMoodOrderByUseCountDesc(mood.name());
        }
        return musicTrackMapper.findAllByOrderByUseCountDesc();
    }

    public List<MusicTrackEntity> recommend() {
        return musicTrackMapper.findTop10ByOrderByUseCountDesc();
    }

    @Transactional
    public MusicTrackEntity upload(String name, String artist, MusicCategory category,
                                   MusicMood mood, int durationSeconds,
                                   InputStream inputStream, String filename, long fileSize) {
        String storageKey = mediaStorage.upload(inputStream, StorageType.MUSIC, filename, fileSize);

        MusicTrackEntity track = new MusicTrackEntity();
        track.setName(name);
        track.setArtist(artist);
        track.setCategory(category != null ? category : MusicCategory.BGM);
        track.setMood(mood != null ? mood : MusicMood.NEUTRAL);
        track.setDurationSeconds(durationSeconds);
        track.setStorageKey(storageKey);
        track.setFileSize(fileSize);
        track.setUseCount(0);

        musicTrackMapper.insert(track);
        return track;
    }

    @Transactional
    public void delete(Long id) {
        MusicTrackEntity track = Optional.ofNullable(musicTrackMapper.selectById(id))
                .orElseThrow(() -> new BusinessException("Music track not found: " + id));

        mediaStorage.delete(track.getStorageKey());
        musicTrackMapper.deleteById(track.getId());
        log.info("Deleted music track: id={}, name={}", id, track.getName());
    }

    @Transactional
    public void incrementUseCount(Long id) {
        Optional.ofNullable(musicTrackMapper.selectById(id)).ifPresent(track -> {
            track.setUseCount(track.getUseCount() + 1);
            musicTrackMapper.updateById(track);
        });
    }
}
