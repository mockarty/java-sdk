// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.api.TrashApi;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.exception.PurgeConfirmationException;
import ru.mockarty.model.BulkPurgeResult;
import ru.mockarty.model.BulkRestoreResult;
import ru.mockarty.model.RestoreResult;
import ru.mockarty.model.TrashItem;
import ru.mockarty.model.TrashListOptions;
import ru.mockarty.model.TrashListResult;
import ru.mockarty.model.TrashSummary;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Recycle Bin examples — list, restore, purge cascade groups.
 *
 * <p>Environment:</p>
 * <ul>
 *   <li>{@code MOCKARTY_BASE_URL} — server URL (default http://localhost:5770)</li>
 *   <li>{@code MOCKARTY_API_KEY} — API key</li>
 *   <li>{@code MOCKARTY_NAMESPACE} — namespace to operate on</li>
 * </ul>
 */
public class TrashExample {

    public static void main(String[] args) throws MockartyException {
        String ns = System.getenv().getOrDefault("MOCKARTY_NAMESPACE", "sandbox");

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(System.getenv().getOrDefault("MOCKARTY_BASE_URL", "http://localhost:5770"))
                .apiKey(System.getenv("MOCKARTY_API_KEY"))
                .namespace(ns)
                .build()) {

            printSummary(client, ns);
            listRecentlyTrashed(client, ns);
            restoreExample(client, ns);
            purgeExample(client, ns);
        }
    }

    /** Aggregate counts per entity type (for badge rendering). */
    private static void printSummary(MockartyClient client, String ns) throws MockartyException {
        TrashSummary summary = client.trash().summary(ns);
        System.out.printf("Recycle Bin — %s (total %d)%n", ns, summary.getTotal());
        for (TrashSummary.Count c : summary.getCounts()) {
            System.out.printf("  %-18s %d%n", c.getEntityType(), c.getCount());
        }
    }

    /** List items closed within the last 7 days, filtered to mocks + stores. */
    private static void listRecentlyTrashed(MockartyClient client, String ns)
            throws MockartyException {
        TrashListResult list = client.trash().listTrash(ns, new TrashListOptions()
                .entityType("mock")
                .entityType("store")
                .fromTime(Instant.now().minus(7, ChronoUnit.DAYS))
                .limit(50));
        System.out.printf("%nLast 7 days (%d of %d shown):%n",
                list.getItems().size(), list.getTotal());
        for (TrashItem it : list.getItems()) {
            String flag = it.isRestoreAvailable() ? "OK" : "BLOCKED";
            System.out.printf("  [%s] %s %s (%s) closed_by=%s cascade=%s%n",
                    flag, it.getEntityType(), it.getName(), it.getId(),
                    it.getClosedBy(), it.getCascadeGroupId());
        }
    }

    /** Single cascade restore — idempotent. */
    private static void restoreExample(MockartyClient client, String ns) {
        String cascade = System.getenv("MOCKARTY_CASCADE_ID");
        if (cascade == null || cascade.isEmpty()) {
            return;
        }
        try {
            RestoreResult r = client.trash().restoreCascade(ns, cascade);
            System.out.printf("%nRestored cascade %s — %d rows%n",
                    r.getCascadeGroupId(), r.getRestoredCount());
        } catch (MockartyException e) {
            System.err.printf("restore failed: %s%n", e.getMessage());
        }
    }

    /** Bulk purge — IRREVERSIBLE. Guarded by the confirmation phrase. */
    private static void purgeExample(MockartyClient client, String ns) {
        String csv = System.getenv("MOCKARTY_PURGE_CASCADE_IDS");
        if (csv == null || csv.isEmpty()) {
            return;
        }
        List<String> ids = List.of(csv.split(","));
        try {
            BulkPurgeResult r = client.trash().bulkPurge(ns, ids,
                    TrashApi.PURGE_CONFIRMATION_PHRASE,
                    "demo purge " + Instant.now());
            System.out.printf("%nPurged %d / Failed %d / NotFound %d%n",
                    r.getPurged().size(), r.getFailed().size(), r.getNotFound().size());
            for (BulkPurgeResult.Outcome o : r.getPurged()) {
                System.out.printf("  OK %s (%s) — %d rows%n",
                        o.getCascadeGroupId(), o.getEntityType(), o.getRowsDeleted());
            }
        } catch (PurgeConfirmationException e) {
            // Client-side guard fired: no request was sent to the server.
            System.err.printf("refusing to purge: %s%n", e.getMessage());
        } catch (MockartyException e) {
            System.err.printf("purge failed: %s%n", e.getMessage());
        }
    }
}
