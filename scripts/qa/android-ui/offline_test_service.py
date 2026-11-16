#!/usr/bin/env python3
"""Keep TC-12261's file-update request reachable while the phone is offline.

The test disables Wi-Fi and mobile data before TeamOwner replaces the online PDF.
ADB reverse connects the phone's loopback port to this relay on the CI runner,
which forwards the request to the configured test service.

This connection does not give Wire general internet access. run_ui_tests.sh
creates the forwarding rule and stops the relay when the attempt finishes.
"""

import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
import sys
from urllib.parse import urlsplit

from resolve_test_service_url import validate_url


def serve(service_url: str, port_file: str) -> None:
    # Starts a loopback relay and writes its runner-side port for adb reverse.
    upstream = urlsplit(validate_url(service_url))

    class FileUpdateHandler(BaseHTTPRequestHandler):
        def do_POST(self) -> None:
            # Forwards the file update and returns the service's status and response body.
            connection_type = http.client.HTTPSConnection if upstream.scheme == "https" else http.client.HTTPConnection
            connection = connection_type(upstream.hostname, upstream.port, timeout=120)
            try:
                body = self.rfile.read(int(self.headers.get("Content-Length", "0")))
                connection.request("POST", self.path, body, {"Content-Type": "application/json"})
                response = connection.getresponse()
                data = response.read()
                self.send_response(response.status)
                self.send_header("Content-Type", response.getheader("Content-Type", "application/json"))
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            except (OSError, http.client.HTTPException):
                self.send_error(502, "Test service connection failed")
            finally:
                connection.close()

    with ThreadingHTTPServer(("127.0.0.1", 0), FileUpdateHandler) as server:
        Path(port_file).write_text(str(server.server_address[1]), encoding="utf-8")
        server.serve_forever()


if __name__ == "__main__":
    serve(sys.argv[1], sys.argv[2])
