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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import com.tanzu.streaming.runtime.udf.aggregator.AbstractPayloadConverter;
import com.tanzu.streaming.runtime.udf.aggregator.Aggregate;
import com.tanzu.streaming.runtime.udf.aggregator.Aggregator;
import com.tanzu.streaming.runtime.udf.aggregator.IdentityReleaser;
import com.tanzu.streaming.runtime.udf.aggregator.PayloadConverter;
import org.apache.avro.generic.GenericRecord;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
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
	public Aggregate<UserScores> aggregate() {

		return new Aggregate<UserScores>() {
			@Override
			public void aggregate(MessageHeaders headers, UserScores userScoreUpdate,
					ConcurrentHashMap<String, UserScores> outputAggregatedState) {

				if (!outputAggregatedState.containsKey(userScoreUpdate.getUser())) {
					outputAggregatedState.putIfAbsent(userScoreUpdate.getUser(), userScoreUpdate);
				}
				else {
					((UserScores) outputAggregatedState.get(userScoreUpdate.getUser()))
							.getUserScoreSum()
							.addAndGet(userScoreUpdate.getUserScoreSum().get());
				}
			}
		};
	}

	@Bean
	public PayloadConverter<UserScores> payloadConverter(GamingUserScoreApplicationProperties properties) {

		return new AbstractPayloadConverter<UserScores>(properties.getAvroSchemaUri()) {
			@Override
			public UserScores fromPayload(byte[] payload, MimeType payloadContentType) {
				Object twaRecord = this.fromPayloadRaw(payload, payloadContentType);

				if (twaRecord instanceof GenericRecord) {
					GenericRecord record = (GenericRecord) twaRecord;
					return new UserScores("" + record.get("fullName"), "" + record.get("team"),
							new AtomicLong(Long.valueOf("" + record.get("score"))));
				}
				else if (twaRecord instanceof Map) {
					Map<String, Object> map = (Map<String, Object>) twaRecord;
					return new UserScores((String) map.get("fullName"), (String) map.get("team"),
							new AtomicLong(Long.valueOf((Integer) map.get("score"))));
				}
				throw new RuntimeException("Unknown Record type");
			}

			@Override
			public byte[] toPayload(UserScores userScore, MimeType targetContentType) {
				return String.format("{\"user\":\"%s\", \"team\":\"%s\", \"userTotalScore\":\"%s\"}",
						userScore.getUser(), userScore.getTeam(), userScore.getUserScoreSum())
						.getBytes();
			}
		};
	}

	@Bean
	public Aggregator<UserScores> aggregator(Aggregate<UserScores> aggregate,
			PayloadConverter<UserScores> payloadConverter) {
		return new Aggregator<UserScores>(aggregate, new IdentityReleaser<UserScores>(), payloadConverter);
	}

	@Bean
	public Function<Message<byte[]>, Message<byte[]>> userScore(Aggregator<UserScores> aggregator) {
		return inputMultipartMessage -> {
			return aggregator.onMessage(inputMultipartMessage);
		};
	}
}
