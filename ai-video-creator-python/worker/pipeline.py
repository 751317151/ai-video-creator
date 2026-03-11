"""
Pipeline - orchestrates the video creation workflow.
Script data comes from Java via MQ. Worker only does: TTS + Video Composition.
"""
import asyncio
import json
import logging
import time
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Optional, Callable

from worker.config import config
from worker.tts.synthesizer import TTSSynthesizer
from worker.video.composer import VideoComposer, VideoResult, RenderConfig

logger = logging.getLogger(__name__)


@dataclass
class PipelineRequest:
    """All parameters needed for video creation, extracted from VideoTaskMessage."""
    job_id: str
    topic: Optional[str] = None
    script_json: Optional[str] = None
    video_type: str = "knowledge"
    video_source: str = "pexels_video"

    # TTS
    voice: Optional[str] = None
    tts_rate: Optional[str] = None

    # Rendering
    video_width: int = 1080
    video_height: int = 1920
    video_fps: int = 30
    subtitle_font_size: int = 48
    subtitle_color: str = "white"

    # Media sources
    pexels_api_key: Optional[str] = None
    ai_video_provider: Optional[str] = None
    ai_video_api_key: Optional[str] = None

    # BGM
    bgm_storage_path: Optional[str] = None
    bgm_volume: float = 0.3


@dataclass
class PipelineResult:
    success: bool
    video_result: Optional[VideoResult] = None
    title: str = ""
    description: str = ""
    error: Optional[str] = None
    duration_seconds: int = 0
    file_size_bytes: int = 0
    generation_time_ms: int = 0


class VideoPipeline:
    def __init__(self, progress_callback: Callable[[str, int], None] = None):
        self.progress_cb = progress_callback or (lambda msg, pct: None)

    async def run(self, request: PipelineRequest) -> PipelineResult:
        pipeline_start = time.time()

        try:
            output_name = str(uuid.uuid4())[:8]

            # -- Parse script from Java --
            script_data = self._parse_script(request)
            segments = script_data.get("segments", [])
            title = script_data.get("title", request.topic or "Video")
            description = script_data.get("hook", "")

            if not segments:
                return PipelineResult(success=False, error="No script segments provided")

            narration = " ".join(seg.get("text", "") for seg in segments)
            if not narration.strip():
                return PipelineResult(success=False, error="Script segments contain no text")

            # -- Step 1: TTS --
            self.progress_cb("Synthesizing voice...", 15)
            logger.info("Step 1: TTS for job %s", request.job_id)

            tts = TTSSynthesizer(
                voice=request.voice,
                rate=request.tts_rate,
            )
            audio_path = config.audio_dir / f"{output_name}.mp3"
            tts_result = await tts.synthesize(narration, audio_path)

            # -- Step 2: Compose video --
            self.progress_cb("Composing video...", 40)
            logger.info("Step 2: Video composition for job %s", request.job_id)

            render_cfg = RenderConfig(
                width=request.video_width,
                height=request.video_height,
                fps=request.video_fps,
                subtitle_font_size=request.subtitle_font_size,
                subtitle_color=request.subtitle_color,
                pexels_api_key=request.pexels_api_key,
                ai_video_provider=request.ai_video_provider,
                ai_video_api_key=request.ai_video_api_key,
                video_source=request.video_source or "pexels_video",
            )

            composer = VideoComposer(cfg=render_cfg, output_dir=config.output_dir)
            video_result = await asyncio.to_thread(
                composer.compose,
                script_segments=segments,
                title=title,
                tts_result=tts_result,
                output_name=output_name,
            )

            # -- Step 3: Mix BGM (optional) --
            if request.bgm_storage_path:
                self.progress_cb("Mixing background music...", 80)
                self._mix_bgm(video_result, request, output_name)

            self.progress_cb("Done!", 100)

            # Calculate stats
            generation_time_ms = int((time.time() - pipeline_start) * 1000)
            file_size = video_result.video_path.stat().st_size if video_result.video_path.exists() else 0

            return PipelineResult(
                success=True,
                video_result=video_result,
                title=title,
                description=description,
                duration_seconds=int(tts_result.duration),
                file_size_bytes=file_size,
                generation_time_ms=generation_time_ms,
            )

        except Exception as e:
            logger.exception("Pipeline failed for job %s", request.job_id)
            generation_time_ms = int((time.time() - pipeline_start) * 1000)
            return PipelineResult(
                success=False,
                error=str(e),
                generation_time_ms=generation_time_ms,
            )

    def _parse_script(self, request: PipelineRequest) -> dict:
        """Parse script JSON from Java. Falls back to minimal structure."""
        if request.script_json:
            try:
                data = json.loads(request.script_json)
                if isinstance(data, dict) and "segments" in data:
                    return data
            except json.JSONDecodeError:
                logger.warning("Invalid script_json for job %s, using topic fallback", request.job_id)

        # Fallback: create a simple single-segment script from topic
        if request.topic:
            return {
                "title": request.topic,
                "hook": request.topic,
                "segments": [
                    {"text": request.topic, "image_keyword": "abstract", "duration": 10}
                ],
                "tags": [],
            }

        return {"title": "Untitled", "segments": [], "tags": []}

    def _mix_bgm(self, video_result: VideoResult, request: PipelineRequest, output_name: str):
        """Mix background music if a BGM path is provided."""
        import subprocess
        import shutil

        bgm_path = Path(request.bgm_storage_path)
        if not bgm_path.exists():
            logger.warning("BGM file not found: %s", bgm_path)
            return

        try:
            bgm_output = config.videos_dir / f"{output_name}_bgm.mp4"
            volume = request.bgm_volume

            cmd = [
                "ffmpeg", "-y",
                "-i", str(video_result.video_path),
                "-i", str(bgm_path),
                "-filter_complex",
                f"[1:a]volume={volume},aloop=loop=-1:size=2e+09[bgm];"
                f"[0:a][bgm]amix=inputs=2:duration=first:dropout_transition=2[aout]",
                "-map", "0:v", "-map", "[aout]",
                "-c:v", "copy", "-c:a", "aac",
                "-shortest",
                str(bgm_output),
            ]
            result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)

            if result.returncode == 0:
                shutil.move(str(bgm_output), str(video_result.video_path))
                logger.info("BGM mixed successfully for %s", output_name)
            else:
                logger.warning("BGM mixing failed: %s", result.stderr[-200:])
        except Exception as e:
            logger.warning("BGM mixing error: %s", e)
