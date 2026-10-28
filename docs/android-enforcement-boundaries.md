# Android Enforcement Boundaries

This document records design constraints for RP. Capabilities must be verified against supported Android versions and Play policy before implementation.

## Ordinary app permissions

An ordinary app can control its own screens and can request specific user-granted capabilities. It cannot universally inspect other apps' private UI or forcibly prevent its owner from uninstalling it.

## Accessibility services

The current prototype includes an opt-in accessibility service that observes foreground window package transitions only. When a user-selected app becomes foreground while a commitment timer is active, RP opens a gate screen. It sets canRetrieveWindowContent=false and does not read text, images, messages, or video frames. The user must enable the service manually in Android Settings. This can be disabled or bypassed and is not a universal or tamper-proof lock. Distribution must comply with Android accessibility and store policies.

Accessibility may expose selected UI structure only when the service requests and is permitted to retrieve it. Coverage varies by app, secure window, and OS version. RP must not treat it as a universal private-content API.

## VPN and DNS filtering

A local VPN or DNS/domain filter can block network destinations when traffic uses the configured path. Domain filtering cannot reliably classify every image or video, and encrypted traffic, embedded content, alternate networks, and apps using custom networking affect coverage.

## On-device content classification

A local model may classify content only when RP legitimately obtains the image, video frame, or supported signal. RP must not assume it can access arbitrary WhatsApp media or encrypted video-call frames. Model confidence, consent, false positives, battery cost, and device performance need evaluation.

## Device-owner / managed-device provisioning

Device-owner mode can support stronger policy controls on properly provisioned devices, but setup is more involved and typically intended for managed/dedicated devices. It is not a capability an ordinary consumer app can silently grant itself.

## Root

Root may enable deeper device-specific integration but adds security, compatibility, support, and bypass risks. Root does not guarantee a phone cannot be rebooted, reflashed, or otherwise recovered. Any root prototype must be opt-in, tested on a spare device, and must never modify partitions or boot settings without explicit informed consent.

## Safety invariants

- Preserve emergency communication and essential device functions.
- Never hide monitoring or collect private content covertly.
- Explain each permission before requesting it.
- Offer a documented recovery path for bugs and false positives.
- Use bounded, precommitted restrictions with clear expiry.
- Do not describe an in-app countdown as a system-wide lock.
