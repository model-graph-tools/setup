#!/bin/bash

# Removes all resources on OpenShift

oc delete all --selector application=modelgraphtools
