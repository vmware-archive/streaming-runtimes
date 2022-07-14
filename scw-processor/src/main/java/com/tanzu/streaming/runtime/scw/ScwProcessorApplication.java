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
package com.tanzu.streaming.runtime.scw;

import java.util.function.Consumer;
import java.util.function.Function;

import com.tanzu.streaming.runtime.processor.common.avro.AvroMessageReader;
import com.tanzu.streaming.runtime.processor.common.avro.AvroSchemaMessageConvertor;
import com.tanzu.streaming.runtime.processor.common.avro.AvroSchemaReaderWriter;
import com.tanzu.streaming.runtime.processor.common.avro.AvroSchemaRegistryMessageConvertor;
import com.tanzu.streaming.runtime.scw.processor.EventTimeProcessor;
import com.tanzu.streaming.runtime.scw.processor.window.IdleWindowsReleaser;
import com.tanzu.streaming.runtime.scw.processor.window.IdleWindowsWatchdog;
import com.tanzu.streaming.runtime.scw.processor.window.TumblingWindowService;
import com.tanzu.streaming.runtime.scw.processor.window.state.InMemoryState;
import com.tanzu.streaming.runtime.scw.processor.window.state.RocksDBWindowState;
import com.tanzu.streaming.runtime.scw.processor.window.state.State;
import com.tanzu.streaming.runtime.scw.timestamp.JsonPathTimestampAssigner;
import com.tanzu.streaming.runtime.scw.timestamp.MessageHeaderTimestampAssigner;
import com.tanzu.streaming.runtime.scw.timestamp.ProcTimestampAssigner;
import com.tanzu.streaming.runtime.scw.timestamp.RecordTimestampAssigner;
import com.tanzu.streaming.runtime.scw.watermark.WatermarkService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.spel.SpelFunctionProperties;
import org.springframework.cloud.function.grpc.FunctionGrpcProperties;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

@SpringBootApplication
@EnableConfigurationProperties({ ScwProcessorApplicationProperties.class,
		FunctionGrpcProperties.class, SpelFunctionProperties.class })
public class ScwProcessorApplication {

	static final Log logger = LogFactory.getLog(ScwProcessorApplication.class);

	private ScwProcessorApplicationProperties properties;

	public ScwProcessorApplication(ScwProcessorApplicationProperties properties) {
		logger.info("skipUdf: " + properties.isSkipUdf());
		logger.info("skipAggregation: " + properties.isSkipAggregation());
		logger.info("spelTransformation: " + properties.isEnableSpelTransformation());
		logger.info("forceGrpcPayloadCollection: " + properties.isForceGrpcPayloadCollection());
		logger.info("window: " + properties.getWindow());
		logger.info("maxOutOfOrderness: " + properties.getMaxOutOfOrderness());
		logger.info("allowedLateness: " + properties.getAllowedLateness());
		logger.info("LateEventMode: " + properties.getLateEventMode());
		logger.info("input.timestampExpression: " + properties.getInput().getTimestampExpression());
		logger.info("input.schemaRegistryUri: " + properties.getInput().getSchemaRegistryUri());
		logger.info("input.schemaUri: " + properties.getInput().getSchemaUri());
		logger.info("input.output.headers: " + properties.getOutput().getHeaders());
		logger.info("stateType: " + properties.getStateType());

		this.properties = properties;
	}

	public static void main(String[] args) {
		SpringApplication.run(ScwProcessorApplication.class, args);
	}

	@Bean
	public AvroMessageReader avroConverter() {
		if (StringUtils.hasText(this.properties.getInput().getSchemaRegistryUri())) {
			return new AvroSchemaRegistryMessageConvertor(this.properties.getInput().getSchemaRegistryUri());
		}
		else if (this.properties.getInput().getSchemaUri() != null) {
			return new AvroSchemaMessageConvertor(
					AvroSchemaReaderWriter.from(this.properties.getInput().getSchemaUri()));
		}
		logger.warn("No Avro Converter is Configured");
		return null;
	}

	@Bean
	public RecordTimestampAssigner<byte[]> timestampAssigner(AvroMessageReader avroMessageReader) {

		if (this.properties.getInput().getTimestampExpression() == null ||
				this.properties.getInput().getTimestampExpression().equalsIgnoreCase("proc")) {
			return new ProcTimestampAssigner();
		}

		if (this.properties.getInput().getTimestampExpression().startsWith("header.")) {
			return new MessageHeaderTimestampAssigner(
					this.properties.getInput().getTimestampExpression().substring("header.".length()));
		}

		String timestampExpression = this.properties.getInput().getTimestampExpression();
		timestampExpression = (timestampExpression.startsWith("payload."))
				? timestampExpression = timestampExpression.substring("payload.".length())
				: timestampExpression;

		return new JsonPathTimestampAssigner(timestampExpression, avroMessageReader);
	}

