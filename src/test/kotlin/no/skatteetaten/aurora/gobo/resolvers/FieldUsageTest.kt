package no.skatteetaten.aurora.gobo.resolvers

// FIXME usage test
/*
class FieldUsageTest {
    private val usage = FieldUsage()

    @Test
    fun `Get field name from SelectionSet`() {
        val id = SelectionSet.newSelectionSet().selections(listOf(Field("id"), Field("id"))).build()
        val databaseSchema = SelectionSet.newSelectionSet().selections(listOf(Field("databaseSchema", id))).build()
        usage.update(databaseSchema)

        val fields = usage.fields
        assertThat(fields.keys).all {
            hasSize(2)
            contains("databaseSchema")
            contains("databaseSchema.id")
        }
        assertThat(fields["databaseSchema"]?.sum()).isEqualTo(1L)
        assertThat(fields["databaseSchema.id"]?.sum()).isEqualTo(2L)
    }
}
*/
