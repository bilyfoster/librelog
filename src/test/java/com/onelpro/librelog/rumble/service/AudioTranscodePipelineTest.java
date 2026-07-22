package com.onelpro.librelog.rumble.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AudioTranscodePipelineTest {

    @TempDir
    Path tempDir;

    /** Records the command instead of shelling out to ffmpeg/ffprobe. */
    private static class RecordingPipeline extends AudioTranscodePipeline {
        String[] lastCommand;
        String cannedOutput = "";
        int cannedExitCode = 0;

        RecordingPipeline(Path transcodeDirectory) {
            super(transcodeDirectory);
        }

        @Override
        protected int runProcess(String[] cmd, StringBuilder capturedOutput) {
            lastCommand = cmd;
            capturedOutput.append(cannedOutput);
            return cannedExitCode;
        }
    }

    private RecordingPipeline newPipeline() {
        return new RecordingPipeline(tempDir.resolve("transcode"));
    }

    private File rawUpload() throws Exception {
        File f = tempDir.resolve("raw-upload.wav").toFile();
        Files.writeString(f.toPath(), "fake audio bytes");
        return f;
    }

    @Test
    void ffmpegCommandCarriesLoudnormFormatAndDefaultMetadata() throws Exception {
        RecordingPipeline pipeline = newPipeline();
        pipeline.normalizeAndFormatAudio(rawUpload(), "My Track");

        List<String> cmd = Arrays.asList(pipeline.lastCommand);
        assertThat(cmd).contains("loudnorm=I=-14:TP=-2.0:LRA=11");
        assertThat(cmd).contains("libmp3lame");
        assertThat(cmd).contains("192k");
        assertThat(cmd).contains("44100");
        assertThat(cmd).contains("artist=LibreLog VT");
        assertThat(cmd).contains("title=My Track");
        assertThat(cmd.get(0)).isEqualTo("ffmpeg");
        assertThat(cmd.get(cmd.size() - 1)).endsWith(".mp3");
    }

    @Test
    void threeArgOverloadUsesGivenArtistAndTitle() throws Exception {
        RecordingPipeline pipeline = newPipeline();
        pipeline.normalizeAndFormatAudio(rawUpload(), "DJ Smooth", "Morning Intro");

        List<String> cmd = Arrays.asList(pipeline.lastCommand);
        assertThat(cmd).contains("artist=DJ Smooth");
        assertThat(cmd).contains("title=Morning Intro");
    }

    @Test
    void blankTitleFallsBackToUntitled() throws Exception {
        RecordingPipeline pipeline = newPipeline();
        pipeline.normalizeAndFormatAudio(rawUpload(), " ");

        assertThat(Arrays.asList(pipeline.lastCommand)).contains("title=Untitled Voice Track");
    }

    @Test
    void nonZeroFfmpegExitThrowsWithOutput() {
        RecordingPipeline pipeline = newPipeline();
        pipeline.cannedExitCode = 1;
        pipeline.cannedOutput = "boom";

        assertThatThrownBy(() -> pipeline.normalizeAndFormatAudio(rawUpload(), "x"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void probeDurationParsesAndRoundsSeconds() throws Exception {
        RecordingPipeline pipeline = newPipeline();
        pipeline.cannedOutput = "63.812345\n";

        int seconds = pipeline.probeDurationSeconds(rawUpload());

        assertThat(seconds).isEqualTo(64);
        List<String> cmd = Arrays.asList(pipeline.lastCommand);
        assertThat(cmd.get(0)).isEqualTo("ffprobe");
        assertThat(cmd).contains("format=duration");
        assertThat(cmd).contains("default=noprint_wrappers=1:nokey=1");
    }

    @Test
    void probeDurationThrowsOnFailureAndOnGarbage() {
        RecordingPipeline failing = newPipeline();
        failing.cannedExitCode = 1;
        failing.cannedOutput = "no such file";
        File raw;
        try {
            raw = rawUpload();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertThatThrownBy(() -> failing.probeDurationSeconds(raw))
                .isInstanceOf(IllegalStateException.class);

        RecordingPipeline garbage = newPipeline();
        garbage.cannedOutput = "not-a-number";
        assertThatThrownBy(() -> garbage.probeDurationSeconds(raw))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not-a-number");
    }
}
