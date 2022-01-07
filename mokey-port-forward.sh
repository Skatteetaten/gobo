#!/bin/bash
echo "Add \"integrations.mokey.url=http://localhost:9999\" to run configurations"
oc project aup
oc port-forward $(oc get pods -o custom-columns=POD:.metadata.name --no-headers | grep mokey-) 9999:8080