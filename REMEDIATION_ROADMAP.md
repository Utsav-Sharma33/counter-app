# Counter App Remediation Roadmap

**Repository:** `Utsav-Sharma33/counter-app`  
**Target branch:** `main`  
**Baseline commit:** `3bc35230994f45bb2634d4919cc17f39e4e27133`  
**Audit date:** 2026-11-11  
**Status:** Phase 1 and Phase 2 implementation committed; build and test execution pending

## Purpose

Track the code, reliability, build, testing, security, and release-readiness work required to make the Android counter application maintainable and production-ready. Work should proceed in the order below. Each module should be completed with its implementation and tests in one focused commit. Do not mark an item complete until its acceptance criteria have been verified.

## Audit summary

The repository is a Kotlin Android application using Jetpack Compose, Room, coroutines, and notification actions. The source tree has no test source files or checked-in Gradle wrapper files. The project uses a CI-generated wrapper approach; the workflow must install a pinned Gradle distribution and generate the wrapper before invoking it.

### Confirmed findings from source inspection

1. **CI Gradle bootstrap:** The absence of `gradlew`, `gradlew.bat`, and `gradle/wrapper/` from the repository is intentional under the CI-generated-wrapper approach. However, the workflow invoked `./gradlew` without first installing Gradle and generating those files. Historical Actions run #4 confirms failure at `chmod +x ./gradlew`.
2. **Compose compiler configuration:** Kotlin is configured at 2.0.0, while the app configures `kotlinCompilerExtensionVersion = "2.0.0"`. The project should use the Kotlin 2.x Compose compiler Gradle plugin consistently rather than retaining the legacy compiler-extension configuration.
3. **Database data-loss risk:** Room uses `fallbackToDestructiveMigration()` with database version 1 and no migrations.
4. **Counter concurrency:** decrement is implemented as a read followed by a separate update. The database should enforce the non-negative invariant atomically. Increment also needs an explicit overflow policy.
5. **Notification receiver lifecycle:** `BroadcastReceiver` launches work in a receiver-owned coroutine scope without `goAsync()`. The process may be reclaimed after `onReceive()` returns.
6. **Notification lifecycle:** the inspected activity does not publish a notification when counters are created or selected, while `onDestroy()` calls `dismissAllNotifications()`. This can remove notifications during ordinary activity destruction or configuration changes.
7. **Backup configuration:** `data_extraction_rules.xml` contains a `domain-config` root with domain entries. Android's data-extraction rules require the documented `data-extraction-rules` structure with cloud-backup and device-transfer rules. Review both backup XML files together and test the resulting behavior.
8. **Validation consistency:** duplicate-name checks are in the ViewModel only, so concurrent operations and non-UI callers can bypass them. No database uniqueness constraint exists.
9. **Test coverage:** no test source files were present in the inspected repository tree despite test dependencies being declared.
10. **CI release trigger:** the workflow's release step checks for a tag ref, but the workflow trigger only includes branch pushes, pull requests, and manual dispatch. Tag pushes therefore do not trigger this workflow as written.
11. **Release configuration:** release minification is enabled, but no release signing configuration is present. Play Store distribution needs a secure signing and artifact process.

## Remediation principles

- Preserve existing user data. Never use destructive migrations as a normal upgrade path.
- Enforce important invariants at the database boundary, not only in UI validation.
- Keep database and other blocking work off the main thread.
- Use lifecycle-aware collection for UI state.
- Keep notification actions reliable under process death and Android background-execution limits.
- Add tests alongside each implementation change.
- Do not claim a build or test passes unless it has actually been run and its result inspected.
- Keep secrets, signing keys, and passwords out of source control.
- Prefer small, reviewable commits, one completed module per commit.
- CI workflows are not to be run as part of this roadmap unless explicitly requested. Workflow configuration can be corrected as a code change and reviewed statically.

## Prioritized work plan

### Phase 1 — Build and project foundation

#### 1.1 Bootstrap the Gradle wrapper in CI
**Files:** `.github/workflows/build.yml` (generated wrapper files remain in the runner workspace)

