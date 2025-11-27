# Update Feature Unit Tests

## Overview

Comprehensive unit tests have been created to ensure the auto-update functionality works correctly
and reliably.

## Test Coverage

### 1. UpdateCheckerTest

Tests the core update checking logic:

#### Version Code Parsing

- ✅ Single digit versions (1 → 10000)
- ✅ Two digit versions (1.2 → 10200)
- ✅ Three digit versions (1.2.3 → 10203)
- ✅ Invalid format handling (fallback behavior)

#### JSON Parsing

- ✅ Valid GitHub release JSON extraction
- ✅ Missing APK file detection
- ✅ Version comparison (older vs newer)
- ✅ Malformed JSON handling

#### Data Integrity

- ✅ AppUpdate data class value retention

### 2. UpdateAutoCheckTest

Tests the automatic update check behavior:

#### Timing Logic

- ✅ Don't check if disabled
- ✅ Don't check if last check < 24 hours
- ✅ Check if last check > 24 hours
- ✅ Check if never checked before (first run)

#### Notification Logic

- ✅ Don't show if version dismissed
- ✅ Show if different version available
- ✅ Show if no version dismissed yet

#### Preference Management

- ✅ Last check time updates
- ✅ Dismissed version saves
- ✅ Auto-check enabled by default
- ✅ User can disable/enable auto-check

## Running Tests

### Run All Update Tests

```bash
./gradlew :app:testDebugUnitTest --tests "com.kema.k2look.update.*"
```

### Run Specific Test Class

```bash
./gradlew :app:testDebugUnitTest --tests "UpdateCheckerTest"
./gradlew :app:testDebugUnitTest --tests "UpdateAutoCheckTest"
```

### Run Single Test

```bash
./gradlew :app:testDebugUnitTest --tests "UpdateCheckerTest.extractVersionCode should parse three digit version"
```

## Test Results Location

After running tests, results are available at:

- **HTML Report:** `app/build/reports/tests/testDebugUnitTest/index.html`
- **XML Results:** `app/build/test-results/testDebugUnitTest/`

## CI Integration

These tests run automatically in the CI pipeline as part of:

```yaml
./gradlew :app:testDebugUnitTest --continue
```

## Test Structure

### UpdateCheckerTest

```kotlin
-setup() - Initialize mocks
        -extractVersionCode tests (4 tests)
-parseReleaseJson tests (5 tests)
-AppUpdate data class test(1 test)
```

### UpdateAutoCheckTest

```kotlin
-setup() - Initialize mocks
        -Timing logic tests(4 tests)
-Notification logic tests(3 tests)
-Preference management tests(4 tests)
```

## Key Test Scenarios

### 1. Version Code Calculation

Ensures semantic versioning is properly converted to integer codes:

```
"1" → 10000
"1.2" → 10200
"1.2.3" → 10203
```

### 2. 24-Hour Check Interval

Verifies auto-check respects the once-per-day limit:

```
Last check: 12 hours ago → Don't check
Last check: 48 hours ago → Check
Last check: Never (0) → Check
```

### 3. Dismissed Version Tracking

Ensures users aren't repeatedly notified about dismissed updates:

```
Dismissed: "1.5", Available: "1.5" → No notification
Dismissed: "1.4", Available: "1.5" → Show notification
Dismissed: null, Available: "1.5" → Show notification
```

### 4. JSON Parsing Robustness

Handles various GitHub API response scenarios:

```
✅ Valid JSON with APK → Extract update info
❌ Valid JSON, no APK → Return null
❌ Old version → Return null
❌ Malformed JSON → Return null (graceful fail)
```

## Mocking Strategy

Tests use **MockK** for:

- Context mocking
- PreferencesManager mocking
- BuildConfig value overriding
- Isolated unit testing

## What's NOT Tested

The following are integration/UI tests, not unit tests:

- ❌ Actual network calls to GitHub API
- ❌ APK download process
- ❌ System installer invocation
- ❌ UI dialog rendering
- ❌ Notification display
- ❌ FileProvider functionality

These require instrumented tests or manual testing on real devices.

## Adding New Tests

### For New Version Formats

```kotlin
@Test
fun `extractVersionCode should handle new format`() {
    val result = versionCode.invoke(checker, "2.0.0-beta1") as Int
    assertEquals(20000, result)
}
```

### For New Business Logic

```kotlin
@Test
fun `should skip prerelease versions by default`() {
    val update = AppUpdate(
        version = "1.5-beta",
        isPrerelease = true,
        // ... other fields
    )

    val shouldShow = !update.isPrerelease
    assertTrue(shouldShow)
}
```

## Best Practices

1. **Test One Thing** - Each test validates a single behavior
2. **Clear Names** - Test names describe what they verify
3. **Arrange-Act-Assert** - Standard test structure
4. **Mock External Dependencies** - No real network/file I/O
5. **Verify Edge Cases** - Test boundary conditions

## Continuous Improvement

### Current Coverage

- ✅ Version parsing logic
- ✅ JSON parsing
- ✅ Timing logic
- ✅ Preference management
- ✅ Notification conditions

### Future Additions

- [ ] Network error scenarios
- [ ] Timeout handling
- [ ] Rate limiting behavior
- [ ] Multiple APK assets handling
- [ ] Prerelease filtering options

## Troubleshooting

### Tests Fail to Compile

```bash
# Ensure all dependencies are synced
./gradlew :app:clean
./gradlew :app:build
```

### MockK Errors

```bash
# Check that mockk is in testImplementation dependencies
# Should be in app/build.gradle.kts
```

### BuildConfig Mocking Issues

```kotlin
// Use mockkObject and every to override BuildConfig
mockkObject(BuildConfig)
every { BuildConfig.VERSION_CODE } returns 10000
```

## Summary

✅ **20+ unit tests** covering critical update functionality  
✅ **Automated CI execution** on every commit  
✅ **Clear test names** describing behavior  
✅ **Comprehensive scenarios** including edge cases  
✅ **Isolated testing** with mocks  
✅ **Documentation** for maintenance

These tests ensure the auto-update feature is reliable, handles errors gracefully, and behaves
predictably across different scenarios.

