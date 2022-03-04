RULES.MK ?= ../rules.mk
include $(RULES.MK)

path ?= .
multibinder-grpc.path := $(abspath $(path))

include $(multibinder-grpc.path)/rules.mk

multibinder-grpc.sources := $(shell find $(multibinder-grpc.path) -type f -iname '*.java') \
	$(multibinder-grpc.path)/pom.xml \
	$(multibinder-grpc.path)/VERSION \

$(eval $(call VERSION_template,multibinder-grpc,$(multibinder-grpc.path)))

# The tools should always build based on the version in VERSION file.
multibinder-grpc.version := $(multibinder-grpc.version.release)

multibinder-grpc.artifact.release = $(multibinder-grpc.out_dir)/multibinder-grpc-$(multibinder-grpc.version).jar

# We want to use the branch here, it's okay as this isn't something we're pushing
multibinder-grpc.tag := $(multibinder-grpc.version.branch)
multibinder-grpc.image_name := multibinder-grpc:$(multibinder-grpc.tag)
multibinder-grpc.image := $(multibinder-grpc.build_dir)/image.$(multibinder-grpc.tag)

$(multibinder-grpc.image): $(multibinder-grpc.path)/Dockerfile $(multibinder-grpc.sources)
	@mkdir -p $(@D)
	DOCKER_BUILDKIT=1 docker build -t $(multibinder-grpc.image_name) -f $< $(multibinder-grpc.path)
	printf $(multibinder-grpc.image_name) > $@

$(multibinder-grpc.artifact.release): $(multibinder-grpc.image)
	@mkdir -p $(@D)
	docker cp `docker create $(multibinder-grpc.image_name)`:/out/$(notdir $(multibinder-grpc.artifact.release)) $@
	touch $@

multibinder-grpc.artifacts := $(multibinder-grpc.artifact.release) $(multibinder-grpc.artifact.release).sha256
multibinder-grpc.publish := $(multibinder-grpc.build_dir)/publish.$(multibinder-grpc.version)
$(multibinder-grpc.publish): $(GSUTIL) $(multibinder-grpc.artifacts)
	$(GSUTIL) cp $(multibinder-grpc.artifacts) $(multibinder-grpc.bucket)
	$(GSUTIL) retention temp set $(addprefix $(multibinder-grpc.bucket)/,$(notdir $(multibinder-grpc.artifacts)))
	$(GSUTIL) acl ch -u AllUsers:R $(addprefix $(multibinder-grpc.bucket)/,$(notdir $(multibinder-grpc.artifacts)))
	touch $@

.PHONY: multibinder-grpc.tests
multibinder-grpc.tests: $(multibinder-grpc.image) $(multibinder-grpc.sources)
	# TODO: I do not know how to run this application stand-alone and/or run the test
	docker run $(shell cat $<)

.PHONY: multibinder-grpc.clean
multibinder-grpc.clean:
	$(RM) -r $(multibinder-grpc.build_dir)
	$(RM) -r $(multibinder-grpc.out_dir)

.PHONY: multibinder-grpc multibinder-grpc.publish
multibinder-grpcs multibinder-grpc: $(multibinder-grpc.artifacts)
publish-multibinder-grpcs multibinder-grpc.publish: $(multibinder-grpc.publish)
print-sha multibinder-grpcs.print-sha: $(multibinder-grpc.artifact.release).print-sha
tests multibinder-grpcs.tests: multibinder-grpc.tests
clean: multibinder-grpc.clean