Tasks:
- Install a pinned Gradle distribution using the Gradle setup action.
- Generate the wrapper during the workflow before any `./gradlew` command.
- Pin the wrapper distribution to Gradle 8.4, which is the required baseline for the current Android Gradle Plugin 8.3.0 configuration.
- Keep lint, test, and APK build commands using the generated wrapper.
- Do not require generated wrapper files to be committed to the repository unless the project later changes its wrapper policy.

Acceptance:
- CI installs Gradle 8.4 and generates `gradlew` plus `gradle/wrapper/` before attempting to execute them.
- The generated wrapper uses the pinned distribution.
- A clean build is verified in an available local build environment before marking complete.

#### 1.2 Align Kotlin and Compose compiler
**Files:** `build.gradle.kts`, `app/build.gradle.kts`, optionally `settings.gradle.kts`

Tasks:
- Apply the Kotlin Compose compiler plugin at the same Kotlin version.
- Remove the legacy `composeOptions.kotlinCompilerExtensionVersion` setting if using the Kotlin 2.x plugin.
- Confirm plugin and dependency versions are mutually compatible.
- Remove duplicate or unused dependencies after checking actual imports.

Acceptance:
- Plugin configuration is internally consistent.
- Debug and release compilation are verified.
- No dependency is upgraded solely on the basis of an old version-audit document; check compatibility first.

#### 1.3 Correct backup and manifest configuration
**Files:** `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/AndroidManifest.xml`

Tasks:
- Replace the malformed data-extraction XML with the documented Android structure.
- Define intentional cloud-backup and device-transfer inclusion/exclusion rules.
- Verify whether the database should be backed up and ensure restore behavior is coherent.
- Review notification permission handling for supported Android versions.

Acceptance:
- XML resources parse and pass Android resource validation.
- Backup and restore behavior is documented and tested on supported API levels.
- No unintended application data is included or excluded.

### Phase 2 — Data integrity and domain rules

#### 2.1 Safe Room migration strategy
**Files:** `app/src/main/kotlin/com/example/counter/data/CounterDatabase.kt`, future migration files under `data/migrations/`, `app/build.gradle.kts`

Tasks:
- Remove `fallbackToDestructiveMigration()`.
- Export Room schemas and check them into the repository.
- Add explicit migrations for every schema change.
- Define a deliberate policy for unknown downgrade paths.
- Add migration tests using Room migration testing utilities.

Acceptance:
- Existing counter rows survive supported upgrades.
- Migration tests cover each registered migration.
- No normal upgrade path silently deletes user data.

#### 2.2 Atomic counter operations and numeric bounds
**Files:** `data/CounterDao.kt`, `domain/CounterRepository.kt`, `data/CounterEntity.kt`

Tasks:
- Make decrement a single conditional SQL update with `count > 0`.
- Define behavior at the maximum supported count and prevent integer overflow.
- Check affected-row counts where callers need to distinguish missing counters from unchanged values.
- Ensure timestamps are updated only when a mutation actually occurs.
- Consider whether `Int` is sufficient or whether the persisted count should be `Long`; if changed, include a migration.

Acceptance:
- Concurrent decrements never produce a negative count.
- Concurrent increments follow a documented overflow policy.
- Missing IDs do not appear to have been successfully updated.
- DAO tests cover boundaries and concurrent calls.

#### 2.3 Centralize validation and uniqueness
**Files:** `data/CounterEntity.kt`, `data/CounterDao.kt`, `domain/CounterRepository.kt`, `ui/CounterViewModel.kt`, database migration/schema files

Tasks:
- Enforce trimmed, non-empty names in the domain/repository layer.
- Decide whether counter names are globally unique and case-insensitive; current UI behavior implies that they are.
- If that rule is retained, implement a normalized name key or another database-safe uniqueness strategy and migrate existing data safely.
- Handle uniqueness conflicts as a typed domain result rather than relying on a ViewModel pre-check.
- Set a consistent maximum name length in one place.

Acceptance:
- Duplicate creation and rename attempts are rejected even when concurrent.
- Validation is applied to all callers, not only Compose UI.
- Existing duplicate names are handled by a documented migration policy.

