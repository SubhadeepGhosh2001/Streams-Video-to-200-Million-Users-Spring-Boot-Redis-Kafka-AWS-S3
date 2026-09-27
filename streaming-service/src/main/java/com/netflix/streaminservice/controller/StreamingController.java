package com.netflix.streaminservice.controller;

import com.netflix.streaminservice.dto.StreamingResponse;
import com.netflix.streaminservice.service.StreamingService;
import com.netflix.streaminservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stream")
@Slf4j
@RequiredArgsConstructor
public class StreamingController {
    private final StreamingService streamingService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    @GetMapping("/{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(String movieId) {
        log.info("Received request to get streaming URL for movie: {}", movieId);
        String plalistKey = redisTemplate.opsForValue().get(MASTER_PLAYLIST_KEY_PREFIX + movieId);
        if (plalistKey == null) return ResponseEntity.notFound().build();
        StreamingResponse response = streamingService.getStreamingUrl(movieId, plalistKey);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{movieId}/playlist")
    public ResponseEntity<String>getSignedPlaylist(
            @PathVariable String movieId,
            @RequestParam String path
    ){
        String signedPlaylist=streamingService.getSignedPlaylist(movieId,path);
        return ResponseEntity.ok()
                .header("Content-Type","application/x-mpegURL")
                .body(signedPlaylist);

}}
