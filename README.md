# Gobo

<img align="right" src="https://vignette.wikia.nocookie.net/muppet/images/5/50/Gobo-fraggle.jpg/revision/latest/scale-to-width-down/280?cb=20101220032515">

Gobo provides a GraphQL endpoint combining multiple resources from other components such as [Boober](https://github.com/Skatteetaten/boober), [Mokey](https://github.com/Skatteetaten/mokey) and [Cantus](https://github.com/Skatteetaten/cantus).

The component is named after Gobo from the TV-show Fraggle Rock (https://muppet.fandom.com/wiki/Gobo_Fraggle).

## Development

## Running gobo locally
Use `./gradlew bootRun`, this will automatically port-forward a connection to mokey. This is required to run gobo on your local machine.

For details on the port-forwarding, see script `mokey-port-forward.sh`.

## Contract tests
To run contract tests against a snapshot stub-jar file, set `gobo.stub.repository: snapshots` in `src/test/resources/application.yaml`.

## Download static playground files from graphql-kotlin
The playground files must be included to run the gobo-playground application in the browser in offline environment. 
This apply for running in secure environment e.g. test environment within Skatteetaten.

To download static playground files from graphql-kotlin, run command: `./gradlew download-playground`
