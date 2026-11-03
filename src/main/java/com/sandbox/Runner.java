package com.sandbox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Runner {

    public static RunResult run(List<String> command, Path workDir, String stdin,
                                long timeoutMs, int maxOutputBytes)
            throws IOException, InterruptedException {
        long start = System.nanoTime();
        Process p = new ProcessBuilder(command).directory(workDir.toFile()).start();

        CompletableFuture<String> out =
            CompletableFuture.supplyAsync(() -> read(p.getInputStream(), maxOutputBytes));
        CompletableFuture<String> err =
            CompletableFuture.supplyAsync(() -> read(p.getErrorStream(), maxOutputBytes));

        // Feed stdin on its own thread so a program that never reads can't block us.
        CompletableFuture.runAsync(() -> {
            try (OutputStream os = p.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // process exited before reading everything
            }
        });

        boolean finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        killTree(p);
        p.waitFor();

        long millis = (System.nanoTime() - start) / 1_000_000;
        int exit = finished ? p.exitValue() : -1;
        return new RunResult(out.join(), err.join(), exit, !finished, millis);
    }

    private static void killTree(Process p) {
        p.descendants().forEach(ProcessHandle::destroyForcibly);
        p.destroyForcibly();
    }

    private static String read(InputStream in, int max) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        boolean truncated = false;
        try {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                int room = max - buf.size();
                if (room > 0) buf.write(chunk, 0, Math.min(n, room));
                if (n > room) truncated = true;
            }
        } catch (IOException e) {
            // stream closed because the process was killed
        }
        String s = buf.toString(StandardCharsets.UTF_8);
        return truncated ? s + "\n[output truncated]" : s;
    }
}