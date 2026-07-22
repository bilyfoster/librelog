package com.onelpro.librelog.rumble.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Ships a processed audio file to the Jazz (LibreTime) server over SFTP, then runs the
 * import trigger script there over an SSH exec channel. The trigger prints SUCCESS on
 * stdout when the import completes; anything else is treated as a failure.
 */
@Service
public class JazzSftpService {

    private static final int CONNECT_TIMEOUT_MS = 15_000;

    private final String host;
    private final String user;
    private final int port;
    private final String keyPath;
    private final String importPath;
    private final String importTrigger;

    public JazzSftpService(
            @Value("${librelog.jazz.host:}") String host,
            @Value("${librelog.jazz.user:rumble_bridge}") String user,
            @Value("${librelog.jazz.port:22}") int port,
            @Value("${librelog.jazz.key-path:}") String keyPath,
            @Value("${librelog.jazz.import-path:/srv/libretime/rumble_import}") String importPath,
            @Value("${librelog.jazz.import-trigger:/usr/local/bin/rumble_import_trigger.sh}") String importTrigger) {
        this.host = host;
        this.user = user;
        this.port = port;
        this.keyPath = keyPath;
        this.importPath = importPath;
        this.importTrigger = importTrigger;
    }

    public boolean isConfigured() {
        return host != null && !host.isBlank();
    }

    public void uploadAndImport(File localFile, String remoteFileName) {
        if (!isConfigured()) {
            throw new IllegalStateException("Jazz SFTP is not configured (librelog.jazz.host is empty)");
        }
        if (localFile == null || !localFile.isFile()) {
            throw new IllegalArgumentException("localFile must point to an existing file");
        }
        String remotePath = importPath + "/" + remoteFileName;
        Session session = null;
        try {
            session = openSession();
            session.connect(CONNECT_TIMEOUT_MS);

            ChannelSftp sftp = channelSftp(session);
            try {
                sftp.connect(CONNECT_TIMEOUT_MS);
                sftp.put(localFile.getAbsolutePath(), remotePath);
            } finally {
                sftp.disconnect();
            }

            ExecResult result = exec(session, importTrigger + " " + remotePath);
            if (result.exitStatus() != 0 || !result.output().contains("SUCCESS")) {
                throw new IllegalStateException("Jazz import failed (exit " + result.exitStatus()
                        + "): " + result.output());
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Jazz transfer failed: " + e.getMessage(), e);
        } finally {
            if (session != null) session.disconnect();
        }
    }

    protected Session openSession() throws JSchException {
        JSch jsch = new JSch();
        if (keyPath != null && !keyPath.isBlank()) {
            jsch.addIdentity(keyPath);
        }
        Session session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        return session;
    }

    protected ChannelSftp channelSftp(Session session) throws JSchException {
        return (ChannelSftp) session.openChannel("sftp");
    }

    protected ExecResult exec(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        try {
            channel.setCommand(command);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            channel.setOutputStream(out);
            channel.setErrStream(err);
            channel.connect(CONNECT_TIMEOUT_MS);
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }
            String combined = (out.toString(StandardCharsets.UTF_8) + "\n"
                    + err.toString(StandardCharsets.UTF_8)).trim();
            return new ExecResult(channel.getExitStatus(), combined);
        } finally {
            channel.disconnect();
        }
    }

    protected record ExecResult(int exitStatus, String output) {
    }
}
