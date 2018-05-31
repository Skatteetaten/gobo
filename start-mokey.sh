#!/usr/bin/env bash
export STUBRUNNER_IDS="no.skatteetaten.aurora:mokey:develop-SNAPSHOT:stubs:9876"
docker run --rm \
  -e "STUBRUNNER_IDS=${STUBRUNNER_IDS}" \
  -e "STUBRUNNER_REPOSITORY_ROOT=http://aurora/nexus/service/local/repositories/snapshots/content" \
  -e "STUBRUNNER_STUBS_MODE=REMOTE" \
  -p "8083:8083" -p "9876:9876" \
  springcloud/spring-cloud-contract-stub-runner:2.0.0.RC2