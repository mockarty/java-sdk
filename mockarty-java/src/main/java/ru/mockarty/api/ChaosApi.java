// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.api;

import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import ru.mockarty.model.ChaosExperiment;
import ru.mockarty.model.ChaosProfile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * API for chaos engineering operations.
 *
 * <p>Provides experiment lifecycle management, Kubernetes cluster profiles,
 * fault injection presets, pod/deployment operations, and experiment reporting.</p>
 *
 * <p>All paths match the server-side route registration in
 * internal/chaos/api.go RegisterChaosRoutes().</p>
 */
public class ChaosApi {

    private final MockartyClient client;

    public ChaosApi(MockartyClient client) {
        this.client = client;
    }

    // ---- Presets ----

    /**
     * Lists all available chaos experiment presets.
     * Server wraps response as {@code {"presets": [...], "count": N}}.
     *
     * @return presets wrapper map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listPresets() throws MockartyException {
        return client.get("/api/v1/chaos/presets", Map.class);
    }

    // ---- Experiments ----

    /**
     * Lists chaos experiments with default parameters.
     * Server wraps response as {@code {"experiments": [...], "total": N, "limit": N, "offset": N}}.
     *
     * @return experiments wrapper map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> list() throws MockartyException {
        return client.get("/api/v1/chaos/experiments", Map.class);
    }

    /**
     * Lists chaos experiments with filtering and pagination.
     *
     * @param namespace the namespace to filter by (nullable)
     * @param status    the status to filter by (nullable)
     * @param limit     the maximum number of results
     * @param offset    the offset for pagination
     * @return experiments wrapper map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> list(String namespace, String status, int limit, int offset)
            throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/experiments?");
        path.append("limit=").append(limit);
        path.append("&offset=").append(offset);
        if (namespace != null && !namespace.isEmpty()) {
            path.append("&namespace=").append(encode(namespace));
        }
        if (status != null && !status.isEmpty()) {
            path.append("&status=").append(encode(status));
        }
        return client.get(path.toString(), Map.class);
    }

    /**
     * Gets a chaos experiment by ID.
     *
     * @param id the experiment ID
     * @return the chaos experiment
     */
    public ChaosExperiment get(String id) throws MockartyException {
        return client.get("/api/v1/chaos/experiments/" + encode(id), ChaosExperiment.class);
    }

    /**
     * Creates a new chaos experiment.
     *
     * @param experiment the experiment to create
     * @return the created experiment
     */
    public ChaosExperiment create(ChaosExperiment experiment) throws MockartyException {
        return client.post("/api/v1/chaos/experiments", experiment, ChaosExperiment.class);
    }

    /**
     * Updates an existing chaos experiment (must be in 'pending' status).
     *
     * @param id         the experiment ID
     * @param experiment the updated experiment data
     * @return the updated experiment
     */
    public ChaosExperiment update(String id, ChaosExperiment experiment) throws MockartyException {
        return client.put("/api/v1/chaos/experiments/" + encode(id), experiment, ChaosExperiment.class);
    }

    /**
     * Deletes a chaos experiment (cannot delete running experiments).
     *
     * @param id the experiment ID to delete
     */
    public void delete(String id) throws MockartyException {
        client.delete("/api/v1/chaos/experiments/" + encode(id));
    }

