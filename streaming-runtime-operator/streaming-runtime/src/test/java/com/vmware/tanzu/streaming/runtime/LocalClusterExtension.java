package com.vmware.tanzu.streaming.runtime;

import java.io.IOException;

import io.kubernetes.client.extended.controller.ControllerManager;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_METHOD;
import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;
import static org.junit.platform.commons.util.ReflectionUtils.findFields;
import static org.junit.platform.commons.util.ReflectionUtils.isNotStatic;
import static org.junit.platform.commons.util.ReflectionUtils.makeAccessible;

/**
 * JUnit 5 extension that creates, configures and deletes Kind cluster. Additionally fields in a test
 * or parameters in a lifecycle or test method which are of {@link ApiClient} or {@link TestK8SClient}
 * types are auto-created and injected.
 */
public class LocalClusterExtension extends OncePerClassBeforeAllCallback implements ParameterResolver {

	public static final String API_CLIENT_STORE_KEY = ApiClient.class.getSimpleName();

	public static final String TEST_CLIENT_STORE_KEY = TestK8SClient.class.getSimpleName();

	@Override
	public void oncePerClassBeforeAll(ExtensionContext context) throws ApiException {
		{
			// NOTE: Moved this to the @Bean method that creates the ApiClient so that the cluster will exist
			//       at bean creation time.
			// LocalClusterManager.createAndWait(getClusterName(context));

			registerLocalClusterAsCloseableResource(context);

			prepareLocalCluster(context);
		}

		addToGlobalStore(context);

		injectDependants(context);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> parameterType = parameterContext.getParameter().getType();
		return supportsInjectable(parameterType);
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		Class<?> parameterType = parameterContext.getParameter().getType();
		return getInjectable(extensionContext, parameterType);
	}

	private void registerLocalClusterAsCloseableResource(ExtensionContext context) {

		final String clusterName = getClusterName(context);

		context.getStore(Namespace.GLOBAL).put(clusterName + CloseableResource.class.getSimpleName(),
				(CloseableResource) () -> {
					final ControllerManager controllerLauncher = SpringExtension.getApplicationContext(context).getBean(ControllerManager.class);
					controllerLauncher.shutdown();
					LocalClusterManager.delete(clusterName);
				});
	}

	private void prepareLocalCluster(ExtensionContext context) throws ApiException {
		TestK8SClient testClient = getOrCreateTestClient(context);
		testClient.createCrd();
//		testClient.createNamespace(Environment.ACS_INSTALLATION_NAMESPACE);
	}

	private void addToGlobalStore(ExtensionContext context) {
		getOrCreateApiClient(context);
		getOrCreateTestClient(context);
	}

	private ApiClient createApiClient(ExtensionContext context) {
		ApplicationContext spring = SpringExtension.getApplicationContext(context);
		return spring.getBean(ApiClient.class);
	}

	private ApiClient getOrCreateApiClient(ExtensionContext context) {
		return context
				.getStore(Namespace.GLOBAL)
				.getOrComputeIfAbsent(
						API_CLIENT_STORE_KEY,
						key -> createApiClient(context),
						ApiClient.class);
	}

	private TestK8SClient getOrCreateTestClient(ExtensionContext context) {
		return context
				.getStore(Namespace.GLOBAL)
				.getOrComputeIfAbsent(
						TEST_CLIENT_STORE_KEY,
						key -> {
							try {
								return new TestK8SClient(getOrCreateApiClient(context));
							}
							catch (IOException notFoundExc) {
								throw new ExtensionConfigurationException(
										"Failed to create TestK8SClient for '" + getClusterName(context) + "' cluster",
										notFoundExc);
							}
						},
						TestK8SClient.class);
	}

	private String getClusterName(ExtensionContext context) {
		return context.getRequiredTestClass().getSimpleName().toLowerCase();
	}

	private void injectDependants(ExtensionContext context) {

		findFields(
				context.getRequiredTestClass(),
				field -> supportsInjectable(field.getType()),
				TOP_DOWN)

				.forEach(field -> {
					if (isNotStatic(field)) {
						if (context.getTestInstanceLifecycle().orElse(PER_METHOD) == PER_METHOD) {
							throw new ExtensionConfigurationException(
									field.getType().getSimpleName() + " field [" + field + "] must be static" +
											" unless the test class is annotated with @TestInstance(Lifecycle.PER_CLASS)");
						}
					}
					try {
						makeAccessible(field).set(
								context.getTestInstance().orElse(null),
								getInjectable(context, field.getType()));
					}
					catch (Throwable exc) {
						throw new ExtensionConfigurationException(
								"Failed to inject " + field,
								exc);
					}
				});
	}

	private Object getInjectable(ExtensionContext context, Class<?> injectableType) {
		if (ApiClient.class.isAssignableFrom(injectableType)) {
			return getOrCreateApiClient(context);
		}
		if (TestK8SClient.class.isAssignableFrom(injectableType)) {
			return getOrCreateTestClient(context);
		}
		throw new ExtensionConfigurationException(
				"Unsupported injectable type: " + injectableType);
	}

	private boolean supportsInjectable(Class<?> injectableType) {
		if (ApiClient.class.isAssignableFrom(injectableType)) {
			return true;
		}
		if (TestK8SClient.class.isAssignableFrom(injectableType)) {
			return true;
		}
		return false;
	}
}