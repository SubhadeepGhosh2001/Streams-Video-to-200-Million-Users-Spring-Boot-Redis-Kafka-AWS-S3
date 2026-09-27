package com.netflix.contentservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(nullable = false)
    private String title;
    @Column(length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    private Genre genre;
    private String director;
    private String cast;
    private int releaseYear;
    public double rating;
    private String thumbnailUrl;
    private int durationMinutes; // Duration in minutes

    //S3 key for the movie file
    private String videoS3Key;

    //HLS master playlist URL for the streaming video
    private String hlsUrl;
    @Enumerated(EnumType.STRING)
    private VideoStatus videoStatus;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
