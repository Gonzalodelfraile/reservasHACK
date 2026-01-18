# ReservasHack AI Coding Instructions

## Project Overview
ReservasHack is an Android/Kotlin Compose app for managing library table reservations. It integrates with a "TakeASpot" booking service (hosted at a custom API endpoint) and uses Firebase Auth + Firestore for multi-account session management.

**Key Tech Stack:**
- Android (API 26+), Kotlin 1.9.22, Jetpack Compose
- Hilt for dependency injection (KAPT required)
- Retrofit + OkHttp for API networking
- Firebase Auth + Firestore for user accounts
- Jsoup for HTML parsing (bookings page scraping)

## Architecture Pattern: Clean Architecture + Repository Pattern

### Layer Structure
```
domain/           # Business logic, interfaces, domain models
  ├── model/      # Pure Kotlin data classes (no dependencies)
  ├── repository/ # Interfaces defining contract
  └── usecase/    # Optional business logic wrappers

data/             # Implementation layer, integrates external sources
  ├── remote/     # API clients (TakeASpotApi, DTOs, Retrofit)
  ├── local/      # Encrypted session storage
  └── repository/ # Implementations (@Inject constructor patterns)

ui/               # Presentation layer
  ├── screens/    # Composable screens + ViewModels
  ├── navigation/ # Hilt + NavController setup
  ├── shared/     # Cross-screen event coordination
  └── theme/      # Material3 styling
```

### Data Flow: API → Repository → ViewModel → Composable
1. **TakeASpotApi** (Retrofit interface) makes HTTP calls with multipart/form-data
2. **LibraryRepositoryImpl** wraps API with error handling + domain model mapping
3. **HomeViewModel/BookingsViewModel** expose `State` objects for Composables
4. **Screens** observe state via `collectAsState()` and call viewModel functions

## Critical Patterns

### 1. ViewModel State Management
Use **mutableStateOf** for UI state (not StateFlow):
```kotlin
var state by mutableStateOf<HomeState>(HomeState.Loading)
    private set  // Force callers to use functions, not direct assignment
```
Prefer sealed classes for state variants: `Loading | Success | Error`

### 2. Repository Error Handling
Return `Result<T>` type:
```kotlin
override suspend fun getLibraryInfo(): Result<LibraryService> {
    return try {
        // API call
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 3. Hilt Dependency Injection
- Use `@HiltViewModel` on ViewModels with `@Inject` constructor
- Use `@Inject` on repositories; bind interfaces in `RepositoryModule`
- Firebase instances provided by `FirebaseModule` as `@Singleton`
- Multipart API calls need `RequestBody` wrapping via `.toRequestBody()`

### 4. Multi-Account Session Management
- **AccountRepository** (Firebase Firestore) stores `UserAccount` list per authenticated user
- **SessionRepository** (encrypted SharedPreferences) holds active session cookies
- Both are injected into repositories that need them
- **SharedEventViewModel** broadcasts `reloadDataEvent` when switching accounts

### 5. Result Types & Optional Values
- API responses use `Response<T>` from Retrofit (check `isSuccessful` + `body()`)
- Domain models omit optional fields; use nullable types for absence
- Always validate before using: `response.body() ?: return failure`

## Common Developer Workflows

### Building & Running
```bash
# Build (watch mode not configured; single builds only)
./gradlew build

# Debug APK for testing
./gradlew assembleDebug

