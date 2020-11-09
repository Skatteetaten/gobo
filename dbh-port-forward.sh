#!/bin/bash
echo "Add \"integrations.dbh.url=http://localhost:9090\" to run configurations"
oc port-forward $(oc get pods -o custom-columns=POD:.metadata.name --no-headers | grep dbh) 9090:8080