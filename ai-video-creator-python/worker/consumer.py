"""RocketMQ Consumer for video-task-submit topic."""
from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rocketmq.client import PushConsumer, ConsumeStatus

from worker.messages import VideoTaskMessage

if TYPE_CHECKING:
    from worker.task_handler import TaskHandler

logger = logging.getLogger(__name__)

TOPIC = "video-task-submit"


class TaskConsumer:
    """Consumes video task messages from RocketMQ and dispatches to handler."""

    def __init__(self, namesrv: str, group: str, handler: TaskHandler):
        self._namesrv = namesrv
        self._group = group
        self._handler = handler
        self._consumer: PushConsumer | None = None

    def start(self):
        self._consumer = PushConsumer(self._group)
        self._consumer.set_name_server_address(self._namesrv)
        self._consumer.subscribe(TOPIC, self._on_message)
        self._consumer.start()
        logger.info("Consumer started: group=%s, topic=%s", self._group, TOPIC)

    def _on_message(self, msg):
        try:
            body = msg.body.decode("utf-8") if isinstance(msg.body, bytes) else msg.body
            logger.info("Received message: body_len=%d", len(body))

            task_msg = VideoTaskMessage.from_json(body)

            if task_msg.action == "CANCEL":
                self._handler.handle_cancel(task_msg.job_id)
            else:
                self._handler.handle_create(task_msg)

            return ConsumeStatus.CONSUME_SUCCESS
        except Exception:
            logger.exception("Failed to process message")
            return ConsumeStatus.RECONSUME_LATER

    def stop(self):
        if self._consumer:
            self._consumer.shutdown()
            logger.info("Consumer stopped.")
