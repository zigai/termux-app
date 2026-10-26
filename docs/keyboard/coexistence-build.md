# Coexistence development build

The `coexistence` build type installs beside the standard Termux app for on-device keyboard testing. It uses:

- application ID and shared user ID `com.termux.keyboard`;
- app label `Termux Keyboard`;
- private prefix `/data/data/com.termux.keyboard/files/usr`;
- the existing debug signing key; and
- an aarch64 bootstrap compiled for that private prefix.

The normal `debug` and `release` build types remain `com.termux`.

## Bootstrap

Official Termux packages are compiled for `/data/data/com.termux/files/usr` and must not be mixed with this build. The currently verified bootstrap source is official `termux/termux-packages` commit `4f2a0806bb180239783c8b920f8424cd4390395d`. Build an aarch64 bootstrap from those sources after setting this value in `scripts/properties.sh`:

```bash
TERMUX_APP__PACKAGE_NAME="com.termux.keyboard"
```

Then run:

```bash
./scripts/run-docker.sh ./scripts/build-bootstraps.sh --architectures aarch64
```

Copy the resulting archive into this checkout:

```bash
cp bootstrap-aarch64.zip /path/to/termux-app/app/src/main/cpp/bootstrap-coexistence-aarch64.zip
```

Bootstrap archives are generated, ignored by Git, and must not be committed with source changes.

This development build has no compatible remote package repository. Do not install or upgrade packages from the official Termux repositories inside it. Use the separately installed standard Termux app for a normal package-managed environment.

## Build and install

```bash
./gradlew :app:testCoexistenceUnitTest :app:assembleCoexistence
adb install app/build/outputs/apk/coexistence/termux-keyboard_apt-android-7-coexistence_universal.apk
```

Verify both packages remain installed:

```bash
adb shell pm list packages | grep -E '^package:com\.termux(\.keyboard)?$'
```
