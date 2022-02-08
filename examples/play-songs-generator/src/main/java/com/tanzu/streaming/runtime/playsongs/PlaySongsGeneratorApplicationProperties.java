package com.tanzu.streaming.runtime.playsongs;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("play.songs.generator")
public class PlaySongsGeneratorApplicationProperties {
    private String playEventsTopic = "play-events";
    private String songsTopic = "song-feed";

    private String kafkaServer = "localhost:9094";
    private String schemaRegistryServer = "http://localhost:8081";

    private Long minChartableDuration = 30 * 1000L;

    private Long waitBetweenPlaySongMs = 500L;

    public Long getWaitBetweenPlaySongMs() {
        return waitBetweenPlaySongMs;
    }

    public void setWaitBetweenPlaySongMs(Long waitBetweenPlaySongMs) {
        this.waitBetweenPlaySongMs = waitBetweenPlaySongMs;
    }

    public String getPlayEventsTopic() {
        return playEventsTopic;
    }

    public void setPlayEventsTopic(String playEventsTopic) {
        this.playEventsTopic = playEventsTopic;
    }

    public String getSongsTopic() {
        return songsTopic;
    }

    public void setSongsTopic(String songsTopic) {
        this.songsTopic = songsTopic;
    }

    public String getKafkaServer() {
        return kafkaServer;
    }

    public void setKafkaServer(String kafkaServer) {
        this.kafkaServer = kafkaServer;
    }

    public String getSchemaRegistryServer() {
        return schemaRegistryServer;
    }

    public void setSchemaRegistryServer(String schemaRegistryServer) {
        this.schemaRegistryServer = schemaRegistryServer;
    }

    public Long getMinChartableDuration() {
        return minChartableDuration;
    }

    public void setMinChartableDuration(Long minChartableDuration) {
        this.minChartableDuration = minChartableDuration;
    }
    
    public Map<String, Object> getCommonProperties() {
        Map<String, Object> commonProperties = new HashMap<>();
        commonProperties.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, this.getSchemaRegistryServer());
        commonProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.getKafkaServer());
        commonProperties.put(ProducerConfig.RETRIES_CONFIG, 0);
        commonProperties.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        commonProperties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        commonProperties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return commonProperties;
    }

}
