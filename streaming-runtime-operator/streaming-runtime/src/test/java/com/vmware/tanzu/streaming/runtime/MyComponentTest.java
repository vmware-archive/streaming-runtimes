package com.vmware.tanzu.streaming.runtime;

import io.kubernetes.client.openapi.ApiException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static java.util.UUID.randomUUID;
import static org.awaitility.Awaitility.await;

@KubernetesComponentTest
@SpringBootTest(properties = "clusterName=createcomponenttest")
@ActiveProfiles("componenttests")
public class MyComponentTest {

	private TestK8SClient testK8SClient;

	private String createNamespace;

	@BeforeAll
	void setupCreateScenario() throws Exception {
		this.createNamespace = "create-component-test-" + randomUUID().toString().substring(0, 8);
		//createScenario = new ClusterConfigurationSourceScenario()
		//		.withCreateSlice().acsNamespace(createNamespace)
		//		.run(testK8SClient);
	}

	@Test
	void shouldCreateConfigMap() throws ApiException {
		System.out.println("Boza");
		//await(format("configMap %s/%s", createNamespace, createScenario.getConfigMapRevisionNames().peek()))
		//		.atMost(Duration.ofMinutes(1))
		//		.pollInterval(Duration.ofSeconds(1))
		//		.until(() -> testK8SClient.configMapExists(
		//				createScenario.getConfigMapRevisionNames().peek(),
		//				createNamespace));
		//
		//assertThat(testK8SClient.configMapData(
		//		createScenario.getConfigMapRevisionNames().peek(),
		//		createNamespace))
		//		.containsEntry("cook.special", "Cake a la mode");
	}
}
