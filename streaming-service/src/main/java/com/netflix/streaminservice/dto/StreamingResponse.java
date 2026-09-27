package com.netflix.streaminservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamingResponse {
    private String movieId;
    private String streamingUrl;
    private String quality;
    private long expiredMinutes;

}
