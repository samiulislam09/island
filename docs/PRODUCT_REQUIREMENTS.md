# Product Requirements Document — Island (Android Live Activities)

Version 0.1 · 2026-11-11 · Status: Draft for approval

---

## 1. Product vision

**Island turns the dead space around your phone's camera into a live, glanceable, touchable control surface for what's happening right now.** Music, charging, timers, and incoming notifications appear there in real time.

What makes it Android-first rather than a copy:

- **Adaptive to the hardware.** It wraps punch-holes, notches, and centered or corner cameras, and works on devices with no cutout. It is not one fixed pill.
- **User-owned.** Size, position, shape, colors, motion, and which activities may appear are all configurable. Per-app notification rules are available.
- **Multi-activity aware.** Two concurrent activities can share the island (split pill), with deterministic priority rules the user can override.
- **Private by construction.** It processes everything on-device and has no network permission for data. Notification content is transient and never stored.
- **Light on battery.** It is event-driven with zero polling. It does nothing when nothing changes.

### 1.1 Positioning against platform features

| Competitor / platform | What it does | Our differentiation |
|---|---|---|
| Android 16+ **Live Updates** (promoted notifications, status-bar chips) | System-level progress chips for apps that adopt the API | Works on Android 10–15 too. Unified surface for *all* sources (media, battery, any notification), customization, interaction without pulling the shade |
| Samsung **Now Bar / Live Notifications** (One UI 7+) | Lock-screen and status-bar live info on Samsung only | Cross-OEM, home-screen/in-app presence, user-configurable |
| Existing Play "island" apps | Many depend on AccessibilityService, show ads, or collect data | No Accessibility, no ads, no data collection, premium motion quality, reliability engineering per OEM |

> Strategic risk: platform features are converging on this space. Our durable value is **customization, cross-version and cross-OEM consistency, and privacy**, not the mere existence of a pill.

---

## 2. Target users

| Persona | Description | Primary needs |
|---|---|---|
| **P1 Customizer** ("Theo", 24) | Enthusiast who uses launchers and icon packs and follows r/androidthemes | Visual polish, themes, fine control over position and shape. Most likely Pro buyer |
| **P2 Music listener** ("Amara", 31) | Streams music all day, switches apps constantly | Glanceable track info and one-tap controls without pulling the shade |
| **P3 Focused professional** ("Kenji", 38) | Notification fatigue | Calm previews, strict per-app filtering, privacy on screen-share or in public |
| **P4 Ex-iPhone switcher** ("Lena", 27) | Misses the iOS interaction model | Familiar but better. Simple onboarding |

**Non-target for MVP:** tablets as a primary form factor, Android TV, Wear OS, Android Auto, enterprise/managed devices.

---

## 3. User stories

Priority: **P0** = MVP must, **P1** = MVP should (cut first if late), **P2** = post-MVP.

### 3.1 Onboarding and permissions
| ID | Story | Pri |
|---|---|---|
| US-01 | As a new user, I see what the island does in under 30 s before being asked for any permission. | P0 |
| US-02 | As a user, I understand *why* each permission is needed and what data it touches before I am sent to system settings. | P0 |
| US-03 | As a user who declines notification access, I can still use the battery and timer activities. | P0 |
| US-04 | As a user on Xiaomi, OnePlus, Realme, or Samsung, I get device-specific guidance to stop the system from killing the island. | P1 |
| US-05 | As a user, I can revoke any permission and the app degrades gracefully with a clear explanation. | P0 |

### 3.2 Island engine
| ID | Story | Pri |
|---|---|---|
| US-10 | As a user, I see a compact pill positioned around my camera cutout. | P0 |
| US-11 | Tapping the pill expands it to show details and controls. Tapping outside or swiping up collapses it. | P0 |
| US-12 | Long-pressing the pill opens a quick menu (open app, hide for 1 h, settings for this activity). | P0 |
| US-13 | With two live activities, I see a split pill and can swipe horizontally to switch the primary one. | P1 |
| US-14 | The island hides automatically in full-screen video and games. | P0 (best-effort, see risk P5) |
| US-15 | The island hides in landscape by default (configurable). | P0 |
| US-16 | Transient activities (a notification preview) disappear automatically after a configurable timeout. | P0 |
| US-17 | I can turn the island off temporarily from a Quick Settings tile. | P1 |

