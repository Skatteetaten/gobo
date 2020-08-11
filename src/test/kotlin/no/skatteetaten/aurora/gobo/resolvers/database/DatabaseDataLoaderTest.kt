package no.skatteetaten.aurora.gobo.resolvers.database

/*
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class DatabaseDataLoaderTest {

    private val server = MockWebServer()
    private val dbhUrl = server.url("/").toString()
    private val sharedSecretReader = mockk<SharedSecretReader> {
        every { secret } returns "abc"
    }
    private val applicationConfig = ApplicationConfig(
        connectionTimeout = 500,
        readTimeout = 500,
        writeTimeout = 500,
        applicationName = "",
        sharedSecretReader = sharedSecretReader
    )
    private val dbhClient = applicationConfig.webClientDbh(dbhUrl, WebClient.builder())
    private val applicationService = ApplicationServiceBlocking(ApplicationService(dbhClient))
    private val dataLoader = DatabaseDataLoader(applicationService)

    @BeforeEach
    fun setUp() {
        TestObjectMapperConfigurer.objectMapper = testObjectMapper()
    }

    @AfterEach
    fun tearDown() {
        TestObjectMapperConfigurer.reset()
        server.shutdown()
    }

    @Test
    fun `Get ApplicationDeployments by database ids`() {
        val resource1 = ApplicationDeploymentWithDbResourceBuilder("123").build()
        val resource2 = ApplicationDeploymentWithDbResourceBuilder("456").build()
        val request = server.execute(listOf(resource1, resource2)) {
            val result = dataLoader.getByKeys(User("username", "token"), mutableSetOf("123", "456"))
            assertThat(result["123"]?.get()?.size).isEqualTo(1)
        }.first()

        assertThat(request?.path).isEqualTo("/api/auth/applicationdeploymentbyresource/databases")
    }
}
 */
