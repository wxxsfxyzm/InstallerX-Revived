# AGENTS.md

## Purpose

This file defines repository-specific instructions for coding agents working on **InstallerX Revived**.

Use it to decide:

* where a change belongs,
* which constraints must be preserved,
* what to verify before claiming a task is complete.

Task-specific maintainer instructions take precedence over this file. When the request is narrow, make the smallest coherent change that satisfies it.

For substantial features, invasive refactors, or behavior changes that span several files, sketch a short implementation plan before editing. Keep the plan aligned with the actual implementation as the work proceeds.

---

## Read these first when relevant

* `README.md` — product scope, supported install flows, user-facing feature boundaries.
* `CONTRIBUTING.md` — translation policy, build prerequisites, contribution expectations.
* `.github/workflows/pr-check.yml` — the default CI build matrix used for pull requests.
* `settings.gradle.kts`, `app/build.gradle.kts`, and
  `gradle/libs.versions.toml` — before touching Gradle, repositories, flavors, versions, or dependencies.

Do not duplicate or contradict those files casually. Update this file only for stable, repository-wide rules that agents should repeatedly follow.

---

## Repository overview

InstallerX Revived is a community-maintained Android installer with:

* dialog, notification, and automatic installation flows,
* support for APK, APKS, APKM, XAPK, APKs inside ZIP files, and batch APK installation,
* profile-driven install options and install flags,
* privileged workflows involving Root, Shizuku, Dhizuku, and hidden APIs,
* switchable UI families based on Material 3 Expressive and Miuix.

Several product behaviors are intentionally flow-specific. Do **not** assume a feature supported in dialog installation is also valid for notification or automatic installation unless the existing code and docs already establish that.

---

## Critical project constraints

* Preserve the **online/offline** product boundary. The offline flavor must not silently gain network-only behavior or permissions.
* Prefer the repository’s existing **native API** paths and abstractions. Do not introduce shell-command implementations as a shortcut unless the maintainer explicitly requests it.
* Treat flow-specific behavior as flow-specific. A capability that exists for dialog installation is not automatically valid for notification or automatic installation.
* When changing behavior that is described in `README.md`, update it or call out the documentation impact in the handoff.

---

## Project layout

### Top-level areas

* `app/` — main Android application.
* `hidden-api/` — hidden API declarations/helpers consumed by the app.
* `build-plugins/` — shared Gradle convention plugins.
* `baselineprofile/` — Android baseline profile generation.
* `.github/workflows/` — CI and release automation.

Do not assume every top-level directory is an included Gradle module. Confirm active modules in
`settings.gradle.kts` before making module-level assumptions.

### Main Kotlin package map

Under `app/src/main/java/com/rosan/installer/`:

* `core/` — shared low-level app infrastructure.
* `data/` — persistence, concrete providers, repositories, and mappers.
* `di/` — Koin modules and initialization wiring.
* `domain/` — domain models, repository contracts, providers, use cases, and business rules.
* `framework/` — Android/platform-facing integration code.
* `ui/` — screens, widgets, navigation, themes, and UI-specific models.
* `util/` — utility helpers.

Preserve this separation. Do not move behavior into a convenient but wrong layer just to finish faster.

---

## Build prerequisites

### Toolchain

* Use the repository Gradle Wrapper: `./gradlew ...`.
* The project requires **JDK 25**.
* Kotlin/JVM toolchains and Android compile settings are centrally defined; do not downgrade or loosen them unless the task explicitly requires it.

### GitHub Packages authentication

The project resolves snapshot `miuix` artifacts from GitHub Packages.

For local builds, credentials are expected outside the repository, typically in the global Gradle properties file:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_PERSONAL_ACCESS_TOKEN
```

The token needs `read:packages` access. CI may instead use `GITHUB_ACTOR` and `GITHUB_TOKEN`.

Never commit credentials, inline them into tracked files, or weaken the existing credential handling.

---

## Default verification

### Standard smoke build

The default repository-level verification target is the same pair used by pull request CI:

```bash
./gradlew assembleOnlinePreviewDebug assembleOfflinePreviewDebug \
  -PAPP_ID="com.rosan.installer.x.revived.test"
