/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vmware.tanzu.streaming.runtime.config;

import com.vmware.tanzu.streaming.models.V1alpha1Processor;
import com.vmware.tanzu.streaming.models.V1alpha1ProcessorList;
import com.vmware.tanzu.streaming.runtime.ProcessorReconciler;
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
public class ProcessorConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessorConfiguration.class);

	public static final String PROCESSOR_CONTROLLER_NAME = "ProcessorController";
	private static final int WORKER_COUNT = 4;

	@Bean
	public SharedIndexInformer<V1alpha1Processor> processorsInformer(
			ApiClient apiClient, SharedInformerFactory sharedInformerFactory) {
		GenericKubernetesApi<V1alpha1Processor, V1alpha1ProcessorList> genericApi =
				new GenericKubernetesApi<>(
						V1alpha1Processor.class,
						V1alpha1ProcessorList.class,
						"streaming.tanzu.vmware.com",
						"v1alpha1",
						"processors",
						apiClient);
		return sharedInformerFactory.sharedIndexInformerFor(genericApi, V1alpha1Processor.class, 0);
	}

	@Bean
	@Qualifier("processorController")
	Controller processorController(SharedInformerFactory factory, ProcessorReconciler processorReconciler,
			SharedIndexInformer<V1alpha1Processor> processorsInformer) {
		return ControllerBuilder.defaultBuilder(factory)
				.watch(this::createProcessorControllerWatch)
				.withReconciler(processorReconciler)
				.withName(PROCESSOR_CONTROLLER_NAME)
				.withWorkerCount(WORKER_COUNT)
				.withReadyFunc(processorsInformer::hasSynced)
				.build();
	}

	private DefaultControllerWatch<V1alpha1Processor> createProcessorControllerWatch(WorkQueue<Request> workQueue) {
		return ControllerBuilder.controllerWatchBuilder(V1alpha1Processor.class, workQueue)
				.withOnAddFilter(stream -> {
					LOG.info(String.format("[%s] Event: Add Processor '%s'",
							PROCESSOR_CONTROLLER_NAME, stream.getMetadata().getName()));
					return true;
				})
				.withOnUpdateFilter((oldStream, newStream) -> {
					LOG.info(String.format(
							"[%s] Event: Update Processor '%s' to '%s'",
							PROCESSOR_CONTROLLER_NAME, oldStream.getMetadata().getName(),
							newStream.getMetadata().getName()));
					return true;
				})
				.withOnDeleteFilter((deletedStream, deletedFinalStateUnknown) -> {
					LOG.info(String.format("[%s] Event: Delete Processor '%s'",
							PROCESSOR_CONTROLLER_NAME, deletedStream.getMetadata().getName()));
					return false;
				})
				.build();
	}
}
