package no.skatteetaten.aurora.gobo.resolvers.affiliation

/*
@Component
class AffiliationQueryResolver(
    val affiliationServiceBlocking: AffiliationServiceBlocking
) : GraphQLQueryResolver {

    fun getAffiliations(
        affiliation: String? = null,
        checkForVisibility: Boolean = false,
        dfe: DataFetchingEnvironment
    ): AffiliationsConnection {
        val affiliationNames = if (affiliation == null) {
            getAffiliations(checkForVisibility, dfe.currentUser().token)
        } else {
            listOf(affiliation)
        }

        val edges = affiliationNames.map { AffiliationEdge(Affiliation(it)) }
        return AffiliationsConnection(edges, null)
    }

    private fun getAffiliations(checkForVisibility: Boolean, token: String) = if (checkForVisibility) {
        affiliationServiceBlocking.getAllVisibleAffiliations(token)
    } else {
        affiliationServiceBlocking.getAllAffiliations()
    }
}

@Component
class AffiliationResolver(
    private val databaseService: DatabaseService,
    private val websealAffiliationService: WebsealAffiliationService
) : GraphQLResolver<Affiliation> {

    fun databaseInstances(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<DatabaseInstance> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database instances")
        return databaseService.getDatabaseInstances().filter {
            it.labels["affiliation"] == affiliation.name
        }.map {
            DatabaseInstance.create(it)
        }
    }

    fun databaseSchemas(affiliation: Affiliation, dfe: DataFetchingEnvironment): List<DatabaseSchema> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get database schemas")
        return databaseService.getDatabaseSchemas(affiliation.name).map { DatabaseSchema.create(it, affiliation) }
    }

    fun websealStates(affiliation: Affiliation, dfe: DataFetchingEnvironment): CompletableFuture<List<WebsealState>> {
        if (dfe.isAnonymousUser()) throw AccessDeniedException("Anonymous user cannot get WebSEAL states")
        return dfe.multipleKeysLoader(AffiliationDataLoader::class).load(affiliation.name)
    }
}
*/
