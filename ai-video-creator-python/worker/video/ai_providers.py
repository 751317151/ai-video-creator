"""
AI Video Providers - generate short video clips via AI generation APIs.

Each provider wraps a submit-then-poll workflow. All API keys are passed as
constructor parameters; nothing is read from global settings or env vars.
"""
from __future__ import annotations

import hashlib
import logging
import time
from abc import ABC, abstractmethod
from pathlib import Path
from typing import Optional

import httpx

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Base class
# ---------------------------------------------------------------------------

_DEFAULT_POLL_INTERVAL = 5.0
_DEFAULT_POLL_TIMEOUT = 300.0
_MAX_SUBMIT_RETRIES = 3
_MAX_DOWNLOAD_RETRIES = 3
_DOWNLOAD_TIMEOUT = 120


class AIVideoProvider(ABC):
    """Base class for AI video generation providers."""

    def __init__(self, api_key: str, provider_id: str) -> None:
        self._api_key = api_key
        self._provider_id = provider_id

    # -- public interface ---------------------------------------------------

    def fetch(
        self,
        keyword: str,
        index: int,
        duration: float,
        clips_dir: Path,
    ) -> Optional[Path]:
        """Generate an AI video clip for *keyword* and save it under *clips_dir*.

        Returns the local file path on success, or ``None`` on any failure.
        """
        cache_key = f"ai_{self._provider_id}_{keyword}_{index}"
        safe_name = hashlib.md5(cache_key.encode()).hexdigest()[:12]
        output_path = clips_dir / f"{safe_name}.mp4"
        if output_path.exists():
            return output_path

        prompt = self._build_prompt(keyword, duration)

        # Submit with retries + exponential back-off
        job_id = self._submit_with_retries(prompt, duration)
        if job_id is None:
            return None

        # Poll until complete
        video_url = self._poll_with_timeout(job_id)
        if video_url is None:
            return None

        # Download
        if not self._download(video_url, output_path):
            return None

        logger.info(
            "[%s] AI video saved: %s (keyword=%r)",
            self._provider_id, output_path.name, keyword,
        )
        return output_path

    # -- abstract methods ---------------------------------------------------

    @abstractmethod
    def _submit_job(self, prompt: str, duration: float) -> str:
        """Submit a generation job. Returns the job / task id.

        Raises on transient or permanent failure so the retry wrapper can
        decide whether to retry.
        """

    @abstractmethod
    def _poll_until_complete(self, job_id: str, timeout: float) -> str:
        """Block until the job finishes and return the result video URL.

        Raises on timeout or permanent failure.
        """

    # -- helpers (overridable) ----------------------------------------------

    def _build_prompt(self, keyword: str, duration: float) -> str:
        return (
            f"Generate a short, cinematic video clip about: {keyword}. "
            f"Duration approximately {duration:.1f} seconds. "
            "High quality, smooth motion, visually appealing."
        )

    # -- internal retry / download logic ------------------------------------

    def _submit_with_retries(self, prompt: str, duration: float) -> Optional[str]:
        delay = 2.0
        for attempt in range(1, _MAX_SUBMIT_RETRIES + 1):
            try:
                return self._submit_job(prompt, duration)
            except Exception as exc:
                logger.warning(
                    "[%s] submit attempt %d/%d failed: %s",
                    self._provider_id, attempt, _MAX_SUBMIT_RETRIES, exc,
                )
                if attempt < _MAX_SUBMIT_RETRIES:
                    time.sleep(delay)
                    delay *= 2
        logger.error("[%s] all submit attempts exhausted", self._provider_id)
        return None

    def _poll_with_timeout(self, job_id: str) -> Optional[str]:
        try:
            return self._poll_until_complete(job_id, _DEFAULT_POLL_TIMEOUT)
        except Exception as exc:
            logger.error(
                "[%s] polling failed for job %s: %s",
                self._provider_id, job_id, exc,
            )
            return None

    def _download(self, url: str, output_path: Path) -> bool:
        delay = 2.0
        for attempt in range(1, _MAX_DOWNLOAD_RETRIES + 1):
            try:
                with httpx.Client(
                    timeout=_DOWNLOAD_TIMEOUT, follow_redirects=True,
                ) as client:
                    with client.stream("GET", url) as resp:
                        resp.raise_for_status()
                        with open(output_path, "wb") as fh:
                            for chunk in resp.iter_bytes(chunk_size=8192):
                                fh.write(chunk)
                return True
            except Exception as exc:
                logger.warning(
                    "[%s] download attempt %d/%d failed: %s",
                    self._provider_id, attempt, _MAX_DOWNLOAD_RETRIES, exc,
                )
                if output_path.exists():
                    output_path.unlink(missing_ok=True)
                if attempt < _MAX_DOWNLOAD_RETRIES:
                    time.sleep(delay)
                    delay *= 2
        logger.error("[%s] all download attempts exhausted", self._provider_id)
        return False


# ---------------------------------------------------------------------------
# ZhipuAI  (CogVideoX)
# ---------------------------------------------------------------------------