### 3.3 Activities
| ID | Story | Pri |
|---|---|---|
| US-20 | **Music:** the island shows title, artist, and artwork for the active media session, and lets me play/pause/skip without leaving my app. | P0 |
| US-21 | **Music:** expanded view shows a progress bar that updates smoothly with no polling while collapsed. | P0 |
| US-22 | **Music:** I can seek by dragging the progress bar when the session supports it. | P1 |
| US-23 | **Battery:** plugging in shows a short charging animation with the percentage. | P0 |
| US-24 | **Battery:** I'm alerted once at a configurable low-battery threshold and once when full (if charging). | P0 |
| US-25 | **Notification:** a new notification from an allowed app appears as a preview with app icon, app name, and title. | P0 |
| US-26 | **Notification:** expanding shows the text. Tapping opens the notification's target. | P0 |
| US-27 | **Notification:** I choose per app: show / show without content / never. | P0 |
| US-28 | **Notification:** content is hidden when the device is locked or a screen is being shared/recorded (when detectable). | P0 / P1 |
| US-29 | **Timer:** I start a countdown from the app or the island long-press menu and see it live in the pill. | P0 |
| US-30 | **Stopwatch:** I can start, pause, resume, lap (P2), and dismiss. | P0 |
| US-31 | **Timer:** a running timer survives the app process being killed and the phone being rebooted (re-armed on boot). | P0 / P1 |
| US-32 | **Timer:** I'm alerted when the countdown ends even if the island process was killed (within the platform's alarm precision). | P0 |

### 3.4 Customization
| ID | Story | Pri |
|---|---|---|
| US-40 | I adjust pill width, height, vertical offset, horizontal offset, and corner radius with a live preview. | P0 |
| US-41 | I choose light, dark, AMOLED black, or system theme, and an accent color. | P0 |
| US-42 | I choose animation speed (Relaxed / Standard / Snappy / Reduced-motion). | P0 |
| US-43 | I enable or disable each activity type and set its priority order. | P0 |
| US-44 | I set per-activity appearance (e.g., artwork-tinted background for music). | P1 (some Pro) |
| US-45 | Pro: premium themes, advanced motion presets, premium layouts. | P1 (gated in Week 6 or v1.1) |

### 3.5 Privacy and trust
| ID | Story | Pri |
|---|---|---|
| US-50 | I can read, inside the app, exactly what data is processed and that nothing leaves the device. | P0 |
| US-51 | I can wipe all app data from settings. | P0 |
| US-52 | The app works fully offline. | P0 |

---

## 4. Functional requirements

Requirement IDs are referenced by the roadmap and tests.

### 4.1 Island engine (FR-ENG)
- **FR-ENG-01** The island renders in a single overlay window of type `TYPE_APPLICATION_OVERLAY`. One window serves all activity types.
- **FR-ENG-02** Presentation states: `Hidden`, `Minimal` (dot/mini-pill), `Compact`, `CompactSplit`, `Expanded`.
- **FR-ENG-03** Transitions between states are spring-animated. Interrupted animations retarget from the current value with no snapping.
- **FR-ENG-04** Gestures: tap → expand/primary action; long-press (≥ 400 ms) → quick menu; swipe up on expanded → collapse; horizontal swipe on compact → cycle activities; swipe down on compact → expand.
- **FR-ENG-05** The visible set is at most **1 primary + 1 secondary** activity. Other activities are queued by priority.
- **FR-ENG-06** Each activity has an expiration policy: `Persistent` (while source active), `Timed(duration)`, or `UntilDismissed`.
- **FR-ENG-07** Minimum display time (default 2.5 s) and hysteresis prevent flicker when priorities change rapidly.
- **FR-ENG-08** Position is derived from the display cutout (`DisplayCutout.boundingRectTop`) when present, otherwise from the configured top-center. User offsets are applied relative to this anchor.
- **FR-ENG-09** On rotation, fold/unfold, display change, or density change, position is recomputed within 1 frame of the configuration callback.
- **FR-ENG-10** The island hides when the device is in landscape (default on), when immersive/full-screen is detected (best effort), and when the user snoozes it.
- **FR-ENG-11** Touches outside the island bounds always reach the underlying app. The window is never larger than the current island bounds plus a 0–8 dp touch slop.

### 4.2 Music (FR-MUS)
- **FR-MUS-01** Discover active sessions via `MediaSessionManager.getActiveSessions(listenerComponent)` and `OnActiveSessionsChangedListener`.
- **FR-MUS-02** Track the session that is **playing**. If none is playing, track the most recently active one for a 30 s linger.
- **FR-MUS-03** Show title, artist, artwork (downscaled to ≤ 256 px on the long edge), and play state.
- **FR-MUS-04** Controls: play/pause, next, previous (shown only if the `PlaybackState.actions` bitmask supports them); seek if `ACTION_SEEK_TO`.
- **FR-MUS-05** Progress is extrapolated from `PlaybackState.position`, `lastPositionUpdateTime`, and `playbackSpeed`. The UI ticker runs only while the expanded view is visible.
- **FR-MUS-06** The media app's own media notification is suppressed from the Notification activity (no double display).

