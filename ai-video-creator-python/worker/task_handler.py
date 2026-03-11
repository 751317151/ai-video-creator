"""Task handler: bridges RocketMQ messages to the video pipeline."""
from __future__ import annotations

import asyncio
import logging
import threading
from concurrent.futures import ThreadPoolExecutor

from worker.messages import VideoResultMessage, VideoTaskMessage
from worker.pipeline import PipelineRequest, VideoPipeline
from worker.producer import MessageProducer

logger = logging.getLogger(__name__)


class TaskHandler:
    """Handles video task messages by running the pipeline."""

    def __init__(self, producer: MessageProducer, max_concurrent: int = 2):
        self._producer = producer
        self._executor = ThreadPoolExecutor(max_workers=max_concurrent)
        self._running_jobs: dict[str, bool] = {}
        self._lock = threading.Lock()

    def handle_create(self, msg: VideoTaskMessage):
        """Submit task to thread pool (called from MQ consumer thread)."""
        with self._lock:
            if msg.job_id in self._running_jobs:
                logger.warning("Job already running: %s", msg.job_id)
                return
            self._running_jobs[msg.job_id] = True

        self._executor.submit(self._run_in_thread, msg)

    def handle_cancel(self, job_id: str):
        """Mark a job as cancelled."""
        with self._lock:
            if job_id in self._running_jobs:
                self._running_jobs[job_id] = False
                logger.info("Job cancel requested: %s", job_id)

    def _run_in_thread(self, msg: VideoTaskMessage):
        """Run the async pipeline in a new event loop within a thread."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        try:
            loop.run_until_complete(self._execute_pipeline(msg))
        except Exception:
            logger.exception("Pipeline execution failed for job %s", msg.job_id)
            self._send_failure(msg.job_id, "Pipeline execution error")
        finally:
            loop.close()
            with self._lock:
                self._running_jobs.pop(msg.job_id, None)

    async def _execute_pipeline(self, msg: VideoTaskMessage):
        """Execute the video pipeline and send results via MQ."""
        logger.info("Starting pipeline for job %s, topic: %s", msg.job_id, msg.topic)

        def on_progress(message: str, percent: int):
            with self._lock:
                if not self._running_jobs.get(msg.job_id, False):
                    return
            self._producer.send_progress(msg.job_id, percent, message)

        request = PipelineRequest(
            job_id=msg.job_id,
            topic=msg.topic,
            script_json=msg.script_json,
            video_type=msg.video_type or "knowledge",
            video_source=msg.video_source or "pexels_video",
            voice=msg.voice,
            tts_rate=msg.tts_rate,
            video_width=msg.video_width,
            video_height=msg.video_height,
            video_fps=msg.video_fps,
            subtitle_font_size=msg.subtitle_font_size,
            subtitle_color=msg.subtitle_color,
            pexels_api_key=msg.pexels_api_key,
            ai_video_provider=msg.ai_video_provider,
            ai_video_api_key=msg.ai_video_api_key,
            bgm_storage_path=msg.bgm_storage_path,
            bgm_volume=msg.bgm_volume,
        )

        pipeline = VideoPipeline(progress_callback=on_progress)
        result = await pipeline.run(request)

        if result.success and result.video_result:
            vr = result.video_result
            self._producer.send_result(VideoResultMessage(
                job_id=msg.job_id,
                status="COMPLETED",
                video_path=str(vr.video_path) if vr.video_path else "",
                thumbnail_path=str(vr.thumbnail_path) if vr.thumbnail_path else "",
                title=result.title or "",
                description=result.description or "",
                duration_seconds=result.duration_seconds,
                file_size_bytes=result.file_size_bytes,
                generation_time_ms=result.generation_time_ms,
            ))
            logger.info("Job %s completed: %s", msg.job_id, vr.video_path)
        else:
            self._send_failure(msg.job_id, result.error or "Unknown pipeline error")

    def _send_failure(self, job_id: str, error: str):
        logger.error("Job %s failed: %s", job_id, error)
        self._producer.send_result(VideoResultMessage(
            job_id=job_id,
            status="FAILED",
            error=error,
        ))