    /**
     * Runs (starts) a pending chaos experiment.
     *
     * @param id the experiment ID to run
     * @return run status response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(String id) throws MockartyException {
        return client.post("/api/v1/chaos/experiments/" + encode(id) + "/run", null, Map.class);
    }

    /**
     * Aborts a running chaos experiment.
     *
     * @param id the experiment ID to abort
     * @return abort status response
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> abort(String id) throws MockartyException {
        return client.post("/api/v1/chaos/experiments/" + encode(id) + "/abort", null, Map.class);
    }

    // ---- Experiment Metrics & Reporting ----

    /**
     * Gets metric snapshots for a chaos experiment.
     *
     * @param id the experiment ID
     * @return metrics wrapper with experimentId, snapshots, count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetrics(String id) throws MockartyException {
        return client.get("/api/v1/chaos/experiments/" + encode(id) + "/metrics", Map.class);
    }

    /**
     * Gets timeline events for a chaos experiment.
     *
     * @param id the experiment ID
     * @return events wrapper with experimentId, events, count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getEvents(String id) throws MockartyException {
        return client.get("/api/v1/chaos/experiments/" + encode(id) + "/events", Map.class);
    }

    /**
     * Gets the full report for a chaos experiment.
     *
     * @param id the experiment ID
     * @return report data including experiment, events, metrics, results
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getReport(String id) throws MockartyException {
        return client.get("/api/v1/chaos/experiments/" + encode(id) + "/report", Map.class);
    }

    /**
     * Downloads the experiment report in a specific format.
     *
     * @param id     the experiment ID
     * @param format the report format: "html", "json", "junit", "allure"
     * @return the report as raw bytes
     */
    public byte[] downloadReport(String id, String format) throws MockartyException {
        return client.getBytes("/api/v1/chaos/experiments/" + encode(id) + "/report/download?format=" + encode(format));
    }

    /**
     * Gets a cluster state snapshot for an experiment.
     *
     * @param id the experiment ID
     * @return snapshot data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSnapshot(String id) throws MockartyException {
        return client.get("/api/v1/chaos/experiments/" + encode(id) + "/snapshot", Map.class);
    }

    // ---- Queue ----

    /**
     * Gets the current experiment queue status for a cluster.
     *
     * @param clusterId the cluster ID
     * @return queue status data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getQueueStatus(String clusterId) throws MockartyException {
        return client.get("/api/v1/chaos/queue/" + encode(clusterId), Map.class);
    }

    // ---- Profiles (Infrastructure / Kubernetes Clusters) ----

    /**
     * Lists all infrastructure profiles.
     * Server wraps response as {@code {"profiles": [...], "count": N}}.
     *
     * @return profiles wrapper map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listProfiles() throws MockartyException {
        return client.get("/api/v1/chaos/profiles", Map.class);
    }

    /**
     * Lists infrastructure profiles filtered by namespace.
     *
     * @param namespace the namespace to filter by (nullable)
     * @return profiles wrapper map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listProfiles(String namespace) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/profiles");
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(encode(namespace));
        }
        return client.get(path.toString(), Map.class);
    }

    /**
     * Creates a new infrastructure (K8s cluster) profile.
     *
     * @param profile the profile to create
     * @return the created profile
     */
    public ChaosProfile createProfile(ChaosProfile profile) throws MockartyException {
        return client.post("/api/v1/chaos/profiles", profile, ChaosProfile.class);
    }

    /**
     * Updates an existing infrastructure profile.
     *
     * @param id      the profile ID
     * @param profile the updated profile data
     * @return the updated profile
     */
    public ChaosProfile updateProfile(String id, ChaosProfile profile) throws MockartyException {
        return client.put("/api/v1/chaos/profiles/" + encode(id), profile, ChaosProfile.class);
    }

    /**
     * Deletes an infrastructure profile.
     *
     * @param id the profile ID to delete
     */
    public void deleteProfile(String id) throws MockartyException {
        client.delete("/api/v1/chaos/profiles/" + encode(id));
    }

