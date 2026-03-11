"""
Minimal health check HTTP endpoint.
Exposes /health for container orchestration (Docker, K8s).
"""
import json
import logging
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler

logger = logging.getLogger(__name__)


class HealthHandler(BaseHTTPRequestHandler):
    """Simple HTTP handler that responds to /health."""

    def do_GET(self):
        if self.path == "/health":
            body = json.dumps({"status": "UP", "service": "avc-python-worker"})
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(body.encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        # Suppress default HTTP log noise
        pass


def start_health_server(port: int = 8081) -> HTTPServer:
    """Start health check server in a daemon thread."""
    server = HTTPServer(("0.0.0.0", port), HealthHandler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    logger.info("Health server started on port %d", port)
    return server
