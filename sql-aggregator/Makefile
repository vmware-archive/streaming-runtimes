RULES.MK ?= ../rules.mk
include $(RULES.MK)

path ?= .
sql-aggregator.path := $(abspath $(path))

include $(sql-aggregator.path)/rules.mk

sql-aggregator.sources := $(shell find $(sql-aggregator.path) -type f -iname '*.java') \
	$(sql-aggregator.path)/pom.xml \
	$(sql-aggregator.path)/VERSION \

$(eval $(call VERSION_template,sql-aggregator,$(sql-aggregator.path)))

# The tools should always build based on the version in VERSION file.
sql-aggregator.version := $(sql-aggregator.version.release)

sql-aggregator.artifact.release = $(sql-aggregator.out_dir)/sql-aggregator-$(sql-aggregator.version).jar

# We want to use the branch here, it's okay as this isn't something we're pushing
sql-aggregator.tag := $(sql-aggregator.version.branch)
sql-aggregator.image_name := sql-aggregator:$(sql-aggregator.tag)
sql-aggregator.image := $(sql-aggregator.build_dir)/image.$(sql-aggregator.tag)

$(sql-aggregator.image): $(sql-aggregator.path)/Dockerfile $(sql-aggregator.sources)
	@mkdir -p $(@D)
	DOCKER_BUILDKIT=1 docker build -t $(sql-aggregator.image_name) -f $< $(sql-aggregator.path)
	printf $(sql-aggregator.image_name) > $@

$(sql-aggregator.artifact.release): $(sql-aggregator.image)
	@mkdir -p $(@D)
	docker cp `docker create $(sql-aggregator.image_name)`:/out/$(notdir $(sql-aggregator.artifact.release)) $@
	touch $@

sql-aggregator.artifacts := $(sql-aggregator.artifact.release) $(sql-aggregator.artifact.release).sha256
sql-aggregator.publish := $(sql-aggregator.build_dir)/publish.$(sql-aggregator.version)
$(sql-aggregator.publish): $(GSUTIL) $(sql-aggregator.artifacts)
	$(GSUTIL) cp $(sql-aggregator.artifacts) $(sql-aggregator.bucket)
	$(GSUTIL) retention temp set $(addprefix $(sql-aggregator.bucket)/,$(notdir $(sql-aggregator.artifacts)))
	$(GSUTIL) acl ch -u AllUsers:R $(addprefix $(sql-aggregator.bucket)/,$(notdir $(sql-aggregator.artifacts)))
	touch $@

.PHONY: sql-aggregator.tests
sql-aggregator.tests: $(sql-aggregator.image) $(sql-aggregator.sources)
	# TODO: I do not know how to run this application stand-alone and/or run the test
	docker run $(shell cat $<)

.PHONY: sql-aggregator.clean
sql-aggregator.clean:
	$(RM) -r $(sql-aggregator.build_dir)
	$(RM) -r $(sql-aggregator.out_dir)

.PHONY: sql-aggregator sql-aggregator.publish
sql-aggregators sql-aggregator: $(sql-aggregator.artifacts)
publish-sql-aggregators sql-aggregator.publish: $(sql-aggregator.publish)
print-sha sql-aggregators.print-sha: $(sql-aggregator.artifact.release).print-sha
tests sql-aggregators.tests: sql-aggregator.tests
clean: sql-aggregator.clean
