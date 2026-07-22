package com.onelpro.librelog.rumble.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class AudioTranscodePipeline {

    private static final String DEFAULT_ARTIST = "LibreLog VT";

    private final Path transcodeDirectory;

    public AudioTranscodePipeline(
            @Value("${librelog.rumble.transcode-dir:/var/librelog/transcode}") Path transcodeDirectory) {
        this.transcodeDirectory = transcodeDirectory;
    }

    public File normalizeAndFormatAudio(File rawUploadFile, String trackTitle) throws Exception {
        return normalizeAndFormatAudio(rawUploadFile, DEFAULT_ARTIST, trackTitle);
    }

    public File normalizeAndFormatAudio(File rawUploadFile, String artistTag, String trackTitle) throws Exception {
        if (rawUploadFile == null || !rawUploadFile.isFile()) {
            throw new IllegalArgumentException("rawUploadFile must point to an existing file");
        }
        Files.createDirectories(transcodeDirectory);
        File processedOutputFile = transcodeDirectory.resolve(UUID.randomUUID() + ".mp3").toFile();

        String[] ffmpegCommand = {
                "ffmpeg", "-i", rawUploadFile.getAbsolutePath(),
                "-af", "loudnorm=I=-14:TP=-2.0:LRA=11",
                "-acodec", "libmp3lame",
                "-b:a", "192k",
                "-ar", "44100",
                "-ac", "2",
                "-metadata", "artist=" + safeArtist(artistTag),
                "-metadata", "title=" + safeTitle(trackTitle),
                "-y", processedOutputFile.getAbsolutePath()
        };

        StringBuilder output = new StringBuilder();
        int returnCode = runProcess(ffmpegCommand, output);
        if (returnCode != 0) {
            throw new IllegalStateException("FFmpeg execution failure. Exit code "
                    + returnCode + System.lineSeparator() + output);
        }

        return processedOutputFile;
    }

    public int probeDurationSeconds(File audioFile) throws Exception {
        if (audioFile == null || !audioFile.isFile()) {
            throw new IllegalArgumentException("audioFile must point to an existing file");
        }
        String[] ffprobeCommand = {
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                audioFile.getAbsolutePath()
        };
        StringBuilder output = new StringBuilder();
        int returnCode = runProcess(ffprobeCommand, output);
        if (returnCode != 0) {
            throw new IllegalStateException("FFprobe execution failure. Exit code "
                    + returnCode + System.lineSeparator() + output);
        }
        try {
            return (int) Math.round(Double.parseDouble(output.toString().trim()));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not parse ffprobe duration output: " + output, e);
        }
    }

    protected int runProcess(String[] cmd, StringBuilder capturedOutput) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String logLine;
            while ((logLine = reader.readLine()) != null) {
                capturedOutput.append(logLine).append(System.lineSeparator());
            }
        }

        return process.waitFor();
    }

    private static String safeArtist(String artistTag) {
        return artistTag == null || artistTag.isBlank() ? DEFAULT_ARTIST : artistTag;
    }

    private static String safeTitle(String trackTitle) {
        return trackTitle == null || trackTitle.isBlank() ? "Untitled Voice Track" : trackTitle;
    }
}
