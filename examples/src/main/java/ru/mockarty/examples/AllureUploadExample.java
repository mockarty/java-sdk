package ru.mockarty.examples;

import java.nio.file.Path;
import ru.mockarty.MockartyClient;

/** Upload result JSON, nested steps and small media. Missing files fail preflight.
 * Limits: 256 KiB/file, 1 MiB and 32 unique files/result, 32 MiB/batch.
 * Container fixtures are not imported by this helper. */
public final class AllureUploadExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("pass the allure-results directory");
        try (MockartyClient client = MockartyClient.create(System.getenv("MOCKARTY_SERVER"), System.getenv("MOCKARTY_API_KEY"))) {
            int count = client.externalRuns().uploadAllureDir(System.getenv("MOCKARTY_NAMESPACE"), Path.of(args[0])).size();
            System.out.println("Uploaded " + count + " results");
        }
    }
}
