#!/bin/bash
echo "Add \"integrations.mokey.url=http://localhost:9999\" to run configurations"
oc project aup
oc port-forward $(oc get pods -o custom-columns=POD:.metadata.name --no-headers | grep -E "mokey-[0-9]+") 9999:8080