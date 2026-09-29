package com.netflix.contentservice.service;

import com.netflix.contentservice.dto.MovieRequest;
import com.netflix.contentservice.dto.MovieResponse;
import com.netflix.contentservice.model.Movie;
import com.netflix.contentservice.model.VideoStatus;
import com.netflix.contentservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContentService {
    private final MovieRepository movieRepository;

    public MovieResponse addMovie(MovieRequest movieRequest) {
        // Logic to add a movie to the repository
        // Convert MovieRequest to Movie entity and save it
        // Return a MovieResponse object
        Movie movie = new Movie();
        movie.setTitle(movieRequest.getTitle());
        movie.setDescription(movieRequest.getDescription());
        movie.setGenre(movieRequest.getGenre());
        movie.setDirector(movieRequest.getDirector());
        movie.setCast(movieRequest.getCast());
        movie.setReleaseYear(movieRequest.getReleaseYear());
        movie.setRating(movieRequest.getRating());
        movie.setThumbnailUrl(movieRequest.getThumbnailUrl());
        movie.setDurationMinutes(movieRequest.getDurationMinutes());
        movie.setVideoStatus(VideoStatus.PENDING);
        Movie savedMovie = movieRepository.save(movie);
        log.info("Movie added successfully: {}", savedMovie.getId());

        return mapToMovieResponse(savedMovie);
    }
    //Get All movies
    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        return movies.stream().map(this::mapToMovieResponse).toList();
    }
    public MovieResponse getMovieById(String id) {
        Movie movie = movieRepository.findById(id).orElseThrow(() -> new RuntimeException("Movie not found"+ id));
        return mapToMovieResponse(movie);
    }

    public List<MovieResponse> getMoviesByGenre(com.netflix.contentservice.model.Genre genre) {
        List<Movie> movies = movieRepository.findByGenre(genre);
        return movies.stream().map(this::mapToMovieResponse).toList();
    }
    public List<MovieResponse> searchMovies(String title) {
        List<Movie> movies = movieRepository.findByTitleContainingIgnoreCase(title);
        return movies.stream().map(this::mapToMovieResponse).toList();
    }
    public void updateVideoKey(String movieId, String videoKey) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
        movie.setVideoS3Key(videoKey);
        movie.setVideoStatus(VideoStatus.UPLOADED);
        movieRepository.save(movie);
        log.info("Updated video key for movie {}: {}", movieId, videoKey);
    }
    public void updateHlsUrl(String movieId, String hlsUrl) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
        movie.setHlsUrl(hlsUrl);
        movie.setVideoStatus(VideoStatus.READY);
        movieRepository.save(movie);
        log.info("Movie {} is ready with HLS URL: {}", movieId, hlsUrl);
    }
    public void updateVideoStatus(String movieId,VideoStatus videoStatus){
        Movie movie=movieRepository.findById(movieId)
                .orElseThrow(()->new RuntimeException("Movie not found:"+movieId));
        movie.setVideoStatus(videoStatus);
        movieRepository.save(movie);

    }

    private MovieResponse mapToMovieResponse(Movie movie) {
        MovieResponse response = new MovieResponse();

        response.setId(movie.getId());
        response.setTitle(movie.getTitle());
        response.setDescription(movie.getDescription());
        response.setGenre(movie.getGenre());
        response.setDirector(movie.getDirector());
        response.setCast(movie.getCast());
        response.setReleaseYear(movie.getReleaseYear());
        response.setRating(movie.getRating());
        response.setThumbnailUrl(movie.getThumbnailUrl());
        response.setDurationMinutes(movie.getDurationMinutes());
        response.setVideoS3Key(movie.getVideoS3Key());
        response.setVideoStatus(movie.getVideoStatus());
        response.setHlsUrl(movie.getHlsUrl());
        response.setCreatedAt(movie.getCreatedAt());

        return response;
    }
}
