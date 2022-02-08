package com.vmware.tanzu.streaming.runtime;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * The reason is the fact that JUnit 5 extension added on a test is inherited
 * by its @Nested tests and this is to guarantee the {@code BeforeAllCallback}
 * is executed ONCE by outer-most test this extension is applied on.
 */
public abstract class OncePerClassBeforeAllCallback implements BeforeAllCallback {

	protected abstract void oncePerClassBeforeAll(ExtensionContext context) throws Exception;

	@Override
	public final void beforeAll(ExtensionContext context) throws Exception {

		final String executedKey = getClass().getSimpleName() + ".executed";

		final ExtensionContext.Store globalStore = context.getStore(Namespace.GLOBAL);

		if (globalStore.get(executedKey) != null) {
			return;
		}

		oncePerClassBeforeAll(context);

		globalStore.put(executedKey, Boolean.TRUE);
	}

}