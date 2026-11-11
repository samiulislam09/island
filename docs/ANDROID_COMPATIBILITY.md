# Android Compatibility, Permissions & Play Compliance — Island

Version 0.1 · 2026-11-11 · Status: Draft for approval

> **Sources.** Official Android documentation (developer.android.com) and Google Play Console Help are primary. Where behavior is OEM-specific or observed in the field rather than documented, it is labeled **[OEM]** or **[Field]**. Everything labeled **[Verify]** must be confirmed by a prototype or by re-reading the current docs at implementation time. Android 17 (API 37) was released in 2026, and Play has required `targetSdk 36` for new apps and updates since 2026-08-31.

Legend: **[Official]** documented platform behavior · **[OEM]** manufacturer-specific · **[Field]** widely reported, not documented · **[Unreliable]** don't depend on it · **[Special]** special/app-op permission · **[Play]** Google Play policy constraint.

---

## 1. Platform baseline

| Setting | Value | Reason |
|---|---|---|
| `minSdk` | 29 (Android 10) | Brief baseline. `Display.getCutout()` and gesture nav available. Re-evaluate 31 at end of Week 5 (ADR-02) |
| `targetSdk` | 36 (Android 16) | **[Play]** required for new apps and updates since 2026-08-31 |
| `compileSdk` | 37 (Android 17) | Access to newest APIs and lint checks. Behavior unchanged until target 37 |
| JDK for build | 21 | The existing template uses toolchain 26, which is not appropriate for AGP |
| Kotlin | 2.4.x (matches existing template's 2.4.20) | Verify AGP and Compose compiler compatibility on day 1 |

---

## 2. API compatibility matrix

| Capability | API(s) | 29 (10) | 30 (11) | 31–32 (12/12L) | 33 (13) | 34 (14) | 35 (15) | 36 (16) | 37 (17) |
|---|---|---|---|---|---|---|---|---|---|
| Overlay window | `TYPE_APPLICATION_OVERLAY` + SAW | ✅ | ✅ | ✅ untrusted-touch rules | ✅ | ✅ | ✅ FGS-start needs *visible* overlay | ✅ | ✅ |
| Window context | `createWindowContext` | ❌ use service ctx | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Window metrics/insets | `WindowManager.currentWindowMetrics` | ❌ use Jetpack WindowManager | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Cutout rects | `DisplayCutout.boundingRectTop` | ✅ (`getBoundingRectTop` 29) | ✅ | ✅ + `getCutoutPath` 31 | ✅ | ✅ | ✅ | ✅ | ✅ |
| Cutout layout mode | `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS` | ❌ use `SHORT_EDGES` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Notification listener | `NotificationListenerService` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ OTP content redacted for untrusted listeners | ✅ | ✅ |
| Listener access check | `NotificationManager.isNotificationListenerAccessGranted` | ✅ (27+) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Media sessions | `MediaSessionManager.getActiveSessions(component)` | ✅ needs enabled NLS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Battery | `ACTION_BATTERY_CHANGED` (sticky), power connected/disconnected | ✅ context-registered | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Runtime notif permission | `POST_NOTIFICATIONS` | n/a | n/a | n/a | ✅ runtime | ✅ | ✅ | ✅ | ✅ |
| Exact alarms | `SCHEDULE_EXACT_ALARM` | n/a (exact allowed) | n/a | ✅ granted by default | ✅ | ⚠️ **denied by default** for new installs | ⚠️ | ⚠️ | ⚠️ |
| FGS types | `foregroundServiceType` | optional | optional | optional | optional | **required** + per-type permission | ✅ + time limits for `dataSync`/`mediaProcessing` | ✅ | ✅ |
| BAL: sending PendingIntent | `ActivityOptions.setPendingIntentBackgroundActivityStartMode` | n/a | n/a | n/a | n/a | ✅ sender opt-in required | ✅ | ✅ | ✅ + IntentSender hardening (target 37) |
| Screen-recording detection | `WindowManager.addScreenRecordingCallback` | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Live Updates (promoted notifs) | `ProgressStyle`, `setRequestPromotedOngoing`, `POST_PROMOTED_NOTIFICATIONS` | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Dynamic color | `dynamicLightColorScheme` | ❌ fallback palette | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Predictive back (app UI) | `OnBackPressedDispatcher` | — | — | — | opt-in | opt-in | default for target 35+ | ✅ | ✅ |
| Edge-to-edge (app UI) | `enableEdgeToEdge()` | opt-in | opt-in | opt-in | opt-in | opt-in | enforced at target 35 | opt-out removed at target 36 | ✅ |
| Large-screen orientation | Manifest orientation/resizability | honored | honored | honored | honored | honored | honored | **ignored on sw ≥ 600 dp at target 36** | ✅ |

---

## 3. Permissions inventory

| Permission | Kind | Necessity | Requested when | Notes |
|---|---|---|---|---|
| `SYSTEM_ALERT_WINDOW` | **[Special]** app-op, Settings screen | **Required** for the island | Onboarding step 2, after explanation | **[Play]** send users to the system settings screen (`ACTION_MANAGE_OVERLAY_PERMISSION` with `package:` URI). Android 11+ ignores the package URI and opens the list. Instruct the user accordingly |
| `BIND_NOTIFICATION_LISTENER_SERVICE` (declared on service) + user grant of notification access | **[Special]** | **Strongly recommended** (notifications, media, process anchoring) | Onboarding step 3 with prominent disclosure | Intent `ACTION_NOTIFICATION_LISTENER_SETTINGS`. API 30+ supports `ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS` with component extra. Android 13+ side-loaded APKs need "Allow restricted settings" (not an issue for Play installs) |
| `POST_NOTIFICATIONS` | Runtime (33+) | Optional | When the user first starts a timer, or enables Reliability mode | |
| `SCHEDULE_EXACT_ALARM` | **[Special]** (31+) | Optional | When the user starts a first timer longer than 1 min, with an explanation | Listen for `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. **Do NOT use `USE_EXACT_ALARM`** — **[Play]** restricted to alarm/clock/calendar apps |
| `RECEIVE_BOOT_COMPLETED` | Normal | Required for timer re-arm | Manifest only | Receiver re-arms alarms only. No FGS start from boot |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Normal | **Optional** (Reliability mode only) | When the user opts into Reliability mode | **[Play]** requires an FGS declaration in Play Console (App content) with description and video. Declare **only** if Reliability mode ships |
| `VIBRATE` | Normal | Optional (haptics) | Manifest | Prefer `View.performHapticFeedback` (no permission needed) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Normal, but **[Play]** restricted | **Do not declare** | — | Deep-link to `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (list) instead. This needs no permission and avoids policy risk |
| `QUERY_ALL_PACKAGES` | **[Play]** restricted | **Do not declare** | — | App labels and icons for notifications come from `sbn.packageName` + `PackageManager` (visibility constraints in the note below). Use `<queries>` with `<intent>` for `MAIN/LAUNCHER` to list apps in the per-app rules screen |
| `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS` | Dangerous | **Not in MVP** | — | Call activity is P2, via notification `CATEGORY_CALL` only |
| `PACKAGE_USAGE_STATS` | **[Special]** | **Not in MVP** | — | Would help full-screen detection. A Play sensitivity and trust cost |
| `BIND_ACCESSIBILITY_SERVICE` | **[Play]** heavily restricted | **Never** | — | ADR-07 |
| `INTERNET` | Normal | **Avoid in MVP** | — | Check the merged manifest from all libraries in CI |

> **[Verify] Package visibility:** on API 30+, `PackageManager.getApplicationLabel/Icon` for an arbitrary package requires visibility. A package that posts a notification we receive via the NLS is **not** automatically visible. Plan: declare `<queries><intent><action MAIN/><category LAUNCHER/></intent></queries>`, which covers launchable apps (almost all notifying apps). For non-launchable packages, fall back to `Notification.extras` `EXTRA_SUBSTITUTE_APP_NAME` / small icon from `sbn.notification.smallIcon`. Prototype in Week 2.

---

## 4. Background execution and restrictions

### 4.1 Overlay (`SYSTEM_ALERT_WINDOW`, `TYPE_APPLICATION_OVERLAY`)
- **[Official]** Since Android 8, apps must use `TYPE_APPLICATION_OVERLAY`. The system z-orders these windows **below critical system windows** (status bar, notification shade, IME, system dialogs).
- **[Official]** Android 12: touches passing through a *non-touchable* overlay from another UID are blocked when the window opacity is > 0.8 (`InputManager.getMaximumObscuringOpacityForTouch`). Our window is touchable and minimal-size, so this doesn't apply.
- **[Official]** Android 12+: apps can hide non-system overlays on their windows (`HIDE_OVERLAY_WINDOWS`). Permission controllers, banking apps, and some password managers do this. **The island disappears on those screens. This is expected.**
- **[Official]** Android 15: an app holding SAW may start an FGS from the background **only if it currently has a visible overlay window**.
- **[OEM]** Xiaomi/HyperOS has an *additional* "Display pop-up windows while running in the background" permission. Without it, the overlay may not show while our app isn't foreground. Some ColorOS/Realme builds have a "floating windows" toggle separate from AOSP SAW.
- **[Verify]** Whether `TYPE_APPLICATION_OVERLAY` appears on top of the keyguard varies by version and OEM. Design for both: redact content when locked either way.
- **[Unreliable]** Drawing "on top of" the status bar via `FLAG_LAYOUT_NO_LIMITS` or negative `y`. It may position over the status bar area but remains below it in z-order.

### 4.2 Notification listener
- **[Official]** User grants access in Settings. The system binds the service and keeps it bound while access is enabled. `requestRebind()` and `requestUnbind()` are available.
- **[Official]** Android 15+: untrusted listeners receive **redacted** content for notifications where the system detects an OTP.
- **[Official]** `getActiveNotifications()` on connect returns the current set. Use it to rebuild state without re-alerting.
- **[Field]** Some OEMs (MIUI/HyperOS, ColorOS) don't rebind after a force stop or kill until the app is opened or the device restarts. Mitigation: OEM guidance + Reliability mode + `requestRebind` from a `JobScheduler` job when the app is opened.
- **[Official]** Callbacks arrive on the main thread. Keep work there < 1 ms: map minimal fields, then hand off.

### 4.3 Media sessions
- **[Official]** `MediaSessionManager.getActiveSessions(ComponentName)` requires that the component is an **enabled notification listener** of the calling app. Otherwise it throws `SecurityException`.
- **[Official]** `MediaController.TransportControls` work for any session the app can see.
- **[Field]** Some apps (certain browsers, podcast apps) publish incomplete metadata, no artwork, or wrong `actions` bits. The renderer must handle nulls, and buttons are hidden when unsupported.
- **[Official]** Artwork bitmaps can be large (up to several MB). Downscale immediately on a background thread and never keep the original.
- **[Official]** Android 17 "background audio hardening" restricts apps *playing* audio from the background. It doesn't affect *controlling* other apps' sessions. **[Verify]** at target 37.

### 4.4 Broadcast receivers
- **[Official]** `ACTION_BATTERY_CHANGED` can only be context-registered (sticky). Power connected/disconnected are not exempt from the Android 8 implicit-broadcast manifest ban, so they are **context-registered** in the running process.
- **[Official]** `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, and `MY_PACKAGE_REPLACED` are manifest-registerable.
- **[Official]** Android 14+: context-registered receivers must specify `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED`. System broadcasts are exempt but the flag is still recommended. Use `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)`.

### 4.5 Foreground services
- **[Official]** Android 12+: apps can't start an FGS while in the background, except with listed exemptions (SAW holder **with a visible overlay on 15+**, exact alarm firing, etc.).
- **[Official]** Android 14+: every FGS must declare a `foregroundServiceType` with its matching permission. `specialUse` requires a `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="..."/>`.
- **[Official]** Android 15+: `BOOT_COMPLETED` receivers can't start FGS of certain types (dataSync, camera, mediaPlayback, phoneCall, mediaProjection, microphone). `specialUse` is not on that list. **We don't start an FGS from boot anyway.**
- **[Official]** Android 15+: `dataSync` and `mediaProcessing` have a 6 h/24 h limit. **Not used.**
- **[Play]** FGS types must be declared in Play Console with use-case description and video. Misdeclared types are rejected. The April 2026 policy update (effective 2026-08-26) removed geofencing as an approved use case, which shows Google actively narrows FGS uses.
- **[Field]** `specialUse` is reviewed case by case. Overlay-hosting apps have reported approval with a clear, user-initiated, user-visible justification. **Not guaranteed.**

### 4.6 Launching activities from the overlay (tap-to-open)
- **[Official]** SAW holders are exempt from background-activity-launch (BAL) restrictions for their *own* starts.
- **[Official]** Android 14+: when *sending* someone else's `PendingIntent`, the sender must explicitly opt in to grant its BAL privileges: `ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)` (newer API levels add a visibility-scoped mode, `MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE` — **[Verify]** exact API level. Prefer it where available, because the island is visible at tap time). Pass via `PendingIntent.send(context, code, intent, null, null, null, options.toBundle())`.
- **[Official]** Android 17 (target 37): BAL rules extend to `IntentSender`; `sendIntent()` needs an explicit sender opt-in.
- **[Field]** Collapse the shade or keyguard first. If the device is locked, use `KeyguardManager.requestDismissKeyguard` only from an Activity, so route through a transparent trampoline Activity in our app, which launches the PendingIntent while we're "foreground". **Prototype P3 decides between direct send and trampoline.**

### 4.7 Doze, App Standby Buckets, battery optimization
- **[Official]** Doze defers alarms (except `*AllowWhileIdle`, rate-limited to roughly once per 9 min per app), jobs, and network. Our design is callback-driven and mostly unaffected. The NLS still receives notifications in Doze (as they arrive).
- **[Official]** Standby buckets affect jobs and alarms, not bound services. `Restricted` bucket (Android 12+) may limit alarms. Timer alerts rely on exact alarms, which work in all buckets if permitted.
- **[Official]** Android 13+ "Background restriction" toggle (FGS Task Manager / battery usage "Restricted") can stop the app. That's user choice; detect it via `ActivityManager.isBackgroundRestricted()` and explain.

### 4.8 Display cutouts and insets
- **[Official]** `DisplayCutout` gives bounding rects per edge, safe insets, and waterfall insets (30+). The cutout path is available on 31+.
- **[Official]** Cutout changes with rotation (moves to a side edge in landscape) and per display on foldables (inner display often has no cutout or a different one).
- **[OEM]** Some OEMs have a "hide cutout / full-screen apps" setting that changes reported cutouts and status-bar height.
- **[Field]** Under-display cameras (e.g., some Samsung Fold inner screens) report a small or no cutout. Fallback to top-center.

### 4.9 Android 16 Live Updates (future integration)
- **[Official]** Apps can post promoted ongoing notifications (`Notification.ProgressStyle` or other eligible styles) that require `POST_PROMOTED_NOTIFICATIONS`, `setRequestPromotedOngoing(true)`, and `FLAG_ONGOING_EVENT`. They must represent an activity in progress with a distinct start and end. Users can demote them. `NotificationManager.canPostPromotedNotifications()` reports status.
- **Our P2 uses:**
  1. Our own timer can additionally post a Live Update on Android 16+, giving system-native presence when the island is hidden.
  2. Read other apps' promoted notifications via the NLS (`Notification.hasPromotableCharacteristics()` / extras) and render rides, deliveries, and navigation in the island. This is low-cost, high-value, and no third-party SDK is needed.

---

## 5. Notification policy considerations
- Notification content is personal data. **[Play]** User Data policy requires a prominent in-app disclosure (before the system prompt) when accessing personal or sensitive data in ways a user might not expect, plus a privacy policy. Disclosure must state what is accessed, how it's used, and that it stays on device.
- Don't re-post other apps' notifications as our own (spoofing/impersonation concerns). The island is a separate surface and our own notifications are only timer and Reliability mode.
- Respect DND: use `Ranking.matchesInterruptionFilter()` and suppress alerts when it's false.
- Respect `VISIBILITY_SECRET` and `VISIBILITY_PRIVATE` (use `publicVersion` when locked).

---

## 6. Google Play compliance checklist

| # | Item | Requirement | How we satisfy it | Owner/Week |
|---|---|---|---|---|
| C1 | Notification access disclosure | Prominent in-app disclosure before sending to settings: what is read, why, retention, on-device only | Dedicated onboarding screen with "Continue" plus "Not now". Text reviewed against policy | W4/W6 |
| C2 | Overlay permission explanation | Explain why, send to system settings screen, no deceptive flows | Onboarding screen with illustration. Handles "came back without granting" | W4 |
| C3 | Runtime permissions | Request in context, handle denial, no repeated nagging | `POST_NOTIFICATIONS` on first timer. Exact alarm explained on first timer | W4 |
| C4 | Privacy policy | Public URL, linked in Play listing **and** in-app | Static page (GitHub Pages or similar, no tracking) | W6 |
| C5 | User consent | Affirmative action before notification processing begins | NLS ignores events until a `consentGiven` flag is true in DataStore (defense in depth if access was granted outside onboarding) | W3 |
| C6 | Notification data processing | Purpose limitation: display on island only | No storage, no logs, no transmission. Enforced by code review checklist and test (DATABASE_DESIGN §5) | W2–W6 |
| C7 | Data minimization | Collect only fields needed | Mapper extracts only package, title, text, category, icon, timestamps | W2 |
| C8 | Secure local storage | Sensitive data not persisted | Only settings, per-app rules, timers, and themes are stored. None of it is notification content. `dataExtractionRules` and `fullBackupContent` are defined explicitly: settings and rules are included in backup, active timers are excluded (device-specific clocks) | W3 |
| C9 | Data Safety form | Accurate declaration | "No data collected, no data shared." On-device processing is not "collection" per Play definitions. Re-evaluate if crash reporting is added | W6 |
| C10 | Accessibility API | Not used | Not declared. CI fails if manifest contains `BIND_ACCESSIBILITY_SERVICE` | W1 |
| C11 | FGS requirements | Declare types with justification | Only if Reliability mode ships: `specialUse` declaration plus video. Otherwise no FGS declared | W5/W6 |
| C12 | Target API | targetSdk ≥ 36 | Set in convention plugin | W1 |
| C13 | Metadata/branding | No Apple trademarks, no misleading claims | Name/listing review. Avoid "Dynamic Island" in title and description; comparisons only per policy | W6 |
| C14 | Restricted permissions | No `QUERY_ALL_PACKAGES`, `USE_EXACT_ALARM`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, Accessibility | Merged-manifest check in CI | W1 |
| C15 | Subscriptions policy (if Pro) | Clear pricing, trial terms, easy cancellation link | Paywall shows price/period and a link to Play subscription center | W6/v1.1 |
| C16 | Families policy | Not targeting children | Target audience 18+ (or 13+) in Play Console | W6 |
| C17 | App access for review | Reviewer can see functionality | Provide review notes + demo video showing permissions and features | W6 |

---

## 7. Manufacturer-specific behavior

| OEM / skin | Known behavior | Impact | Mitigation | Confidence |
|---|---|---|---|---|
| **Google Pixel** (stock) | Reference AOSP behavior. Android 16 Live Updates chips in the status bar | Baseline | Primary dev device | [Official] |
| **Samsung One UI 6–8** | "Sleeping apps" / "Deep sleeping apps" lists. Adaptive battery. Now Bar/Live Notifications (One UI 7+) for supported apps. Edge panels. Some devices have a "camera cutout: hide" option | Process may be killed if the app goes unused (sleeping list after inactivity). Visual competition with Now Bar | Guidance to add the app to "Never sleeping apps". Test overlay with Now Bar active | [OEM][Field] |
| **Xiaomi / Redmi / POCO (MIUI → HyperOS 1/2/3)** | Extra "Display pop-up windows while running in background" and "Autostart" permissions. Aggressive killing. Battery saver "No restrictions" needed. Swiping away from recents kills services | Overlay invisible without the pop-up permission. Listener not rebound after kill | Detect Xiaomi → guided setup with deep links (`miui.intent.action.APP_PERM_EDITOR`, **[Unreliable]** — must fall back to app details settings). Reliability mode recommended | [OEM][Field] |
| **OnePlus (OxygenOS 13+ on ColorOS base)** | "Battery optimization → Don't optimize". App freezing ("deep optimization"). Recents lock | Killing after screen-off | Guidance + Reliability mode | [Field] |
| **Realme / OPPO (ColorOS / Realme UI)** | "Auto launch", "Allow background activity", separate floating-window toggle on some builds | Overlay and listener reliability | Guidance + test | [Field] |
| **Motorola (My UX)** | Close to AOSP. Some models have corner or unusual cutouts | Positioning edge cases | Corner-camera fallback | [Field] |
| **Foldables (Galaxy Z Fold/Flip, Pixel Fold)** | Display switch on fold. Different cutouts per display. Flip cover screens | Window must re-attach or reposition. Cover screen may not allow overlays | Listen to display changes. Recreate window context per display. Hide on cover screen | [Official][Verify] |

Reference for users: dontkillmyapp.com (cite in-app as an external link, no embedding).

### 7.1 Minimum physical device set
| Device | Why |
|---|---|
| Pixel 8/9 (Android 16/17) | Reference, Live Updates |
| Pixel 4a or Android 10/11 device (or emulator) | minSdk coverage |
| Samsung Galaxy A-series (One UI 7/8) | Volume market, mid-range performance |
| Samsung Galaxy S-series or Z Flip/Fold | Flagship + foldable |
| Xiaomi/Redmi on HyperOS 2/3 | Most aggressive restrictions |
| OnePlus Nord / Realme on ColorOS 15/16 | ColorOS family |
| Motorola G-series | Low-end performance, cutout variance |

---

## 8. Features likely to differ across manufacturers

| Feature | Pixel | Samsung | Xiaomi | OnePlus/Realme | Motorola |
|---|---|---|---|---|---|
| Overlay appears from background | ✅ | ✅ | ⚠️ needs pop-up permission | ⚠️ some builds | ✅ |
| Survives hours idle without FGS | ✅ likely | ⚠️ sleeping-apps | ❌ likely killed | ⚠️ | ✅ likely |
| Listener rebind after kill | ✅ | ✅ | ⚠️ | ⚠️ | ✅ |
| Overlay over lock screen | [Verify] | [Verify] | [Verify] | [Verify] | [Verify] |
| Media session metadata quality | App-dependent | App-dependent | App-dependent | App-dependent | App-dependent |
| Status bar icons overlap pill | Low (centered cutout) | Low | Medium (dense status bars) | Medium | Varies (corner cutouts) |

---

## 9. Risk register

| ID | Risk | Likelihood | Impact | Mitigation | Prototype |
|---|---|---|---|---|---|
| R1 | Pill z-order under the status bar looks broken on some devices | High | High | Lane-based compact layout. "Below status bar" alternative position | **P1** (W1) |
| R2 | OEM kills process; listener not rebound | High (Xiaomi, OPPO) | High | Reliability mode FGS, OEM guidance, open-app rebind | **P2** (W1–W2, multi-day soak) |
| R3 | Tap-to-open fails due to BAL | Medium | High | Sender opt-in, trampoline Activity, launcher fallback | **P3** (W2) |
| R4 | Window resize jank or touch blocking | Medium | High | ≤ 2 relayouts per transition. Minimal window | **P4** (W1) |
| R5 | Cannot detect full-screen apps | High | Medium | Insets heuristic, hide in landscape, quick snooze | **P5** (W1) |
| R6 | Play rejects `specialUse` FGS | Medium | Medium | Make Reliability mode optional and removable. Ship without it if needed | W5 closed-track submission |
| R7 | Play flags NLS + SAW combo during review | Medium | High | Disclosures, privacy policy, video, early closed-track submission | W5 |
| R8 | Compose overlay exceeds memory budget | Medium | Medium | Measure W1. Bitmap discipline | P4 |
| R9 | Package visibility blocks app labels/icons | Medium | Low | `<queries>` launcher intent + notification-provided fallbacks | W2 |
| R10 | Exact alarm denied → late timer alert | High (Android 14+) | Medium | In-app explanation, inexact fallback, honest UI | W2 |
| R11 | Platform convergence reduces demand (Live Updates, Now Bar) | Medium | High | Differentiation + Live Updates consumption (P2) | — |
| R12 | Apple trademark complaint | Low | High | Neutral naming and listing | W6 |
