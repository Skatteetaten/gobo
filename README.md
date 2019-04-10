# Gobo

<img align="right" src="https://vignette.wikia.nocookie.net/muppet/images/5/50/Gobo-fraggle.jpg/revision/latest/scale-to-width-down/280?cb=20101220032515">

Gobo provides a GraphQL endpoint combining multiple resources from other components such as [Boober](https://github.com/Skatteetaten/boober), [Mokey](https://github.com/Skatteetaten/mokey) and [Cantus](https://github.com/Skatteetaten/cantus).

The component is named after Gobo from the TV-show Fraggle Rock (https://muppet.fandom.com/wiki/Gobo_Fraggle).

## Development

Quickly deploy to test environment (fish shell):

    ./gradlew clean build -x test;
    oc start-build gobo --from-file=(ls build/distributions/gobo-*-Leveransepakke.zip) --wait -n paas-mokey
    

## Filter tests

There are two tasks in the gradle script that can filter the tests: `testExclude` and `testOnly`  
By passing tags it is possible to exclude or only run the specified test tags.  
For example to skip the graphql and MockWebServer tests run the following command:  

    ./gradlew testExclude -Ptags=graphql,mockwebserver 

Tags:
* **graphql**, tests for the graphql resolvers
* **spring**, tests using the spring container
* **mockwebserver**, tests using [MockWebServer](https://github.com/square/okhttp/tree/master/mockwebserver)


## GraphQL tracing

To enable tracing: `gobo.graphql.tracing-enabled=true`


## Contract tests

To run contract tests against a snapshot stub-jar file, set `gobo.stub.repository: snapshots` in `application.yaml`.