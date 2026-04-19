// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import com.fasterxml.jackson.databind.JavaType;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.PurgeConfirmationException;
import ru.mockarty.model.BulkPurgeResult;
import ru.mockarty.model.BulkRestoreResult;
import ru.mockarty.model.PurgeNowResult;
import ru.mockarty.model.RestoreResult;
import ru.mockarty.model.TrashListOptions;
import ru.mockarty.model.TrashListResult;
import ru.mockarty.model.TrashSettings;
import ru.mockarty.model.TrashSettingsUpdate;
import ru.mockarty.model.TrashSummary;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API for the Recycle Bin / Soft-Delete subsystem.
 *
 * <p>Wraps the namespace- and admin-scoped endpoints from
 * {@code internal/webui/integration_routes_trash*.go}. Bulk-purge requests
 * are guarded client-side by the confirmation phrase so we never send a
 * request the server will reject with an ambiguous 400.</p>
 */
public class TrashApi {

    /**
     * Exact confirmation phrase accepted by the server-side bulk-purge
     * endpoints. Mirrors {@code internal/webui.TrashPurgeConfirmationPhrase}.
     */
    public static final String PURGE_CONFIRMATION_PHRASE =
            "I understand this is permanent";

    private static final String ADMIN_BASE = "/api/v1/admin/trash";

    private final MockartyClient client;

    public TrashApi(MockartyClient client) {
        this.client = client;
    }

    // ── List ────────────────────────────────────────────────────────

    /** GET /api/v1/namespaces/:ns/trash. */
    public TrashListResult listTrash(String namespace, TrashListOptions opts)
            throws MockartyException {
        return client.get(nsBase(namespace) + buildQuery(opts), TrashListResult.class);
    }

    /** GET /api/v1/admin/trash — platform-wide list (admin / support). */
    public TrashListResult adminListTrash(TrashListOptions opts) throws MockartyException {
        return client.get(ADMIN_BASE + buildQuery(opts), TrashListResult.class);
    }

    // ── Summary ─────────────────────────────────────────────────────

    /** GET /api/v1/namespaces/:ns/trash/summary. */
    public TrashSummary summary(String namespace) throws MockartyException {
        return client.get(nsBase(namespace) + "/summary", TrashSummary.class);
    }

    /** GET /api/v1/admin/trash/summary. */
    public TrashSummary adminSummary() throws MockartyException {
        return client.get(ADMIN_BASE + "/summary", TrashSummary.class);
    }

    // ── Settings ────────────────────────────────────────────────────

    /** GET /api/v1/namespaces/:ns/trash/settings. */
    public TrashSettings getSettings(String namespace) throws MockartyException {
        return client.get(nsBase(namespace) + "/settings", TrashSettings.class);
    }

    /** PUT /api/v1/namespaces/:ns/trash/settings. */
    public TrashSettings updateSettings(String namespace, int retentionDays, boolean enabled)
            throws MockartyException {
        TrashSettingsUpdate body = new TrashSettingsUpdate(retentionDays, enabled);
        return client.put(nsBase(namespace) + "/settings", body, TrashSettings.class);
    }

    /** GET /api/v1/admin/trash/settings/global. */
    public TrashSettings getGlobalSettings() throws MockartyException {
        return client.get(ADMIN_BASE + "/settings/global", TrashSettings.class);
    }

    /** PUT /api/v1/admin/trash/settings/global (platform admin only). */
    public TrashSettings updateGlobalSettings(int retentionDays, boolean enabled)
            throws MockartyException {
        TrashSettingsUpdate body = new TrashSettingsUpdate(retentionDays, enabled);
        return client.put(ADMIN_BASE + "/settings/global", body, TrashSettings.class);
    }

    // ── Single cascade restore ──────────────────────────────────────

    /** POST /api/v1/namespaces/:ns/trash/restore-cascade/:cascade. */
    public RestoreResult restoreCascade(String namespace, String cascadeGroupId)
            throws MockartyException {
        requireCascade(cascadeGroupId);
        String path = nsBase(namespace) + "/restore-cascade/" + encode(cascadeGroupId);
        return client.post(path, null, RestoreResult.class);
    }

    /** POST /api/v1/admin/trash/restore-cascade/:cascade. */
    public RestoreResult adminRestoreCascade(String cascadeGroupId) throws MockartyException {
        requireCascade(cascadeGroupId);
        String path = ADMIN_BASE + "/restore-cascade/" + encode(cascadeGroupId);
        return client.post(path, null, RestoreResult.class);
    }

    // ── Bulk restore ────────────────────────────────────────────────

