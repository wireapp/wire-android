#!/usr/bin/env python3
"""Loopback-only TCP relay for the existing Swift fixture's privileged WSS port."""
import argparse
import select
import socket
import socketserver

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--listen-port', type=int, default=443)
parser.add_argument('--upstream-port', type=int, default=8443)
options = parser.parse_args()
if not (0 <= options.listen_port <= 65535 and 1 <= options.upstream_port <= 65535):
    parser.error('Invalid port')


class Relay(socketserver.BaseRequestHandler):
    def handle(self):
        try:
            with socket.create_connection(('127.0.0.1', options.upstream_port), timeout=5) as upstream:
                upstream.settimeout(None)
                peers = {self.request: upstream, upstream: self.request}
                while True:
                    ready, _, _ = select.select(list(peers), [], [], 120)
                    if not ready:
                        return
                    for source in ready:
                        data = source.recv(65536)
                        if not data:
                            return
                        peers[source].sendall(data)
        except OSError:
            return


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


with Server(('127.0.0.1', options.listen_port), Relay) as server:
    print('Loopback TCP relay 127.0.0.1:%d -> 127.0.0.1:%d; Ctrl-C to stop.' %
          (server.server_address[1], options.upstream_port), flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