#### 2.4 Repository behavior and error model
**Files:** `domain/CounterRepository.kt`, new domain result/error types under `domain/`

Tasks:
- Define explicit outcomes for invalid input, duplicate names, missing counters, and storage failures.
- Avoid swallowing meaningful mutation outcomes.
- Keep Android UI concerns out of the repository.
- Ensure repository methods have deterministic contracts.

Acceptance:
- Repository unit tests cover success and failure outcomes.
- Callers can distinguish validation errors from unexpected storage errors.

### Phase 3 — Notification reliability

**Implementation status (2026-11-11):** Receiver lifecycle and tracked-notification synchronization have been implemented and committed. Receiver action unit tests and notification tracking instrumentation tests have been added. Build, unit-test, instrumentation-test, permission-flow, and physical-device validation remain pending. CI has not been run.

#### 3.1 Receiver execution lifecycle
**Files:** `notification/CounterActionReceiver.kt`, potentially a worker under `notification/` or `worker/`, `AndroidManifest.xml`

Tasks:
- Remove the receiver-owned, long-lived coroutine scope.
- Use `goAsync()` with a bounded coroutine and always call `PendingResult.finish()` in a `finally` block, or move durable work to an appropriate WorkManager job if the operation needs deferral/retries.
- Use an IO dispatcher for database work.
- Validate action strings and counter IDs.
- Handle deleted counters and failures without crashing or leaving stale notifications.

Acceptance:
- Receiver work completes reliably within the receiver execution window.
- Pending results are always finished.
- Tests cover unknown actions, invalid IDs, deleted rows, and repository failures.

#### 3.2 Notification lifecycle and state synchronization
**Files:** `notification/CounterNotificationManager.kt`, `ui/CounterViewModel.kt`, `MainActivity.kt`, relevant UI callbacks

Tasks:
- Define when a counter notification should be created, updated, and dismissed.
- Publish/update notifications after relevant counter mutations and when the user explicitly enables tracking for a counter.
- Do not dismiss all notifications from `Activity.onDestroy()`.
- Dismiss only the relevant counter notification when that counter is deleted or tracking is disabled.
- Ensure notification IDs and PendingIntent request codes do not collide.
- Keep notification permission checks and user-facing permission requests in the appropriate activity/UI layer.
- Review notification channel importance and ongoing-notification behavior.

Acceptance:
- Notification count always reflects persisted counter state after an action.
- Activity recreation does not unexpectedly cancel notifications.
- Deleting one counter does not cancel unrelated counter notifications.
- Permission denial does not crash the app.

#### 3.3 Notification action security and UX
**Files:** `CounterNotificationManager.kt`, `CounterActionReceiver.kt`, `AndroidManifest.xml`, `res/values/strings.xml`

Tasks:
- Keep the receiver non-exported unless a documented external integration requires otherwise.
- Use explicit intents and stable, collision-safe PendingIntent identities.
- Use localized string resources for notification labels and action descriptions.
- Ensure notification actions remain correct when multiple counters exist.

Acceptance:
- Actions affect only the intended counter.
- Multiple counters can have simultaneous notifications without PendingIntent cross-talk.
- Notification content is accessible and understandable.

### Phase 4 — ViewModel and Compose architecture

#### 4.1 ViewModel state and operation handling
**Files:** `ui/CounterViewModel.kt`, new UI state/event types under `ui/`

Tasks:
- Use a single well-defined UI state or clearly separated StateFlows with documented semantics.
- Collect repository flows with lifecycle-aware coroutine patterns.
- Avoid setting a global loading flag for every small mutation if it blocks the whole screen.
- Prevent loading state from remaining stuck after errors or cancellation.
- Map domain errors to safe, user-readable messages.
- Ensure selected counter state is cleared or updated when the selected row disappears.
- Prevent repeated taps from creating duplicate operations where necessary.

Acceptance:
- ViewModel tests cover initial load, updates, errors, selection, and concurrent user actions.
- Loading and error states are deterministic.
- Raw exception details are not exposed directly to users.