    /** POST /api/v1/namespaces/:ns/trash/restore. */
    public BulkRestoreResult bulkRestore(String namespace,
                                         Collection<String> cascadeGroupIds,
                                         String reason) throws MockartyException {
        Map<String, Object> body = restoreBody(cascadeGroupIds, reason);
        return client.post(nsBase(namespace) + "/restore", body, BulkRestoreResult.class);
    }

    /** POST /api/v1/admin/trash/restore. */
    public BulkRestoreResult adminBulkRestore(Collection<String> cascadeGroupIds,
                                              String reason) throws MockartyException {
        Map<String, Object> body = restoreBody(cascadeGroupIds, reason);
        return client.post(ADMIN_BASE + "/restore", body, BulkRestoreResult.class);
    }

    // ── Bulk purge (IRREVERSIBLE) ───────────────────────────────────

    /**
     * POST /api/v1/namespaces/:ns/trash/purge.
     *
     * @param confirmation MUST equal {@link #PURGE_CONFIRMATION_PHRASE}.
     * @throws PurgeConfirmationException if the phrase is missing or wrong.
     */
    public BulkPurgeResult bulkPurge(String namespace,
                                     Collection<String> cascadeGroupIds,
                                     String confirmation,
                                     String reason) throws MockartyException {
        Map<String, Object> body = purgeBody(cascadeGroupIds, confirmation, reason);
        return client.post(nsBase(namespace) + "/purge", body, BulkPurgeResult.class);
    }

    /** POST /api/v1/admin/trash/purge (platform admin only). */
    public BulkPurgeResult adminBulkPurge(Collection<String> cascadeGroupIds,
                                          String confirmation,
                                          String reason) throws MockartyException {
        Map<String, Object> body = purgeBody(cascadeGroupIds, confirmation, reason);
        return client.post(ADMIN_BASE + "/purge", body, BulkPurgeResult.class);
    }

    /** POST /api/v1/admin/trash/purge-now — synchronous retention tick. */
    public PurgeNowResult adminPurgeNow() throws MockartyException {
        return client.post(ADMIN_BASE + "/purge-now", null, PurgeNowResult.class);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private static String nsBase(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new IllegalArgumentException("namespace is required");
        }
        return "/api/v1/namespaces/" + encode(namespace.trim()) + "/trash";
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static void requireCascade(String cascadeGroupId) {
        if (cascadeGroupId == null || cascadeGroupId.trim().isEmpty()) {
            throw new IllegalArgumentException("cascade_group_id is required");
        }
    }

    private static Map<String, Object> restoreBody(Collection<String> ids, String reason) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("cascade_group_ids is required");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cascade_group_ids", ids);
        if (reason != null && !reason.isEmpty()) {
            body.put("reason", reason);
        }
        return body;
    }

    private static Map<String, Object> purgeBody(Collection<String> ids,
                                                 String confirmation,
                                                 String reason) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("cascade_group_ids is required");
        }
        if (!PURGE_CONFIRMATION_PHRASE.equals(confirmation)) {
            throw new PurgeConfirmationException(
                    "bulk purge requires confirmation phrase \""
                            + PURGE_CONFIRMATION_PHRASE + "\"");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cascade_group_ids", ids);
        body.put("confirmation", confirmation);
        if (reason != null && !reason.isEmpty()) {
            body.put("reason", reason);
        }
        return body;
    }

    private static String buildQuery(TrashListOptions opts) {
        if (opts == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendList(sb, "type", opts.getEntityTypes());
        append(sb, "q", opts.getSearch());
        append(sb, "closed_by", opts.getClosedBy());
        append(sb, "cascade", opts.getCascadeGroupId());
        if (opts.getFromTime() != null) {
            append(sb, "from", formatInstant(opts.getFromTime()));
        }
        if (opts.getToTime() != null) {
            append(sb, "to", formatInstant(opts.getToTime()));
        }
        if (opts.getLimit() > 0) {
            append(sb, "limit", Integer.toString(opts.getLimit()));
        }
        if (opts.getOffset() > 0) {
            append(sb, "offset", Integer.toString(opts.getOffset()));
        }
        if (sb.length() == 0) {
            return "";
        }
        return "?" + sb;
    }

    private static void append(StringBuilder sb, String key, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(encode(key)).append('=').append(encode(value));
    }

    private static void appendList(StringBuilder sb, String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        StringBuilder csv = new StringBuilder();
        for (String v : values) {
            if (v == null || v.isEmpty()) continue;
            if (csv.length() > 0) csv.append(',');
            csv.append(v);
        }
        if (csv.length() == 0) {
            return;
        }
        append(sb, key, csv.toString());
    }

    private static String formatInstant(Instant instant) {
        // RFC3339 ISO-8601 UTC rendering (e.g. "2026-04-19T12:00:00Z") —
        // matches the format accepted by the Go server's query parser.
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }
}
