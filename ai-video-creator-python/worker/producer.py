"""RocketMQ Producer for video-progress and video-result topics."""
from __future__ import annotations

import logging

from rocketmq.client import Producer, Message

from worker.messages import ProgressUpdateMessage, VideoResultMessage

logger = logging.getLogger(__name__)

TOPIC_PROGRESS = "video-progress"
TOPIC_RESULT = "video-result"


class MessageProducer:
    """Sends progress updates and final results to RocketMQ."""

    def __init__(self, namesrv: str, group: str):
        self._namesrv = namesrv
        self._group = group
        self._producer: Producer | None = None

    def start(self):
        self._producer = Producer(self._group)
        self._producer.set_name_server_address(self._namesrv)
        self._producer.start()
        logger.info("Producer started: group=%s", self._group)

    def send_progress(self, job_id: str, percent: int, message: str):
        msg = ProgressUpdateMessage(job_id=job_id, percent=percent, message=message)
        self._send(TOPIC_PROGRESS, msg.to_json())
        logger.debug("Progress sent: job=%s, percent=%d", job_id, percent)

    def send_result(self, result: VideoResultMessage):
        self._send(TOPIC_RESULT, result.to_json())
        logger.info("Result sent: job=%s, status=%s", result.job_id, result.status)

    def _send(self, topic: str, body: str):
        if not self._producer:
            logger.error("Producer not started, cannot send to %s", topic)
            return
        try:
            mq_msg = Message(topic)
            mq_msg.set_body(body.encode("utf-8"))
            self._producer.send_sync(mq_msg)
        except Exception:
            logger.exception("Failed to send message to %s", topic)

    def stop(self):
        if self._producer:
            self._producer.shutdown()
            logger.info("Producer stopped.")
