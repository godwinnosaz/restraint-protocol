# Restraint Protocol (RP)

**A free, privacy-first Android recovery and digital-restraint app.**

Restraint Protocol is being rebuilt as a native Android application focused on helping people who want to reduce or stop compulsive pornography use. RP is designed to interrupt a risky moment with a clear warning, supportive Scripture, and user-configured consequences.

> Current milestone: native Android foundation. The warning and timed-restriction flow is an in-app prototype; it does **not yet block other apps or inspect their content**.

## Product principles

- Free core protection; no mandatory subscription.
- Local-first privacy: do not upload private images, messages, screenshots, or call content.
- Clear, user-configured commitments and consequences.
- Encouraging, non-shaming recovery language.
- Safety first: essential communications and emergency access must not be intentionally disabled.
- Honest capability boundaries: no claim of universal content detection or unbreakable lockdown.

## Technology

- Kotlin
- Android SDK
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL

## Open the project

1. Open this repository in Android Studio.
2. Allow Gradle to sync and install the Android SDK platform requested by the build.
3. Run the `app` configuration on an emulator or test device.

This initial scaffold intentionally does not include a Gradle wrapper binary. Android Studio can import the Gradle project; a wrapper can be generated locally with the installed Gradle distribution.

## Current scope

### Included
- Native Compose application shell.
- Home screen describing the restraint flow.
- In-app warning with Scripture and supportive copy.
- User-selectable demo restriction duration.
- A monotonic, in-app countdown and automatic expiry.
- Basic unit tests for restriction duration policy.

### Not yet implemented
- Cross-app explicit-content detection.
- WhatsApp or video-call inspection.
- Device-owner or root enforcement.
- Explicit-content classification inside other apps.
- Persistent timers across process death/reboot.
- On-device image/video classifier.
- Backend, account system, AI service, or blockchain.

These are intentionally separate milestones because Android's capabilities and permissions must be validated before we promise them.

## Project structure

```text
.
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/brainiactechne/restraintprotocol/
│       │       ├── MainActivity.kt
│       │       └── ui/theme/Color.kt
│       └── test/java/com/brainiactechne/restraintprotocol/
│           └── CommitmentPolicyTest.kt
├── docs/
│   ├── architecture.md
│   └── android-enforcement-boundaries.md
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Build and test

From a machine with a compatible Gradle installation and Android SDK:

```bash
gradle test
gradle assembleDebug
```

To configure the optional server endpoint, use an HTTPS base URL:

```bash
gradle -PrpSyncBaseUrl=https://your-api.example.com test assembleDebug
```

The base URL must be a deployed Restraint Protocol backend. Account sign-in and remote sync remain optional; the app's local commitment flow works offline. Or use Android Studio's Gradle sync, unit-test, and Run actions.

## Privacy and safety

The app does not request screen capture, VPN, device administrator, or root privileges. Its optional Accessibility Service must be enabled manually and observes foreground package transitions only; it does not read screen text, messages, images, or videos. No explicit-content detector is implemented.

Any future monitoring feature must be opt-in, clearly explained, limited to the intended purpose, and evaluated against Android policy and platform restrictions. A detected image alone must not automatically trigger a long lockout because classifiers can produce false positives.

## License

All rights reserved unless a separate license is added by the project owner.
