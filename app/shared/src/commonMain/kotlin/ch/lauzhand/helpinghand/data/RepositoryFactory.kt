package ch.lauzhand.helpinghand.data

enum class RepositoryMode { FAKE, REMOTE }

/**
 * The one place that decides where the data comes from.
 * FAKE: in-memory demo data, works offline. REMOTE: the Ktor server (start it with :server:run).
 * Android emulator: pass baseUrl = "http://10.0.2.2:8081".
 */
fun createRepository(
    mode: RepositoryMode = RepositoryMode.FAKE,
    baseUrl: String = RemoteRepository.DEFAULT_BASE_URL,
): HelpRepository = when (mode) {
    RepositoryMode.FAKE -> FakeRepository()
    RepositoryMode.REMOTE -> RemoteRepository(baseUrl)
}
