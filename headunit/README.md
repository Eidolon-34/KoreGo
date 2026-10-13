# Persephone RF — Android head-unit edition

A small native Android app for local RF discovery and consent-based training. It lives beside the existing OUI-SPY Flutter companion and uses a separate package (`com.heathen.persephone`). The original companion/firmware are not dependencies of this APK.

## What works in this build

- Android Wi-Fi **result listening** and BLE advertisement discovery without an ESP32.
- Optional **passive ESP32-S3 over direct USB CDC-ACM**, with bundled receive-only firmware. This adds received Wi-Fi beacons and client probe-request metadata, including devices that do not expose an AP.
- Landscape dashboard, large controls, Wi-Fi/BLE/watchlist filters, evidence detail, smoothed RSSI trace, and stale-signal indicators.
- Local JSONL recordings, complete CSV exports, offline session replay, and a clearly labeled synthetic demo mode.
- Persistent Android folder selector: choose a writable SD-card folder, USB-storage folder, or another document provider. Completed sessions automatically copy there as JSONL and CSV. Private recovery copies remain if the card is missing/full.
- Receiver GPS coordinates only when a fix has accuracy and is at most ten seconds old. No invented transmitter coordinates or RSSI-to-distance estimates.
- Pwnagotchi-inspired companion expressions and observation epochs, without handshake collection, deauthentication, or password attacks.
- Passcode approval before starting discovery, selecting storage, exporting/deleting/replaying data, changing settings, requesting Wi-Fi scans, and performing a connected training action.
- A single connected training action: **GATT service UUID / characteristic-property inventory** of an explicitly approved, freshly observed BLE target. No characteristic reads or writes. Wi-Fi capability review is offline.

This is a compiled training preview, not a claim of radio-specific hardware qualification. Hardware scanning, BLE connection behavior, USB enumeration, SD mounting, and automotive power interruption still need the acceptance checks below on the actual radio. The Amazon product page could not be verified; the app's Diagnostics screen reports the installed Android version, resolution, permissions, and exposed features.

## Install and first run

Minimum Android: **8.0 / API 26**. Sideload the debug APK through the radio's file manager, or use:

```sh
adb install -r persephone-rf-debug.apk
```

1. Open **Persephone RF**. Initial passcode: **9818**.
2. Settings → Change passcode → approve with `9818` → enter a personal 6–12 digit passcode twice. Connected inventory and explicit Wi-Fi scan requests are blocked while the initial passcode is still in use.
3. Enable Wi-Fi, Bluetooth, and Android Location. Grant precise location and, on Android 12+, nearby Bluetooth permissions.
4. Sessions → Choose save folder / SD card → select a writable **folder** on the card. Android 11+ can prohibit selecting storage roots; choose a subfolder such as `Persephone`.
5. Overview → Start discovery → approve. If permissions are requested, return and press Start again to approve the actual session. After a permission denial, the next approved session can run with the remaining capabilities. Settings has permission setup and diagnostics controls.
6. Stop & save finalizes the private recording and copies it to the selected folder. Leaving the app also stops/finalizes recording. Selecting a folder does not resume scanning.

The debug signing key varies by build machine. An update built with a different key will not install over the existing APK. Export recordings before replacing an installation; production signing is intentionally not configured here.

## Discovery and detection limits

**Native Android discovery is not guaranteed RF-silent.** By default this app does not request Wi-Fi scans: it consumes fresh system results/broadcasts. The OS or other apps may scan, and Android BLE scanning may send standard scan requests. The explicitly approved Wi-Fi button may transmit ordinary probe requests throughout the scan area. Android normally limits foreground requests to four scans per two minutes. Cached Wi-Fi results older than 30 seconds are discarded, and repeated platform result timestamps are not logged again.

An Android application cannot put an arbitrary head-unit chipset into monitor mode through these APIs, read all 802.11 frames, detect all client-only devices, or guarantee that a vendor's Bluetooth audio chip supports Android BLE. GPS and USB-host availability also vary. No raw PCAP, Remote ID decoder, classic Bluetooth/SDP inventory, firmware OTA, or OUI-SPY BLE command integration is claimed in this edition.

