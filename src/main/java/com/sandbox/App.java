package com.sandbox;

import java.util.List;
import java.util.Map;

public class App {
    public static void main(String[] args) throws Exception {
        List<TestCase> tests = List.of(
            new TestCase("2 3\n", "5\n"),
            new TestCase("10 -4\n", "6\n"));

        Map<String, String> submissions = new java.util.LinkedHashMap<>();

        submissions.put("correct", """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] a) {
                    Scanner s = new Scanner(System.in);
                    System.out.println(s.nextInt() + s.nextInt());
                }
            }""");

        submissions.put("wrong answer", """
            import java.util.Scanner;
            public class Main {
                public static void main(String[] a) {
                    Scanner s = new Scanner(System.in);
                    System.out.println(s.nextInt() + s.nextInt() + 1);
                }
            }""");

        submissions.put("compile error", """
            public class Main {
                public static void main(String[] a) {
                    System.out.println("hi")
                }
            }""");

        submissions.put("infinite loop", """
            public class Main {
                public static void main(String[] a) { while (true) {} }
            }""");

        submissions.put("out of memory", """
            import java.util.*;
            public class Main {
                public static void main(String[] a) {
                    List<long[]> hog = new ArrayList<>();
                    while (true) hog.add(new long[1_000_000]);
                }
            }""");

        submissions.put("exception", """
            public class Main {
                public static void main(String[] a) {
                    System.out.println(1 / (a.length));
                }
            }""");

        for (var e : submissions.entrySet()) {
            System.out.println(e.getKey() + " -> " + Judge.judge(e.getValue(), tests));
        }
    }
}