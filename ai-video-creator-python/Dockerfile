FROM python:3.11-slim

# Install FFmpeg (required for subtitle burning and video processing)
RUN apt-get update && \
    apt-get install -y --no-install-recommends ffmpeg && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy worker code
COPY worker/ worker/

# Create output directories
RUN mkdir -p output/videos output/audio output/images output/subtitles output/clips

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8081/health')" || exit 1

EXPOSE 8081

ENTRYPOINT ["python", "-m", "worker"]
