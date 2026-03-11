"""
Minimal worker configuration.
The worker only needs MQ address and a working directory.
All video-specific config (API keys, rendering params) comes via MQ messages.
"""
from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field, field_validator
from pathlib import Path
import logging

logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).parent.parent


class WorkerConfig(BaseSettings):
    """Lean configuration: only infrastructure settings, no business config."""

    # -- RocketMQ connection --
    rocketmq_namesrv: str = "localhost:9876"
    mq_consumer_group: str = "avc-python-task-consumer-group"
    mq_producer_group: str = "avc-python-producer-group"
    mq_max_concurrent_tasks: int = 2

    # -- Working directories --
    output_dir: Path = Field(default_factory=lambda: BASE_DIR / "output")
    videos_dir: Path = Field(default_factory=lambda: BASE_DIR / "output" / "videos")
    audio_dir: Path = Field(default_factory=lambda: BASE_DIR / "output" / "audio")
    images_dir: Path = Field(default_factory=lambda: BASE_DIR / "output" / "images")
    subtitles_dir: Path = Field(default_factory=lambda: BASE_DIR / "output" / "subtitles")
    clips_dir: Path = Field(default_factory=lambda: BASE_DIR / "output" / "clips")

    # -- Health endpoint --
    health_port: int = 8081

    # -- Logging --
    debug: bool = False
    log_level: str = "INFO"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    @field_validator("health_port")
    @classmethod
    def validate_port(cls, v: int) -> int:
        if not 1 <= v <= 65535:
            raise ValueError("Port must be between 1 and 65535")
        return v

    def ensure_dirs(self) -> None:
        """Create output directories if they don't exist."""
        for d in [self.output_dir, self.videos_dir, self.audio_dir,
                  self.images_dir, self.subtitles_dir, self.clips_dir]:
            d.mkdir(parents=True, exist_ok=True)


config = WorkerConfig()
config.ensure_dirs()
