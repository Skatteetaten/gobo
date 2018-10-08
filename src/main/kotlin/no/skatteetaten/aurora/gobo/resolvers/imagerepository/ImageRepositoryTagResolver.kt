package no.skatteetaten.aurora.gobo.resolvers.imagerepository

import com.coxautodev.graphql.tools.GraphQLResolver
import graphql.schema.DataFetchingEnvironment
import no.skatteetaten.aurora.gobo.resolvers.loader
import org.springframework.stereotype.Component

@Component
class ImageRepositoryTagResolver : GraphQLResolver<ImageTag> {

    fun lastModified(imageTag: ImageTag, dfe: DataFetchingEnvironment) = dfe.loader(ImageTag::class).load(imageTag)
}