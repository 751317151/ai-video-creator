"""
Message dataclasses matching Java DTOs for RocketMQ communication.
Updated to match the enhanced VideoTaskMessage from Java Phase 2.
"""
from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Optional


@dataclass
class VideoTaskMessage:
    """
    Consumed from topic: video-task-submit (Java -> Python).
    Contains ALL configuration — worker needs no local config for video processing.
    """

    job_id: str = ""
    action: str = "CREATE"
    topic: Optional[str] = None
    script_json: Optional[str] = None
    video_type: Optional[str] = None
    video_source: Optional[str] = None

    # TTS config
    voice: Optional[str] = None
    tts_rate: Optional[str] = None

    # Video rendering config
    video_width: int = 1080
    video_height: int = 1920
    video_fps: int = 30
    subtitle_font_size: int = 48
    subtitle_color: str = "white"

    # Media source config
    pexels_api_key: Optional[str] = None
    ai_video_provider: Optional[str] = None
    ai_video_api_key: Optional[str] = None

    # BGM config
    bgm_storage_path: Optional[str] = None
    bgm_volume: float = 0.3

    # Extra
    extra_requirements: Optional[str] = None
    template_id: Optional[str] = None

    @classmethod
    def from_json(cls, raw: str) -> VideoTaskMessage:
        data = json.loads(raw)
        return cls(
            job_id=data.get("job_id", ""),
            action=data.get("action", "CREATE"),
            topic=data.get("topic"),
            script_json=data.get("script_json"),
            video_type=data.get("video_type"),
            video_source=data.get("video_source"),
            voice=data.get("voice"),
            tts_rate=data.get("tts_rate"),
            video_width=data.get("video_width", 1080),
            video_height=data.get("video_height", 1920),
            video_fps=data.get("video_fps", 30),
            subtitle_font_size=data.get("subtitle_font_size", 48),
            subtitle_color=data.get("subtitle_color", "white"),
            pexels_api_key=data.get("pexels_api_key"),
            ai_video_provider=data.get("ai_video_provider"),
            ai_video_api_key=data.get("ai_video_api_key"),
            bgm_storage_path=data.get("bgm_storage_path"),
            bgm_volume=data.get("bgm_volume", 0.3),
            extra_requirements=data.get("extra_requirements"),
            template_id=data.get("template_id"),
        )


@dataclass
class ProgressUpdateMessage:
    """Produced to topic: video-progress (Python -> Java)."""

    job_id: str = ""
    percent: int = 0
    message: str = ""

    def to_json(self) -> str:
        return json.dumps(
            {"job_id": self.job_id, "percent": self.percent, "message": self.message},
            ensure_ascii=False,
        )


@dataclass
class VideoResultMessage:
    """Produced to topic: video-result (Python -> Java)."""

    job_id: str = ""
    status: str = "COMPLETED"
    video_path: Optional[str] = None
    thumbnail_path: Optional[str] = None
    title: Optional[str] = None
    description: Optional[str] = None
    duration_seconds: int = 0
    file_size_bytes: int = 0
    generation_time_ms: int = 0
    error: Optional[str] = None

    def to_json(self) -> str:
        return json.dumps(
            {
                "job_id": self.job_id,
                "status": self.status,
                "video_path": self.video_path or "",
                "thumbnail_path": self.thumbnail_path or "",
                "title": self.title or "",
                "description": self.description or "",
                "duration_seconds": self.duration_seconds,
                "file_size_bytes": self.file_size_bytes,
                "generation_time_ms": self.generation_time_ms,
                "error": self.error or "",
            },
            ensure_ascii=False,
        )
