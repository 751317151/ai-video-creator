package com.avc.app.repository;

import com.avc.app.BaseIntegrationTest;
import com.avc.common.enums.JobStatus;
import com.avc.infra.entity.JobEntity;
import com.avc.infra.mapper.JobMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest extends BaseIntegrationTest {

    @Autowired
    private JobMapper jobMapper;

    @BeforeEach
    void setUp() {
        jobMapper.delete(null);
    }

    @Test
    void shouldSaveAndFindJob() {
        JobEntity job = createJob("test-topic", JobStatus.QUEUED);
        jobMapper.insert(job);

        var found = jobMapper.selectById(job.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTopic()).isEqualTo("test-topic");
        assertThat(found.getStatus()).isEqualTo(JobStatus.QUEUED);
    }

    @Test
    void shouldFindAllOrderByCreatedAtDesc() {
        jobMapper.insert(createJob("first", JobStatus.QUEUED));
        jobMapper.insert(createJob("second", JobStatus.QUEUED));
        jobMapper.insert(createJob("third", JobStatus.QUEUED));

        IPage<JobEntity> page = jobMapper.selectPageOrdered(new Page<>(1, 10));
        assertThat(page.getRecords()).hasSize(3);
        // Most recent should be first
        assertThat(page.getRecords().get(0).getTopic()).isEqualTo("third");
    }

    @Test
    void shouldFindByStatus() {
        jobMapper.insert(createJob("queued-1", JobStatus.QUEUED));
        jobMapper.insert(createJob("queued-2", JobStatus.QUEUED));
        jobMapper.insert(createJob("running-1", JobStatus.RUNNING));
        jobMapper.insert(createJob("completed-1", JobStatus.COMPLETED));

        List<JobEntity> queuedJobs = jobMapper.selectByStatus(JobStatus.QUEUED.name());
        assertThat(queuedJobs).hasSize(2);

        List<JobEntity> runningJobs = jobMapper.selectByStatus(JobStatus.RUNNING.name());
        assertThat(runningJobs).hasSize(1);
    }

    @Test
    void shouldCountByStatus() {
        jobMapper.insert(createJob("q1", JobStatus.QUEUED));
        jobMapper.insert(createJob("q2", JobStatus.QUEUED));
        jobMapper.insert(createJob("r1", JobStatus.RUNNING));

        assertThat(jobMapper.countByStatus(JobStatus.QUEUED.name())).isEqualTo(2);
        assertThat(jobMapper.countByStatus(JobStatus.RUNNING.name())).isEqualTo(1);
        assertThat(jobMapper.countByStatus(JobStatus.COMPLETED.name())).isEqualTo(0);
    }

    @Test
    void shouldSupportPagination() {
        for (int i = 0; i < 25; i++) {
            jobMapper.insert(createJob("topic-" + i, JobStatus.QUEUED));
        }

        IPage<JobEntity> page0 = jobMapper.selectPageOrdered(new Page<>(1, 10));
        assertThat(page0.getRecords()).hasSize(10);
        assertThat(page0.getTotal()).isEqualTo(25);
        assertThat(page0.getPages()).isEqualTo(3);

        IPage<JobEntity> page2 = jobMapper.selectPageOrdered(new Page<>(3, 10));
        assertThat(page2.getRecords()).hasSize(5);
    }

    @Test
    void shouldAcceptFullUuidId() {
        String fullUuidId = "job_" + UUID.randomUUID().toString().replace("-", "");
        JobEntity job = new JobEntity();
        job.setId(fullUuidId);
        job.setTopic("uuid-test");
        job.setStatus(JobStatus.QUEUED);

        jobMapper.insert(job);

        assertThat(jobMapper.selectById(fullUuidId)).isNotNull();
        assertThat(fullUuidId.length()).isEqualTo(36); // "job_" (4) + 32 hex = 36
    }

    private JobEntity createJob(String topic, JobStatus status) {
        JobEntity job = new JobEntity();
        job.setId("job_" + UUID.randomUUID().toString().replace("-", ""));
        job.setTopic(topic);
        job.setStatus(status);
        return job;
    }
}
