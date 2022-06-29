RULES.MK := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))/rules.mk
include $(RULES.MK)

# Include subdirs
#SUBDIRS := streaming-runtime-operator multibinder-grpc sql-aggregator
SUBDIRS := multibinder-grpc sql-aggregator
$(foreach dir,$(SUBDIRS),$(eval $(call INCLUDE_FILE, $(dir))))

all:

#operator:
sql-aggregator:
multibinder-grpc:

#publish-streaming-operator:
#publish-stream-data-generator:
publish-multibinder-grpc:
publish-sql-aggregator:
#publish-udf-examples:

tests:
#streaming-runtime-operator-tests:
multibinder-grpc-tests:
sql-aggregator-tests:
#stream-data-generator-tests:
#smoke-tests:

clean:

# TODO: Maybe move this to the docs folder
.PHONY: docs.clean docs.clean
docs.build:
	docker build -t streaming-runtime-site:v1 --build-arg=USER=$(shell id -u) -f $(abspath $(ROOT_DIR))/docs/Dockerfile $(abspath $(ROOT_DIR))

.PHONY: docs.build docs.serve
docs.serve:
	docker run -p 8000:8000 -v $(abspath $(ROOT_DIR))/docs:/usr/src/mkdocs/build/docs streaming-runtime-site:v1

.PHONY: docs.clean
docs.clean:
	docker rm streaming-runtime-site:v1