### 4.3 Battery (FR-BAT)
- **FR-BAT-01** Context-registered receiver for `ACTION_BATTERY_CHANGED`, `ACTION_POWER_CONNECTED`, `ACTION_POWER_DISCONNECTED`, `ACTION_BATTERY_LOW`, `ACTION_BATTERY_OKAY`.
- **FR-BAT-02** Emit only when `(levelPercent, pluggedType, status)` changes (`distinctUntilChanged`). Temperature and voltage churn is ignored.
- **FR-BAT-03** Charging-connected activity: `Timed(4 s)` with a charging animation.
- **FR-BAT-04** Low battery at a user threshold (default 15 %): `Timed(6 s)`, once per discharge cycle.
- **FR-BAT-05** Full: once per charge cycle when the level reaches 100 % or status is `FULL` (configurable).

### 4.4 Notifications (FR-NOT)
- **FR-NOT-01** `NotificationListenerService` posts `NotificationPosted`, `NotificationRemoved`, and `RankingUpdated` events.
- **FR-NOT-02** Default filters drop: own package, group summaries, `FLAG_ONGOING_EVENT` and foreground-service notifications (unless allow-listed), `CATEGORY_TRANSPORT` media notifications, notifications whose ranking says `isAmbient` / importance `< DEFAULT`, notifications suppressed by DND (`!matchesInterruptionFilter`), and updates to an already-seen key without new content (`onlyAlertOnce` semantics).
- **FR-NOT-03** Per-app rule: `SHOW_FULL`, `SHOW_REDACTED` (app name plus "New notification"), `BLOCK`. Default for new apps is configurable (default `SHOW_FULL` for messaging categories, `SHOW_REDACTED` otherwise).
- **FR-NOT-04** Content is hidden when the keyguard is locked, or when `Notification.visibility == VISIBILITY_SECRET`/`PRIVATE` with no public version.
- **FR-NOT-05** Tap sends `contentIntent` using the sender-side background-activity-start opt-in (see ANDROID_COMPATIBILITY §4.6). Failure falls back to launching the app's launch intent.
- **FR-NOT-06** Dismiss from the island optionally also cancels the system notification (setting, default **off**).
- **FR-NOT-07** Notification text is held in memory only, max 20 items, and cleared on removal or expiry.

### 4.5 Timer and stopwatch (FR-TIM)
- **FR-TIM-01** Timer state is stored as `(startedAtWallClock, startedAtElapsedRealtime, durationMs, accumulatedPausedMs, state)`. Remaining time is computed, never counted down.
- **FR-TIM-02** UI updates at 1 Hz in compact (aligned to whole seconds) and only while visible.
- **FR-TIM-03** Expiry alarm: `AlarmManager.setExactAndAllowWhileIdle` when `canScheduleExactAlarms()`, otherwise `setAndAllowWhileIdle` (inexact) with a UI note. The alarm fires a notification and island alert.
- **FR-TIM-04** After reboot, `BOOT_COMPLETED` re-arms the alarms of running timers (elapsed time is recomputed from wall clock).
- **FR-TIM-05** MVP supports 1 countdown and 1 stopwatch at a time.

### 4.6 Settings and customization (FR-SET)
- **FR-SET-01** All appearance changes preview live in the running island within 100 ms.
- **FR-SET-02** Settings persist in DataStore. Per-app rules, timers, and custom themes persist in Room.
- **FR-SET-03** "Reset to defaults" and "Delete all data" are available.
- **FR-SET-04** Settings export/import is **P2**.

---

## 5. Non-functional requirements

Numbers are **targets to validate** on the reference devices (TESTING_STRATEGY.md §6), not guarantees.

| ID | Category | Target |
|---|---|---|
| NFR-01 | Event → first island frame latency | p50 ≤ 50 ms, p95 ≤ 120 ms (from system callback to frame committed) |
| NFR-02 | Animation smoothness | ≤ 1 % janky frames during transitions on Pixel 7a-class hardware. No frame > 32 ms on 60 Hz |
| NFR-03 | Idle CPU | 0 wakeups attributable to the app while nothing changes. < 0.5 % avg CPU over 30 min idle with island visible |
| NFR-04 | Memory | Overlay process PSS ≤ 80 MB with island visible; Java heap ≤ 30 MB; no growth > 5 MB over a 4 h soak |
| NFR-05 | Battery | ≤ 1 % of total battery drain per 24 h attributable to the app in typical use (Battery Historian / Android Vitals) |
| NFR-06 | App start | Main app cold start TTID ≤ 600 ms on reference mid-range (with Baseline Profile) |
| NFR-07 | Recovery | After process death, island visible again ≤ 3 s after the system rebinds the listener. Timer state exact to ± 1 s |
| NFR-08 | Reboot | Island reappears after unlock without the user opening the app (via listener rebind). Timers re-armed |
| NFR-09 | Stability | Crash-free users ≥ 99.5 %, ANR rate ≤ 0.2 % (Play Vitals bad-behavior threshold is 0.47 %) |
| NFR-10 | Privacy | Zero network calls carrying user data. No `INTERNET` permission in MVP, or if Billing requires it, no app-originated data traffic |
| NFR-11 | Accessibility (main app) | TalkBack-labelled controls, 48 dp touch targets, font scale 200 % without truncation of critical text |
| NFR-12 | Size | Download size ≤ 12 MB (R8 full mode, resource shrinking) |

