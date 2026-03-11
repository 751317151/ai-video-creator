"""
AI Video Creator - Lean MQ Worker Entry Point.

Usage:
    python -m worker.main

Starts the RocketMQ consumer, health endpoint, and waits for tasks.
All video-specific configuration comes via MQ messages from Java.
"""
import logging
import signal
import sys
import time

# Fix Windows console encoding
if sys.platform == "win32":
    import codecs
    if sys.stdout.encoding != "utf-8":
        try:
            sys.stdout.reconfigure(encoding="utf-8")
        except AttributeError:
            sys.stdout = codecs.getwriter("utf-8")(sys.stdout.buffer, "strict")
    if sys.stderr.encoding != "utf-8":
        try:
            sys.stderr.reconfigure(encoding="utf-8")
        except AttributeError:
            sys.stderr = codecs.getwriter("utf-8")(sys.stderr.buffer, "strict")

from worker.config import config
from worker.consumer import TaskConsumer
from worker.producer import MessageProducer
from worker.task_handler import TaskHandler
from worker.health import start_health_server


def setup_logging():
    """Configure logging for the worker."""
    level = logging.DEBUG if config.debug else getattr(logging, config.log_level.upper(), logging.INFO)

    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
        stream=sys.stdout,
    )

    # Suppress noisy loggers
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)


def main():
    setup_logging()
    logger = logging.getLogger(__name__)

    logger.info("=" * 60)
    logger.info("AI Video Creator - MQ Worker")
    logger.info("RocketMQ: %s", config.rocketmq_namesrv)
    logger.info("Consumer Group: %s", config.mq_consumer_group)
    logger.info("Max Concurrent: %d", config.mq_max_concurrent_tasks)
    logger.info("Output Dir: %s", config.output_dir)
    logger.info("Health Port: %d", config.health_port)
    logger.info("=" * 60)

    # Start health endpoint
    start_health_server(config.health_port)

    # Start MQ producer
    producer = MessageProducer(
        namesrv=config.rocketmq_namesrv,
        group=config.mq_producer_group,
    )
    producer.start()

    # Start task handler
    handler = TaskHandler(
        producer=producer,
        max_concurrent=config.mq_max_concurrent_tasks,
    )

    # Start MQ consumer
    consumer = TaskConsumer(
        namesrv=config.rocketmq_namesrv,
        group=config.mq_consumer_group,
        handler=handler,
    )
    consumer.start()

    logger.info("Worker running. Press Ctrl+C to stop.")

    # Wait for shutdown signal
    stop = False

    def handle_signal(*_):
        nonlocal stop
        stop = True

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    try:
        while not stop:
            time.sleep(1)
    finally:
        logger.info("Shutting down...")
        consumer.stop()
        producer.stop()
        logger.info("Worker stopped.")


if __name__ == "__main__":
    main()