    /**
     * Tests connectivity to a K8s cluster using a saved profile.
     *
     * @param id the profile ID to test
     * @return test result with connected, capabilities, etc.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> testProfile(String id) throws MockartyException {
        return client.post("/api/v1/chaos/profiles/" + encode(id) + "/test", null, Map.class);
    }

    /**
     * Tests connectivity to a K8s cluster using inline kubeconfig data (not a saved profile).
     *
     * @param kubeconfig the kubeconfig content
     * @param context    the kubectl context name (nullable)
     * @return test result with connected, capabilities, etc.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> testInlineConnection(String kubeconfig, String context) throws MockartyException {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("kubeconfig", kubeconfig);
        if (context != null) {
            body.put("context", context);
        }
        return client.post("/api/v1/chaos/profiles-test", body, Map.class);
    }

    /**
     * Connects to a K8s cluster profile and initializes the chaos engine.
     *
     * @param id the profile ID to connect
     * @return connection result with connected, capabilities, etc.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> connectProfile(String id) throws MockartyException {
        return client.post("/api/v1/chaos/profiles/" + encode(id) + "/connect", null, Map.class);
    }

    // ---- Operator Management ----

    /**
     * Generates a YAML manifest for installing the chaos operator in K8s.
     *
     * @param adminUrl the Mockarty admin URL (nullable)
     * @param image    the operator image (nullable, defaults to mockarty/chaos-operator:latest)
     * @return the operator manifest as a string
     */
    public String generateOperatorManifest(String adminUrl, String image) throws MockartyException {
        Map<String, Object> body = new java.util.HashMap<>();
        if (adminUrl != null) {
            body.put("adminUrl", adminUrl);
        }
        if (image != null) {
            body.put("image", image);
        }
        return client.post("/api/v1/chaos/operator/manifest", body, String.class);
    }

    /**
     * Generates the operator manifest for manual application (kubectl apply).
     *
     * @param namespace the K8s namespace for the operator (nullable, defaults to "mockarty-chaos")
     * @return install result with status, message, manifest
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> installOperator(String namespace) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/operator/install");
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(encode(namespace));
        }
        return client.post(path.toString(), null, Map.class);
    }

    /**
     * Generates the operator manifest for manual application (kubectl apply)
     * using the default namespace.
     *
     * @return install result with status, message, manifest
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> installOperator() throws MockartyException {
        return installOperator(null);
    }

    /**
     * Gets the chaos operator status in a namespace.
     *
     * @param namespace the namespace to check (defaults to "mockarty-chaos" on server)
     * @return operator status data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getOperatorStatus(String namespace) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/operator/status");
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(encode(namespace));
        }
        return client.get(path.toString(), Map.class);
    }

    // ---- Kubernetes Cluster Operations ----

    /**
     * Gets the topology of a connected Kubernetes cluster.
     *
     * @param clusterId the cluster ID
     * @param namespace the namespace to inspect (nullable for all namespaces)
     * @return topology data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTopology(String clusterId, String namespace) throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/clusters/" + encode(clusterId) + "/topology");
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(encode(namespace));
        }
        return client.get(path.toString(), Map.class);
    }

    /**
     * Kills a specific pod in a Kubernetes cluster.
     * Uses DELETE method with namespace and name as path params.
     *
     * @param namespace   the pod namespace
     * @param name        the pod name
     * @param gracePeriod the grace period in seconds (0 for immediate)
     */
    public void killPod(String namespace, String name, int gracePeriod) throws MockartyException {
        String path = "/api/v1/chaos/pods/" + encode(namespace) + "/" + encode(name);
        if (gracePeriod > 0) {
            path += "?gracePeriod=" + gracePeriod;
        }
        client.delete(path);
    }

