package com.onelpro.librelog.rumble.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JazzSftpServiceTest {

    @TempDir
    Path tempDir;

    /** Fakes the JSch seam: no real SSH server involved. */
    private static class FakeJazz extends JazzSftpService {
        final Session session = mock(Session.class);
        final ChannelSftp sftp = mock(ChannelSftp.class);
        String executedCommand;
        ExecResult nextExecResult = new ExecResult(0, "import done SUCCESS");

        FakeJazz(String host) {
            super(host, "rumble_bridge", 22, "/keys/id_rsa",
                    "/srv/libretime/rumble_import", "/usr/local/bin/rumble_import_trigger.sh");
        }

        @Override
        protected Session openSession() {
            return session;
        }

        @Override
        protected ChannelSftp channelSftp(Session s) {
            return sftp;
        }

        @Override
        protected ExecResult exec(Session s, String command) {
            executedCommand = command;
            return nextExecResult;
        }
    }

    private File localFile() throws Exception {
        File f = tempDir.resolve("track.mp3").toFile();
        Files.writeString(f.toPath(), "fake mp3 bytes");
        return f;
    }

    @Test
    void uploadsToImportPathAndRunsTrigger() throws Exception {
        FakeJazz jazz = new FakeJazz("jazz.example.com");
        File local = localFile();

        jazz.uploadAndImport(local, "abc-123.mp3");

        verify(jazz.sftp).put(local.getAbsolutePath(), "/srv/libretime/rumble_import/abc-123.mp3");
        assertThat(jazz.executedCommand)
                .isEqualTo("/usr/local/bin/rumble_import_trigger.sh /srv/libretime/rumble_import/abc-123.mp3");
    }

    @Test
    void nonZeroExitStatusThrowsWithRemoteOutput() throws Exception {
        FakeJazz jazz = new FakeJazz("jazz.example.com");
        jazz.nextExecResult = new JazzSftpService.ExecResult(1, "libretime-api: file rejected");

        assertThatThrownBy(() -> jazz.uploadAndImport(localFile(), "x.mp3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("libretime-api: file rejected");
    }

    @Test
    void zeroExitWithoutSuccessMarkerThrows() throws Exception {
        FakeJazz jazz = new FakeJazz("jazz.example.com");
        jazz.nextExecResult = new JazzSftpService.ExecResult(0, "nothing happened");

        assertThatThrownBy(() -> jazz.uploadAndImport(localFile(), "x.mp3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nothing happened");
    }

    @Test
    void unconfiguredHostFailsFast() throws Exception {
        FakeJazz jazz = new FakeJazz(" ");

        assertThat(jazz.isConfigured()).isFalse();
        assertThatThrownBy(() -> jazz.uploadAndImport(localFile(), "x.mp3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }
}
