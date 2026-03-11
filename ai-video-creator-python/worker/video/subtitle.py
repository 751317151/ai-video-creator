"""
Subtitle Renderer - generates SRT files and provides FFmpeg filter for burning subtitles.
"""
import logging
from pathlib import Path
from datetime import timedelta

import pysrt

logger = logging.getLogger(__name__)


class SubtitleRenderer:

    def __init__(self, font_size: int = 48, color: str = "white"):
        self.font_size = font_size
        self.color = color

    def generate_srt(self, segments: list[dict], output_path: Path) -> Path:
        """
        Generate an SRT subtitle file from timed segments.
        segments: [{"text": "...", "start": 0.5, "end": 2.3}]
        """
        subs = pysrt.SubRipFile()

        for i, seg in enumerate(segments, start=1):
            item = pysrt.SubRipItem(
                index=i,
                start=self._seconds_to_srt_time(seg["start"]),
                end=self._seconds_to_srt_time(seg["end"]),
                text=seg["text"],
            )
            subs.append(item)

        subs.save(str(output_path), encoding="utf-8")
        logger.info("SRT saved: %s (%d entries)", output_path.name, len(subs))
        return output_path

    @staticmethod
    def _seconds_to_srt_time(seconds: float) -> pysrt.SubRipTime:
        td = timedelta(seconds=seconds)
        total_ms = int(td.total_seconds() * 1000)
        h = total_ms // 3_600_000
        m = (total_ms % 3_600_000) // 60_000
        s = (total_ms % 60_000) // 1000
        ms = total_ms % 1000
        return pysrt.SubRipTime(h, m, s, ms)

    def get_ffmpeg_subtitle_filter(self, srt_path: Path) -> str:
        """
        Return an FFmpeg subtitles filter string with styled Chinese subtitles.
        """
        srt_escaped = str(srt_path).replace("\\", "/").replace(":", "\\:")

        return (
            f"subtitles='{srt_escaped}':"
            f"force_style='FontName=Arial,FontSize={self.font_size},"
            f"PrimaryColour=&H00FFFFFF,OutlineColour=&H80000000,"
            f"BackColour=&H80000000,BorderStyle=3,Outline=2,"
            f"Shadow=1,Alignment=2,MarginV=80'"
        )
