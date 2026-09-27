package com.netflix.streaminservice.service;


import com.netflix.streaminservice.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedEventConsumer {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX =
            "streaming:playlist:";

    /**
     * Listens to video-encoded Kafka topic.
     * Stores master playlist key in Redis when encoding is complete.
     * This allows StreamingService to quickly find the playlist key by movieId.
     */
    @KafkaListener(
            topics = "video-encoded",
            groupId = "streaming-service-group"
    )
    public void consumeVideoEncodedEvent(VideoEncodedEvent event) {

        log.info(
                "Consumed VideoEncodedEvent for movie: {} success: {}",
                event.getMovieId(),
                event.isSuccess()
        );

        if (event.isSuccess()) {

            String redisKey =
                    MASTER_PLAYLIST_KEY_PREFIX + event.getMovieId();

            redisTemplate.opsForValue().set(
                    redisKey,
                    event.getMasterPlaylistKey()
            );

            log.info(
                    "Stored master playlist key in Redis for movie: {}",
                    event.getMovieId()
            );
        } else {
            log.error(
                    "Video encoding failed for movie: {}. Error: {}",
                    event.getMovieId(),
                    event.getErrorMessage()
            );
        }
    }
}