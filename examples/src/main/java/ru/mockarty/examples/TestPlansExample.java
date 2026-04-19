// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.PlanRunCancelledException;
import ru.mockarty.exception.PlanRunFailedException;
import ru.mockarty.exception.PreconditionFailedException;
import ru.mockarty.model.AdHocItem;
import ru.mockarty.model.AdHocRunResponse;
import ru.mockarty.model.AllureReport;
import ru.mockarty.model.CreateAdHocRunRequest;
import ru.mockarty.model.PatchOptions;
import ru.mockarty.model.PatchPlanRequest;
import ru.mockarty.model.PlanWebhook;
import ru.mockarty.model.TestPlan;
import ru.mockarty.model.TestPlanItem;
import ru.mockarty.model.TestPlanRun;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Test Plans end-to-end example covering the three canonical scenarios:
 *
 * <ol>
 *   <li><b>basic</b>  – create a plan, trigger it, poll until done</li>
 *   <li><b>stream</b> – subscribe to the live SSE event stream</li>
 *   <li><b>ci</b>     – attach a webhook, trigger, wait, download Allure zip</li>
 * </ol>
 *
 * <p>All configuration is read from environment variables:
 * {@code MOCKARTY_SERVER}, {@code MOCKARTY_TOKEN}, {@code MOCKARTY_NAMESPACE},
 * {@code PLAN_ID}, {@code RUN_ID}, {@code COLLECTION_ID},
 * {@code CI_WEBHOOK_URL}, {@code CI_WEBHOOK_SECRET}.</p>
 */
public class TestPlansExample {

    public static void main(String[] args) throws IOException {
        String scenario = args.length > 0 ? args[0] : "basic";

        try (MockartyClient client = MockartyClient.builder()
                .baseUrl(env("MOCKARTY_SERVER", "http://localhost:5770"))
                .apiKey(envRequired("MOCKARTY_TOKEN"))
                .namespace(env("MOCKARTY_NAMESPACE", "sandbox"))
                .timeout(Duration.ofMinutes(5))
                .build()) {

            switch (scenario) {
                case "basic":
                    basic(client);
                    break;
                case "stream":
                    stream(client);
                    break;
                case "ci":
                    System.exit(ci(client));
                    break;
                case "adhoc":
                    adhoc(client);
                    break;
                case "lifecycle":
                    lifecycle(client);
                    break;
                default:
                    System.err.println("unknown scenario: " + scenario +
                            " (use basic | stream | ci | adhoc | lifecycle)");
                    System.exit(64);
            }
        }
    }

    private static void basic(MockartyClient client) {
        TestPlan plan = new TestPlan()
                .name("SDK Example Smoke")
                .description("Created by the Java SDK example")
                .items(List.of(new TestPlanItem()
                        .order(0)
                        .type("functional")
                        .resourceId(envRequired("COLLECTION_ID"))
                        .name("Smoke")));
        TestPlan created = client.testPlans().create(plan);
        System.out.printf("Plan created: %s (#%d)%n",
                created.getId(), created.getNumericId());

        TestPlanRun run = client.testPlans().run(created.getId(), null, null);
        System.out.println("Triggered run: " + run.getId());

        try {
            TestPlanRun done = client.testPlans().waitForRun(
                    run.getId(), Duration.ofSeconds(5), Duration.ofMinutes(30));
            System.out.printf("Run completed: total=%d failed=%d%n",
                    done.getTotalItems(), done.getFailedItems());
        } catch (PlanRunFailedException e) {
            System.err.println("Run failed: " + e.getMessage());
            System.exit(1);
        } catch (PlanRunCancelledException e) {
            System.err.println("Run cancelled: " + e.getMessage());
            System.exit(2);
        }
    }

    private static void stream(MockartyClient client) {
        String runId = envRequired("RUN_ID");
        client.testPlans().streamRun(runId, event ->
                System.out.printf("[%s] %s%n", event.getKind(), event.getRaw()));
        System.out.println("stream closed.");
    }

