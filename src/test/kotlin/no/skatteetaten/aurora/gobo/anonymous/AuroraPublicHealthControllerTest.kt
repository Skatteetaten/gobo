package no.skatteetaten.aurora.gobo.anonymous

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.SpringTestTag
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import no.skatteetaten.aurora.gobo.security.BearerAuthenticationManager
import no.skatteetaten.aurora.gobo.security.OpenShiftAuthenticationUserDetailsService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringTestTag
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
