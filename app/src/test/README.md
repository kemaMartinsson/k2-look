# K2Look Unit Tests

## Overview

Comprehensive unit test suite for the K2Look Karoo extension application.

## Test Coverage

### 1. PreferencesManager Tests (`PreferencesManagerTest.kt`)

- ✅ Auto-connect settings (Karoo & ActiveLook)
- ✅ Last connected glasses storage
- ✅ Timeout configurations (reconnect & startup)
- ✅ Reconnect during rides settings
- ✅ Disconnect when idle settings
- ✅ Default value validation
- ✅ Value persistence

### 2. Markdown Parser Tests (`MarkdownParserTest.kt`)

- ✅ Plain text handling
- ✅ Bold text (`**text**`)
- ✅ Italic text (`*text*`)
- ✅ Headers (`<h2>text</h2>`)
- ✅ Mixed formatting
- ✅ Newline preservation
- ✅ Complex changelog parsing
- ✅ Edge cases (empty, unclosed tags)
- ✅ Special characters

### 3. MainViewModel Tests (`MainViewModelTest.kt`)

- ✅ UI state initialization
- ✅ Preference updates
- ✅ Timeout validation (1-60 range)
- ✅ Debug mode toggle
- ✅ Number formatting
- ✅ Metric data handling
- ✅ Connection state management
- ✅ Advanced metrics (HR zones, power, VAM)

## Running Tests

### Command Line

```bash
# Run all unit tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTest --info

# Run specific test class
./gradlew test --tests "com.kema.k2look.util.PreferencesManagerTest"

# Run tests with detailed output
./gradlew test --info
```

### Android Studio

1. Right-click on `app/src/test` folder
2. Select "Run 'Tests in 'test''"
3. Or right-click individual test files to run specific tests

## Test Dependencies

```kotlin
// Unit Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("androidx.arch.core:core-testing:2.2.0")
testImplementation("com.google.truth:truth:1.1.5")

// Android Instrumented Tests
androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
```

## Writing New Tests

### Test Structure

```kotlin
class MyComponentTest {

    private lateinit var component: MyComponent

    @Before
    fun setup() {
        // Initialize test dependencies
        component = MyComponent()
    }

    @After
    fun tearDown() {
        // Clean up
        unmockkAll()
    }

    @Test
    fun `test description in readable format`() {
        // Given
        val input = "test data"

        // When
        val result = component.doSomething(input)

        // Then
        assertEquals("expected", result)
    }
}
```

### Best Practices

1. **Use descriptive test names** with backticks for readability
2. **Follow Given-When-Then** structure
3. **Mock external dependencies** with MockK
4. **Test edge cases** and error handling
5. **Keep tests fast** and independent
6. **Verify behavior**, not implementation details

## Continuous Integration

Tests run automatically on:

- Every push to main branch
- Every pull request
- Before releases

## Coverage Goals

- **Target**: 80% code coverage
- **Critical paths**: 100% coverage
    - PreferencesManager
    - Markdown parser
    - ViewModel state management

## Future Test Additions

- [ ] Service layer tests (KarooDataService, ActiveLookService)
- [ ] Bridge integration tests
- [ ] Compose UI tests
- [ ] End-to-end instrumented tests
- [ ] Performance tests

## Troubleshooting

### Tests Not Running

```bash
# Clean and rebuild
./gradlew clean test
```

### Mock Issues

Make sure MockK is properly configured:

```kotlin
@Before
fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
}
```

### Coroutine Test Issues

Use `UnconfinedTestDispatcher` for immediate execution:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
```

## Resources

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [MockK Documentation](https://mockk.io/)
- [Kotlin Coroutines Test](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Android Testing Guide](https://developer.android.com/training/testing)

---

**Last Updated**: 2025-01-25
**Test Framework**: JUnit 4 + MockK
**Coverage Tool**: JaCoCo (to be configured)

