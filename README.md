# Gobo

<img align="right" src="https://vignette.wikia.nocookie.net/muppet/images/5/50/Gobo-fraggle.jpg/revision/latest/scale-to-width-down/280?cb=20101220032515">

Gobo provides a GraphQL endpoint combining multiple resources from other components such as [Boober](https://github.com/Skatteetaten/boober), [Mokey](https://github.com/Skatteetaten/mokey) and [Cantus](https://github.com/Skatteetaten/cantus).

The component is named after Gobo from the TV-show Fraggle Rock (https://muppet.fandom.com/wiki/Gobo_Fraggle).

## Development

### Setup
 
In order to use this project you must set repositories in your `~/.gradle/init.gradle` file
 
     allprojects {
         ext.repos= {
             mavenCentral()
             jcenter()
         }
         repositories repos
         buildscript {
          repositories repos
         }
     }

We use a local repository for distributionUrl in our gradle-wrapper.properties, you need to change it to a public repo in order to use the gradlew command. `../gradle/wrapper/gradle-wrapper.properties`

    <...>
    distributionUrl=https\://services.gradle.org/distributions/gradle-<version>-bin.zip
    <...>

Quickly deploy to test environment (fish shell):

    ./gradlew clean build -x test;
    oc start-build gobo --from-file=(ls build/distributions/gobo-*-Leveransepakke.zip) --wait -n paas-mokey
    

## GraphQL tracing

To enable tracing: `gobo.graphql.tracing-enabled=true`


## Contract tests

To run contract tests against a snapshot stub-jar file, set `gobo.stub.repository: snapshots` in `application.yaml`.


## Download static playground files from graphql-kotlin
To download static playground files from graphql-kotlin, run command
./gradlew download-playground
