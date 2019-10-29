package no.skatteetaten.aurora.gobo.anonymous

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.SpringTestTag
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.security.BearerAuthenticationManager
import no.skatteetaten.aurora.gobo.security.OpenShiftAuthenticationUserDetailsService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringTestTag
@WebMvcTest(value = [AuroraPublicHealthController::class], properties = ["management.server.port=0"])
class AuroraPublicHealthControllerTest {

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

    @MockBean
    private lateinit var bearerAuthenticationManager: BearerAuthenticationManager

    @MockBean
    private lateinit var openShiftAuthenticationUserDetailsService: OpenShiftAuthenticationUserDetailsService

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `Get truesight data`() {
        given(applicationService.getApplications(emptyList())).willReturn(listOf(ApplicationResourceBuilder().build()))

        mockMvc.perform(get("/public/truesight"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].aktiv").value(true))
            .andExpect(jsonPath("$[0].registrering.tilbyderNavn").value("namespace"))
    }
}
