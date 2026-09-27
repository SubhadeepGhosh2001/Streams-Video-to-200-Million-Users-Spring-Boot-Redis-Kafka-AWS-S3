package com.netflix.streaminservice.service;

import com.netflix.streaminservice.dto.StreamingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String STREAMING_URL_CACHE_PREFIX = "streaming:url:";

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiry}")
    private long presignedUrlExpiry;

    public StreamingResponse getStreamingUrl(
            String movieId,
            String playlistKey
    ) {

        String cacheKey =
                STREAMING_URL_CACHE_PREFIX + movieId;

        // 1. Check Redis
        String cacheUrl =
                redisTemplate.opsForValue().get(cacheKey);

        if (cacheUrl != null) {

            log.info(
                    "Returning cached streaming URL for movie: {}",
                    movieId
            );

            return new StreamingResponse(
                    movieId,
                    cacheUrl,
                    "1080p,720p,480p,360p",
                    presignedUrlExpiry
            );
        }

        // 2. Generate new presigned URL
        String presignedUrl =
                generatePresignedUrl(playlistKey);

        // 3. Store URL in Redis for 55 minutes
        redisTemplate.opsForValue().set(
                cacheKey,
                presignedUrl,
                55,
                TimeUnit.MINUTES
        );

        log.info(
                "Generated new streaming URL for movie: {}",
                movieId
        );

        // 4. Return response
        return new StreamingResponse(
                movieId,
                presignedUrl,
                "1080p,720p,480p,360p",
                presignedUrlExpiry
        );
    }
    public String getSignedPlaylist(String movieId, String playlistPath) {

        // Get base path for this playlist
        String basePath = playlistPath.substring(
                0,
                playlistPath.lastIndexOf('/') + 1
        );

        // Read m3u8 content from S3
        String m3u8Content = readFromS3(playlistPath);

        // Rewrite m3u8 content to replace segment URLs with signed URLs
        String signedContent = rewriteM3u8WithSignedUrls(
                m3u8Content,
                basePath
        );

        return signedContent;
    }
    private String rewriteM3u8WithSignedUrls(
            String content,
            String basePath
    ) {
        StringBuilder rewritten = new StringBuilder();

        String[] lines = content.split("\n");

        for (String line : lines) {

            String trimmed = line.trim();

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                rewritten.append(line).append("\n");
                continue;
            }

            // This is a segment or playlist reference
            // Build full S3 key and sign it
            String fullKey = basePath + trimmed;

            String signedUrl = generatePresignedUrl(fullKey);

            rewritten.append(signedUrl).append("\n");
        }

        return rewritten.toString();
    }
    private String readFromS3(String s3Key) {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> response =
                s3Client.getObject(request);

        return new BufferedReader(new InputStreamReader(response))
                .lines()
                .collect(Collectors.joining("\n"));
    }
    private String generatePresignedUrl(String key) {

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(
                                Duration.ofMinutes(presignedUrlExpiry)
                        )
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner
                .presignGetObject(presignRequest)
                .url()
                .toString();
    }
    public void invalidateCache(String movieId){
        String cacheKey=STREAMING_URL_CACHE_PREFIX+movieId;
        redisTemplate.delete(cacheKey);
    }
}