    /**
     * Gets detailed information about a specific pod.
     *
     * @param namespace the pod namespace
     * @param name      the pod name
     * @return pod detail data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getPodDetail(String namespace, String name) throws MockartyException {
        return client.get("/api/v1/chaos/pods/" + encode(namespace) + "/" + encode(name), Map.class);
    }

    /**
     * Gets logs from a specific pod.
     *
     * @param namespace the pod namespace
     * @param name      the pod name
     * @param container the container name (nullable for default container)
     * @param tailLines the number of lines to tail
     * @return the pod logs
     */
    public String getPodLogs(String namespace, String name, String container, int tailLines)
            throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/pods/" + encode(namespace) + "/" + encode(name) + "/logs");
        path.append("?tailLines=").append(tailLines);
        if (container != null && !container.isEmpty()) {
            path.append("&container=").append(encode(container));
        }
        return client.get(path.toString(), String.class);
    }

    /**
     * Gets detailed information about a specific deployment.
     *
     * @param namespace the deployment namespace
     * @param name      the deployment name
     * @return deployment detail data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDeploymentDetail(String namespace, String name) throws MockartyException {
        return client.get("/api/v1/chaos/deployments/" + encode(namespace) + "/" + encode(name), Map.class);
    }

    /**
     * Scales a Kubernetes deployment.
     *
     * @param namespace the deployment namespace
     * @param name      the deployment name
     * @param replicas  the desired number of replicas
     * @return scale operation result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> scaleDeployment(String namespace, String name, int replicas)
            throws MockartyException {
        Map<String, Object> body = Map.of("replicas", replicas);
        return client.post("/api/v1/chaos/deployments/" + encode(namespace) + "/" + encode(name) + "/scale", body, Map.class);
    }

    /**
     * Restarts a Kubernetes deployment (rolling restart).
     *
     * @param namespace the deployment namespace
     * @param name      the deployment name
     * @return restart result
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> restartDeployment(String namespace, String name) throws MockartyException {
        return client.post("/api/v1/chaos/deployments/" + encode(namespace) + "/" + encode(name) + "/restart", null, Map.class);
    }

    // ---- ConfigMaps ----

    /**
     * Lists all ConfigMaps in a namespace.
     *
     * @param namespace the namespace
     * @return configmaps wrapper with configmaps, count, namespace
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listConfigMaps(String namespace) throws MockartyException {
        return client.get("/api/v1/chaos/configmaps/" + encode(namespace), Map.class);
    }

    /**
     * Gets a specific ConfigMap.
     *
     * @param namespace the ConfigMap namespace
     * @param name      the ConfigMap name
     * @return ConfigMap data
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfigMap(String namespace, String name) throws MockartyException {
        return client.get("/api/v1/chaos/configmaps/" + encode(namespace) + "/" + encode(name), Map.class);
    }

    /**
     * Updates a ConfigMap's data section.
     *
     * @param namespace the ConfigMap namespace
     * @param name      the ConfigMap name
     * @param data      the new data key-value pairs
     */
    public void updateConfigMap(String namespace, String name, Map<String, String> data) throws MockartyException {
        Map<String, Object> body = Map.of("data", data);
        client.put("/api/v1/chaos/configmaps/" + encode(namespace) + "/" + encode(name), body);
    }

    // ---- Services ----

    /**
     * Lists all services in a namespace.
     *
     * @param namespace the namespace
     * @return services wrapper with services, count, namespace
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listServices(String namespace) throws MockartyException {
        return client.get("/api/v1/chaos/services/" + encode(namespace), Map.class);
    }

    // ---- CRDs ----

    /**
     * Lists all Custom Resource Definitions in the cluster.
     *
     * @return CRDs wrapper with crds, count
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listCRDs() throws MockartyException {
        return client.get("/api/v1/chaos/crds", Map.class);
    }

    /**
     * Lists instances of a specific custom resource.
     *
     * @param group     the API group
     * @param version   the API version
     * @param resource  the resource name
     * @param namespace the namespace to filter (nullable for all namespaces)
     * @return CRD resources wrapper
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listCRDResources(String group, String version, String resource, String namespace)
            throws MockartyException {
        StringBuilder path = new StringBuilder("/api/v1/chaos/crds/" + encode(group) + "/" + encode(version) + "/" + encode(resource));
        if (namespace != null && !namespace.isEmpty()) {
            path.append("?namespace=").append(encode(namespace));
        }
        return client.get(path.toString(), Map.class);
    }

    // ---- Kubernetes Events ----

    /**
     * Lists recent Kubernetes events in a namespace.
     *
     * @param namespace the namespace
     * @param limit     the maximum number of events (default 100 on server, max 1000)
     * @return events wrapper with events, count, namespace
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listK8sEvents(String namespace, int limit) throws MockartyException {
        return client.get("/api/v1/chaos/events/" + encode(namespace) + "?limit=" + limit, Map.class);
    }

    /**
     * Lists recent Kubernetes events in a namespace with default limit.
     *
     * @param namespace the namespace
     * @return events wrapper
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listK8sEvents(String namespace) throws MockartyException {
        return client.get("/api/v1/chaos/events/" + encode(namespace), Map.class);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
