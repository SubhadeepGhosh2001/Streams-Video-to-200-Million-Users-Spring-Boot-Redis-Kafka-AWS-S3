package com.netflix.encodingservice.service;

import com.netflix.encodingservice.event.VideoEncodedEvent;
import com.netflix.encodingservice.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;



@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final S3Client s3Client;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;
    @Value("${encoding.base-path}")
    private String basePath;

    private static final List<int[]> VIDEO_QUALITIES= Arrays.asList(
            new int[]{1920, 5000, 1080}, // 1080p 5000 kbps
            new int[]{1280,2800, 720},  // 720p 2800 kbps
            new int[]{854,1200, 480},   // 480p 1200 kbps
            new int[]{640,800, 360}    // 360p 800 kbps
    );
    private static final String VIDEO_ENCODED_TOPIC = "video-encoded";



    public void encodeVideo(VideoUploadedEvent event){
        log.info("Starting encoding platform for movie: {}", event.getMovieId());

        String jobPath=basePath+"/"+event.getMovieId();
try {
            // Encoding logic here
    Files.createDirectories(Paths.get(jobPath));
    Files.createDirectories(Paths.get(jobPath+"/encoded"));
    String localVideoPath=jobPath+"/raw_video.mp4";
    // Download the video from S3 to local path
    downloadFromS3(event.getVideoKey(), localVideoPath);
    for(int[] quality: VIDEO_QUALITIES){
        int width=quality[0];
        int bitrate=quality[1];
        int height=quality[2];
        String qualityDir = jobPath + "/encoded/" + height + "p";
        Files.createDirectories(Paths.get(qualityDir));

        encodeToHLS(localVideoPath, qualityDir, width, height, bitrate);
        log.info("Encoded {}p successfully ", height);

// Step 4: Generate master playlist
        String masterPlaylistPath = jobPath + "/encoded/master.m3u8";
        generateMasterPlaylist(masterPlaylistPath);

        String encodedPrefix = "encoded/" + event.getMovieId() + "/";
        uploadEncodedFilesToS3(jobPath + "/encoded", encodedPrefix);
        String masterPlayListKey = encodedPrefix + "master.m3u8";
        String hlsUrl = "https://" + bucketName + ".s3.amazonaws.com/" + masterPlayListKey;
        // Publish an event to Kafka indicating that the video has been encoded
        VideoEncodedEvent encodedEvent = new VideoEncodedEvent(
                event.getMovieId(),
                hlsUrl,
                masterPlayListKey,
                true,
                null
        );
        kafkaTemplate.send(VIDEO_ENCODED_TOPIC,event.getMovieId(), encodedEvent);
    }

} catch (Exception e) {
            log.error("Error occurred while encoding video: {}", e.getMessage());
            VideoEncodedEvent failedEvent = new VideoEncodedEvent(
                    event.getMovieId(),
                    null,
                    null,
                    false,
                    e.getMessage()
            );
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC,event.getMovieId(), failedEvent);
        }
finally {
    cleanupTemporaryFiles(jobPath);
}
    }
    private void downloadFromS3(String s3Key, String localPath) {
        // Implement the logic to download the video from S3 to the local path
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();
        s3Client.getObject(request, Paths.get(localPath));
    }
    private void encodeToHLS(String inputPath, String outputDir, int width, int height, int bitrate) throws Exception {

        // Implement the logic to encode the video to HLS format using FFmpeg
       String playlistPath = outputDir + "/playlist.m3u8";
       String segmentPath = outputDir + "/segment_%03d.ts ";
        List<String> commands = Arrays.asList(
                ffmpegPath,
                "-i", inputPath,  //Input file
                "-vf", "scale=" + width + ":" + height, //Scale video to desired resolution
                "-c:v", "libx264",  //Use H.264 codec for video
                "-b:v", bitrate + "k", //Set video bitrate
                "-c:a", "aac",  //Use AAC codec for audio
                "-b:a", "128k", //Set audio bitrate
                "-hls_time", "10", //Set segment duration to 10 seconds
                "-hls_list_size", "0", //Set playlist size to unlimited
                "-hls_segment_filename", segmentPath, //Set segment file naming pattern
                "-f", "hls",  //Output format
                playlistPath
        );
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        if(exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed with exit code: " + exitCode);
        }
    }
    private void generateMasterPlaylist(String masterPlaylistPath) throws IOException {
        // Implement the logic to generate the master playlist
        StringBuilder master=new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("EXT-X-VERSION:3\n\n");
        int [][] qualities={{1920, 5000, 1080}, {1280,2800, 720}, {854,1200, 480}, {640,800, 360}};
        for(int[]q : qualities){
            int width=q[0];
            int bitrate=q[1];
            int height=q[2];
            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(bitrate * 1000)
                    .append(",RESOLUTION=")
                    .append(width)
                    .append("x")
                    .append(height)
                    .append(",CODECS=\"avc1.42e01e,mp4a.40.2\"\n");
            master.append(height)
                    .append("p/playlist.m3u8\n");
        }

        Files.writeString(Paths.get(masterPlaylistPath), master.toString());
    }
    private void uploadEncodedFilesToS3(String localDir, String s3Prefix) throws IOException {
        // Implement the logic to upload the encoded files to S3
        File directory = new File(localDir);
        uploadDirectoryToS3(directory,localDir, s3Prefix);
    }
    private void uploadDirectoryToS3(File directory,String baseDir, String s3Prefix) throws IOException {
        // Implement the logic to upload the directory to S3
       for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                uploadDirectoryToS3(file, baseDir, s3Prefix);
            } else {
                String relativrPath = file.getAbsolutePath().substring(baseDir.length() + 1).replace("\\", "/");
                String s3Key = s3Prefix + "/" + relativrPath;
                String contentType = file.getName().endsWith(".m3u8") ? "application/xmpegurl" : "video/mp2T";

                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
                log.debug("Uploaded file {} to S3 with key: {}", file.getAbsolutePath(), s3Key);
            }
        }
    }
    private void cleanupTemporaryFiles(String jobPath) {
        // Implement the logic to clean up temporary files
        try {
            Path dirPath = Paths.get(jobPath);

            if (Files.exists(dirPath)) {
                Files.walk(dirPath)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);

                log.info("Temp files cleaned up for job: {}", jobPath);
        }} catch (IOException e) {
            log.warn("Failed to clean up temporary files for job path: {}. Error: {}", jobPath, e.getMessage());
        }
    }
}
