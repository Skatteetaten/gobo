package no.skatteetaten.aurora.gobo.anonymous

// FIXME aurora public health controller
/*
@WebMvcTest(value = [AuroraPublicHealthController::class], properties = ["management.server.port=0"])
class AuroraPublicHealthControllerTest {

    @MockkBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockkBean
    private lateinit var bearerAuthenticationManager: BearerAuthenticationManager

    @MockkBean
    private lateinit var openShiftAuthenticationUserDetailsService: OpenShiftAuthenticationUserDetailsService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `Get truesight data`() {
        every { applicationService.getApplications(emptyList()) } returns listOf(ApplicationResourceBuilder().build())

        mockMvc.perform(get("/public/truesight"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].aktiv").value(true))
            .andExpect(jsonPath("$[0].registrering.tilbyderNavn").value("namespace"))
    }
}
*/
