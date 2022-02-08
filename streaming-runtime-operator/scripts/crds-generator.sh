#!/bin/bash

# Copyright 2020 The Kubernetes Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This utility script orchestrates the code generation procedure. It is executed inside a crdgen
# container in order to minimize the environment dependencies on the host, being Docker only.

# NOTE: use the generate-streaming-runtime-crd.sh to generate the Streaming Runtime java models.

OUTPUT_DIR=${OUTPUT_DIR:-}
KUBERNETES_CRD_GROUP_PREFIX=${KUBERNETES_CRD_GROUP_PREFIX:-}
PACKAGE_NAME=${PACKAGE_NAME:-}
CLIENT_GEN_DIR="/tmp/kubernetes-client-gen"
CURRENT_KUBECTL_CONTEXT=$(kubectl config current-context)

print_usage() {
  echo "Usage: generate Java model classes from CRDs" >& 2
  echo " -n: the prefix of the target CRD's api group to generate." >& 2
  echo " -p: the base package name of the generated java project. " >& 2
  echo " -o: output directory of the generated java project. " >& 2
  echo " -u: url location of the YAML manifest to install CRDs to a Kubernetes cluster. " >& 2
}

while getopts 'u:n:p:o:' flag; do
  case "${flag}" in
    u) CRD_URLS+=("${OPTARG}") ;;
    n) KUBERNETES_CRD_GROUP_PREFIX="${OPTARG}" ;;
    p) PACKAGE_NAME="${OPTARG}" ;;
    o) OUTPUT_DIR="${OPTARG}" ;;
    *) print_usage
       exit 1 ;;
  esac
done

set -e

# create a KinD cluster on the host
#kind create cluster
# We need to ensure the kubernetes cluster is 1.19 in order to generate API classes see https://github.com/kubernetes-client/java/issues/1710
kind create cluster --name crd-gen --image kindest/node:v1.19.11@sha256:cbecc517bfad65e368cd7975d1e8a4f558d91160c051d0b1d10ff81488f5fb06
kubectl config use-context kind-crd-gen

# install CRDs to the KinD cluster and dump the swagger spec
for url in "${CRD_URLS[@]}"; do
  if [[ ! -z $url ]]; then
    kubectl apply -f "$url"
  fi
done

sleep 5
rm -Rf /tmp/swagger
kubectl get --raw="/openapi/v2" > /tmp/swagger

echo "Verifying CRD installation.."
kubectl get crd -o name \
  | while read L
    do
      if [[ $(kubectl get $L -o jsonpath='{.status.conditions[?(@.type=="NonStructuralSchema")].status}') == "True" ]]; then
        echo "$L failed publishing openapi schema because it's attached non-structral-schema condition."
        kind delete cluster
        exit 1
      fi
      if [[ $(kubectl get $L -o jsonpath='{.spec.preserveUnknownFields}') == "true" ]]; then
        echo "$L failed publishing openapi schema because it explicitly disabled unknown fields pruning."
        kind delete cluster
        exit 1
      fi
      echo "$L successfully installed"
    done

# destroy the KinD cluster
kind delete cluster --name crd-gen
if [ -z "$CURRENT_KUBECTL_CONTEXT" ]
then
  echo "No previous Kubernetes context found!"
else
  kubectl config use-context "$CURRENT_KUBECTL_CONTEXT"
fi


rm -Rf $CLIENT_GEN_DIR
git clone https://github.com/kubernetes-client/gen ${CLIENT_GEN_DIR}|| true
cd $CLIENT_GEN_DIR/openapi

# execute the generation script
bash java-crd-cmd.sh -n "${KUBERNETES_CRD_GROUP_PREFIX}" -p "${PACKAGE_NAME}" -l 2 -o "${CLIENT_GEN_DIR}/gen" -g true < /tmp/swagger

# only keep the api and model classes
rm -Rf "${OUTPUT_DIR}/src/generated/java/${PACKAGE_NAME//.//}"
mkdir -p "${OUTPUT_DIR}/src/generated/java/${PACKAGE_NAME//.//}"
cp -r "${CLIENT_GEN_DIR}/gen/src/main/java/${PACKAGE_NAME//.//}/models" "${OUTPUT_DIR}/src/generated/java/${PACKAGE_NAME//.//}"
cp -r "${CLIENT_GEN_DIR}/gen/src/main/java/${PACKAGE_NAME//.//}/apis" "${OUTPUT_DIR}/src/generated/java/${PACKAGE_NAME//.//}"
