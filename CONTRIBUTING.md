# Contributing

Issues and pull requests are welcome.

- `./gradlew test` must pass. Core logic lives in `glossary-core` (pure JVM); add tests there first.
- Instrumented test (real ML Kit model, needs a device/emulator **with internet**):
  `./gradlew :mlkit-glossary-translate:connectedDebugAndroidTest`
- Adding a language's gender agreement: implement `GenderAgreement` with tests, like `HindiGenderAgreement`.
- Please do not log translated text or glossary contents.
