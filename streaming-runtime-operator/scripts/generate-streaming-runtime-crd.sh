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

# This script generates code for the streaming runtime CRDs
# Usage:
#    ./scripts/generate-streaming-runtime-crd.sh

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"

bash "$PROJECT_ROOT"/scripts/crds-generator.sh \
  -n com.vmware.tanzu.streaming \
  -p com.vmware.tanzu.streaming \
  -o "$PROJECT_ROOT"/streaming-runtime \
  -u "$PROJECT_ROOT"/crds/stream-crd.yaml \
  -u "$PROJECT_ROOT"/crds/cluster-stream-crd.yaml \
  -u "$PROJECT_ROOT"/crds/processor-crd.yaml 
