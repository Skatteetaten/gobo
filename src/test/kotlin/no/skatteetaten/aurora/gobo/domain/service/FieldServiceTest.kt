package no.skatteetaten.aurora.gobo.domain.service

import no.skatteetaten.aurora.gobo.FieldConfiguration
import no.skatteetaten.aurora.gobo.domain.FieldRepository
import no.skatteetaten.aurora.gobo.domain.FieldService
import no.skatteetaten.aurora.gobo.domain.model.FieldDto
import org.assertj.core.api.JUnitSoftAssertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner

// import de.rpr.mycity.domain.city.api.CityConfig
// import de.rpr.mycity.domain.city.api.CityService
// import de.rpr.mycity.domain.city.api.dto.CreateCityDto
// import de.rpr.mycity.domain.city.api.dto.UpdateCityDto
// import de.rpr.mycity.domain.city.entity.CityEntity
// import de.rpr.mycity.domain.city.repository.CityRepository
// import de.rpr.mycity.domain.location.api.CoordinateDto
// import de.rpr.mycity.domain.location.jpa.Coordinate
// import org.assertj.core.api.Assertions.assertThat
// import org.assertj.core.api.JUnitSoftAssertions
// import org.junit.Rule
// import org.junit.Test
// import org.junit.runner.RunWith
// import org.mockito.Mockito.mock
// import org.slf4j.Logger
// import org.springframework.beans.factory.InjectionPoint
// import org.springframework.beans.factory.annotation.Autowired
// import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
// import org.springframework.context.annotation.Bean
// import org.springframework.context.annotation.Scope
// import org.springframework.test.context.ContextConfiguration
// import org.springframework.test.context.junit4.SpringRunner
// import java.time.LocalDateTime

@RunWith(SpringRunner::class)
@ContextConfiguration(
    classes = arrayOf(
//        FieldServiceTest.Config::class,
        FieldConfiguration::class
    )
)
@DataJpaTest
internal class FieldServiceTest {

//    class Config {
//
//        @Bean
//        @Scope("prototype")
//        fun logger(injectionPoint: InjectionPoint): Logger = mock(Logger::class.java)
//    }

    @Autowired
    lateinit var service: FieldService
    @Autowired
    lateinit var repository: FieldRepository

    @get:Rule
    var softly = JUnitSoftAssertions()

    @Test
    fun `'addField' should return created entity`() {
//        val (id, name, count) = service.addField(FieldDto(1, "gobo.usage.usedFields.name", 10))
        val (name, count) = service.addField(FieldDto(name = "gobo.usage.usedFields.name", count = 10))
//        softly.assertThat(id).isEqualTo(1)
        softly.assertThat(name).isEqualTo("gobo.usage.usedFields.name")
        softly.assertThat(count).isEqualTo(10)
    }
}