	@Bean
	public WatermarkService watermarkService() {
		return new WatermarkService()
				.withMaxOutOfOrderness(this.properties.getMaxOutOfOrderness())
				.withAllowedLateness(this.properties.getAllowedLateness());
	}

	@Bean
	@ConditionalOnProperty(value = "scw.processor.stateType", havingValue = "MEMORY", matchIfMissing = true)
	public State inMemoryState() {
		logger.info("Enable In-Memory Window State!");
		return new InMemoryState();
	}

	@Bean
	@ConditionalOnProperty(value = "scw.processor.stateType", havingValue = "ROCKSDB")
	public State rocksDbState() {
		logger.info("Enable RocksDB Window State!");
		return new RocksDBWindowState(this.properties.getRocksDbPath());
	}


	@Bean
	@ConditionalOnProperty(value = "scw.processor.skipAggregation", havingValue = "false", matchIfMissing = true)
	public IdleWindowsReleaser idleWindowsReleaser(TumblingWindowService tumblingWindowService) {
		return new IdleWindowsReleaser(tumblingWindowService, this.properties.getIdleWindowTimeout());
	}

	@Bean
	@ConditionalOnProperty(value = "scw.processor.skipAggregation", havingValue = "false", matchIfMissing = true)
	public IdleWindowsWatchdog idleWindowsReleaserExecutorService(
			IdleWindowsReleaser idleWindowsReleaser) {
		return new IdleWindowsWatchdog(idleWindowsReleaser);
	}

	@Bean
	@ConditionalOnProperty(value = "scw.processor.skipAggregation", havingValue = "false", matchIfMissing = true)
	public TumblingWindowEventTimeProcessor tumblingWindowProcessorService(
			State windowState,
			RecordTimestampAssigner<byte[]> timestampAssigner,
			WatermarkService watermarkService, StreamBridge streamBridge, FunctionGrpcProperties grpcProperties,
			ScwHeaderAugmenter outputHeadersAugmenter) {

		logger.info("Use: TumblingWindowEventTimeProcessor with: " + timestampAssigner.getClass().getSimpleName());

		return new TumblingWindowEventTimeProcessor(windowState, this.properties, timestampAssigner, watermarkService,
				streamBridge, grpcProperties.getPort(), outputHeadersAugmenter);
	}

	@Bean
	public ScwHeaderAugmenter outputHeadersAugmenter(AvroMessageReader avroMessageReader) {
		return new ScwHeaderAugmenter(this.properties.getOutput().getHeaders(), avroMessageReader);
	}

	@Bean
	@ConditionalOnProperty(value = "scw.processor.skipAggregation", havingValue = "true")
	public StatelessEventTimeProcessor statelessEventTimeProcessor(
			WatermarkService watermarkService, RecordTimestampAssigner<byte[]> timestampAssigner,
			ScwHeaderAugmenter headerAugmenter, ScwProcessorApplicationProperties properties,
			FunctionGrpcProperties grpcProperties, StreamBridge streamBridge) {

		logger.info("Use: StatelessEventTimeProcessor with: " + timestampAssigner.getClass().getSimpleName());

		return new StatelessEventTimeProcessor(watermarkService, timestampAssigner, headerAugmenter, properties,
				grpcProperties.getPort(), streamBridge);
	}

	@Bean
	public Consumer<Message<byte[]>> proxy(Function<Message<?>, Message<?>> spelFunction,
			EventTimeProcessor eventTimeProcessor) {

		logger.info("EventTime Processor: " + eventTimeProcessor.getClass().getName());
		logger.info("Enabled SpEL Transformation: " + this.properties.isEnableSpelTransformation());

		return message -> {
			if (this.properties.isEnableSpelTransformation()) {
				Message<?> transformedMessageWithStringPayload = spelFunction.apply(message);
				message = MessageBuilder
						.withPayload(transformedMessageWithStringPayload.getPayload().toString().getBytes())
						.copyHeaders(transformedMessageWithStringPayload.getHeaders())
						.build();
			}

			eventTimeProcessor.onNewMessage(message);
		};

		// AvroSchemaReaderWriter avroReader = AvroSchemaReaderWriter.from(
		// AvroSchemaReaderWriter
		// .toResource("http://localhost:8081/subjects/dataIn-value/versions/latest/schema"));

		// GenericRecord record = avroReader.readRecord(message.getPayload());
		// GenericRecord record = avroMessageReader.toGenericRecord(message);
		// System.out.println(record);
	}

	// public static class DisableAvroSchemaMessageConverter extends AvroSchemaMessageConverter {

	// public DisableAvroSchemaMessageConverter(AvroSchemaServiceManager manager) {
	// super(manager);
	// }

	// @Override
	// protected boolean supports(Class<?> clazz) {
	// return false;
	// }

	// }

	// @Bean
	// public AvroSchemaMessageConverter avroSchemaMessageConverter() {
	// return new DisableAvroSchemaMessageConverter(null);
	// }

}
