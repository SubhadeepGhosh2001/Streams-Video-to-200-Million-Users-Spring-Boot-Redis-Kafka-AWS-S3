package com.netflix.contentservice.model;

public enum VideoStatus {
    //PENDING: The video is uploaded but not yet processed.
    PENDING,
    //UPLOADED: The video has been uploaded to the S3.
    UPLOADED,
    //ENCODING: The video is currently being encoded.
    //FFMPEG is used for encoding the video into HLS format.
    ENCODING,
    //ENCODED: The video has been encoded.
    ENCODED,
    //READY: The video has been processed and is ready for streaming.
    READY,
    //FAILED: The video processing failed.
    FAILED
}
