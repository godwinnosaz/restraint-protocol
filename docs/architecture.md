# RP Architecture — Native Android and offline-first foundation

## Direction

RP is a native Android application using Kotlin and Jetpack Compose. Core data is stored locally in Room/SQLite. WorkManager sends explicitly queued metadata when Android reports a connected network and an HTTPS endpoint is configured.

## Layers

1. Presentation — Compose screens for home, warning, and timed pause.
2. Local data — Room database with commitment state and a durable outbox.
3. Policy — pure Kotlin duration validation and countdown arithmetic.
4. Sync — WorkManager with connected-network constraints, batched idempotent events, and server acknowledgements.
5. Future detection adapters — supported browser/domain signals and optional on-device classification. No content detector is implemented yet.
6. Future enforcement adapters — platform-specific restriction strategies. No cross-app enforcement is implemented yet.

## Privacy and safety

- No images, screenshots, messages, page contents, or video-call contents are collected by the sync worker.
- No endpoint or server credential is hardcoded in the app.
- The outbox remains local until an endpoint is configured and records are acknowledged.
- Timed restrictions must preserve emergency communication and essential device functions.
- A detected image or inferred intent alone is not sufficient for irreversible or prolonged action.

## Milestone sequence

1. Native app and in-app intervention prototype.
2. Offline-first Room data and network-constrained sync client.
3. Add a real authenticated HTTPS backend and integration tests.
4. Implement supported app/domain restriction mechanisms and recovery flows.
5. Evaluate local explicit-content classification with false-positive, privacy, battery, and performance measurements.
6. Test device-owner and optional root approaches separately before release.
