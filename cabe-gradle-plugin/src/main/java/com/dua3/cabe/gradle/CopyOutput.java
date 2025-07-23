package com.dua3.cabe.gradle;

import org.gradle.api.GradleException;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is responsible for copies the output of a Reader to a specified Consumer
 * and stores the first 10 lines to be printed later using {@code }toString()}.
 */
class CopyOutput implements AutoCloseable {
    public static final int MAX_LINES = 10;
    Thread thread;
    List<String> firstLines = new ArrayList<>();

    CopyOutput(Reader reader, Consumer<String> printer) {
        thread = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(reader)) {
                String line;
                while ((line = r.readLine()) != null) {
                    printer.accept(line);
                    if (firstLines.size() < MAX_LINES) {
                        firstLines.add(line);
                    } else if (firstLines.size() == MAX_LINES) {
                        firstLines.add("...");
                    }
                }
            } catch (Exception e) {
                throw new GradleException("exception reading ClassPatcher error output");
            }
        });
        thread.start();
    }

    @Override
    public void close() {
        try {
            thread.join(5000); // Wait 5000ms for the thread to die.
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (thread.isAlive()) {
            thread.interrupt();
        }
    }

    @Override
    public String toString() {
        return String.join("\n", firstLines);
    }
}
