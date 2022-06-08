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

# This script merges all streaming-runtime operator install YAMLs into one single file: install.yaml.
# Usage:
#    ./scripts/build-streaming-runtime-operator-installer.sh
# Produces install.yaml in the same directory.

# To install (uninstall) the operator:
#    kubectl apply -f ./install.yaml --namespace streaming-runtime
#    kubectl delete -f ./install.yaml --namespace streaming-runtime

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"

readonly OUTPUT_FILE="$PROJECT_ROOT"/install.yaml

cat "$PROJECT_ROOT"/manifests/streaming-runtime-namespace.yaml > "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/crds/cluster-stream-crd.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/crds/stream-crd.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/crds/processor-crd.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/streaming-runtime-account.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/streaming-runtime-cluster-role.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/streaming-runtime-cluster-role-binding.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/streaming-runtime-namespaced-role.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/streaming-runtime-namespaced-role-binding.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"
cat "$PROJECT_ROOT"/manifests/cluster-stream-deployment.yaml >> "$OUTPUT_FILE"
printf "\n---\n" >> "$OUTPUT_FILE"

