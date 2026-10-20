# Public Swift API smoke

From the owned Kalium checkout, link `:logic:linkDebugFrameworkMacosArm64`. From the Android repository root, run `python3 tools/kalium-metro-verification/verify-swift-api.py`.

The compile-only caller references existing facade/accessor types, new narrow APIs/factories, consumer contracts and asynchronous Apple account preparation. It checks sampled macOS Swift source compatibility against the emitted framework. It does not execute the calls, certify every exported declaration or establish iOS runtime/embedding behavior. A linked client additionally needs the existing AVS framework.

Kotlin generic consumer assembly results export as `Any?`, with all three callbacks explicit. Observation methods return the exported raw Flow protocol through SKIE. Apple callers must await account preparation before synchronous session access. The receiving runtime has separate existing integration checks in `kalium/nse/scripts/verify-swift-receiving.py`.

## Local WSS relay

The existing receiving verifier binds its TLS WebSocket server to loopback port 443. If the current user cannot bind it, a terminal operator can start `sudo /usr/bin/python3 -I tools/kalium-metro-verification/local-wss-relay.py` from this repository. This small relay listens only on 127.0.0.1:443 and forwards opaque TCP bytes to 127.0.0.1:8443. Stop it with Ctrl-C after verification.

Run `python3 tools/kalium-metro-verification/verify-swift-receiving-relayed.py --skip-build` as the regular user. The adapter executes the existing receiving verifier and changes only its literal local server bind from port 443 to 8443. SDK addresses, TLS, receiving assertions and extension-to-app handoff remain the existing workflow. It does not run simulator tooling as root or change system configuration. The default verifier remains unchanged.