The ESP32 firmware is different: Wi-Fi `WIFI_MODE_NULL` + promiscuous management-frame receive, channels 1–11, and **passive** BLE scanning. It never starts an AP/station scan, associates, sends probes, advertises, connects to BLE targets, or injects frames. It collects only metadata, not traffic payloads or handshakes. Directed probe-request SSIDs are omitted. Wi-Fi security from this receiver is only `[OPEN]` / `[PRIVACY]`, not a complete WPA decoder. The first advertised BLE service and manufacturer company ID are recorded; absent identifiers are not inferred.

Name, service, and manufacturer evidence are shown as **unverified signatures**, not proof of ownership/device type. The app uses a small explainable signature set rather than broadly labeling common chip vendors as surveillance. Spoofing, randomized addresses, radio coverage, hidden APs, changing firmware, and missing advertisements can create false positives or missed detections. Watchlist matching is case-insensitive substring matching over names, addresses, evidence, and advertised service UUIDs.

## Consent and passcode behavior

- Salted PBKDF2-HMAC-SHA256 (120,000 rounds); passcodes are not recorded in audit logs.
- Five failed attempts impose a persisted 60-second cooldown. Every connected action requires its own passcode approval, an owner/consent reference, and an explicit ownership/informed-consent checkbox.
- The exact target must be a connectable BLE address observed within 30 seconds, during a real native discovery session. Demo/replay/USB observations cannot start connected training.
- The approval audit must persist before radio actions run. Inventory closes after completion, failure, app exit, or 15 seconds. It never reads/writes characteristic values.
- App backup/device-transfer are excluded; the only exported Android component is the launcher Activity. There is no Internet permission or upload service. The original upstream companion's robot exploitation controls and firmware commands are not reachable from this APK.
- The public initial PIN is a setup value, **not theft protection**. Change it. An app PIN cannot defend against root access, reflashing/uninstalling/clearing app data, changing the system clock, or extracting an unlocked SD card. SD exports are plaintext training data; private files use Android's app sandbox. UI screenshots are blocked on-device.
- Operators enter consent references themselves; the app does not independently verify a volunteer's identity or consent. Use connected training while parked, with a dedicated owned/volunteer device. A passenger can operate discovery during a driving demonstration.

## Build and test

Install JDK 17 or newer, Android SDK platform 35, and PlatformIO 6.1.18. Point `ANDROID_HOME` at the SDK, or create an ignored `local.properties` with `sdk.dir=...`.

```sh
cd headunit
./test-core.sh
./test-firmware.sh
./gradlew testDebugUnitTest lintDebug assembleDebug
pio run -d receiver
```

The checked-in Gradle wrapper pins Gradle 8.9 with a distribution checksum. Production dependencies: **none**; native Android APIs only. Test dependencies: JUnit 4.13.2 / Robolectric 4.14.1. ESP32 platform/libraries are pinned in `receiver/platformio.ini`.

Outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/previews/landscape-0.png` through `landscape-3.png` (native Android 15 software renders, 960×480)
- `receiver/.pio/build/passive_xiao_s3/firmware.bin`
- `receiver/.pio/build/passive_s3_devkit/firmware.bin`

Checks include core policy/CSV helpers, persisted PIN changes and cooldown, foreground stop behavior, bounded device views with continued recording, demo connected-action blocking, USB telemetry validation, and an actual Android DocumentsProvider simulation that exports 600 observations and preserves the private source after a simulated full card. The beacon parser is fuzzed with 100,000 malformed frames under address/undefined-behavior sanitizers. Leak detection is disabled in that small host test because some sandbox runtimes use ptrace. Android layout/lifecycle tests cover API 28 and API 35; they do not simulate real RF hardware.

## ESP32 receiver setup

Supported compile targets:

| Target | Board | Connection |
|---|---|---|
| `passive_xiao_s3` | Seeed XIAO ESP32-S3 | Direct native USB CDC |
| `passive_s3_devkit` | ESP32-S3-DevKitC-1-N8 | **Native USB** port, not the UART bridge |

These are existing board profiles; no display/GPS/button pins are driven. The exact LilyGO pager model/revision is required before making its profile. Do **not** flash the generic DevKit profile onto an unidentified pager. Pager display/GPS/LoRa support is not included; ESP32 Wi-Fi/BLE cannot detect arbitrary sub-GHz signals.

For a matching XIAO only:

```sh
pio run -d receiver -e passive_xiao_s3 -t upload --upload-port /dev/ttyACM0
```

Replace the port with your actual board's port. PlatformIO uploads bootloader, partitions, and firmware at the proper offsets; `firmware.bin` alone is not a factory image. Then connect the board by a USB data/OTG cable to the head unit, approve a real discovery session, and select **Passive USB receiver…**. Grant Android's USB permission prompt. CP210x/CH340 UART bridges are not supported by this driver. ESP32-S3 direct CDC avoids an extra driver dependency, provided the radio exposes USB host.

The receiver starts with its radios idle. The app first sends `HELLO`; compatible firmware replies `{"persephone_passive":1}`. Only then does the approved app send `START`. `KEEP` refreshes the watchdog once per second. `STOP`, loss of USB DTR, or three seconds without a keepalive stops reception. Boot, disconnect, and app exit do not leave an autonomous scanner running. This handshake is compatibility identification, not cryptographic authentication; use only a receiver you control.

Firmware queue: 64 metadata events. USB/Android deduplicate observations to at most once per second per address; very dense RF environments can drop firmware events. This is awareness logging, not a lossless capture instrument. Private sessions cap at 32 MiB and stop below 10 MiB free storage; start another session after exporting. The live window/replay holds at most 512 device addresses; exports contain the full recorded event stream. Live list shows up to 100 rows by RSSI. Replay tolerates incomplete final JSONL lines after power loss. Normal recording flushes at least once per second when events arrive; sudden power loss may lose buffered events. A file provider that fails mid-export is cleaned up where possible, and the local source is retained.

## Radio acceptance before the presentation

1. Diagnostics: verify Android >=8, true Android BLE support, Location, GPS reception, writable SD subfolder, and direct USB CDC if using the receiver.
2. On an owned AP/BLE beacon, verify actual address/name/RSSI, movement trend, and stale flags after the transmitter stops. A GPS-tagged observation marks where the radio received it, not where the device resides.
3. Compare the same beacons with an independent trusted receiver. Check received client probes with an owned ESP32 test setup; native Android is expected to miss them.
4. Stop/restart the app; verify the prior recording, replay, exact JSONL/CSV counts, and SD exports. Remove/unmount the card before Stop and confirm the private session remains exportable.
5. Try wrong PINs, wait through cooldown, and confirm demo/replay cannot connect. With explicit consent, inventory an owned BLE beacon's services, then verify it disconnects. Deny/revoke Bluetooth permissions and confirm graceful failure.
6. Verify the ESP32 stops within three seconds when the app exits/USB disconnects. With an independent capture receiver, confirm the bundled ESP32 emits no Wi-Fi probes, deauth, association, or BLE scan requests. Ordinary Android discovery is a separate behavior.
7. Test the vehicle's ignition/power cycle and document-provider persistence. Rehearse Demo if the venue's RF/ROM behavior differs. Keep the screen operator parked or in the passenger seat.

## References and attribution

- [H34THEN/oui-spy-w4tch3r](https://github.com/H34THEN/oui-spy-w4tch3r): host repository and existing ESP32/Flutter architecture. This native edition does not send its engine-control protocol.
- [FlockSquawk](https://github.com/f1yaw4y/FlockSquawk): inspiration for metadata-based, explainable awareness. Its source was not bundled.
- [flock-you](https://github.com/colonelpanichacks/flock-you): provenance for the small name/service/manufacturer signature set (`Flock`, `Penguin-`, `FS Ext Battery`, `B4:1E:52`, accessory UUID, Raven services, XUNTONG `0x09C8`). These public identifiers are hypotheses, not validated device identities.
- [Pwnagotchi](https://github.com/evilsocket/pwnagotchi): inspiration for companion moods/epochs only; no code or offensive capture features imported.
- [Android Wi-Fi scanning](https://developer.android.com/develop/connectivity/wifi/wifi-scan), [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions), [storage access framework](https://developer.android.com/training/data-storage/shared/documents-files).