    private static int ci(MockartyClient client) throws IOException {
        String planId = envRequired("PLAN_ID");

        try {
            client.testPlans().addWebhook(planId, new PlanWebhook()
                    .url(envRequired("CI_WEBHOOK_URL"))
                    .events(List.of("run.completed", "run.failed"))
                    .secret(System.getenv("CI_WEBHOOK_SECRET"))
                    .enabled(true));
        } catch (Exception err) {
            System.err.println("attach webhook: " + err + " (continuing)");
        }

        TestPlanRun run = client.testPlans().run(planId, null, null);
        System.out.println("Triggered run " + run.getId());

        int exitCode = 0;
        TestPlanRun done = null;
        try {
            done = client.testPlans().waitForRun(
                    run.getId(), Duration.ofSeconds(10), Duration.ofMinutes(30));
        } catch (PlanRunFailedException e) {
            exitCode = 1;
        } catch (PlanRunCancelledException e) {
            exitCode = 2;
        }

        try (FileOutputStream out = new FileOutputStream("report.zip")) {
            client.testPlans().downloadReportZip(run.getId(), out);
        }

        System.out.printf("Final: %s (failed=%d total=%d)%n",
                exitCode == 0 ? "PASS" : (exitCode == 1 ? "FAIL" : "CANCELLED"),
                done == null ? 0 : done.getFailedItems(),
                done == null ? 0 : done.getTotalItems());
        return exitCode;
    }

    /**
     * Bundle a collection + a fuzz config into a master ad-hoc run
     * without authoring a persisted plan. Great for CI one-shots.
     */
    private static void adhoc(MockartyClient client) {
        AdHocRunResponse resp = client.testPlans().createAdHocRun(
                new CreateAdHocRunRequest()
                        .name("SDK Ad-Hoc Smoke")
                        .schedule("parallel")
                        .items(List.of(
                                new AdHocItem()
                                        .refId(envRequired("COLLECTION_ID"))
                                        .type("functional")
                                        .order(0),
                                new AdHocItem()
                                        .refId(env("FUZZ_CONFIG_ID", ""))
                                        .type("fuzz")
                                        .order(1))));
        System.out.printf("Ad-hoc plan=%s run=%s status=%s%n",
                resp.getPlanId(), resp.getRunId(), resp.getStatus());

        try {
            TestPlanRun done = client.testPlans().waitForRun(
                    resp.getRunId(), Duration.ofSeconds(5), Duration.ofMinutes(30));
            System.out.printf("Ad-hoc run completed: total=%d failed=%d%n",
                    done.getTotalItems(), done.getFailedItems());
        } catch (PlanRunFailedException e) {
            System.err.println("Ad-hoc run failed: " + e.getMessage());
            System.exit(1);
        } catch (PlanRunCancelledException e) {
            System.err.println("Ad-hoc run cancelled: " + e.getMessage());
            System.exit(2);
        }
    }

    /**
     * Full lifecycle: create → patch → run → wait → fetch JSON report →
     * delete. Demonstrates TP-10 features:
     * <ul>
     *   <li>{@code patch(...)} with {@link PreconditionFailedException} on
     *       concurrent updates</li>
     *   <li>{@code getRunReport(...)} for namespace-scoped Allure JSON</li>
     * </ul>
     */
    private static void lifecycle(MockartyClient client) {
        String ns = env("MOCKARTY_NAMESPACE", "sandbox");

        TestPlan created = client.testPlans().create(new TestPlan()
                .name("SDK Lifecycle Demo")
                .namespace(ns)
                .items(List.of(new TestPlanItem()
                        .order(0)
                        .type("functional")
                        .resourceId(envRequired("COLLECTION_ID")))));
        System.out.printf("created: %s (#%d)%n",
                created.getId(), created.getNumericId());

        try {
            TestPlan patched = client.testPlans().patch(
                    created.getId(),
                    new PatchPlanRequest()
                            .description("updated via Java SDK")
                            .enabled(true),
                    new PatchOptions().namespace(ns));
            System.out.printf("patched: description=%s%n", patched.getDescription());
        } catch (PreconditionFailedException e) {
            System.err.println("patch conflict: " + e.getMessage());
        }

        TestPlanRun run = client.testPlans().run(created.getId(), null, null);
        try {
            client.testPlans().waitForRun(run.getId(),
                    Duration.ofSeconds(3), Duration.ofMinutes(15));
        } catch (PlanRunFailedException | PlanRunCancelledException e) {
            System.err.println("run ended: " + e.getMessage());
        }

        try {
            AllureReport report = client.testPlans().getRunReport(
                    ns, created.getId(), run.getId());
            System.out.printf("report: status=%s items=%d rawBytes=%d%n",
                    report.getStatus(),
                    report.getItems() == null ? 0 : report.getItems().size(),
                    report.getRaw() == null ? 0 : report.getRaw().length);
        } catch (Exception e) {
            System.err.println("fetch report: " + e.getMessage());
        }

        client.testPlans().delete(created.getId());
        System.out.println("deleted plan " + created.getId());
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static String envRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(key + " is required");
        }
        return value;
    }
}
