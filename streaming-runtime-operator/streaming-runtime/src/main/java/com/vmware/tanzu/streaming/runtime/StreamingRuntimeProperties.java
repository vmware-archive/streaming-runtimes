package com.vmware.tanzu.streaming.runtime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("streaming-runtime")
public class StreamingRuntimeProperties {
	private boolean enablePortForward = false;
	private boolean autoProvisionClusterStream = true;

	public boolean isEnablePortForward() {
		return enablePortForward;
	}

	public void setEnablePortForward(boolean enablePortForward) {
		this.enablePortForward = enablePortForward;
	}

	public boolean isAutoProvisionClusterStream() {
		return autoProvisionClusterStream;
	}

	public void setAutoProvisionClusterStream(boolean autoProvisionClusterStream) {
		this.autoProvisionClusterStream = autoProvisionClusterStream;
	}
}
