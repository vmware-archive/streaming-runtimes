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

package io.kubernetes.nativex;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.annotations.JsonAdapter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import org.springframework.aot.context.bootstrap.generator.infrastructure.nativex.NativeConfigurationRegistry;
import org.springframework.nativex.AotOptions;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.TypeHint;
import org.springframework.nativex.type.NativeConfiguration;

/**
 * These hints are inspired by <a href="https://github.com/scratches/spring-controller">
 * Dr. Dave Syer's sample Kubernetes controller</a> and the configuration therein.
 * <p>
 * These types work <a href="https://github.com/kubernetes-client/java">in conjunction
 * with the autoconfiguration provided by the official Kubernetes Java client</a>, most of
 * which is code-generated from Swagger. This support automatically registers any
 * code-generated types that have {@link ApiModel} on it, limiting
 * the registration to the code-generated types in the {@link io.kubernetes} package.
 * <p>
 * This hints class also registers options required to use this with a HTTPS API endpoints
 * with custom character sets.
 *
 * @author Josh Long
 * @author Dave Syer
 */

@NativeHint(//

		options = {"-H:+AddAllCharsets", "--enable-all-security-services", "--enable-https", "--enable-http"},
		types = { //
				@TypeHint( //
						access = {
							org.springframework.nativex.hint.TypeAccess.DECLARED_CLASSES, 
							org.springframework.nativex.hint.TypeAccess.DECLARED_CONSTRUCTORS, 
							org.springframework.nativex.hint.TypeAccess.DECLARED_FIELDS, 
							org.springframework.nativex.hint.TypeAccess.DECLARED_METHODS}, //
						typeNames = { //
								"io.kubernetes.client.informer.cache.ProcessorListener",
								"io.kubernetes.client.extended.controller.Controller",
								"io.kubernetes.client.util.generic.GenericKubernetesApi$StatusPatch",
								"io.kubernetes.client.util.Watch$Response",
								"com.vmware.tanzu.streaming.runtime.uitil.DataSchemaToDdlConverter$TableDdlInfo",
								"com.vmware.tanzu.streaming.runtime.ProcessorReconciler$ApplicationYaml",
								"com.vmware.tanzu.streaming.runtime.ProcessorReconciler$Sql",
								"com.vmware.tanzu.streaming.runtime.ProcessorReconciler$Aggregation"}) //
		}//
)
public class KubernetesApiNativeConfiguration implements NativeConfiguration {

	@Override
	public void computeHints(NativeConfigurationRegistry registry, AotOptions aotOptions) {

		Reflections reflections = new Reflections("io.kubernetes");
		Set<Class<?>> apiModels = reflections.getTypesAnnotatedWith(ApiModel.class);
		Set<Class<?>> jsonAdapters = findJsonAdapters(reflections);

		//com.vmware.tanzu.streaming.models
		Reflections crdReflections = new Reflections("com.vmware.tanzu.streaming.models", new MethodAnnotationsScanner());
		Set<Method> annotatedMethods = crdReflections.getMethodsAnnotatedWith(ApiModelProperty.class);
		Set<Class<?>> crdModels = annotatedMethods.stream().map(Method::getDeclaringClass).collect(Collectors.toSet());

		Set<Class<?>> all = new HashSet<>();
		all.addAll(jsonAdapters);
		all.addAll(apiModels);
		all.addAll(crdModels);
		all.forEach(clzz -> registry.reflection()
			.forType(clzz)
			.withAccess(org.springframework.nativex.hint.TypeAccess.values())
			.build());
	}

	private <R extends Annotation> Set<Class<?>> findJsonAdapters(Reflections reflections) {
		var jsonAdapterClass = JsonAdapter.class;
		return reflections.getTypesAnnotatedWith(jsonAdapterClass).stream().flatMap(clazz -> {
			var list = new HashSet<Class<?>>();
			var annotation = clazz.getAnnotation(jsonAdapterClass);
			if (null != annotation) {
				list.add(annotation.value());
			}
			list.add(clazz);

			return list.stream();
		}).collect(Collectors.toSet());
	}
}
