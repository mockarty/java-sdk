// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.api;

import java.util.List;
import java.util.Map;

public class CapabilityCatalog {
    private List<Descriptor> capabilities;
    private int count;
    private int skipped;

    public List<Descriptor> getCapabilities() { return capabilities; }
    public void setCapabilities(List<Descriptor> capabilities) { this.capabilities = capabilities; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public static class Descriptor {
        private Schemas schemas;
        private Policy policy;
        private Resource resource;
        private Provenance provenance;
        private Trust trust;
        private Executor executor;
        private Health health;
        private Compatibility compatibility;
        private Availability availability;
        private List<String> hosts;
        private String contractVersion;
        private String key;
        private String version;
        private String provider;
        private String kind;
        private String title;
        private String description;
        private String featureKey;
        private boolean builtin;

        public Schemas getSchemas() { return schemas; }
        public void setSchemas(Schemas schemas) { this.schemas = schemas; }
        public Policy getPolicy() { return policy; }
        public void setPolicy(Policy policy) { this.policy = policy; }
        public Resource getResource() { return resource; }
        public void setResource(Resource resource) { this.resource = resource; }
        public Provenance getProvenance() { return provenance; }
        public void setProvenance(Provenance provenance) { this.provenance = provenance; }
        public Trust getTrust() { return trust; }
        public void setTrust(Trust trust) { this.trust = trust; }
        public Executor getExecutor() { return executor; }
        public void setExecutor(Executor executor) { this.executor = executor; }
        public Health getHealth() { return health; }
        public void setHealth(Health health) { this.health = health; }
        public Compatibility getCompatibility() { return compatibility; }
        public void setCompatibility(Compatibility compatibility) { this.compatibility = compatibility; }
        public Availability getAvailability() { return availability; }
        public void setAvailability(Availability availability) { this.availability = availability; }
        public List<String> getHosts() { return hosts; }
        public void setHosts(List<String> hosts) { this.hosts = hosts; }
        public String getContractVersion() { return contractVersion; }
        public void setContractVersion(String contractVersion) { this.contractVersion = contractVersion; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getFeatureKey() { return featureKey; }
        public void setFeatureKey(String featureKey) { this.featureKey = featureKey; }
        public boolean isBuiltin() { return builtin; }
        public void setBuiltin(boolean builtin) { this.builtin = builtin; }
    }

    public static class Schemas {
        private Map<String, Object> input;
        private Map<String, Object> output;
        private Map<String, Object> settings;
        private Map<String, Object> result;
        private Map<String, Object> evidence;
        private Map<String, Object> admission;

        public Map<String, Object> getInput() { return input; }
        public void setInput(Map<String, Object> input) { this.input = input; }
        public Map<String, Object> getOutput() { return output; }
        public void setOutput(Map<String, Object> output) { this.output = output; }
        public Map<String, Object> getSettings() { return settings; }
        public void setSettings(Map<String, Object> settings) { this.settings = settings; }
        public Map<String, Object> getResult() { return result; }
        public void setResult(Map<String, Object> result) { this.result = result; }
        public Map<String, Object> getEvidence() { return evidence; }
        public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
        public Map<String, Object> getAdmission() { return admission; }
        public void setAdmission(Map<String, Object> admission) { this.admission = admission; }
    }

    public static class Policy {
        private List<String> requiredRoles;
        private List<String> requiredPermissions;
        private String sideEffect;
        private String idempotency;
        private DataBoundary dataBoundary;
        private long timeoutMillis;
        private int maxRetries;

        public List<String> getRequiredRoles() { return requiredRoles; }
        public void setRequiredRoles(List<String> requiredRoles) { this.requiredRoles = requiredRoles; }
        public List<String> getRequiredPermissions() { return requiredPermissions; }
        public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
        public String getSideEffect() { return sideEffect; }
        public void setSideEffect(String sideEffect) { this.sideEffect = sideEffect; }
        public String getIdempotency() { return idempotency; }
        public void setIdempotency(String idempotency) { this.idempotency = idempotency; }
        public DataBoundary getDataBoundary() { return dataBoundary; }
        public void setDataBoundary(DataBoundary dataBoundary) { this.dataBoundary = dataBoundary; }
        public long getTimeoutMillis() { return timeoutMillis; }
        public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    }

    public static class DataBoundary {
        private List<String> classes;
        private List<String> residencies;
        private List<String> allowedHosts;
        private String networkScope;

        public List<String> getClasses() { return classes; }
        public void setClasses(List<String> classes) { this.classes = classes; }
        public List<String> getResidencies() { return residencies; }
        public void setResidencies(List<String> residencies) { this.residencies = residencies; }
        public List<String> getAllowedHosts() { return allowedHosts; }
        public void setAllowedHosts(List<String> allowedHosts) { this.allowedHosts = allowedHosts; }
        public String getNetworkScope() { return networkScope; }
        public void setNetworkScope(String networkScope) { this.networkScope = networkScope; }
    }

    public static class Resource {
        private long memoryBytes;
        private long scratchBytes;
        private long costUpperBoundMicros;
        private int cpuUnits;
        private int maxConcurrency;

        public long getMemoryBytes() { return memoryBytes; }
        public void setMemoryBytes(long memoryBytes) { this.memoryBytes = memoryBytes; }
        public long getScratchBytes() { return scratchBytes; }
        public void setScratchBytes(long scratchBytes) { this.scratchBytes = scratchBytes; }
        public long getCostUpperBoundMicros() { return costUpperBoundMicros; }
        public void setCostUpperBoundMicros(long costUpperBoundMicros) { this.costUpperBoundMicros = costUpperBoundMicros; }
        public int getCpuUnits() { return cpuUnits; }
        public void setCpuUnits(int cpuUnits) { this.cpuUnits = cpuUnits; }
        public int getMaxConcurrency() { return maxConcurrency; }
        public void setMaxConcurrency(int maxConcurrency) { this.maxConcurrency = maxConcurrency; }
    }

    public static class Provenance {
        private String sourceKind;
        private String sourceRef;
        private String digest;
        private String publisher;

        public String getSourceKind() { return sourceKind; }
        public void setSourceKind(String sourceKind) { this.sourceKind = sourceKind; }
        public String getSourceRef() { return sourceRef; }
        public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
        public String getDigest() { return digest; }
        public void setDigest(String digest) { this.digest = digest; }
        public String getPublisher() { return publisher; }
        public void setPublisher(String publisher) { this.publisher = publisher; }
    }

    public static class Trust {
        private String level;
        private String isolation;
        private String signatureKeyId;
        private boolean verified;

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public String getIsolation() { return isolation; }
        public void setIsolation(String isolation) { this.isolation = isolation; }
        public String getSignatureKeyId() { return signatureKeyId; }
        public void setSignatureKeyId(String signatureKeyId) { this.signatureKeyId = signatureKeyId; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
    }

    public static class Executor {
        private String kind;
        private String binding;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getBinding() { return binding; }
        public void setBinding(String binding) { this.binding = binding; }
    }

    public static class Health {
        private String kind;
        private String probe;
        private long timeoutMillis;

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getProbe() { return probe; }
        public void setProbe(String probe) { this.probe = probe; }
        public long getTimeoutMillis() { return timeoutMillis; }
        public void setTimeoutMillis(long timeoutMillis) { this.timeoutMillis = timeoutMillis; }
    }

    public static class Compatibility {
        private String minHostVersion;
        private String maxHostVersion;

        public String getMinHostVersion() { return minHostVersion; }
        public void setMinHostVersion(String minHostVersion) { this.minHostVersion = minHostVersion; }
        public String getMaxHostVersion() { return maxHostVersion; }
        public void setMaxHostVersion(String maxHostVersion) { this.maxHostVersion = maxHostVersion; }
    }

    public static class Availability {
        private String reason;
        private boolean available;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}
