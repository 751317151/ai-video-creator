package com.avc.app.service;

import com.avc.app.BaseIntegrationTest;
import com.avc.infra.entity.VideoStatisticEntity;
import com.avc.infra.mapper.VideoStatisticMapper;
import com.avc.infra.service.StatisticsService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsServiceTest extends BaseIntegrationTest {

    @Autowired
    private StatisticsService statisticsService;

    @Autowired
    private VideoStatisticMapper statisticMapper;

    @BeforeEach
    void setUp() {
        statisticMapper.delete(null);
    }

    @Test
    void shouldRecordStatistic() {
        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "");

        VideoStatisticEntity stat = statisticsService.recordStat(
                jobId, "Test Video", "knowledge",
                65, 15_000_000L, 45_000L
        );

        assertThat(stat.getId()).isNotNull();
        assertThat(stat.getJobId()).isEqualTo(jobId);
        assertThat(stat.getTitle()).isEqualTo("Test Video");
        assertThat(stat.getVideoType()).isEqualTo("knowledge");
        assertThat(stat.getDurationSeconds()).isEqualTo(65);
        assertThat(stat.getFileSizeBytes()).isEqualTo(15_000_000L);
        assertThat(stat.getGenerationTimeMs()).isEqualTo(45_000L);
        assertThat(stat.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnSummaryWithEmptyData() {
        Map<String, Object> summary = statisticsService.getSummary();

        assertThat(summary).containsEntry("totalVideos", 0L);
        assertThat(summary).containsEntry("videosLast7Days", 0L);
        assertThat(summary).containsEntry("videosLast30Days", 0L);
        assertThat(summary).containsEntry("totalViews", 0L);
        assertThat(summary).containsEntry("totalLikes", 0L);
    }

    @Test
    void shouldReturnSummaryWithData() {
        createStat("job1", "Video 1", "knowledge", 60, 10_000_000L, 30_000L, 100, 50);
        createStat("job2", "Video 2", "story", 90, 20_000_000L, 60_000L, 200, 80);

        Map<String, Object> summary = statisticsService.getSummary();

        assertThat(summary).containsEntry("totalVideos", 2L);
        assertThat(summary).containsEntry("totalViews", 300L);
        assertThat(summary).containsEntry("totalLikes", 130L);
    }

    @Test
    void shouldListVideosPaginated() {
        for (int i = 0; i < 15; i++) {
            createStat("job" + i, "Video " + i, "knowledge", 60, 10_000_000L, 30_000L, 0, 0);
        }

        IPage<VideoStatisticEntity> page0 = statisticsService.listVideos(0, 10);
        assertThat(page0.getRecords()).hasSize(10);
        assertThat(page0.getTotal()).isEqualTo(15);

        IPage<VideoStatisticEntity> page1 = statisticsService.listVideos(1, 10);
        assertThat(page1.getRecords()).hasSize(5);
    }

    @Test
    void shouldReturnVideoTypeBreakdown() {
        createStat("job1", "V1", "knowledge", 60, 10_000_000L, 30_000L, 0, 0);
        createStat("job2", "V2", "knowledge", 60, 10_000_000L, 30_000L, 0, 0);
        createStat("job3", "V3", "story", 60, 10_000_000L, 30_000L, 0, 0);

        List<Map<String, Object>> breakdown = statisticsService.getVideoTypeBreakdown();

        assertThat(breakdown).isNotEmpty();
        assertThat(breakdown.get(0).get("video_type")).isEqualTo("knowledge");
        assertThat(((Number) breakdown.get(0).get("cnt")).longValue()).isEqualTo(2L);
    }

    @Test
    void shouldReturnPerformanceMetrics() {
        createStat("job1", "V1", "knowledge", 60, 10_000_000L, 30_000L, 100, 50);
        createStat("job2", "V2", "story", 90, 20_000_000L, 60_000L, 200, 80);

        Map<String, Object> perf = statisticsService.getPerformance();

        assertThat(perf).containsKey("avgDurationSeconds");
        assertThat(perf).containsKey("avgGenerationTimeMs");
        assertThat(perf).containsKey("avgFileSizeBytes");
        assertThat(perf).containsKey("totalViews");
        assertThat(perf).containsKey("totalLikes");
        assertThat(perf).containsKey("totalShares");
        assertThat((double) perf.get("avgDurationSeconds")).isEqualTo(75.0);
    }

    @Test
    void shouldReturnDailyTrends() {
        createStat("job1", "V1", "knowledge", 60, 10_000_000L, 30_000L, 0, 0);
        createStat("job2", "V2", "story", 90, 20_000_000L, 60_000L, 0, 0);

        List<Map<String, Object>> trends = statisticsService.getDailyTrends(30);

        assertThat(trends).isNotEmpty();
    }

    private void createStat(String jobId, String title, String videoType,
                            int duration, long fileSize, long genTime,
                            long views, long likes) {
        VideoStatisticEntity stat = new VideoStatisticEntity();
        stat.setJobId(jobId);
        stat.setTitle(title);
        stat.setVideoType(videoType);
        stat.setDurationSeconds(duration);
        stat.setFileSizeBytes(fileSize);
        stat.setGenerationTimeMs(genTime);
        stat.setViewCount(views);
        stat.setLikeCount(likes);
        stat.setShareCount(0);
        stat.setCommentCount(0);
        statisticMapper.insert(stat);
    }
}
