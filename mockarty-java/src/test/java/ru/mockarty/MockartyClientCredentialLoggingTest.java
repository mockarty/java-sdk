package ru.mockarty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockartyClientCredentialLoggingTest {
    @Test
    void requestTraceDoesNotExposeRejectedConnectionCredential() throws Exception {
        String javaBinary = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", javaBinary).toString(),
                "-Dorg.slf4j.simpleLogger.log.ru.mockarty.MockartyClient=trace",
                "-cp", childClasspath(), ConnectionCredentialLogProbe.class.getName())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        assertFalse(output.contains(ConnectionCredentialLogProbe.SECRET), output);
    }

    private static String childClasspath() throws Exception {
        Set<String> entries = new LinkedHashSet<>();
        addCodeSource(entries, ConnectionCredentialLogProbe.class);
        addCodeSource(entries, MockartyClient.class);
        addCodeSource(entries, ObjectMapper.class);
        addCodeSource(entries, JsonFactory.class);
        addCodeSource(entries, JsonProperty.class);
        addCodeSource(entries, LoggerFactory.class);
        addCodeSource(entries, Class.forName("org.slf4j.simple.SimpleLogger"));
        return String.join(File.pathSeparator, entries);
    }

    private static void addCodeSource(Set<String> entries, Class<?> type) throws Exception {
        URL location = type.getProtectionDomain().getCodeSource().getLocation();
        entries.add(Path.of(location.toURI()).toString());
    }
}
