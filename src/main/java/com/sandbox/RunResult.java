   package com.sandbox;

   public record RunResult(String stdout, String stderr, int exitCode, boolean timedOut, long millis) {}