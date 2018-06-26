# Gobo

## Development

Quickly deploy to test environment (fish shell):

    ./gradlew clean build -x test;
    oc start-build gobo --from-file=(ls build/distributions/gobo-*-Leveransepakke.zip) --wait -n paas-mokey