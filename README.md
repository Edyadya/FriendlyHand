# Helping Hand

Built at LauzHack Lausanne.

Helping Hand connects people living on the street in Lausanne with neighbours who want to help.

- **Volunteers** meet people on the street. With the person's consent, they add them to the app with a first name, a usual area and time, a few words in their own voice and what would help. Needs can be **small** (a winter coat, a phone charger) or **big** (a money goal, for example a CHF 150 ID renewal fee).
- **Donors** browse the people in their area. For a small need they can bring the item themselves or pay CHF 20 for it. For a big goal they can give towards it. Completed goals count towards a partner voucher.
- When a donor **pays** for an item, a **volunteer buys it and brings it** to the person, then marks it delivered.
- The volunteer then confirms with the person that they really received it.

Status of a need: `OPEN → CLAIMED / FUNDING → FULFILLED → CONFIRMED`.

People are only ever shown by **area**, never at an exact spot. Only volunteers can see a recent "last seen" location.

It runs on **Android, the web (JS and Wasm) and desktop**, with an optional **Ktor server**, all from one Kotlin Multiplatform codebase.

## Demo access codes

On the login screen you pick a role and enter your name:

| Role      | Access code     |
|-----------|-----------------|
| Donor     | none            |
| Volunteer | `LAUSANNE-2026` |

The codes are shown as a hint on the login screen, because this is a hackathon demo. They're defined in one place: `core/.../model/Access.kt` (`AccessCodes`).

## Fake or Remote data

The app reads its data through `HelpRepository`. Two constants at the top of `app/shared/src/commonMain/kotlin/ch/lauzhand/helpinghand/App.kt` choose where it comes from:

```kotlin
val DATA_SOURCE = RepositoryMode.FAKE          // FAKE or REMOTE
const val SERVER_BASE_URL = "http://localhost:8081"
```

- **FAKE** (default): in-memory demo data on the device. It works offline, and each device has its own data. This is the safe choice for a demo.
- **REMOTE**: all devices share data through the Ktor server. Start the server first, then set `SERVER_BASE_URL`:
  - desktop and web on the same laptop: `http://localhost:8081`
  - Android emulator: `http://10.0.2.2:8081`
  - real phone: `http://<laptop LAN IP>:8081`, with the phone on the same Wi-Fi and port 8081 allowed by the laptop's firewall

In Remote mode the app reloads the lists every 3 seconds, so changes made on another device show up without logging in again.

## Running

Gradle commands from the project root:

| What | Command |
|---|---|
| Server (port 8081) | `./gradlew :server:run` |
| Web, JS (works in all browsers; has the real MapLibre map) | `./gradlew :app:webApp:jsBrowserDevelopmentRun` → http://localhost:8080 |
| Web, Wasm (faster, modern browsers) | `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun` → http://localhost:8080 |
| Desktop | `./gradlew :app:desktopApp:run` (hot reload: `./gradlew :app:desktopApp:hotRun --auto`) |
| Android | `./gradlew :app:androidApp:installDebug` with an emulator or phone connected (just the APK: `assembleDebug`) |

Web JS only: open http://localhost:8080/?map to see the real map preview.

### Building and testing

On a laptop with little RAM, avoid `./gradlew build`. Build only the targets you need:

```
./gradlew :server:test :app:shared:compileKotlinJvm :app:shared:compileKotlinWasmJs :app:shared:compileKotlinJs :app:androidApp:assembleDebug
```

Shared app tests: `:app:shared:jvmTest` (desktop), `:app:shared:testAndroidHostTest`, `:app:shared:wasmJsTest`, `:app:shared:jsTest`.

## Architecture

```
core/          Kotlin Multiplatform: shared by ALL apps AND the server
app/shared/    Compose Multiplatform: shared by the Android, web and desktop apps
app/androidApp, app/desktopApp, app/webApp   small entry points that call App()
server/        Ktor (JVM)
```

**`core` (commonMain)** holds the logic the apps and the server must agree on:
- `Models.kt`: the `@Serializable` data classes (`Person`, `Need`, `Zone`, `Account`…) and the request/response bodies of the API. These classes are both the app's model and the JSON format.
- `NeedRules.kt`: the status changes (`claimedBy`, `withDonation`, `fulfilled`, `confirmed`). Both `FakeRepository` and the server use them, so the rules are the same everywhere.
- `Access.kt`: the access codes and the last-seen privacy rules.
- `Seed.kt`: the demo data that both the Fake repository and the server start with.

**`app/shared` commonMain** holds everything the user sees, written once for all platforms:
- `App.kt` (data source switch) and `ui/` (`AppShell` handles login, the header and navigation; then the Login, Home, Profile, Forms, Rewards and zone-map screens).
- `data/`: `HelpRepository` (what the screens use), `FakeRepository` (in memory), `RemoteRepository` (Ktor client to the server) and `createRepository()`.

**Platform-specific code** is kept small:
- the HTTP engine for `RemoteRepository`: OkHttp on Android and desktop, the browser's fetch on JS and Wasm
- the real map (`LausanneMap`, MapLibre) on Android and web JS only. Desktop and Wasm show a placeholder there and use the drawn zone map.
- `Platform.*.kt` and the entry points in `androidApp`, `desktopApp` and `webApp`.

**`server`** keeps an in-memory store, seeded from `Seed`, and exposes a JSON API with CORS for any host:
- `POST /login` returns a token. Send it as `Authorization: Bearer <token>`.
- `GET /zones`, `/people`, `/needs`, `/donors/{id}/completed-goals`
- `POST /people`, `/people/{id}/last-seen`, `/needs`
- `POST /needs/{id}/claim | donate | fulfil | confirm`

The server checks the role for each action: volunteers add people and needs and confirm, donors claim and donate, and a volunteer marks paid items delivered, while a donor who brings an item marks it delivered themselves. Donors never receive exact last-seen locations.
