package com.netflix.video_service.controller;


import com.netflix.video_service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/videos")
@Slf4j
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
@PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(@PathVariable String movieId, @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Video upload request received for movie: {} file size: {} MB", movieId, file.getSize() / (1024 * 1024));
        // Implement the logic to handle video upload
        if(file.isEmpty()) {
            log.error("Failed to upload video for movie: {}. File is empty.", movieId);
            return ResponseEntity.badRequest().body("File is empty");
        }
        String videoKey = videoService.uploadVideo(movieId, file);
        return ResponseEntity.ok("Video uploaded successfully with key: " + videoKey + "Encoding started automatically via kafka");
    }
}
