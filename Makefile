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