#### 4.2 Compose state and accessibility
**Files:** `ui/CounterScreen.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `res/values/strings.xml`

Tasks:
- Replace plain `collectAsState()` with lifecycle-aware collection where appropriate.
- Keep composables driven by state and callbacks instead of passing the ViewModel deep into UI components.
- Add explicit content descriptions to increment/decrement and other icon-only controls.
- Add confirmation for destructive reset and delete-all operations.
- Ensure errors are announced and remain visible long enough to be actionable.
- Review small-screen layouts, font scaling, contrast, touch target sizes, and keyboard behavior.
- Add stable keys to lazy list items.

Acceptance:
- UI tests cover create, rename, increment, decrement, reset, delete, and error flows.
- Controls are usable with TalkBack and large font settings.
- UI state survives recomposition and configuration changes where expected.

### Phase 5 — Automated testing

**Files:** `app/src/test/`, `app/src/androidTest/`, `app/build.gradle.kts`

Tasks:
- Add coroutine test utilities and a consistent test dispatcher/rule.
- Add DAO/database tests using an in-memory Room database.
- Add repository tests with a fake DAO or database-backed test.
- Add ViewModel tests for state transitions and validation.
- Add receiver/notification tests with suitable Android test doubles.
- Add Compose UI tests for primary workflows and accessibility semantics.
- Add migration tests for every schema migration.
- Configure coverage reporting and exclude generated code, resources, and external/generated sources from the denominator where justified.

Coverage target:
- At least 95% line coverage for core domain/repository logic.
- Report DAO, ViewModel, notification, and UI coverage separately.
- Do not claim 95% application-wide coverage if only domain logic meets the target.

Acceptance:
- Tests are deterministic and do not depend on network access or wall-clock timing.
- Coverage report is generated and reviewed.
- Every bug fix includes a regression test where feasible.

### Phase 6 — Release and operational readiness

#### 6.1 Release build and signing
**Files:** `app/build.gradle.kts`, `gradle.properties`, `.gitignore`, CI workflow and repository secret configuration

Tasks:
- Add a release signing configuration that reads credentials from environment variables or CI secrets.
- Never commit keystores, passwords, or signing credentials.
- Verify R8/minification rules against Room and Kotlin serialization/reflection use, if applicable.
- Produce reproducible release artifacts and document versionCode/versionName increments.

Acceptance:
- Release artifact is signed with the intended key.
- No secret appears in Git history or build logs.
- Minified release build is installed and smoke-tested.

#### 6.2 CI workflow correctness
**File:** `.github/workflows/build.yml`

Tasks:
- Keep wrapper-based build commands.
- Add tag push trigger if automated GitHub Releases are intended, or remove the unreachable tag-only release step.
- Use least-privilege workflow permissions.
- Pin third-party actions to reviewed versions or immutable SHAs according to project policy.
- Upload artifacts only after successful build steps.
- Make the final status message reflect actual job success/failure rather than always printing “Build completed”.

Acceptance:
- Workflow triggers match the documented release process.
- Failure states are not reported as successful.
- Build, lint, and tests are independently visible.

#### 6.3 Dependency and SDK maintenance
**Files:** `build.gradle.kts`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`

Tasks:
- Review current stable versions and compatibility as a coordinated upgrade, not by blindly following the February 2026 version report.
- Remove unused DataStore, Timber, LiveData, or duplicate dependencies if no code requires them.
- Review targetSdk and compileSdk against current Android/Play requirements at release time.
- Add dependency update documentation or automation only if desired.

Acceptance:
- Dependency versions are compatible and reproducible.
- No unused dependencies remain without justification.
- Target SDK policy is documented.

## File-by-file tracking checklist

