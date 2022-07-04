package no.skatteetaten.aurora.gobo.security

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import org.junit.jupiter.api.Test

class PsatSecretReaderTest {

    @Test
    fun `Init psat throw exception given null values`() {
        assertThat { PsatSecretReader(null, PsatTokenValues()) }
            .isNotNull()
            .isFailure()
            .isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun `Get secrets given secret value`() {
        val psatSecretReader = PsatSecretReader(null, PsatTokenValues(mapOf("aud-file-a" to "abc123")))
        assertThat(psatSecretReader.secret["aud-file-a"]).isEqualTo("abc123")
    }

    @Test
    fun `Get secrets given invalid value entry`() {
        assertThat { PsatSecretReader(null, PsatTokenValues(mapOf("token-err" to "", "aud-file-a" to "abc123"))) }
            .isFailure()
            .isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun `Get secrets given secret location`() {
        val psatSecretReader = PsatSecretReader("src/test/resources/psat-token", PsatTokenValues())
        assertThat(psatSecretReader.secret).hasSize(2)
        assertThat(psatSecretReader.secret.keys).containsAll("aud-file-a", "aud-file-b")
        assertThat(psatSecretReader.secret["aud-file-a"]).isEqualTo("abc123")
    }

    @Test
    fun `Get secret given non-existing secret location throw exception`() {
        assertThat { PsatSecretReader("non-existing-path", PsatTokenValues()) }
            .isNotNull()
            .isFailure()
            .isInstanceOf(IllegalArgumentException::class)
    }
}
