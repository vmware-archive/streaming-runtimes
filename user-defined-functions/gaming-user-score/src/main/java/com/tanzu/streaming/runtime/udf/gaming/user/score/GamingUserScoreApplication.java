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
package com.tanzu.streaming.runtime.udf.gaming.user.score;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.tanzu.streaming.runtime.processor.common.avro.AvroSchemaReaderWriter;
import com.tanzu.streaming.runtime.processor.common.avro.AvroUtil;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollection;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollectionSeDe;
import com.tanzu.streaming.runtime.processor.common.proto.GrpcPayloadCollection.Builder;
import org.apache.avro.generic.GenericRecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeType;

@SpringBootApplication
@EnableConfigurationProperties({ GamingUserScoreApplicationProperties.class })
public class GamingUserScoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(GamingUserScoreApplication.class, args);
	}

	public static class UserScores {
		private final String user;
		private final String team;
		private final AtomicLong userScoreSum;

		public UserScores(String user, String team, AtomicLong userScoreSum) {
			this.user = user;
			this.team = team;
			this.userScoreSum = userScoreSum;
		}

		public String getUser() {
			return user;
		}

		public String getTeam() {
			return team;
		}

		public AtomicLong getUserScoreSum() {
			return userScoreSum;
		}
	}

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> userScore() {

		return aggregate -> {
			
			MimeType inputContentType = GrpcPayloadCollectionSeDe.CONTENT_TYPE_RESOLVER.resolve(aggregate.getHeaders());

			if (inputContentType == null) {
				// TODO
				System.out.println("Missing content type:" + inputContentType);				
			}

			if (!inputContentType.getType().equals("multipart")) {
				System.out.println("Invalid content type:" + inputContentType);
			}

			MimeType payloadContentType = MimeType.valueOf("application/" + inputContentType.getSubtype());
			
			ConcurrentHashMap<String, UserScores> userScores = new ConcurrentHashMap<>();
			try {

				// Decode from PB array
				GrpcPayloadCollection payloadCollection = GrpcPayloadCollection.parseFrom(aggregate.getPayload());
				for (ByteString bs : payloadCollection.getPayloadList()) {

					UserScores userScore = null;

					if (AvroUtil.isAvroContentType(payloadContentType)) {

						byte[] singlePayload = bs.toByteArray();
						AvroSchemaReaderWriter avroReader = AvroSchemaReaderWriter.from(
								AvroSchemaReaderWriter
										.toResource(
												"http://localhost:8081/subjects/dataIn-value/versions/latest/schema"));
						GenericRecord record = avroReader.readRecord(singlePayload);

						String user = "" + record.get("fullName");
						String team = "" + record.get("team");
						Object score = record.get("score");
						Long scoreLong = Long.valueOf("" + score);

						userScore = new UserScores(user, team, new AtomicLong(scoreLong));
					}
					else {

						Map<String, Object> map = new ObjectMapper().readValue(bs.toByteArray(), Map.class);
						userScore = new UserScores((String) map.get("fullName"), (String) map.get("team"),
								new AtomicLong(Long.valueOf((Integer) map.get("score"))));
					}

					if (!userScores.containsKey(userScore.getUser())) {
						userScores.putIfAbsent(userScore.getUser(), userScore);
					}
					else {
						userScores.get(userScore.getUser()).getUserScoreSum()
								.addAndGet(userScore.getUserScoreSum().get());
					}
				}
			}
			catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
			catch (StreamReadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (DatabindException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Encode to PB array
			Builder builder = GrpcPayloadCollection.newBuilder();
			for (String user : userScores.keySet()) {
				UserScores userScore = userScores.get(user);
				String userScoreJson = String.format("{\"user\":\"%s\", \"team\":\"%s\", \"userTotalScore\":\"%s\"}",
						userScore.getUser(), userScore.getTeam(), userScore.getUserScoreSum());
				builder.addPayload(ByteString.copyFrom(userScoreJson.getBytes()));
			}
			byte[] responseCollectionPayload = builder.build().toByteArray();

			return MessageBuilder.withPayload(responseCollectionPayload)
					.setHeader(MessageHeaders.CONTENT_TYPE, MimeType.valueOf("multipart/json"))
					.build();
		};
	}

}
