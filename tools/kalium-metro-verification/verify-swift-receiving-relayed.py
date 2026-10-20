#!/usr/bin/env python3
"""Run the existing verifier with its local server behind the loopback TCP relay."""
import importlib.util
import pathlib
import sys

root = pathlib.Path(__file__).resolve().parents[2]
script = root / 'kalium/nse/scripts/verify-swift-receiving.py'
sys.path.insert(0, str(script.parent))
spec = importlib.util.spec_from_file_location('kalium_existing_swift_verifier', script)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
original_server = module.ThreadingHTTPServer


class RelayedServer(original_server):
    def __init__(self, address, *args, **kwargs):
        if address == ('127.0.0.1', 443):
            address = ('127.0.0.1', 8443)
        super().__init__(address, *args, **kwargs)


module.ThreadingHTTPServer = RelayedServer
module.main()
