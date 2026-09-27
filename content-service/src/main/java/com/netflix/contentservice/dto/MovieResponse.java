package com.netflix.contentservice.dto;

import com.netflix.contentservice.model.Genre;
import com.netflix.contentservice.model.VideoStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
public class MovieResponse {
    private String id;
    private String title;
    private String description;
    private Genre genre;
    private String director;
    private String cast;
    private int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes; // Duration in minutes

    //S3 key for the movie file
    private String videoS3Key;

    //HLS master playlist URL for the streaming video
    private String hlsUrl;
    private VideoStatus videoStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
