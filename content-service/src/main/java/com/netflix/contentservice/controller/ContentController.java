package com.netflix.contentservice.controller;


import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/movies")
@Slf4j
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
 @PostMapping
 public ResponseEntity<MovieResponse> _addMovie(@Valid @RequestBody MovieRequest movieRequest) {
        log.info("Received request to add movie: {}", movieRequest);
         return  ResponseEntity.status(HttpStatus.CREATED).body(contentService.addMovie(movieRequest));
    }
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getallMovies() {
        log.info("Received request to get all movies");
        return ResponseEntity.ok(contentService.getAllMovies());
    }
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(@PathVariable Genre genre) {
        log.info("Received request to get movies by genre: {}", genre);
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable String movieId) {
        log.info("Received request to get movie by ID: {}", movieId);
        return ResponseEntity.ok(contentService.getMovieById(movieId));
    }
    @GetMapping("/search")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String title) {
        log.info("Received request to search movies by title: {}", title);
        return ResponseEntity.ok(contentService.searchMovies(title));
    }
}