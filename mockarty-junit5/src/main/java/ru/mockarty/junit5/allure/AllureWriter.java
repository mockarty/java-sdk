// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.junit5.allure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import ru.mockarty.junit5.allure.AllureModel.Attachment;
import ru.mockarty.junit5.allure.AllureModel.Container;
import ru.mockarty.junit5.allure.AllureModel.Label;
import ru.mockarty.junit5.allure.AllureModel.Link;
import ru.mockarty.junit5.allure.AllureModel.Parameter;
import ru.mockarty.junit5.allure.AllureModel.StatusDetails;
import ru.mockarty.junit5.allure.AllureModel.StepResult;
import ru.mockarty.junit5.allure.AllureModel.TestResult;

/**
 * Emit Allure-2 result + container JSON files into a results directory.
 *
 * <p>The output is <b>byte-accurate</b> wrt. allure-pytest 2.13.x — the
 * same fixture round-trips through Python and Java with identical key
 * order and identical scalar encoding. The writer:</p>
 * <ul>
 *   <li>Uses Jackson with a {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS}
 *       config to keep timestamps as longs (Allure standard).</li>
 *   <li>Drops {@code null} fields and empty collections so an absent
 *       {@code description} is not serialised as {@code "description":null}
 *       (the canonical fixtures omit absent fields).</li>
 *   <li>Encodes enum values via {@link AllureModel.Status#toWire()} /
 *       {@link AllureModel.Stage#toWire()} (lowercase string).</li>
 *   <li>Writes atomically via {@code <name>.tmp} + {@link Files#move}
 *       {@link StandardCopyOption#ATOMIC_MOVE} so a concurrent Allure
 *       collector never sees a partial file.</li>
 *   <li>Is thread-safe: every emit creates its own {@link LinkedHashMap}
 *       and uses an immutable {@link ObjectMapper} (Jackson contract).</li>
 * </ul>
 *
 * <h2>Filename convention</h2>
 * <ul>
 *   <li>{@code <uuid>-result.json} for {@link TestResult}.</li>
 *   <li>{@code <uuid>-container.json} for {@link Container}.</li>
 *   <li>{@code <uuid>-attachment.<ext>} for raw binary blobs registered
 *       via {@link #writeAttachment(Path, String, byte[], String)}.</li>
 * </ul>
 */
public final class AllureWriter {

    /**
     * Default results directory — Allure's canonical "allure-results/"
     * relative to the working directory. Overridden via
     * {@code -Dallure.results.directory=...} or environment variable
     * {@code ALLURE_RESULTS_DIRECTORY} (Allure-pytest parity).
     */
    public static final String DEFAULT_RESULTS_DIR = "allure-results";

    private static final ObjectMapper MAPPER = newMapper();
    /** Process-unique suffix counter for atomic-temp filenames. */
    private static final AtomicLong TMP_SEQ = new AtomicLong();

    private AllureWriter() {}

