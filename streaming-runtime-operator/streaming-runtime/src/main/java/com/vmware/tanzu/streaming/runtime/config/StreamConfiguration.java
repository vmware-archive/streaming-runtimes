package com.vmware.tanzu.streaming.runtime.config;

import java.util.Objects;

import com.vmware.tanzu.streaming.models.V1alpha1Stream;
import com.vmware.tanzu.streaming.models.V1alpha1StreamList;
import com.vmware.tanzu.streaming.runtime.StreamReconciler;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.DefaultControllerWatch;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StreamConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(StreamConfiguration.class);

	public static final String STREAM_CONTROLLER_NAME = "StreamController";
	private static final int WORKER_COUNT = 4;

	@Bean
	public SharedIndexInformer<V1alpha1Stream> streamsInformer(
			ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1alpha1Stream, V1alpha1StreamList> genericApi =
				new GenericKubernetesApi<>(
						V1alpha1Stream.class,
						V1alpha1StreamList.class,
						"streaming.tanzu.vmware.com",
						"v1alpha1",
						"streams",
						apiClient);
		return sharedInformerFactory.sharedIndexInformerFor(genericApi, V1alpha1Stream.class, 0);
	}

	@Bean
	@Qualifier("streamController")
	Controller streamController(SharedInformerFactory factory, StreamReconciler streamReconciler,
			SharedIndexInformer<V1alpha1Stream> streamInformer) {
		return ControllerBuilder.defaultBuilder(factory)
				.watch(this::createStreamControllerWatch)
				.withReconciler(streamReconciler)
				.withName(STREAM_CONTROLLER_NAME)
				.withWorkerCount(WORKER_COUNT)
				.withReadyFunc(streamInformer::hasSynced)
				.build();
	}

	private DefaultControllerWatch<V1alpha1Stream> createStreamControllerWatch(WorkQueue<Request> workQueue) {
		return ControllerBuilder.controllerWatchBuilder(V1alpha1Stream.class, workQueue)
				.withOnAddFilter(stream -> {
					LOG.info(String.format("[%s] Event: Add Stream '%s'",
							STREAM_CONTROLLER_NAME, stream.getMetadata().getName()));
					return true;
				})
				.withOnUpdateFilter((oldStream, newStream) -> {
					LOG.info(String.format(
							"[%s] Event: Update Stream '%s' to '%s'",
							STREAM_CONTROLLER_NAME, oldStream.getMetadata().getName(),
							newStream.getMetadata().getName()));
					return true;
				})
				.withOnDeleteFilter((deletedStream, deletedFinalStateUnknown) -> {
					LOG.info(String.format("[%s] Event: Delete Stream '%s'",
							STREAM_CONTROLLER_NAME, deletedStream.getMetadata().getName()));
					return true;
				})
				.build();
	}
}
