package com.sandbox;

public record JudgeResult(Verdict verdict, String detail, int passed, int total) {}