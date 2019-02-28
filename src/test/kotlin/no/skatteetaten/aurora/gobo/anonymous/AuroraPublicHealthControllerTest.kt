package no.skatteetaten.aurora.gobo.anonymous

import no.skatteetaten.aurora.gobo.ApplicationResourceBuilder
import no.skatteetaten.aurora.gobo.integration.mokey.ApplicationServiceBlocking
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(value = [AuroraPublicHealthController::class], secure = false)
class AuroraPublicHealthControllerTest {

    @MockBean
    private lateinit var applicationService: ApplicationServiceBlocking

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