```

Run this when the change can reasonably affect app compilation, resources, dependency wiring, or variant-sensitive behavior.

### Report verification honestly

When summarizing work:

* state which commands were run,
* state whether they passed,
* say explicitly when verification was not run or could not be completed.

Do not imply a build or test passed unless it actually did.

---

## Gradle, variants, and dependency rules

### Flavors and build levels

The app currently uses two flavor dimensions:

* connectivity:

    * `online`
    * `offline`
* level:

    * `Unstable`
    * `Preview`
    * `Stable`

Important build behavior includes:

* connectivity-specific `INTERNET_ACCESS_ENABLED`,
* build-level-specific `BUILD_LEVEL`,
* git-hash version suffixes for unstable/preview outputs,
* an optional `VERSION_NAME` Gradle override for release automation.

Do not flatten, rename, or silently bypass flavor logic. If behavior differs by variant, make that relationship explicit.

### Dependencies

* Prefer `gradle/libs.versions.toml` for dependency and plugin version changes.
* Follow the existing version catalog naming style.
* Do not scatter raw dependency coordinates or versions across module build files without a strong reason.
* Respect the current centralized repository setup and `RepositoriesMode.FAIL_ON_PROJECT_REPOS`.
* The GitHub Packages `miuix` repository is intentionally configured in `settings.gradle.kts`; do not duplicate it in subprojects.

### Signing and release flow

* Release and debug signing behavior is handled centrally in `app/build.gradle.kts`.
* Local builds may fall back to the debug keystore when custom signing material is absent.
* Stable release packaging is automated in `.github/workflows/manual-stable-release.yml`.

Do not alter signing fallback, artifact naming, release workflow commands, version generation, or stable-release logic unless the maintainer explicitly asks for that.

---

## Architecture conventions

### Dependency injection

Use the existing Koin structure in `app/src/main/java/com/rosan/installer/di/`.

Relevant modules already include areas such as:

* core,
* device,
* engine,
* initialization,
* installer sessions,
* privileged behavior,
* serialization,
* settings,
* view models.

When introducing a new injectable dependency:

* place it in the most relevant existing module,
* avoid ad-hoc global singletons,
* keep initialization wiring explicit.

### Application initialization

`App.kt` performs important global startup work, including:

* crash handling initialization,
* hidden API exemptions on supported Android versions,
* Monet setup for older platform versions,
* conditional logging setup,
* Koin bootstrap,
* privileged auto-lock service initialization.

Treat the order and presence of this startup logic as sensitive. Do not reorder or remove initialization steps unless the task requires it and the consequences are understood.

### Domain and data boundaries

Keep business rules and platform/data access separate:

* domain models and use cases should not become Android UI utilities,
* data repositories/providers should not grow presentation logic,
* UI code should not bypass established repository/use-case boundaries when a domain path already exists.

Prefer extending the existing pattern used by the nearest comparable feature.

---

## Settings-related changes

Settings in this repository are layered and should stay that way.

### Typical settings layout

Domain side:

* `domain/settings/model/`
* `domain/settings/provider/`
* `domain/settings/repository/`
* `domain/settings/usecase/`
* `domain/settings/util/`

Data side:

* `data/settings/local/`
* `data/settings/mapper/`
* `data/settings/provider/`
* `data/settings/repository/`

### Checklist for adding or changing a setting

When a setting is persisted or exposed through app state, verify whether the change needs:

1. a domain model or state update,
2. provider/repository contract changes,
3. data-layer storage or mapping updates,
4. DI wiring updates,
5. UI state/action/view-model changes where that feature is presented,
6. both Material 3 and Miuix screen updates when both UI families expose the same setting,
7. English and Simplified Chinese string updates.

Do not implement only the visible switch while leaving persistence, mapping, or downstream behavior inconsistent.

---

## UI conventions

### Material 3 Expressive and Miuix are separate UI families

The repository keeps page implementations under distinct paths such as:

* `ui/page/main/`
* `ui/page/miuix/`

Respect that split.

Do not:

* leak Miuix-only components into Material 3 screens without intent,
* rebuild Material 3 screens with Miuix assumptions,
* change shared logic while checking only one UI family.

When a feature exists in both design systems, preserve semantic consistency while allowing implementation details to remain native to each UI family.

### Reusable UI components

For common widgets and shared components:

* keep dependencies as narrow as the existing component boundary allows,
* do not hardcode behavior that should be supplied by the caller,
* avoid locking reusable components to one screen-specific style or workflow,
* prefer composable APIs that remain extensible rather than baking product decisions into generic widgets.

Follow nearby component patterns before inventing a new style.

---

## Text, translation, and wording

### Translation policy

Per repository contribution policy:

* English and Simplified Chinese strings are maintained by developers.
* Other languages should go through Weblate rather than direct translation PRs.

### When changing user-visible strings

* Update English and Simplified Chinese together when the changed text belongs to both maintained locales.
* Preserve established product terminology unless the task is explicitly a wording cleanup.
* Keep safety caveats and compatibility warnings precise. Do not soften them just to make copy shorter.
* When text distinguishes concepts such as global authorization, profile authorization, install initiator, requester, or system limitations, keep those distinctions intact.

---

## Privileged and installation behavior

This codebase interacts with highly sensitive platform behavior, including:

* app installation flows,
* install flags and profile inheritance,
* user/all-user installation handling,
* Root, Shizuku, Dhizuku, and hidden API paths,
* ROM-specific compatibility workarounds,
* attempts to bypass OEM interception only where the project already supports that behavior.

### Rules for sensitive changes

Before changing privileged or install behavior:

1. identify the exact flow being modified: dialog, notification, automatic, profile, or system integration,
2. check whether the feature is already documented as flow-specific,
3. preserve permission boundaries and do not imply capabilities the current authorizer cannot provide,
4. avoid widening behavior from one privileged backend to another without explicit evidence,
5. keep ROM compatibility behavior explicit instead of hiding it behind vague generic logic.

If a change affects data safety, install success, system-API fallbacks, or ROM-specific behavior, explain the tradeoff clearly in the final summary.

---

## Source and API discipline

* Prefer native APIs and the repository’s existing abstractions over ad-hoc shell-command workflows.
* Reuse existing platform wrappers, providers, and repositories before adding parallel paths.
* Avoid introducing reflection, hidden API access, or privileged shortcuts where an existing maintained path already exists.
* When adding compatibility logic, keep version checks and backend checks local and readable.

---

## CI and workflow boundaries

The workflow directory contains purpose-specific automation such as:

* pull request build checks,
* preview/dev automation,
* release automation,
* CodeQL or other maintenance workflows.

When editing workflows:

* change only the workflow relevant to the request,
* preserve least-privilege token permissions unless explicitly required,
* keep package-auth requirements intact,
* do not conflate PR validation with release packaging.

Release automation deserves extra caution because it may involve version inputs, signing material, artifact renaming, and draft release creation.

---

## Recommended agent workflow

For implementation tasks, follow this order:

1. Restate the concrete behavior being changed.
2. Locate the smallest relevant area of the repository.
3. Find the nearest existing pattern and extend it.
4. Update all affected layers, not only the most visible file.
5. Run the narrowest meaningful verification, defaulting to the CI-equivalent smoke build when compilation impact is plausible.
6. Summarize:

    * what changed,
    * why this shape was chosen,
    * what was verified,
    * what remains unverified.

Prefer targeted, reviewable edits over sweeping refactors.

---

## Common mistakes to avoid

* Editing one UI family and forgetting the parallel Material 3 or Miuix surface.
* Adding a setting toggle without updating persistence or state propagation.
* Adding repositories to module Gradle files despite centralized repository management.
* Hardcoding dependency versions outside the version catalog.
* Modifying release/signing/version automation during unrelated work.
* Treating dialog-only install behavior as universally available.
* Weakening or deleting compatibility warnings from strings without understanding why they exist.
* Reordering app initialization code as a “cleanup.”
* Claiming a build passed when no verification was run.

---

## Maintainer-facing handoff format

When finishing a task, give a compact handoff that includes:

* **Changed:** files or areas updated.
* **Behavior:** what users or maintainers should expect now.
* **Verification:** commands run and result.
* **Notes:** migration concerns, unverified scenarios, or follow-up risks only when genuinely relevant.

Keep the report factual and specific.
