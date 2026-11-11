# Week 1 Prototype Findings & Milestone 1 Verification Report

- **Date:** 2026-11-11
- **Status:** Complete & Verified
- **Milestone:** Milestone 1 (Foundation & Floating Overlay)

---

## 1. Prototype Findings

### Prototype P1: Cutout Placement & Geometry
- **Objective:** Verify centered alignment around the physical/simulated camera cutout and safe status-bar positioning.
- **Implementation:** `PositionCalculator` calculates the center X coordinate `specs.cutoutBoundingRect?.centerX ?: (displayWidth / 2)` and clamps within safe display boundaries.
- **Verification Evidence:**
  - Table-driven unit test `PositionCalculatorTest` validates:
    - Center punch-hole: Pill centers horizontally on `displayWidth / 2` with cutout top alignment.
    - No-cutout fallback: Centers horizontally and offsets below status bar (`top >= statusBarHeight`).
    - Landscape: Returns `isVisible = false` and 0 width/height.
- **Finding / Decision:** The geometric abstraction `CutoutSpecs` and `PositionCalculator` reliably adapt to both punch-hole cutouts and notchless displays without requiring complex hardware-specific hacks.

---

### Prototype P4: Window Resize Coalescing (ADR-03)
- **Objective:** Ensure window resizing via `updateViewLayout` is capped at ≤ 2 calls per state transition to prevent IPC thrashing, GPU reallocation, and animation stutter.
- **Implementation:** `WindowController` handles `onTransitionStart()` (expanding layout bounds before Compose animation starts) and `onTransitionEnd()` (settling touch bounds after animation ends). Intermediate frame animation ticks (`onAnimationTick()`) perform zero `WindowManager` IPC calls.
- **Verification Evidence:**
  - `WindowControllerTest.transition update coalesces to at most 2 layout updates` passes, asserting exactly 2 calls to `updateViewLayout` across multiple intermediate animation ticks.
- **Finding / Decision:** ADR-03 holds. Window layout updating is completely decoupled from frame-by-frame Compose drawing.

---

### Prototype P5: Landscape & Fullscreen Orientation Handling
- **Objective:** Ensure overlay does not cover fullscreen games, videos, or camera viewfinders in landscape orientation.
- **Implementation:** `PositionCalculator` monitors `isLandscape = specs.isLandscape` and returns `PillBounds(0, 0, 0, 0, isVisible = false)` when device enters landscape mode.
- **Verification Evidence:**
  - `PositionCalculatorTest.landscape mode hides the overlay` passes cleanly.
- **Finding / Decision:** Fullscreen landscape suppression works cleanly at the domain level before any view rendering occurs.

---

## 2. Verification Summary

| Component | Status | Evidence |
|---|---|---|
| Multi-Module Project (14 Modules) | Passed | `./gradlew projects` lists all 14 modules |
| Version Catalog (`libs.versions.toml`) | Passed | Pinned stable dependencies across all modules |
| Composite `build-logic` Convention Plugins | Passed | Clean compilation of JVM, Android Library, Application, Compose, and Test plugins |
| Domain Models (`:core:model`, `:core:common`) | Passed | `explicitApi()` enabled, zero `android.*` dependencies, 100% unit tests pass |
| Overlay Window & Pill (`:overlay`) | Passed | `OverlayHost`, `WindowController`, `PositionCalculator`, and `IslandPill` compiled and unit tested |
| Test Harness (`:app`) | Passed | `MainActivity` compiled into 24MB `app-debug.apk` |
| Full Test Suite (`./gradlew test`) | Passed | 273 actionable tasks, 0 failures |
