// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Wraps a running {@code mockarty/cli:latest-mock} instance.
 *
 * <p>Designed as a drop-in replacement for {@code WireMockContainer}
 * and {@code MockServerContainer}. The container itself runs the real
 * {@code mockarty-cli mock serve} process baked into the image; this
 * class only owns the Docker lifecycle and exposes ergonomic admin
 * URLs + {@link #apply(Object)} / {@link #reset()} helpers so existing
 * WireMock and Mockarty native test bodies port over without changes.
 *
 * <p>Compatible with both manual lifecycle ({@code container.start();
 * ... container.stop();}) and JUnit5 auto-discovery via
 * {@link MockartyContainerExtension}.
 *
 * <p>Functional-options style: every optional knob is a chainable
 * {@code withXxx(...)} method that returns {@code this}, mirroring
 * GenericContainer's builder pattern and the Go / Python SDK
 * equivalents.
 */
public class MockartyContainer extends GenericContainer<MockartyContainer> {

    /** Canonical CLI image baked from {@code cmd/cli} with the {@code mock serve} entrypoint. */
    public static final String DEFAULT_IMAGE = "mockarty/cli:latest-mock";

    /** Unified HTTP listener inside the container (WireMock + Mockarty + Mockoon multiplex). */
    public static final int MOCK_PORT = 8080;

    /** Prometheus metrics + {@code /health}. */
    public static final int METRICS_PORT = 9090;

    /** In-container directory the CLI scans on startup for stub files. */
    public static final String STUBS_MOUNT = "/data/stubs";

    /** In-container directory mapped by {@link #withMappingDirectory}. */
    public static final String MAPPINGS_MOUNT = "/mocks";

    /** In-container path mapped by {@link #withHarReplay}. */
    public static final String HAR_MOUNT = "/har/traffic.har";

    /** Env-var the CLI reads to decide which stub dialect to expect.
     *  MUST match the CLI's applyMockServeEnv reader. The earlier
     *  draft used "MOCKARTY_STUB_FORMAT", which the CLI silently
     *  ignored (left container in auto-detect). Review #109/H1. */
    public static final String FORMAT_ENV = "MOCKARTY_MOCK_FORMAT";

    /** Env-var pointing the CLI at an in-container mappings directory. */
    public static final String MOCK_DIR_ENV = "MOCKARTY_MOCK_DIR";

    /** Env-var pointing the CLI at an in-container HAR file. */
    public static final String HAR_REPLAY_ENV = "MOCKARTY_HAR_REPLAY";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient;
    private Format format = Format.AUTO;
    /** Image name captured at construction time. Exposed via
     *  {@link #configuredImageName()} so callers can read the image
     *  reference without triggering testcontainers' lazy docker pull
     *  (which {@code getDockerImageName()} does). Useful in builder
     *  unit tests that run without a docker daemon. */
    private final String configuredImageName;

    /** Default constructor — uses {@link #DEFAULT_IMAGE}. */
    public MockartyContainer() {
        this(DockerImageName.parse(DEFAULT_IMAGE));
    }

    /** Override the image (e.g. private registry, pinned digest). */
    public MockartyContainer(DockerImageName imageName) {
        super(Objects.requireNonNull(imageName, "imageName"));
        this.configuredImageName = imageName.asCanonicalNameString();
        withExposedPorts(MOCK_PORT, METRICS_PORT);
        withEnv(FORMAT_ENV, format.slug());
        // Wait on the WireMock-compat admin health endpoint served on
        // the SAME 8080 listener as the mocks. The earlier draft polled
        // /health on METRICS_PORT (9090, Prometheus), which the
        // mock-serve subcommand doesn't bind — the container blocked
        // until startup timeout and then errored. Review #109/H2.
        waitingFor(Wait.forHttp("/__admin/health")
            .forPort(MOCK_PORT)
            .forStatusCode(200)
            .withStartupTimeout(STARTUP_TIMEOUT));
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();
    }

    /** Image-string shortcut. */
    public MockartyContainer(String imageReference) {
        this(DockerImageName.parse(imageReference));
    }

    /**
     * Return the docker image reference as supplied to the constructor.
     *
     * <p>Unlike {@link #getDockerImageName()} (inherited from
     * GenericContainer), this accessor does NOT trigger testcontainers'
     * lazy image-pull resolution — it just reads the locally cached
     * string captured at construction time. Use it in builder unit
     * tests that run on hosts without a docker daemon, or against
     * placeholder image names that aren't pullable.</p>
     */
    public String configuredImageName() {
        return configuredImageName;
    }

    // -----------------------------------------------------------------
    // Builder methods (functional-options analogue)
    // -----------------------------------------------------------------

    /**
     * Select the stub dialect; defaults to {@link Format#AUTO}. The
     * value is forwarded to the container as {@code MOCKARTY_STUB_FORMAT}.
     */
    public MockartyContainer withFormat(Format f) {
        this.format = Objects.requireNonNull(f, "format");
        withEnv(FORMAT_ENV, f.slug());
        return this;
    }

    /**
     * Mount a host-side stub file into {@link #STUBS_MOUNT} inside the
     * container. May be called repeatedly to mount multiple files.
     */
    public MockartyContainer withStubFile(Path hostPath) {
        Objects.requireNonNull(hostPath, "hostPath");
        Path abs = hostPath.toAbsolutePath();
        withCopyFileToContainer(
            MountableFile.forHostPath(abs),
            STUBS_MOUNT + "/" + abs.getFileName());
        return this;
    }

    /** String-path overload. */
    public MockartyContainer withStubFile(String hostPath) {
        if (hostPath == null || hostPath.isBlank()) {
            throw new IllegalArgumentException("stub file path must not be empty");
        }
        return withStubFile(Path.of(hostPath));
    }

    /**
     * Bind-mount a host directory of stub files (WireMock / Mockoon /
     * native Mockarty JSON) into {@link #MAPPINGS_MOUNT} and tell the
     * CLI to load it at startup. Drop-in replacement for the WireMock
     * testcontainers {@code withMappingFromResource} idiom — point at
     * an existing {@code src/test/resources/mocks} directory and it
     * loads on the first request.
     *
     * <p>The directory must exist; otherwise the container will fail
     * to start with a bind-mount error.
     */
    public MockartyContainer withMappingDirectory(Path hostDir) {
        Objects.requireNonNull(hostDir, "hostDir");
        Path abs = hostDir.toAbsolutePath();
        if (!java.nio.file.Files.isDirectory(abs)) {
            throw new IllegalArgumentException("mappings path is not a directory: " + abs);
        }
        withFileSystemBind(abs.toString(), MAPPINGS_MOUNT,
            org.testcontainers.containers.BindMode.READ_ONLY);
        withEnv(MOCK_DIR_ENV, MAPPINGS_MOUNT);
        return this;
    }

    /** String-path overload. */
    public MockartyContainer withMappingDirectory(String hostDir) {
        if (hostDir == null || hostDir.isBlank()) {
            throw new IllegalArgumentException("mappings dir must not be empty");
        }
        return withMappingDirectory(Path.of(hostDir));
    }

    /**
     * Bind-mount a HAR file into {@link #HAR_MOUNT} and tell the CLI
     * to replay its captured traffic at startup. Layer
     * {@link #withMappingDirectory(Path)} on top to add hand-crafted
     * overrides.
     */
    public MockartyContainer withHarReplay(Path hostFile) {
        Objects.requireNonNull(hostFile, "hostFile");
        Path abs = hostFile.toAbsolutePath();
        if (!java.nio.file.Files.isRegularFile(abs)) {
            throw new IllegalArgumentException("HAR path is not a file: " + abs);
        }
        withFileSystemBind(abs.toString(), HAR_MOUNT,
            org.testcontainers.containers.BindMode.READ_ONLY);
        withEnv(HAR_REPLAY_ENV, HAR_MOUNT);
        return this;
    }

    /** String-path overload. */
    public MockartyContainer withHarReplay(String hostFile) {
        if (hostFile == null || hostFile.isBlank()) {
            throw new IllegalArgumentException("HAR path must not be empty");
        }
        return withHarReplay(Path.of(hostFile));
    }

    // -----------------------------------------------------------------
    // Endpoint URLs
    // -----------------------------------------------------------------

    /**
     * Canonical base URL of the running mock — points at the
     * path-multiplexed listener that accepts both WireMock-compat and
     * Mockarty-native traffic. Use this for in-test HTTP clients.
     */
    public String url() {
        return "http://" + getHost() + ":" + getMappedPort(MOCK_PORT);
    }

    /**
     * WireMock admin API URL — append {@code /mappings}, {@code /reset}, etc.
     * The CLI image serves the WireMock admin API verbatim under
     * {@code /__admin/}.
     */
    public String wireMockUrl() {
        return url() + "/__admin";
    }

    /**
     * Mockarty native admin API URL — append {@code /mocks},
     * {@code /stores}, etc.
     */
    public String mockartyUrl() {
        return url() + "/api/v1";
    }

    /** Prometheus / health endpoint host:port. */
    public String metricsUrl() {
        return "http://" + getHost() + ":" + getMappedPort(METRICS_PORT);
    }

    // -----------------------------------------------------------------
    // Admin operations
    // -----------------------------------------------------------------

    /**
     * Register one Mockarty-native stub via
     * {@code POST /api/v1/mocks}.
     *
     * @param stub anything Jackson can serialise — a {@link java.util.Map},
     *             a generated DTO, a record.
     */
    public void apply(Object stub) {
        if (stub == null) {
            throw new IllegalArgumentException("stub must not be null");
        }
        try {
            byte[] body = MAPPER.writeValueAsBytes(stub);
            postJson(mockartyUrl() + "/mocks", body);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new MockartyContainerException("apply stub failed", e);
        }
    }

    /**
     * Wipe runtime state on the container — clears every applied stub,
     * request history, store contents and counters. Maps to the
     * WireMock-compatible {@code POST /__admin/reset} which the CLI
     * image also wires to its Mockarty internals.
     */
    public void reset() {
        try {
            postJson(wireMockUrl() + "/reset", new byte[0]);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new MockartyContainerException("reset failed", e);
        }
    }

    /** Returns the container stdout+stderr stream up to "now". */
    public String fetchLogs() {
        return getLogs();
    }

    private void postJson(String url, byte[] body) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(HTTP_TIMEOUT);
        if (body.length > 0) {
            req.header("Content-Type", "application/json");
            req.POST(HttpRequest.BodyPublishers.ofByteArray(body));
        } else {
            req.POST(HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> resp = httpClient.send(req.build(), HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) {
            throw new MockartyContainerException(
                "POST " + url + " returned " + resp.statusCode() + ": " + resp.body().trim());
        }
    }
}