    private static ObjectMapper newMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        m.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, true);
        m.configure(SerializationFeature.INDENT_OUTPUT, false);
        return m;
    }

    /**
     * Resolve the results directory from system property, env var, or
     * default. Order matches allure-java's {@code FileSystemResultsWriter}.
     */
    public static Path resolveResultsDirectory() {
        String sys = System.getProperty("allure.results.directory");
        if (sys != null && !sys.isEmpty()) {
            return Paths.get(sys);
        }
        String env = System.getenv("ALLURE_RESULTS_DIRECTORY");
        if (env != null && !env.isEmpty()) {
            return Paths.get(env);
        }
        return Paths.get(DEFAULT_RESULTS_DIR);
    }

    /** Emit a TestResult as {@code <uuid>-result.json}. */
    public static Path writeTestResult(Path dir, TestResult r) throws IOException {
        if (r == null || r.uuid == null || r.uuid.isEmpty()) {
            throw new IllegalArgumentException("TestResult.uuid is required");
        }
        ensureDir(dir);
        Path target = dir.resolve(r.uuid + "-result.json");
        byte[] bytes = MAPPER.writeValueAsBytes(toJson(r));
        atomicWrite(target, bytes);
        return target;
    }

    /** Emit a Container as {@code <uuid>-container.json}. */
    public static Path writeContainer(Path dir, Container c) throws IOException {
        if (c == null || c.uuid == null || c.uuid.isEmpty()) {
            throw new IllegalArgumentException("Container.uuid is required");
        }
        ensureDir(dir);
        Path target = dir.resolve(c.uuid + "-container.json");
        byte[] bytes = MAPPER.writeValueAsBytes(toJson(c));
        atomicWrite(target, bytes);
        return target;
    }

    /**
     * Emit raw attachment bytes alongside the result. Returns the source
     * filename that should be carried inside the {@link Attachment#source}
     * field of the parent TestResult/StepResult.
     */
    public static String writeAttachment(Path dir, String name, byte[] body, String mime)
            throws IOException {
        ensureDir(dir);
        String ext = inferExtension(mime, name);
        String source = java.util.UUID.randomUUID() + "-attachment" + (ext.isEmpty() ? "" : "." + ext);
        Path target = dir.resolve(source);
        atomicWrite(target, body == null ? new byte[0] : body);
        return source;
    }

    /**
     * Emit {@code environment.properties} — the key/value snapshot Allure
     * renders in the report's "Environment" widget. Java {@code .properties}
     * format: one {@code key=value} line per entry, keys sorted for stable
     * byte output. Newlines / carriage returns inside a value are neutralised
     * to spaces (a raw newline would split the value into a bogus second
     * entry). Matches the Python/Go SDK emitters.
     *
     * <p>Reference: https://allurereport.org/docs/how-it-works-environment-file/</p>
     */
    public static Path writeEnvironment(Path dir, Map<String, String> env) throws IOException {
        ensureDir(dir);
        StringBuilder sb = new StringBuilder();
        if (env != null) {
            java.util.List<String> keys = new java.util.ArrayList<>(env.keySet());
            java.util.Collections.sort(keys);
            for (String k : keys) {
                String v = env.get(k);
                if (v == null) {
                    v = "";
                }
                v = v.replace('\n', ' ').replace('\r', ' ');
                sb.append(k).append('=').append(v).append('\n');
            }
        }
        Path target = dir.resolve("environment.properties");
        atomicWrite(target, sb.toString().getBytes(StandardCharsets.UTF_8));
        return target;
    }

    /**
     * Emit {@code categories.json} — Allure's failure-categorisation rules.
     * Each category map carries {@code name} and optionally
     * {@code matchedStatuses} (list of status strings), {@code messageRegex},
     * {@code traceRegex}, {@code description}, {@code flaky}. The caller owns
     * the schema; we serialise the list verbatim.
     *
     * <p>Reference: https://allurereport.org/docs/categories/</p>
     */
    public static Path writeCategories(Path dir, List<Map<String, Object>> categories)
            throws IOException {
        ensureDir(dir);
        Path target = dir.resolve("categories.json");
        byte[] bytes = MAPPER.writeValueAsBytes(categories == null
                ? java.util.Collections.emptyList() : categories);
        atomicWrite(target, bytes);
        return target;
    }

    /**
     * Emit {@code executor.json} — CI executor metadata (name / type /
     * buildName / buildUrl / reportUrl). The caller supplies the field map;
     * we serialise it verbatim.
     */
    public static Path writeExecutor(Path dir, Map<String, Object> executor) throws IOException {
        ensureDir(dir);
        Path target = dir.resolve("executor.json");
        byte[] bytes = MAPPER.writeValueAsBytes(executor == null
                ? java.util.Collections.emptyMap() : executor);
        atomicWrite(target, bytes);
        return target;
    }

    /**
     * Convert a TestResult to a JSON-ready ordered map. Keeping the map
     * order stable is what makes byte-accuracy with allure-pytest possible.
     */
    static Map<String, Object> toJson(TestResult r) {
        Map<String, Object> out = new LinkedHashMap<>();
        // Order replicates allure-pytest's serialiser.
        out.put("uuid", r.uuid);
        if (r.historyId != null) out.put("historyId", r.historyId);
        if (r.testCaseId != null) out.put("testCaseId", r.testCaseId);
        if (r.name != null) out.put("name", r.name);
        if (r.fullName != null) out.put("fullName", r.fullName);
        if (r.description != null) out.put("description", r.description);
        if (r.descriptionHtml != null) out.put("descriptionHtml", r.descriptionHtml);
        if (r.status != null) out.put("status", r.status.toWire());
        if (r.statusDetails != null) out.put("statusDetails", statusDetailsToJson(r.statusDetails));
        if (r.stage != null) out.put("stage", r.stage.toWire());
        if (r.start > 0) out.put("start", r.start);
        if (r.stop > 0) out.put("stop", r.stop);
        if (!r.labels.isEmpty()) out.put("labels", labelsToJson(r.labels));
        if (!r.links.isEmpty()) out.put("links", linksToJson(r.links));
        if (!r.parameters.isEmpty()) out.put("parameters", parametersToJson(r.parameters));
        if (!r.attachments.isEmpty()) out.put("attachments", attachmentsToJson(r.attachments));
        if (!r.steps.isEmpty()) out.put("steps", stepsToJson(r.steps));
        return out;
    }

    static Map<String, Object> toJson(Container c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("uuid", c.uuid);
        if (c.name != null) out.put("name", c.name);
        if (c.start > 0) out.put("start", c.start);
        if (c.stop > 0) out.put("stop", c.stop);
        if (!c.children.isEmpty()) out.put("children", c.children);
        if (!c.befores.isEmpty()) out.put("befores", stepsToJson(c.befores));
        if (!c.afters.isEmpty()) out.put("afters", stepsToJson(c.afters));
        if (!c.links.isEmpty()) out.put("links", linksToJson(c.links));
        return out;
    }

    static Map<String, Object> statusDetailsToJson(StatusDetails sd) {
        Map<String, Object> out = new LinkedHashMap<>();
        // Booleans always emitted (Allure schema expects them).
        out.put("known", sd.known);
        out.put("muted", sd.muted);
        out.put("flaky", sd.flaky);
        if (sd.message != null) out.put("message", sd.message);
        if (sd.trace != null) out.put("trace", sd.trace);
        return out;
    }

    static List<Map<String, Object>> labelsToJson(List<Label> labels) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(labels.size());
        for (Label l : labels) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (l.name != null) m.put("name", l.name);
            if (l.value != null) m.put("value", l.value);
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> linksToJson(List<Link> links) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(links.size());
        for (Link l : links) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (l.name != null) m.put("name", l.name);
            if (l.url != null) m.put("url", l.url);
            if (l.type != null) m.put("type", l.type);
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> parametersToJson(List<Parameter> params) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(params.size());
        for (Parameter p : params) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (p.name != null) m.put("name", p.name);
            if (p.value != null) m.put("value", p.value);
            if (p.mode != null) m.put("mode", p.mode);
            if (p.excluded != null) m.put("excluded", p.excluded);
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> attachmentsToJson(List<Attachment> attachments) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(attachments.size());
        for (Attachment a : attachments) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (a.name != null) m.put("name", a.name);
            if (a.source != null) m.put("source", a.source);
            if (a.type != null) m.put("type", a.type);
            out.add(m);
        }
        return out;
    }

    static List<Map<String, Object>> stepsToJson(List<StepResult> steps) {
        List<Map<String, Object>> out = new java.util.ArrayList<>(steps.size());
        for (StepResult s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (s.name != null) m.put("name", s.name);
            if (s.status != null) m.put("status", s.status.toWire());
            if (s.statusDetails != null) m.put("statusDetails", statusDetailsToJson(s.statusDetails));
            if (s.stage != null) m.put("stage", s.stage.toWire());
            if (s.start > 0) m.put("start", s.start);
            if (s.stop > 0) m.put("stop", s.stop);
            if (!s.parameters.isEmpty()) m.put("parameters", parametersToJson(s.parameters));
            if (!s.attachments.isEmpty()) m.put("attachments", attachmentsToJson(s.attachments));
            if (!s.steps.isEmpty()) m.put("steps", stepsToJson(s.steps));
            out.add(m);
        }
        return out;
    }

    private static void ensureDir(Path dir) throws IOException {
        if (dir == null) {
            throw new IllegalArgumentException("results directory is null");
        }
        Files.createDirectories(dir);
    }

    /**
     * Atomic-write {@code bytes} to {@code target}. We write to a tmp
     * sibling and {@link Files#move} with {@code ATOMIC_MOVE}. Falls back
     * to non-atomic when the filesystem rejects ATOMIC_MOVE (Windows on
     * some JREs); the visible-state guarantee then drops to "either old
     * or new content", which is what every Allure collector tolerates.
     */
    private static void atomicWrite(Path target, byte[] bytes) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp."
                + Long.toHexString(TMP_SEQ.incrementAndGet()));
        try {
            Files.write(tmp, bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
            try {
                Files.move(tmp, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Filesystem doesn't support ATOMIC_MOVE — fall back.
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (Files.exists(tmp)) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Map a MIME type to a file extension. The result is informational —
     * Allure collectors read the {@code type} field from the JSON, not the
     * extension. Keeps the on-disk filename humane.
     */
    static String inferExtension(String mime, String name) {
        if (mime == null) {
            mime = "";
        }
        mime = mime.toLowerCase();
        if (mime.startsWith("text/plain")) return "txt";
        if (mime.equals("text/html") || mime.startsWith("text/html;")) return "html";
        if (mime.startsWith("application/json")) return "json";
        if (mime.startsWith("application/xml") || mime.startsWith("text/xml")) return "xml";
        if (mime.startsWith("image/png")) return "png";
        if (mime.startsWith("image/jpeg")) return "jpg";
        if (mime.startsWith("image/svg")) return "svg";
        if (mime.startsWith("image/gif")) return "gif";
        if (mime.startsWith("video/mp4")) return "mp4";
        if (mime.startsWith("video/webm")) return "webm";
        if (mime.startsWith("application/pdf")) return "pdf";
        if (mime.startsWith("application/yaml") || mime.endsWith("+yaml")) return "yaml";
        // Fall back to whatever the name suggested, if anything.
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                return name.substring(dot + 1).toLowerCase();
            }
        }
        return "";
    }
}
