"""
Video Composer - assembles media clips + audio + subtitles into final MP4.
Uses MoviePy for composition and FFmpeg for subtitle burning.
All config (dimensions, fps, API keys) comes from the MQ task message.
"""
import hashlib
import io
import logging
import shutil
import subprocess
import uuid
from dataclasses import dataclass
from enum import Enum
from pathlib import Path
from typing import Optional

import httpx
from PIL import Image, ImageDraw, ImageFilter

# MoviePy compatibility: fix Pillow 10+ removed ANTIALIAS
if not hasattr(Image, "ANTIALIAS"):
    Image.ANTIALIAS = Image.Resampling.LANCZOS

from moviepy.editor import (
    ImageClip, VideoFileClip, AudioFileClip, CompositeVideoClip,
    concatenate_videoclips, ColorClip,
)

from worker.tts.synthesizer import TTSResult
from worker.video.subtitle import SubtitleRenderer

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Data types
# ---------------------------------------------------------------------------

class MediaType(Enum):
    IMAGE = "image"
    VIDEO = "video"


@dataclass(frozen=True)
class MediaResult:
    path: Path
    media_type: MediaType
    duration: Optional[float] = None
    source: str = ""


@dataclass
class VideoResult:
    video_path: Path
    title: str
    duration: float
    thumbnail_path: Path


@dataclass
class RenderConfig:
    """All rendering parameters, populated from the MQ task message."""
    width: int = 1080
    height: int = 1920
    fps: int = 30
    subtitle_font_size: int = 48
    subtitle_color: str = "white"
    pexels_api_key: Optional[str] = None
    ai_video_provider: Optional[str] = None
    ai_video_api_key: Optional[str] = None
    video_source: str = "pexels_video"


# ---------------------------------------------------------------------------
# Gradient fallback
# ---------------------------------------------------------------------------

FALLBACK_GRADIENTS = [
    [(15, 15, 40), (40, 10, 80)],
    [(5, 30, 60), (10, 80, 120)],
    [(40, 10, 10), (100, 30, 20)],
    [(10, 40, 20), (20, 90, 50)],
    [(30, 20, 50), (80, 40, 100)],
]


def _generate_gradient(width: int, height: int, index: int, output_path: Path) -> Path:
    colors = FALLBACK_GRADIENTS[index % len(FALLBACK_GRADIENTS)]
    img = Image.new("RGB", (width, height))
    draw = ImageDraw.Draw(img)
    for y in range(height):
        t = y / height
        r = int(colors[0][0] + (colors[1][0] - colors[0][0]) * t)
        g = int(colors[0][1] + (colors[1][1] - colors[0][1]) * t)
        b = int(colors[0][2] + (colors[1][2] - colors[0][2]) * t)
        draw.line([(0, y), (width, y)], fill=(r, g, b))
    img = img.filter(ImageFilter.GaussianBlur(radius=2))
    img.save(output_path, "JPEG", quality=90)
    return output_path


def _crop_and_resize(img: Image.Image, width: int, height: int) -> Image.Image:
    target_ratio = width / height
    src_ratio = img.width / img.height
    if src_ratio > target_ratio:
        new_w = int(img.height * target_ratio)
        left = (img.width - new_w) // 2
        img = img.crop((left, 0, left + new_w, img.height))
    else:
        new_h = int(img.width / target_ratio)
        top = (img.height - new_h) // 2
        img = img.crop((0, top, img.width, top + new_h))
    return img.resize((width, height), Image.LANCZOS)


# ---------------------------------------------------------------------------
# Image/Video fetchers (accept pexels key as param, no global settings)
# ---------------------------------------------------------------------------

def fetch_pexels_image(
    keyword: str, index: int, pexels_api_key: str,
    width: int, height: int, images_dir: Path,
) -> Optional[Path]:
    """Fetch a background image from Pexels. Returns None on failure."""
    cache_key = f"{keyword}_{index}"
    safe_name = hashlib.md5(cache_key.encode()).hexdigest()[:12]
    output_path = images_dir / f"{safe_name}.jpg"
    if output_path.exists():
        return output_path

    if not pexels_api_key:
        return None

    try:
        headers = {"Authorization": pexels_api_key}
        url = f"https://api.pexels.com/v1/search?query={keyword}&per_page=5&orientation=portrait"
        with httpx.Client(timeout=15) as client:
            resp = client.get(url, headers=headers)
            resp.raise_for_status()
            data = resp.json()

        photos = data.get("photos", [])
        if not photos:
            return None

        photo = photos[index % len(photos)]
        img_url = photo["src"]["portrait"]
        img_resp = httpx.get(img_url, timeout=30)
        img_resp.raise_for_status()

        img = Image.open(io.BytesIO(img_resp.content)).convert("RGB")
        img = _crop_and_resize(img, width, height)
        # Dark overlay for subtitle readability
        overlay = Image.new("RGBA", img.size, (0, 0, 0, 80))
        img = Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")
        img.save(output_path, "JPEG", quality=90)
        return output_path
    except Exception as e:
        logger.warning("Pexels image fetch failed for '%s': %s", keyword, e)
        return None


