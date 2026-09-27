// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mockarty.model.ExternalAttachment;
import ru.mockarty.model.ExternalStep;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;

/** Validates and snapshots all referenced media before any run is sent. */
final class AllureMedia {
    final List<ExternalStep> steps = new ArrayList<>();
    final List<ExternalAttachment> attachments = new ArrayList<>();
    final Map<String, List<String>> paths = new LinkedHashMap<>();
    int bytes;

    static byte[] readSource(Path directory, String source, int limit) throws IOException {
        if (source == null || source.isEmpty() || source.contains("\\") || source.contains(":")) {
            throw new IOException("allure: invalid source path");
        }
        Path path = directory;
        for (String part : source.split("/", -1)) {
            if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("allure: invalid source path");
            path = path.resolve(part);
            if (Files.isSymbolicLink(path)) throw new IOException("allure: source is a symlink: " + source);
        }
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("allure: source is not a regular file: " + source);
        byte[] body;
        try (InputStream input = Files.newInputStream(path)) { body = input.readNBytes(limit + 1); }
        if (body.length == 0 || body.length > limit) throw new IOException("allure: source is empty or exceeds " + limit + " bytes: " + source);
        return body;
    }

    static AllureMedia collect(JsonNode node, Path directory) throws IOException {
        AllureMedia result = new AllureMedia();
        result.visit(node, directory, "result", "", 0);
        return result;
    }

    private void visit(JsonNode node, Path directory, String path, String prefix, int depth) throws IOException {
        if (node == null || !node.isObject()) throw new IOException("allure: invalid object at " + path);
        if (depth > 64) throw new IOException("allure: step depth exceeds 64");
        JsonNode refs = node.path("attachments");
        if (!refs.isMissingNode() && !refs.isNull()) {
            if (!refs.isArray()) throw new IOException("allure: invalid attachments at " + path);
            for (JsonNode att : refs) {
                if (!att.isObject()) throw new IOException("allure: invalid attachment at " + path);
                String source = att.path("source").asText("");
                if (!paths.containsKey(source)) {
                    if (attachments.size() >= 32) throw new IOException("allure: more than 32 attachments per result");
                    byte[] body = readSource(directory, source, 256 << 10);
                    bytes += body.length;
                    if (bytes > (1 << 20)) throw new IOException("allure: result attachments exceed 1 MiB");
                    String name = att.path("name").asText("");
                    String type = att.path("type").asText("");
                    attachments.add(new ExternalAttachment().name(name.isEmpty() ? source : name)
                            .contentType(type.isEmpty() ? "application/octet-stream" : type).body(body));
                    paths.put(source, new ArrayList<>());
                }
                paths.get(source).add(path);
            }
        }
        JsonNode children = node.path("steps");
        if (!children.isMissingNode() && !children.isNull()) {
            if (!children.isArray()) throw new IOException("allure: invalid steps at " + path);
            int index = 0;
            for (JsonNode step : children) {
                if (!step.isObject()) throw new IOException("allure: invalid step at " + path);
                if (steps.size() >= 5000) throw new IOException("allure: more than 5000 steps");
                String childPath = path + "/steps/" + index++;
                String status = step.path("status").asText("").trim().toLowerCase(Locale.ROOT);
                if (!Arrays.asList("passed", "failed", "broken", "skipped").contains(status)) status = "broken";
                JsonNode detail = step.path("statusDetails");
                String error = (detail.path("message").asText("") + "\n" + detail.path("trace").asText("")).trim();
                long start = step.path("start").asLong(0), stop = step.path("stop").asLong(0);
                long duration = start >= 0 && stop >= start ? stop - start : 0;
                String name = step.path("name").asText("");
                if (!prefix.isEmpty()) name = prefix + " / " + name;
                steps.add(new ExternalStep(name, status).error(error)
                        .durationMs(duration).metadata(Collections.singletonMap("allurePath", childPath)));
                visit(step, directory, childPath, name, depth + 1);
            }
        }
    }
}
