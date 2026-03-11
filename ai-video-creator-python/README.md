# AI Video Creator - Python Worker

Lean MQ worker that handles video rendering (TTS + video composition + subtitle burning).

All business logic (REST API, scheduling, storage, config management, statistics) lives in the Java application. This worker only receives video creation tasks via RocketMQ and produces rendered MP4 files.

## Architecture

```
Java App (Spring Boot)
    |
    | video-task-submit (RocketMQ)
    v
Python Worker
    |-- TTS (Edge TTS, free Chinese voices)
    |-- Media fetching (Pexels image/video, AI video generation)
    |-- Video composition (MoviePy)
    |-- Subtitle burning (FFmpeg)
    |
    | video-progress / video-result (RocketMQ)
    v
Java App
```

## Quick Start

```bash
# Install dependencies
pip install -r requirements.txt

# Ensure FFmpeg is installed
ffmpeg -version

# Configure
cp .env.example .env
# Edit .env with your RocketMQ address

# Run
python -m worker
```

## Docker

```bash
docker build -t avc-python-worker .
docker run -d \
  -e ROCKETMQ_NAMESRV=host:9876 \
  -v ./output:/app/output \
  avc-python-worker
```

## Project Structure

```
worker/
  main.py           Entry point: starts MQ consumer + /health
  config.py          Minimal config (MQ address, work dir)
  messages.py        MQ message DTOs (matches Java records)
  consumer.py        RocketMQ PushConsumer
  producer.py        RocketMQ Producer (progress + result)
  task_handler.py    Thread pool dispatcher
  pipeline.py        Orchestrates TTS + composition
  health.py          HTTP /health for container probes
  tts/
    synthesizer.py   Edge TTS with word-level timing
  video/
    composer.py      MoviePy composition + media fetching
    subtitle.py      SRT generation + FFmpeg filter
    ai_providers.py  AI video generation (ZhipuAI, SiliconFlow)
```

## Configuration

The worker requires only infrastructure settings (`.env`):

| Variable | Default | Description |
|----------|---------|-------------|
| ROCKETMQ_NAMESRV | localhost:9876 | RocketMQ name server |
| MQ_CONSUMER_GROUP | avc-python-task-consumer-group | Consumer group |
| MQ_MAX_CONCURRENT_TASKS | 2 | Max parallel video jobs |
| HEALTH_PORT | 8081 | Health check HTTP port |
| LOG_LEVEL | INFO | Logging level |

All video-specific config (API keys, voice, resolution, FPS, subtitle style, BGM) is sent via the MQ task message from Java.

## Requirements

- Python 3.10+
- FFmpeg (for subtitle burning and audio mixing)
- RocketMQ (shared with Java application)