# Run on connected device
./gradlew installDebug
adb shell am start -n edu.ucam.reservashack/.MainActivity
```

### Testing
- Tests exist in `src/test/` and `src/androidTest/`
- Use `androidx.test.runner.AndroidJUnitRunner` for instrumented tests
- No test coverage setup documented; write tests targeting repository/ViewModel logic

### Debugging Network Calls
- OkHttp logging interceptor enabled; logs visible in Logcat
- Retrofit responses logged; check `isSuccessful` first
- API endpoint hardcoded in `TakeASpotApi` base URL (configured at build time)

## Integration Points & External Dependencies

### Firebase Setup
- Configured via `google-services.json` (not in source; provided by build)
- Auth: Email/password + custom session cookies (dual mechanism)
- Firestore: `users/{uid}/accounts/{docId}` structure for account list
- Analytics enabled but not actively used in code

### TakeASpot API
- Endpoint pattern: `/myturner/api/{operation}` (e.g., `/myturner/api/get-services`)
- Multipart requests: people, date, hour, service, table IDs as text/plain RequestBody
- Response parsing: Mix of JSON (services/slots) and HTML (bookings page)
- Jsoup parses HTML table for booking extraction

### HTTP Session Management
- OkHttp managed cookies automatically (CookieJar)
- Session tied to login flow + stored in encrypted SessionRepository
- Active account context passed to API calls implicitly (session headers)

## Key Files & Their Responsibilities

| File | Purpose |
|------|---------|
| [BookingApp.kt](app/src/main/java/edu/ucam/reservashack/BookingApp.kt) | Hilt app initialization |
| [MainActivity.kt](app/src/main/java/edu/ucam/reservashack/MainActivity.kt) | Entry point, Firebase Auth listener |
| [LibraryRepository.kt](app/src/main/java/edu/ucam/reservashack/domain/repository/LibraryRepository.kt) | Interface for booking operations |
| [LibraryRepositoryImpl.kt](app/src/main/java/edu/ucam/reservashack/data/repository/LibraryRepositoryImpl.kt) | API integration + domain mapping |
| [AccountRepository.kt](app/src/main/java/edu/ucam/reservashack/domain/repository/AccountRepository.kt) | Multi-account Firestore interface |
| [MyBookingsViewModel.kt](app/src/main/java/edu/ucam/reservashack/ui/screens/mybookings/MyBookingsViewModel.kt) | Booking list + cancellation logic |
| [HomeViewModel.kt](app/src/main/java/edu/ucam/reservashack/ui/screens/home/HomeViewModel.kt) | Table search/reservation logic |
| [TakeASpotApi.kt](app/src/main/java/edu/ucam/reservashack/data/remote/TakeASpotApi.kt) | Retrofit interface (all API endpoints) |
| [FirebaseModule.kt](app/src/main/java/edu/ucam/reservashack/di/FirebaseModule.kt) | Singleton Firebase instances |
| [AppNavigation.kt](app/src/main/java/edu/ucam/reservashack/ui/navigation/AppNavigation.kt) | BottomNav + HorizontalPager setup |

## Conventions & Gotchas

### Kotlin/Compose Specifics
- Extensions for `.toRequestBody()`, `.await()` (coroutine suspending)
- `mutableStateOf` assignments trigger recomposition; setter is private
- Prefer `launch {}` in `viewModelScope` for async work (auto-cancels on ViewModel clear)
- `collectAsState()` default values required (e.g., `collectAsState(initial = emptyList())`)

### API-Specific
- Multipart boundaries auto-handled by Retrofit
- HTML bookings response requires Jsoup parsing (not JSON)
- Service ID 845 is the Murcia library (hardcoded fallback in code)
- Date format: "YYYY-MM-DD"; time format: "HH:MM-HH:MM"

### Compose UI Patterns
- Bottom navigation via `HorizontalPager` (swipe-enabled tabs)
- Screens use `hiltViewModel()` to inject ViewModels
- No NavGraph/NavController for main tabs (Pager handles it)
- WebView for add-account flow uses separate navigation route

### Potential Issues
- Hilt kapt annotation processing must run before compilation
- Firebase requires valid `google-services.json` at build time
- OkHttp cookie jar assumes persistent session (test with multiple accounts carefully)
- HTML parsing brittle if TakeASpot page structure changes

## When Adding Features
1. **New API endpoint?** Add to `TakeASpotApi` interface + call in RepositoryImpl
2. **New data model?** Define in `domain/model/`, create DTO in `data/remote/dto/`, add `toDomain()` mapper
3. **New screen?** Create ViewModel with `@HiltViewModel`, bind repo in `RepositoryModule`, add route in `AppNavigation.kt`
4. **Cross-screen communication?** Use `SharedEventViewModel` (scoped to SingletonComponent for lifecycle simplicity)
5. **Account-specific data?** Store in Firestore under `users/{uid}/accounts/{docId}`, listen via `AccountRepository.getActiveAccountId()`

---

**Last Updated:** January 16, 2025  
**Target Kotlin:** 1.9.22 | **Target Compose:** 2024.02.00 BOM
