package com.sandbox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

public class Judge {

    private static final long COMPILE_TIMEOUT_MS = 15_000;
    private static final long RUN_TIMEOUT_MS = 2_000;
    private static final int MAX_OUTPUT = 10_000;

    public static JudgeResult judge(String source, List<TestCase> tests)
            throws IOException, InterruptedException {
        Path dir = Files.createTempDirectory("submission-");
        try {
            Files.writeString(dir.resolve("Main.java"), source);

            RunResult compile = Runner.run(List.of("javac", "Main.java"), dir, "",
                                           COMPILE_TIMEOUT_MS, MAX_OUTPUT);
            if (compile.timedOut() || compile.exitCode() != 0) {
                String msg = compile.timedOut() ? "compilation timed out" : compile.stderr();
                return new JudgeResult(Verdict.COMPILE_ERROR, shorten(msg), 0, tests.size());
            }

            int passed = 0;
            for (int i = 0; i < tests.size(); i++) {
                TestCase t = tests.get(i);
                RunResult r = Runner.run(
                    List.of("java", "-Xmx64m", "-Xss1m", "-XX:+UseSerialGC", "-cp", ".", "Main"),
                    dir, t.input(), RUN_TIMEOUT_MS, MAX_OUTPUT);

                if (r.timedOut()) {
                    return new JudgeResult(Verdict.TIME_LIMIT_EXCEEDED, "test " + (i + 1),
                                           passed, tests.size());
                }
                if (r.exitCode() != 0) {
                    return new JudgeResult(Verdict.RUNTIME_ERROR,
                                           "test " + (i + 1) + ": " + shorten(r.stderr()),
                                           passed, tests.size());
                }
                if (!normalize(r.stdout()).equals(normalize(t.expected()))) {
                    return new JudgeResult(Verdict.WRONG_ANSWER, "test " + (i + 1),
                                           passed, tests.size());
                }
                passed++;
            }
            return new JudgeResult(Verdict.ACCEPTED, "", passed, tests.size());
        } finally {
            deleteRecursively(dir);
        }
    }

    // Ignore \r\n vs \n and trailing spaces/newlines, as most judges do.
    private static String normalize(String s) {
        return s.replace("\r\n", "\n").lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("")
                .stripTrailing();
    }

    private static String shorten(String s) {
        s = s.strip();
        return s.length() <= 300 ? s : s.substring(0, 300) + "...";
    }

    private static void deleteRecursively(Path dir) {
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {}
    }
}