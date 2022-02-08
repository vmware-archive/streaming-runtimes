package com.vmware.tanzu.streaming.runtime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalClusterManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(LocalClusterManager.class);

	public static void createAndWait(String name) {
		delete(name);

		final Instant start = Instant.now();

		final String kindCreateClusterCmd = "kind create cluster --name=" + name + " --kubeconfig=" + name;

		try {
			LOGGER.info("Create cluster {}: start", name);

			Process process = Runtime.getRuntime().exec(kindCreateClusterCmd);

			final int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new RuntimeException("Error creating cluster :" + name + ".\nkind create returned an exit value of: "
						+ exitValue + ".\nSTDERR: " + streamToString(process.getErrorStream()));
			}

			waitForClusterReady(name);

			LOGGER.info("Create cluster {}: completed for {}", name, Duration.between(start, Instant.now()));
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException("Error creating cluster :" + name, e);
		}
	}

	private static void waitForClusterReady(String name) {
		waitForClusterStarted(name);
		waitForClusterLive(name);
	}

	private static void waitForClusterStarted(String name) {
		Awaitility
				.await("Creating cluster with name: " + name)
				.pollDelay(Duration.ofSeconds(20))
				.atMost(Duration.ofMinutes(3))
				.until(() -> {
					File config = new File(name);
					return config.exists();
				});
	}

	private static void waitForClusterLive(String name) {
		Awaitility
				.await("Waiting for cluster " + name + " to be live")
				.atMost(20, TimeUnit.SECONDS)
				.pollDelay(Duration.ofSeconds(5))
				.until(() -> {
					ApiClient apiClient = ClientBuilder
							.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(name)))
							.build()
							.setReadTimeout(0);
					return !new CoreV1Api(apiClient)
							.getAPIResources()
							.getResources()
							.isEmpty();
				});
	}

	public static void delete(String name) {

		final Instant start = Instant.now();

		final String kindDeleteClusterCmd = "kind delete cluster --name=" + name;

		try {
			LOGGER.info("Delete cluster '{}': start", name);

			Process process = Runtime.getRuntime().exec(kindDeleteClusterCmd);

			final int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new RuntimeException("Error deleting cluster :" + name + ".\nkind delete returned an exit value of: "
						+ exitValue + ".\nSTDERR: " + streamToString(process.getErrorStream()));
			}

			File config = new File(name);
			config.delete();

			LOGGER.info("Delete cluster '{}': completed for {}", name, Duration.between(start, Instant.now()));
		}
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static String streamToString(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		StringBuilder stringBuilder = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line).append("\n");
		}

		return stringBuilder.toString();
	}
}
