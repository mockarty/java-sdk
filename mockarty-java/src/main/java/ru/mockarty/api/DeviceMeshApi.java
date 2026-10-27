// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.MeshCommand;
import ru.mockarty.model.MeshDevice;
import ru.mockarty.model.MeshExecutionReport;
import ru.mockarty.model.MeshGrant;
import ru.mockarty.model.MeshSubmitResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lends machines to a workspace and places work on them.
 *
 * <p>Two rules the server enforces and this API keeps visible: only a device's
 * owner can lend it, and claim/report belong to the executing device.
 * {@link #submit} reports whether the idempotency key replayed.
 */
public class DeviceMeshApi {

    private final MockartyClient client;

    public DeviceMeshApi(MockartyClient client) {
        this.client = client;
    }

    /** Lists the devices this caller may place work on. */
    @SuppressWarnings("unchecked")
    public List<MeshDevice> listDevices(String namespace) throws MockartyException {
        Map<String, Object> envelope = client.get(base(namespace) + "/devices", Map.class);
        if (envelope == null || !(envelope.get("devices") instanceof List)) {
            return java.util.Collections.emptyList();
        }
        return client.getObjectMapper().convertValue(envelope.get("devices"),
                client.getObjectMapper().getTypeFactory()
                        .constructCollectionType(List.class, MeshDevice.class));
    }

    /** Lends the device to a colleague for the given capabilities (owner only). */
    @SuppressWarnings("unchecked")
    public MeshGrant createGrant(String namespace, String deviceId, String granteeUserId,
                                 List<String> capabilities) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("granteeUserId", require(granteeUserId, "granteeUserId"));
        if (capabilities != null && !capabilities.isEmpty()) {
            body.put("capabilities", new ArrayList<>(capabilities));
        }
        Map<String, Object> out = client.post(
                base(namespace) + "/devices/" + encode(require(deviceId, "deviceId")) + "/grants",
                body, Map.class);
        return client.getObjectMapper().convertValue(out, MeshGrant.class);
    }

    /** Withdraws a lend: the next submit for that device and capability is refused. */
    public void revokeGrant(String namespace, String deviceId, String granteeUserId) throws MockartyException {
        client.delete(base(namespace) + "/devices/" + encode(require(deviceId, "deviceId"))
                + "/grants/" + encode(require(granteeUserId, "granteeUserId")), null, Map.class);
    }

    /**
     * Queues work. {@code targetDeviceId} may be empty to let the server choose —
     * the returned command then carries a placement reason instead of a device.
     */
    @SuppressWarnings("unchecked")
    public MeshSubmitResult submit(String namespace, String kind, Object payload,
                                   String idempotencyKey, String targetDeviceId,
                                   List<String> requiredCapabilities,
                                   Map<String, String> targetLabels) throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("kind", require(kind, "kind"));
        body.put("idempotencyKey", require(idempotencyKey, "idempotencyKey"));
        if (payload != null) {
            body.put("payload", payload);
        }
        if (requiredCapabilities != null && !requiredCapabilities.isEmpty()) {
            body.put("requiredCapabilities", new ArrayList<>(requiredCapabilities));
        }
        if (targetDeviceId != null && !targetDeviceId.isBlank()) {
            body.put("targetDeviceId", targetDeviceId.trim());
        }
        if (targetLabels != null && !targetLabels.isEmpty()) {
            body.put("targetLabels", new LinkedHashMap<>(targetLabels));
        }
        Map<String, Object> out = client.post(base(namespace) + "/commands", body, Map.class);
        return client.getObjectMapper().convertValue(out, MeshSubmitResult.class);
    }

    /** Reads one command's current state, including the terminal failure reason. */
    @SuppressWarnings("unchecked")
    public MeshCommand getCommand(String namespace, String commandId) throws MockartyException {
        Map<String, Object> out = client.get(
                base(namespace) + "/commands/" + encode(require(commandId, "commandId")), Map.class);
        return client.getObjectMapper().convertValue(out, MeshCommand.class);
    }

    /**
     * Reports progress or the terminal outcome as the executing device. A
     * terminal report is final: the command cannot be revived afterwards.
     */
    @SuppressWarnings("unchecked")
    public MeshCommand report(String namespace, String deviceId, String commandId, String state,
                              String resultSummary, String resultRef, String failureReason)
            throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("state", require(state, "state"));
        if (resultSummary != null && !resultSummary.isEmpty()) {
            body.put("resultSummary", resultSummary);
        }
        if (resultRef != null && !resultRef.isEmpty()) {
            body.put("resultRef", resultRef);
        }
        if (failureReason != null && !failureReason.isEmpty()) {
            body.put("failureReason", failureReason);
        }
        String path = base(namespace) + "/commands/" + encode(require(commandId, "commandId"))
                + "/report?deviceId=" + encode(require(deviceId, "deviceId"));
        Map<String, Object> out = client.post(path, body, Map.class);
        return client.getObjectMapper().convertValue(out, MeshCommand.class);
    }

    /** Returns the durable receipts for one command. */
    public List<MeshExecutionReport> listReports(String namespace, String commandId) throws MockartyException {
        String path = base(namespace) + "/reports?commandId=" + encode(require(commandId, "commandId"));
        Map<?, ?> envelope = client.get(path, Map.class);
        if (envelope == null || !(envelope.get("reports") instanceof List)) {
            return java.util.Collections.emptyList();
        }
        return client.getObjectMapper().convertValue(envelope.get("reports"),
                client.getObjectMapper().getTypeFactory()
                        .constructCollectionType(List.class, MeshExecutionReport.class));
    }

    /**
     * Records an operator decision that survives the device's own heartbeat:
     * {@code true} stops new work reaching it, {@code false} releases an earlier
     * drain. A device that drained itself needs no operator to recover.
     */
    @SuppressWarnings("unchecked")
    public MeshDevice setDrain(String namespace, String deviceId, boolean draining, String reason)
            throws MockartyException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("draining", draining);
        if (reason != null && !reason.isEmpty()) {
            body.put("reason", reason);
        }
        Map<String, Object> out = client.post(
                base(namespace) + "/devices/" + encode(require(deviceId, "deviceId")) + "/drain",
                body, Map.class);
        return client.getObjectMapper().convertValue(out, MeshDevice.class);
    }

    private String base(String namespace) {
        String resolved = namespace == null || namespace.isBlank()
                ? client.getConfig().getNamespace() : namespace;
        return "/api/v1/namespaces/" + encode(require(resolved, "namespace")) + "/device-mesh";
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