> Note on NFR-10: the Play Billing Library talks to the Play Store app over IPC, not over our own network stack. The manifest-merged `INTERNET` permission from libraries must be reviewed. Data Safety is about *collection by the developer*, not about holding the permission.

---

## 6. MVP scope

### In scope (v1.0)
- Floating island engine (states, gestures, priority, expiry, cutout positioning, landscape/immersive hide)
- Activities: **Music, Battery, Notifications, Timer, Stopwatch**
- Customization: size, position, corner radius, theme (4 base themes + accent), animation speed, per-activity enable/priority
- Onboarding, permission setup, dashboard, activity management, privacy settings, about
- Quick Settings tile (P1)
- OEM background-restriction guidance (P1)

### Out of scope (explicit)
- Call activity (needs `READ_PHONE_STATE`/`ANSWER_PHONE_CALLS`, a high policy-scrutiny area). P2 via notification `CATEGORY_CALL` only
- Navigation, delivery tracking, third-party SDK, plugins
- Reading or mirroring **Android 16 Live Updates** from other apps (P2: low effort once the NLS exists, but needs design)
- Notification inline reply (`RemoteInput`) is P2
- Cloud sync, accounts, backend of any kind
- Tablets/ChromeOS optimization (must not crash; layout may be basic)

### Pro gating (not on critical path)
Billing integration is scheduled as a **Week 6 stretch goal**. If it slips, v1.0 ships free with Pro in v1.1. Feature flags for Pro features exist from Week 4 so gating is a configuration change only.

---

## 7. Monetization plan

### 7.1 Tiers
| Free | Pro |
|---|---|
| Island engine, all MVP activities | Premium themes (gradients, artwork-adaptive, glass) |
| 4 base themes + 6 accent colors | Unlimited custom themes and accent picker |
| Standard motion presets | Advanced motion presets (elastic, morph, "liquid") |
| Per-app notification rules (unlimited — privacy is never paywalled) | Premium layouts (large-art music, ring-style battery, dual-timer) |
| | Per-activity appearance overrides |

**Principle:** anything related to privacy, reliability, or accessibility stays free.

### 7.2 Pricing hypotheses (unvalidated)
| Option | Hypothesis (USD, before regional pricing) | Rationale | Concern |
|---|---|---|---|
| Monthly | $1.49 | Low entry | Utility apps have poor monthly retention; churn and support cost |
| Annual | $7.99 | Main subscription anchor | Users resist subscriptions for apps with no server cost |
| Lifetime | $14.99 (launch promo $9.99) | No server costs, so an honest fit for a local-only app | Caps lifetime value; must fund ongoing OEM maintenance |

**Recommendation:** launch with **Annual + Lifetime** only. Monthly adds Play Billing and support complexity with likely low uptake. Revisit with data.

### 7.3 How to test willingness to pay
1. **Pre-launch:** a landing page plus Play pre-registration. Run a 3-option price survey (Van Westendorp) with pre-registrants.
2. **Fake-door test in beta** (closed testing track): a "Pro" screen with prices and an "I'd buy this" button that records a local opt-in only. Users who opt in to a feedback form are counted manually; no analytics SDK is used.
3. **Post-launch:** Play Console **price experiments** for in-app products (lifetime SKU) across regions.
4. **Metrics to watch:** paywall view → purchase conversion, refund rate, and 1-star reviews mentioning price.
5. **Kill criteria:** if conversion is < 1 % of weekly active users after 60 days, re-scope which features are Pro.

---

## 8. Success metrics (first 90 days)

| Metric | Target | Source |
|---|---|---|
| Onboarding completion (all required permissions granted) | ≥ 60 % | Local counters shown to user / Play Console funnel proxy |
| D7 retention | ≥ 35 % | Play Console |
| Crash-free users | ≥ 99.5 % | Play Vitals |
| Rating | ≥ 4.4 | Play Console |
| Battery complaints in reviews | < 5 % of reviews | Manual review tagging |
