ifndef TOOLS_MK # Prevent repeated "-include".
TOOLS_MK := $(lastword $(MAKEFILE_LIST))
TOOLS_INCLUDE_DIR := $(dir $(TOOLS_MK))

# Define the tools here
tools.path := $(abspath $(build_dir)/tools)
tools.bin.path := $(abspath $(tools.path)/bin)
GSUTIL := $(tools.path)/gsutil/gsutil

# TODO: Change this with the githubway to publish things
$(GSUTIL):
	@mkdir -p $(@D)
	curl -sL https://storage.googleapis.com/pub/gsutil.tar.gz | tar -xz -C $(tools.path)

tools.clean:
	$(RM) -rf $(tools.path)

clean .PHONY: tools.clean

endif
