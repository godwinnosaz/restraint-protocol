# RP Architecture — Initial Native Foundation

## Direction

RP is being rebuilt as a native Android application. Kotlin and Jetpack Compose are used for the application and UI. The first milestone demonstrates the intervention flow entirely inside the app; it does not yet monitor or restrict other apps.

## Layers

1. **Presentation** — Compose screens for home, warning, and timed pause.
2. **Commitment policy** — pure Kotlin duration validation and countdown arithmetic.
3. **Future detection adapters** — isolated adapters for supported browser/domain signals and optional on-device classification. No adapter is implemented in this milestone.
4. **Future enforcement adapters** — platform-specific restriction strategies, each capability-gated and tested. No cross-app enforcement is implemented in this milestone.
5. **Future persistence** — local storage for user commitments and deadlines. The current prototype stores its state only in Compose memory.

## Principles

- Do not couple UI code directly to privileged Android operations.
- Model detection confidence and false-positive handling explicitly.
- Store the minimum sensitive data necessary; avoid retaining content itself.
- Keep policy decisions deterministic and testable.
- Enforcement timers should eventually use persisted wall-clock deadlines and be revalidated after process death and reboot.
- A long restriction must never disable emergency calls or essential safety functions.
- A detected image or inferred intent alone is not sufficient for irreversible or prolonged action.

## Milestone sequence

1. Native app foundation and in-app intervention prototype.
2. Persist commitments and timer state; add robust lifecycle tests.
3. Research and implement supported domain filtering and user-selected app restrictions.
4. Evaluate local explicit-content classification on test datasets, with false-positive and privacy measurements.
5. Prototype stronger enforcement on a dedicated test device; assess device-owner and root approaches separately.
6. Conduct security, privacy, accessibility, and usability testing before release.
