"""
Edge TTS Synthesizer - generates audio + word-level timing for subtitle sync.
Uses Microsoft Edge TTS (free, high-quality Chinese voices).
All config (voice, rate) comes from the MQ task message.
"""
import asyncio
import logging
from dataclasses import dataclass
from pathlib import Path

import edge_tts

logger = logging.getLogger(__name__)

DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"
DEFAULT_RATE = "+0%"
DEFAULT_VOLUME = "+0%"


@dataclass
class TTSResult:
    audio_path: Path
    duration: float
    word_timings: list[dict]
    subtitle_segments: list[dict]


class TTSSynthesizer:
    """TTS provider using Microsoft Edge TTS (free, no API key needed)."""

    def __init__(self, voice: str = None, rate: str = None):
        self.voice = voice or DEFAULT_VOICE
        self.rate = rate or DEFAULT_RATE

    async def synthesize(self, text: str, output_path: Path) -> TTSResult:
        """Synthesize text to audio file and return timing information."""
        output_path = Path(output_path)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        if not text or not isinstance(text, str):
            raise ValueError("Text must be a non-empty string")
        if len(text) > 10000:
            raise ValueError("Text too long for TTS (max 10000 characters)")

        logger.info("Synthesizing TTS: voice=%s, text_len=%d", self.voice, len(text))

        communicate = edge_tts.Communicate(
            text=text,
            voice=self.voice,
            rate=self.rate,
            volume=DEFAULT_VOLUME,
        )

        word_timings = []
        audio_chunks = []

        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_chunks.append(chunk["data"])
            elif chunk["type"] == "WordBoundary":
                word_timings.append({
                    "word": chunk["text"],
                    "start": chunk["offset"] / 10_000_000,
                    "end": (chunk["offset"] + chunk["duration"]) / 10_000_000,
                })

        if not audio_chunks:
            raise RuntimeError("TTS produced no audio data")

        with open(output_path, "wb") as f:
            for chunk in audio_chunks:
                f.write(chunk)

        duration = word_timings[-1]["end"] if word_timings else 10.0
        subtitle_segments = self._build_subtitle_segments(word_timings)

        logger.info("TTS done: %s (%.1fs)", output_path.name, duration)

        return TTSResult(
            audio_path=output_path,
            duration=duration,
            word_timings=word_timings,
            subtitle_segments=subtitle_segments,
        )

    def synthesize_sync(self, text: str, output_path: Path) -> TTSResult:
        """Synchronous wrapper for synthesize."""
        return asyncio.run(self.synthesize(text, output_path))

    @staticmethod
    def _build_subtitle_segments(word_timings: list[dict]) -> list[dict]:
        """Group words into subtitle display segments (~12 chars each)."""
        if not word_timings:
            return []

        segments = []
        current_words = []
        current_start = word_timings[0]["start"]
        char_count = 0

        for wt in word_timings:
            current_words.append(wt["word"])
            char_count += len(wt["word"])

            if char_count >= 12 or wt == word_timings[-1]:
                segments.append({
                    "text": "".join(current_words),
                    "start": current_start,
                    "end": wt["end"],
                })
                current_words = []
                current_start = wt["end"]
                char_count = 0

        return segments