def fetch_pexels_video(
    keyword: str, index: int, pexels_api_key: str,
    width: int, height: int, clips_dir: Path,
) -> Optional[Path]:
    """Fetch a short video clip from Pexels Video API. Returns None on failure."""
    cache_key = f"vid_{keyword}_{index}"
    safe_name = hashlib.md5(cache_key.encode()).hexdigest()[:12]
    output_path = clips_dir / f"{safe_name}.mp4"
    if output_path.exists():
        return output_path

    if not pexels_api_key:
        return None

    try:
        headers = {"Authorization": pexels_api_key}
        params = {"query": keyword, "per_page": 5, "orientation": "portrait"}
        with httpx.Client(timeout=15) as client:
            resp = client.get(
                "https://api.pexels.com/videos/search",
                headers=headers, params=params,
            )
            resp.raise_for_status()
            data = resp.json()

        videos = data.get("videos", [])
        if not videos:
            return None

        video = videos[index % len(videos)]
        video_files = video.get("video_files", [])
        if not video_files:
            return None

        # Select best quality MP4
        mp4s = [f for f in video_files if f.get("file_type") == "video/mp4"] or video_files
        mp4s.sort(key=lambda f: abs(f.get("width", 0) - width) + abs(f.get("height", 0) - height))
        video_url = mp4s[0]["link"]

        with httpx.Client(timeout=60, follow_redirects=True) as client:
            with client.stream("GET", video_url) as resp:
                resp.raise_for_status()
                with open(output_path, "wb") as f:
                    for chunk in resp.iter_bytes(chunk_size=8192):
                        f.write(chunk)
        return output_path
    except Exception as e:
        logger.warning("Pexels video fetch failed for '%s': %s", keyword, e)
        return None


# ---------------------------------------------------------------------------
# Composer
# ---------------------------------------------------------------------------

