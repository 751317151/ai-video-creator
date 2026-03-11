package com.avc.infra.service;

import com.avc.infra.entity.VideoStatisticEntity;
import com.avc.infra.mapper.VideoStatisticMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final VideoStatisticMapper statisticMapper;

    public Map<String, Object> getSummary() {
        Instant last7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant last30Days = Instant.now().minus(30, ChronoUnit.DAYS);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalVideos", statisticMapper.selectCount(null));
        summary.put("videosLast7Days", statisticMapper.countSince(last7Days));
        summary.put("videosLast30Days", statisticMapper.countSince(last30Days));
        summary.put("totalViews", statisticMapper.totalViews());
        summary.put("totalLikes", statisticMapper.totalLikes());
        summary.put("avgDurationSeconds", statisticMapper.avgDuration());
        summary.put("avgGenerationTimeMs", statisticMapper.avgGenerationTimeMs());
        return summary;
    }

    public IPage<VideoStatisticEntity> listVideos(int page, int size) {
        return statisticMapper.selectPageOrdered(new Page<>(page + 1, size));
    }

    public List<Map<String, Object>> getVideoTypeBreakdown() {
        return statisticMapper.countByVideoType();
    }

    public List<Map<String, Object>> getPlatformBreakdown() {
        return statisticMapper.countByPlatform();
    }

    @Transactional
    public VideoStatisticEntity recordStat(String jobId, String title, String videoType,
                                           int durationSeconds, long fileSizeBytes,
                                           long generationTimeMs) {
        VideoStatisticEntity stat = new VideoStatisticEntity();
        stat.setJobId(jobId);
        stat.setTitle(title);
        stat.setVideoType(videoType);
        stat.setDurationSeconds(durationSeconds);
        stat.setFileSizeBytes(fileSizeBytes);
        stat.setGenerationTimeMs(generationTimeMs);
        statisticMapper.insert(stat);
        return stat;
    }

    public List<Map<String, Object>> getDailyTrends(int days) {
        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        return statisticMapper.dailyCountSince(since);
    }

    public Map<String, Object> getPerformance() {
        Map<String, Object> perf = new LinkedHashMap<>();
        perf.put("avgDurationSeconds", statisticMapper.avgDuration());
        perf.put("avgGenerationTimeMs", statisticMapper.avgGenerationTimeMs());
        perf.put("avgFileSizeBytes", statisticMapper.avgFileSize());
        perf.put("totalViews", statisticMapper.totalViews());
        perf.put("totalLikes", statisticMapper.totalLikes());
        perf.put("totalShares", statisticMapper.totalShares());
        return perf;
    }
}
