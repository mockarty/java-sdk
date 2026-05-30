// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.tester;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Path-style S3 client over {@code java.net.http} — no AWS SDK
 * dependency. Mirrors {@code sdk/go-sdk/protocols/s3}. Speaks
 * {@code <endpoint>/<bucket>/<key>}; supply a request signer (e.g.
 * SigV4) via the builder when the endpoint requires authentication —
 * against Mockarty S3 mocks none is needed.
 *
 * <p>Out of scope: bucket admin, presigned URLs, the SigV4 algorithm
 * itself.</p>
 */
public final class S3HttpClient implements S3Facet.S3Client {

    private final String endpoint;
    private final HttpClient http;
    private final Consumer<HttpRequest.Builder> signer;

    private S3HttpClient(String endpoint, HttpClient http, Consumer<HttpRequest.Builder> signer) {
        this.endpoint = stripTrailingSlash(endpoint);
        this.http = http;
        this.signer = signer;
    }

    public static Builder builder(String endpoint) {
        return new Builder(endpoint);
    }

    public static S3HttpClient create(String endpoint) {
        return new Builder(endpoint).build();
    }

    @Override
    public S3Facet.PutResult putObject(String bucket, String key, byte[] body, String contentType,
                                       Map<String, String> metadata) throws Exception {
        HttpRequest.Builder b = newRequest(bucket, key, "")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body == null ? new byte[0] : body));
        if (contentType != null && !contentType.isEmpty()) {
            b.header("Content-Type", contentType);
        }
        if (metadata != null) {
            for (Map.Entry<String, String> e : metadata.entrySet()) {
                b.header("x-amz-meta-" + e.getKey(), e.getValue());
            }
        }
        HttpResponse<byte[]> resp = send(b);
        S3Facet.PutResult res = new S3Facet.PutResult();
        res.statusCode = resp.statusCode();
        res.etag = etag(resp);
        if (resp.statusCode() >= 300) { throw error("putObject", resp); }
        return res;
    }

    @Override
    public S3Facet.GetResult getObject(String bucket, String key) throws Exception {
        HttpResponse<byte[]> resp = send(newRequest(bucket, key, "").GET());
        S3Facet.GetResult res = new S3Facet.GetResult();
        res.statusCode = resp.statusCode();
        res.contentType = header(resp, "Content-Type");
        res.etag = etag(resp);
        res.metadata = extractMeta(resp);
        if (resp.statusCode() >= 300) { throw error("getObject", resp); }
        res.body = resp.body();
        return res;
    }

    @Override
    public S3Facet.HeadResult headObject(String bucket, String key) throws Exception {
        HttpResponse<byte[]> resp = send(
                newRequest(bucket, key, "").method("HEAD", HttpRequest.BodyPublishers.noBody()));
        S3Facet.HeadResult res = new S3Facet.HeadResult();
        res.statusCode = resp.statusCode();
        res.exists = resp.statusCode() >= 200 && resp.statusCode() < 300;
        res.contentType = header(resp, "Content-Type");
        res.etag = etag(resp);
        res.metadata = extractMeta(resp);
        try {
            res.contentLength = Long.parseLong(header(resp, "Content-Length"));
        } catch (NumberFormatException ignore) {
            res.contentLength = 0;
        }
        return res;
    }

    @Override
    public S3Facet.ListResult listObjects(String bucket, String prefix) throws Exception {
        String query = "list-type=2";
        if (prefix != null && !prefix.isEmpty()) {
            query += "&prefix=" + URLEncoder.encode(prefix, StandardCharsets.UTF_8);
        }
        HttpResponse<byte[]> resp = send(newRequest(bucket, "", query).GET());
        S3Facet.ListResult res = new S3Facet.ListResult();
        res.statusCode = resp.statusCode();
        if (resp.statusCode() >= 300) { throw error("listObjects", resp); }
        parseListing(new String(resp.body(), StandardCharsets.UTF_8), res);
        return res;
    }

    @Override
    public S3Facet.DeleteResult deleteObject(String bucket, String key) throws Exception {
        HttpResponse<byte[]> resp = send(newRequest(bucket, key, "").DELETE());
        S3Facet.DeleteResult res = new S3Facet.DeleteResult();
        res.statusCode = resp.statusCode();
        if (resp.statusCode() >= 300) { throw error("deleteObject", resp); }
        return res;
    }

    // ── internals ─────────────────────────────────────────────────────

    private HttpRequest.Builder newRequest(String bucket, String key, String query) {
        if (bucket == null || bucket.isEmpty()) {
            throw new IllegalArgumentException("mockarty s3: empty bucket");
        }
        String url = endpoint + "/" + bucket + "/";
        if (key != null && !key.isEmpty()) {
            url = endpoint + "/" + bucket + "/" + (key.startsWith("/") ? key.substring(1) : key);
        }
        if (query != null && !query.isEmpty()) {
            url += "?" + query;
        }
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30));
    }

    private HttpResponse<byte[]> send(HttpRequest.Builder b) throws Exception {
        if (signer != null) { signer.accept(b); }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    private static String header(HttpResponse<?> resp, String name) {
        return resp.headers().firstValue(name).orElse("");
    }

    private static String etag(HttpResponse<?> resp) {
        return header(resp, "ETag").replace("\"", "");
    }

    private static Map<String, String> extractMeta(HttpResponse<?> resp) {
        Map<String, String> out = new HashMap<>();
        resp.headers().map().forEach((k, vals) -> {
            String lk = k.toLowerCase();
            if (lk.startsWith("x-amz-meta-") && !vals.isEmpty()) {
                out.put(lk.substring("x-amz-meta-".length()), vals.get(0));
            }
        });
        return out;
    }

    private Exception error(String op, HttpResponse<byte[]> resp) {
        String body = new String(resp.body(), StandardCharsets.UTF_8);
        Matcher code = Pattern.compile("<Code>([^<]*)</Code>").matcher(body);
        if (code.find()) {
            Matcher msg = Pattern.compile("<Message>([^<]*)</Message>").matcher(body);
            String message = msg.find() ? msg.group(1) : "";
            return new Exception("mockarty s3: " + op + ": " + code.group(1)
                    + " (" + resp.statusCode() + "): " + message);
        }
        return new Exception("mockarty s3: " + op + ": status " + resp.statusCode());
    }

    /**
     * Minimal regex-based ListBucketResult parser. The S3 list XML is
     * a flat, predictable shape; a full XML parser would be overkill and
     * pull a dependency. Namespace-agnostic.
     */
    private static void parseListing(String xml, S3Facet.ListResult res) {
        Matcher trunc = Pattern.compile("<IsTruncated>([^<]*)</IsTruncated>").matcher(xml);
        if (trunc.find()) {
            res.isTruncated = "true".equalsIgnoreCase(trunc.group(1).trim());
        }
        Matcher contents = Pattern.compile("<Contents>(.*?)</Contents>", Pattern.DOTALL).matcher(xml);
        while (contents.find()) {
            String block = contents.group(1);
            S3Facet.ObjectInfo info = new S3Facet.ObjectInfo();
            info.key = tag(block, "Key");
            info.etag = tag(block, "ETag").replace("\"", "");
            try {
                info.size = Long.parseLong(tag(block, "Size"));
            } catch (NumberFormatException ignore) {
                info.size = 0;
            }
            res.objects.add(info);
        }
    }

    private static String tag(String block, String name) {
        Matcher m = Pattern.compile("<" + name + ">([^<]*)</" + name + ">").matcher(block);
        return m.find() ? m.group(1).trim() : "";
    }

    private static String stripTrailingSlash(String s) {
        if (s == null || s.isEmpty()) { return ""; }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    public static final class Builder {
        private final String endpoint;
        private HttpClient http;
        private Consumer<HttpRequest.Builder> signer;

        Builder(String endpoint) { this.endpoint = endpoint; }

        public Builder httpClient(HttpClient client) { this.http = client; return this; }

        /** Per-request signing hook (e.g. SigV4). */
        public Builder signer(Consumer<HttpRequest.Builder> s) { this.signer = s; return this; }

        public S3HttpClient build() {
            HttpClient c = http != null ? http
                    : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
            return new S3HttpClient(endpoint, c, signer);
        }
    }
}