class VideoComposer:
    def __init__(self, cfg: RenderConfig, output_dir: Path):
        self.cfg = cfg
        self.output_dir = output_dir
        self.videos_dir = output_dir / "videos"
        self.images_dir = output_dir / "images"
        self.clips_dir = output_dir / "clips"
        self.subtitles_dir = output_dir / "subtitles"
        self.subtitle_renderer = SubtitleRenderer(
            font_size=cfg.subtitle_font_size,
            color=cfg.subtitle_color,
        )

        for d in [self.videos_dir, self.images_dir, self.clips_dir, self.subtitles_dir]:
            d.mkdir(parents=True, exist_ok=True)

    def compose(
        self, script_segments: list[dict], title: str,
        tts_result: TTSResult, output_name: str = None,
    ) -> VideoResult:
        """Full pipeline: media clips + audio + subtitles -> MP4."""
        output_name = output_name or str(uuid.uuid4())[:8]
        temp_dir = self.output_dir / f"temp_{output_name}"
        temp_dir.mkdir(exist_ok=True)

        try:
            # Step 1: Build media clips per segment
            clips = self._build_media_clips(script_segments, tts_result)

            # Step 2: Concatenate and add audio
            video = concatenate_videoclips(clips, method="compose")
            audio = AudioFileClip(str(tts_result.audio_path))

            if video.duration < audio.duration:
                pad = ColorClip(
                    size=(self.cfg.width, self.cfg.height),
                    color=(0, 0, 0),
                    duration=audio.duration - video.duration,
                )
                video = concatenate_videoclips([video, pad])
            else:
                video = video.subclip(0, audio.duration)

            video = video.set_audio(audio)

            # Step 3: Write intermediate (no subtitles)
            temp_video = temp_dir / "no_subs.mp4"
            temp_audiofile = str(temp_dir / "TEMP_audio.mp4")
            try:
                video.write_videofile(
                    str(temp_video),
                    fps=self.cfg.fps,
                    codec="libx264",
                    audio_codec="aac",
                    temp_audiofile=temp_audiofile,
                    verbose=False,
                    logger=None,
                )
            finally:
                video.close()
                audio.close()
                for c in clips:
                    c.close()

            # Step 4: Generate SRT
            srt_path = self.subtitles_dir / f"{output_name}.srt"
            self.subtitle_renderer.generate_srt(tts_result.subtitle_segments, srt_path)

            # Step 5: Burn subtitles with FFmpeg
            final_path = self.videos_dir / f"{output_name}.mp4"
            self._burn_subtitles(temp_video, srt_path, final_path)

            # Step 6: Extract thumbnail
            thumbnail = self._extract_thumbnail(final_path, output_name)

            # Clean up temp
            shutil.rmtree(temp_dir, ignore_errors=True)

            logger.info("Video created: %s", final_path.name)

            return VideoResult(
                video_path=final_path,
                title=title,
                duration=tts_result.duration,
                thumbnail_path=thumbnail,
            )

        except Exception:
            shutil.rmtree(temp_dir, ignore_errors=True)
            raise

    def _build_media_clips(self, segments: list[dict], tts_result: TTSResult) -> list:
        """Build one clip per script segment using configured media source."""
        clips = []
        total_duration = tts_result.duration
        n_segments = len(segments)
        seg_duration = total_duration / max(n_segments, 1)

        for i, segment in enumerate(segments):
            duration = segment.get("duration", seg_duration)
            keyword = segment.get("image_keyword", "abstract background")
            clip = None

            # Try configured video source
            if self.cfg.video_source == "ai_video":
                path = self._fetch_ai_video(keyword, i, duration)
                if path:
                    clip = self._make_video_clip(path, duration)
            elif self.cfg.video_source == "pexels_video":
                path = fetch_pexels_video(
                    keyword, i, self.cfg.pexels_api_key or "",
                    self.cfg.width, self.cfg.height, self.clips_dir,
                )
                if path:
                    clip = self._make_video_clip(path, duration)
            elif self.cfg.video_source == "image":
                path = fetch_pexels_image(
                    keyword, i, self.cfg.pexels_api_key or "",
                    self.cfg.width, self.cfg.height, self.images_dir,
                )
                if path:
                    clip = self._make_image_clip(path, duration)

            # Fallback: try pexels video if AI video failed
            if clip is None and self.cfg.video_source == "ai_video":
                path = fetch_pexels_video(
                    keyword, i, self.cfg.pexels_api_key or "",
                    self.cfg.width, self.cfg.height, self.clips_dir,
                )
                if path:
                    clip = self._make_video_clip(path, duration)

            # Fallback: try image if video failed
            if clip is None and self.cfg.video_source != "image":
                path = fetch_pexels_image(
                    keyword, i, self.cfg.pexels_api_key or "",
                    self.cfg.width, self.cfg.height, self.images_dir,
                )
                if path:
                    clip = self._make_image_clip(path, duration)

            # Last resort: gradient
            if clip is None:
                grad_path = self.images_dir / f"gradient_{i}.jpg"
                _generate_gradient(self.cfg.width, self.cfg.height, i, grad_path)
                clip = self._make_image_clip(grad_path, duration)

            clips.append(clip)

        return clips

    def _make_image_clip(self, path: Path, duration: float) -> ImageClip:
        clip = (
            ImageClip(str(path))
            .set_duration(duration)
            .resize((self.cfg.width, self.cfg.height))
        )
        # Ken Burns zoom effect
        intensity = 0.02
        clip = clip.resize(lambda t: 1 + intensity * (t / duration))
        return clip

    def _make_video_clip(self, path: Path, duration: float):
        clip = VideoFileClip(str(path))
        if clip.duration >= duration:
            clip = clip.subclip(0, duration)
        else:
            n_loops = int(duration / clip.duration) + 1
            clip = concatenate_videoclips([clip] * n_loops).subclip(0, duration)
        clip = clip.resize((self.cfg.width, self.cfg.height))
        clip = clip.without_audio()
        return clip

    def _fetch_ai_video(self, keyword: str, index: int, duration: float) -> Optional[Path]:
        """Fetch a video clip from an AI video generation provider."""
        if not self.cfg.ai_video_provider or not self.cfg.ai_video_api_key:
            logger.warning("AI video source selected but no provider/key configured")
            return None

        try:
            from worker.video.ai_providers import create_ai_provider
            provider = create_ai_provider(self.cfg.ai_video_provider, self.cfg.ai_video_api_key)
            if provider is None:
                return None
            return provider.fetch(keyword, index, duration, self.clips_dir)
        except Exception as e:
            logger.warning("AI video fetch failed for '%s': %s", keyword, e)
            return None

    def _burn_subtitles(self, input_path: Path, srt_path: Path, output_path: Path):
        sub_filter = self.subtitle_renderer.get_ffmpeg_subtitle_filter(srt_path)
        cmd = [
            "ffmpeg", "-y",
            "-i", str(input_path),
            "-vf", sub_filter,
            "-c:v", "libx264",
            "-c:a", "copy",
            "-preset", "fast",
            "-crf", "23",
            str(output_path),
        ]
        result = subprocess.run(cmd, capture_output=True, text=True)
        if result.returncode != 0:
            logger.warning("FFmpeg subtitle burn failed, copying without subtitles: %s", result.stderr[-200:])
            shutil.copy(input_path, output_path)

    def _extract_thumbnail(self, video_path: Path, name: str) -> Path:
        thumb_path = self.images_dir / f"{name}_thumb.jpg"
        cmd = [
            "ffmpeg", "-y",
            "-i", str(video_path),
            "-vframes", "1",
            "-q:v", "2",
            str(thumb_path),
        ]
        subprocess.run(cmd, capture_output=True)
        return thumb_path
