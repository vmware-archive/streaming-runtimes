package com.tanzu.streaming.runtime.playsongs;

import com.tanzu.streaming.runtime.playsongs.avro.PlayEvent;
import com.tanzu.streaming.runtime.playsongs.avro.Song;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.*;

@SpringBootApplication
@EnableConfigurationProperties(PlaySongsGeneratorApplicationProperties.class)
public class PlaySongsGeneratorApplication implements CommandLineRunner {

    private static final Random random = new Random();

    @Autowired
    private KafkaTemplate<Long, Song> songsTemplate;

    @Autowired
    private KafkaTemplate<String, PlayEvent> playEventsTemplate;

    @Autowired
    private PlaySongsGeneratorApplicationProperties properties;

    public static void main(String[] args) {
        SpringApplication.run(PlaySongsGeneratorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        final List<Song> songs = Arrays.asList(
                new Song(1L, "Fresh Fruit For Rotting Vegetables", "Dead Kennedys", "Chemical Warfare", "Punk"),
                new Song(2L, "We Are the League", "Anti-Nowhere League", "Animal", "Punk"),
                new Song(3L, "Live In A Dive", "Subhumans", "All Gone Dead", "Punk"),
                new Song(4L, "PSI", "Wheres The Pope?", "Fear Of God", "Punk"),
                new Song(5L, "Totally Exploited", "The Exploited", "Punks Not Dead", "Punk"),
                new Song(6L, "The Audacity Of Hype", "Jello Biafra And The Guantanamo School Of Medicine", "Three Strikes", "Punk"),
                new Song(7L, "Licensed to Ill", "The Beastie Boys", "Fight For Your Right", "Hip Hop"),
                new Song(8L, "De La Soul Is Dead", "De La Soul", "Oodles Of O's", "Hip Hop"),
                new Song(9L, "Straight Outta Compton", "N.W.A", "Gangsta Gangsta", "Hip Hop"),
                new Song(10L, "Fear Of A Black Planet", "Public Enemy", "911 Is A Joke", "Hip Hop"),
                new Song(11L, "Curtain Call - The Hits", "Eminem", "Fack", "Hip Hop"),
                new Song(12L, "The Calling", "Hilltop Hoods", "The Calling", "Hip Hop")
        );

        // Send Songs once
        songs.forEach(song -> {
            this.songsTemplate.sendDefault(song.getId(), song);
        });

        // Send a play event every X milliseconds
        int i = 0;
        while (true) {
            final Song song = songs.get(random.nextInt(songs.size()));
            System.out.print(song.getId() + "/");
            i = (i+1) % songs.size();
            if (i == 0) {
                System.out.println();
            }
            this.playEventsTemplate.sendDefault(song.getId() + "", new PlayEvent(song.getId(),
                    this.properties.getMinChartableDuration() * 2));
            Thread.sleep(this.properties.getWaitBetweenPlaySongMs());
        }
    }

    @Bean
    public KafkaTemplate<Long, Song> songsTemplate(PlaySongsGeneratorApplicationProperties properties) {
        final SpecificAvroSerializer<Song> songSerializer = new SpecificAvroSerializer<>();
        songSerializer.configure(Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                properties.getSchemaRegistryServer()), false);

        Map<String, Object> songsProperties = new HashMap<>(properties.getCommonProperties());
        songsProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        songsProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, songSerializer.getClass());

        DefaultKafkaProducerFactory<Long, Song> pf1 = new DefaultKafkaProducerFactory<>(songsProperties);
        KafkaTemplate<Long, Song> songsTemplate = new KafkaTemplate<>(pf1, true);
        songsTemplate.setDefaultTopic(properties.getSongsTopic());

        return songsTemplate;
    }

    @Bean
    public KafkaTemplate<String, PlayEvent> playEventsTemplate(PlaySongsGeneratorApplicationProperties properties) {

        final SpecificAvroSerializer<PlayEvent> playEventSerializer = new SpecificAvroSerializer<>();
        playEventSerializer.configure(Collections.singletonMap(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                properties.getSchemaRegistryServer()), false);

        Map<String, Object> playEventsProperties = new HashMap<>(properties.getCommonProperties());
        playEventsProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        playEventsProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, playEventSerializer.getClass());

        DefaultKafkaProducerFactory<String, PlayEvent> pf = new DefaultKafkaProducerFactory<>(playEventsProperties);
        KafkaTemplate<String, PlayEvent> playEventsTemplate = new KafkaTemplate<>(pf, true);
        playEventsTemplate.setDefaultTopic(properties.getPlayEventsTopic());

        return playEventsTemplate;
    }
}