| File or path | Work required | Priority | Status |
|---|---|---:|---|
| `.github/workflows/build.yml` | Install pinned Gradle and generate wrapper before use | P0 | Implemented; CI intentionally not run |
| `build.gradle.kts` | Align Kotlin/Compose plugin versions | P0 | Implemented; build validation pending |
| `app/build.gradle.kts` | Compose compiler plugin configuration | P0 | Implemented; build validation pending |
| `settings.gradle.kts` | Verify plugin and dependency repositories | P2 | [ ] |
| `gradle.properties` | Review obsolete/unsupported flags | P2 | [ ] |
| `app/src/main/res/xml/data_extraction_rules.xml` | Correct Android data extraction schema | P0 | Implemented; Android runtime validation pending |
| `app/src/main/res/xml/backup_rules.xml` | Align legacy backup inclusion rules | P1 | Implemented; Android runtime validation pending |
| `app/src/main/AndroidManifest.xml` | Permission, receiver, backup and activity config | P1 | [ ] |
| `data/CounterDatabase.kt` | Remove destructive fallback; schema export/migrations | P0 | Implemented; schema export/build validation pending |
| `data/CounterEntity.kt` | Count/name constraints and normalized unique key | P1 | Implemented; build validation pending |
| `data/CounterDao.kt` | Atomic updates, uniqueness, affected-row results | P0 | Implemented; instrumentation execution pending |
| `domain/CounterRepository.kt` | Validation, typed outcomes, concurrency contracts | P1 | Implemented; unit-test execution pending |
| `notification/CounterActionReceiver.kt` | Bounded background execution and error handling | P0 | Implemented; tests added, execution pending |
| `notification/CounterNotificationManager.kt` | Notification lifecycle, tracking state, and PendingIntent identity | P0 | Implemented; runtime validation pending |
| `MainActivity.kt` | Remove destructive notification cleanup; permission flow and notification deep link | P1 | Implemented; runtime validation pending |
| `ui/CounterViewModel.kt` | State, error mapping, loading and selection | P1 | Updated for typed repository outcomes; further Phase 4 work remains |
| `ui/CounterScreen.kt` | Lifecycle state, callbacks, accessibility, UX | P2 | [ ] |
| `ui/theme/Theme.kt`, `ui/theme/Type.kt` | Accessibility and visual consistency review | P3 | [ ] |
| `res/values/strings.xml` | Move user-facing strings to resources | P2 | [ ] |
| `app/proguard-rules.pro` | Validate rules against minified release | P2 | [ ] |
| `.github/workflows/build.yml` | Wrapper, tag trigger, permissions, truthful status | P2 | [ ] |
| `app/src/test/**` | Repository unit tests | P0 | Added; execution pending |
| `app/src/androidTest/**` | Room migration and atomic DAO tests | P0 | Added; execution pending |

## Suggested commit sequence

1. `build: bootstrap pinned Gradle in CI and align Compose compiler`
2. `fix: correct Android backup and data extraction rules`
3. `fix(database): add safe migrations and atomic counter updates`
4. `fix(domain): centralize counter validation and uniqueness`
5. `fix(notifications): make receiver execution lifecycle-safe`
6. `fix(notifications): synchronize notification lifecycle with counter state`
7. `refactor(viewmodel): make UI state and errors deterministic`
8. `refactor(ui): improve lifecycle collection and accessibility`
9. `test: add database and repository regression coverage`
10. `test: add ViewModel notification and Compose UI coverage`
11. `build(release): harden signing and CI release workflow`
12. `chore: update dependencies and document release process`

A commit may be split further if a module is too large, but implementation and its relevant tests should stay together. Do not combine unrelated modules just to reduce the commit count.

## Completion definition

The application is considered ready for release only when:

- [ ] A clean debug build succeeds using the checked-in Gradle wrapper.
- [ ] A minified release build succeeds and is installable.
- [ ] Database upgrades preserve user data.
- [ ] Concurrent counter operations preserve all invariants.
- [ ] Notification actions work with multiple counters and under background execution.
- [ ] Backup/restore behavior is verified.
- [ ] Core business logic reaches at least 95% line coverage with a reviewed report.
- [ ] Critical UI flows and accessibility have been tested.
- [ ] Release signing is configured without committed secrets.
- [ ] CI workflow behavior matches the documented build/release process.
- [ ] Documentation reflects the actual implementation, not aspirational claims.

## Audit limitations

This roadmap is based on static inspection of the repository's `main` branch at the baseline commit listed above. No Gradle build, lint, unit test, emulator test, physical-device test, or backup/restore test was executed during the audit. Findings that depend on runtime behavior must be confirmed during their corresponding remediation phase.
