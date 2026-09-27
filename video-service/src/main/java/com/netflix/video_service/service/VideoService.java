package com.netflix.video_service.service;


import com.netflix.video_service.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final String VIDEO_UPLOADED_TOPIC = "video-uploaded";
 public String uploadVideo(String movieId, MultipartFile file) throws IOException {
        try {
            // Generate a unique key for the video file
            String videoKey = "raw/" + movieId + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(videoKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            // Upload the video to S3
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Video uploaded successfully to S3 with key: {}", videoKey);

            // Publish an event to Kafka indicating that the video has been uploaded
            VideoUploadedEvent event = new VideoUploadedEvent(
                    movieId,
                    videoKey,
                    bucketName,
                    file.getOriginalFilename(),
                    file.getSize()
            );
            kafkaTemplate.send(VIDEO_UPLOADED_TOPIC,movieId, event);
            log.info("Published VideoUploadedEvent to Kafka for movie: {}", movieId);

            return videoKey;
        } catch (Exception e) {
            log.error("Failed to upload video for movie: {}. Error: {}", movieId, e.getMessage());
            throw new RuntimeException("Failed to upload video", e);
        }
    }

}