class ZhipuAIProvider(AIVideoProvider):
    """ZhipuAI CogVideoX-Flash video generation."""

    _BASE_URL = "https://open.bigmodel.cn/api/paas/v4"

    def __init__(self, api_key: str) -> None:
        super().__init__(api_key, provider_id="zhipuai")

    def _submit_job(self, prompt: str, duration: float) -> str:
        url = f"{self._BASE_URL}/videos/generations"
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": "cogvideox-flash",
            "prompt": prompt,
        }
        with httpx.Client(timeout=30) as client:
            resp = client.post(url, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()

        task_id = data.get("id") or data.get("task_id")
        if not task_id:
            raise ValueError(f"ZhipuAI response missing task id: {data}")
        logger.info("[zhipuai] submitted job %s", task_id)
        return str(task_id)

    def _poll_until_complete(self, job_id: str, timeout: float) -> str:
        url = f"{self._BASE_URL}/videos/retrieve"
        headers = {"Authorization": f"Bearer {self._api_key}"}
        deadline = time.monotonic() + timeout

        while time.monotonic() < deadline:
            with httpx.Client(timeout=30) as client:
                resp = client.get(url, headers=headers, params={"id": job_id})
                resp.raise_for_status()
                data = resp.json()

            status = data.get("task_status", "")
            if status == "SUCCESS":
                results = data.get("video_result", [])
                if results and results[0].get("url"):
                    return results[0]["url"]
                raise ValueError(f"ZhipuAI SUCCESS but no video URL: {data}")
            if status in ("FAIL", "FAILED"):
                raise RuntimeError(f"ZhipuAI job {job_id} failed: {data}")

            logger.debug("[zhipuai] job %s status=%s, waiting...", job_id, status)
            time.sleep(_DEFAULT_POLL_INTERVAL)

        raise TimeoutError(f"ZhipuAI job {job_id} timed out after {timeout}s")


# ---------------------------------------------------------------------------
# SiliconFlow
# ---------------------------------------------------------------------------

class SiliconFlowProvider(AIVideoProvider):
    """SiliconFlow Wan2.1-T2V video generation."""

    _BASE_URL = "https://api.siliconflow.cn/v1"

    def __init__(self, api_key: str) -> None:
        super().__init__(api_key, provider_id="siliconflow")

    def _submit_job(self, prompt: str, duration: float) -> str:
        url = f"{self._BASE_URL}/videos/submit"
        headers = {
            "Authorization": f"Bearer {self._api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": "Pro/Wan-AI/Wan2.1-T2V-14B",
            "prompt": prompt,
        }
        with httpx.Client(timeout=30) as client:
            resp = client.post(url, headers=headers, json=payload)
            resp.raise_for_status()
            data = resp.json()

        request_id = data.get("requestId") or data.get("request_id")
        if not request_id:
            raise ValueError(f"SiliconFlow response missing request id: {data}")
        logger.info("[siliconflow] submitted job %s", request_id)
        return str(request_id)

    def _poll_until_complete(self, job_id: str, timeout: float) -> str:
        url = f"{self._BASE_URL}/videos/status/{job_id}"
        headers = {"Authorization": f"Bearer {self._api_key}"}
        deadline = time.monotonic() + timeout

        while time.monotonic() < deadline:
            with httpx.Client(timeout=30) as client:
                resp = client.get(url, headers=headers)
                resp.raise_for_status()
                data = resp.json()

            status = data.get("status", "")
            if status == "Succeed":
                results = data.get("results", {})
                videos = results.get("videos", [])
                if videos and videos[0].get("url"):
                    return videos[0]["url"]
                raise ValueError(
                    f"SiliconFlow Succeed but no video URL: {data}"
                )
            if status in ("Failed", "Cancelled"):
                raise RuntimeError(
                    f"SiliconFlow job {job_id} {status}: {data}"
                )

            logger.debug(
                "[siliconflow] job %s status=%s, waiting...", job_id, status,
            )
            time.sleep(_DEFAULT_POLL_INTERVAL)

        raise TimeoutError(
            f"SiliconFlow job {job_id} timed out after {timeout}s"
        )


# ---------------------------------------------------------------------------
# Factory
# ---------------------------------------------------------------------------

_PROVIDER_MAP: dict[str, type[AIVideoProvider]] = {
    "zhipuai": ZhipuAIProvider,
    "siliconflow": SiliconFlowProvider,
}


def create_ai_provider(
    provider_name: str,
    api_key: str,
) -> Optional[AIVideoProvider]:
    """Create an AI video provider by name.

    Returns ``None`` if the provider name is unknown or the API key is empty.
    """
    if not provider_name or not api_key:
        logger.warning(
            "Cannot create AI provider: provider_name=%r, api_key=%s",
            provider_name,
            "provided" if api_key else "missing",
        )
        return None

    cls = _PROVIDER_MAP.get(provider_name.lower())
    if cls is None:
        logger.warning(
            "Unknown AI video provider: %r (available: %s)",
            provider_name,
            ", ".join(sorted(_PROVIDER_MAP)),
        )
        return None

    return cls(api_key)
