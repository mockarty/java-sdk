// Copyright (c) 2024-2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Mock is the core model representing a single mock definition in Mockarty.
 * It contains all protocol-specific request contexts and response configurations.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Mock {

    @JsonProperty("id")
    private String id;

    @JsonProperty("chainId")
    private String chainId;

    @JsonProperty("namespace")
    private String namespace;

    @JsonProperty("pathPrefix")
    private String pathPrefix;

    @JsonProperty("serverName")
    private String serverName;

    @JsonProperty("http")
    private HttpRequestContext http;

    @JsonProperty("grpc")
    private GrpcRequestContext grpc;

    @JsonProperty("mcp")
    private MCPRequestContext mcp;

    @JsonProperty("socket")
    private SocketRequestContext socket;

    @JsonProperty("soap")
    private SoapRequestContext soap;

    @JsonProperty("graphql")
    private GraphQLRequestContext graphql;

    @JsonProperty("sse")
    private SSERequestContext sse;

    @JsonProperty("kafka")
    private KafkaRequestContext kafka;

    @JsonProperty("rabbitmq")
    private RabbitMQRequestContext rabbitmq;

    @JsonProperty("smtp")
    private SmtpRequestContext smtp;

    @JsonProperty("response")
    private ContentResponse response;

    @JsonProperty("oneOf")
    private OneOf oneOf;

    @JsonProperty("proxy")
    private Proxy proxy;

    @JsonProperty("webhooks")
    private List<Callback> callbacks;

    @JsonProperty("ttl")
    private Long ttl;

    @JsonProperty("useLimiter")
    private Integer useLimiter;

    @JsonProperty("useCounter")
    private Integer useCounter;

    @JsonProperty("priority")
    private Long priority;

    @JsonProperty("tags")
    private List<String> tags;

    @JsonProperty("folderId")
    private String folderId;

    @JsonProperty("createdAt")
    private Long createdAt;

    @JsonProperty("lastUse")
    private Long lastUse;

    @JsonProperty("expireAt")
    private Long expireAt;

    @JsonProperty("closedAt")
    private Long closedAt;

    @JsonProperty("extract")
    private Extract extract;

    @JsonProperty("mStore")
    private Map<String, Object> mockStore;

    public Mock() {
    }

    /**
     * Determines the protocol of this mock based on which request context is set.
     */
    public Protocol protocol() {
        if (smtp != null) return Protocol.SMTP;
        if (kafka != null) return Protocol.KAFKA;
        if (rabbitmq != null) return Protocol.RABBITMQ;
        if (sse != null) return Protocol.SSE;
        if (graphql != null) return Protocol.GRAPHQL;
        if (soap != null) return Protocol.SOAP;
        if (socket != null) return Protocol.SOCKET;
        if (mcp != null) return Protocol.MCP;
        if (grpc != null) return Protocol.GRPC;
        return Protocol.HTTP;
    }

    // Builder-style setters

    public Mock id(String id) {
        this.id = id;
        return this;
    }

    public Mock chainId(String chainId) {
        this.chainId = chainId;
        return this;
    }

    public Mock namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Mock pathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
        return this;
    }

    public Mock serverName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public Mock http(HttpRequestContext http) {
        this.http = http;
        return this;
    }

    public Mock grpc(GrpcRequestContext grpc) {
        this.grpc = grpc;
        return this;
    }

    public Mock mcp(MCPRequestContext mcp) {
        this.mcp = mcp;
        return this;
    }

    public Mock socket(SocketRequestContext socket) {
        this.socket = socket;
        return this;
    }

    public Mock soap(SoapRequestContext soap) {
        this.soap = soap;
        return this;
    }

    public Mock graphql(GraphQLRequestContext graphql) {
        this.graphql = graphql;
        return this;
    }

    public Mock sse(SSERequestContext sse) {
        this.sse = sse;
        return this;
    }

    public Mock kafka(KafkaRequestContext kafka) {
        this.kafka = kafka;
        return this;
    }

    public Mock rabbitmq(RabbitMQRequestContext rabbitmq) {
        this.rabbitmq = rabbitmq;
        return this;
    }

    public Mock smtp(SmtpRequestContext smtp) {
        this.smtp = smtp;
        return this;
    }

    public Mock response(ContentResponse response) {
        this.response = response;
        return this;
    }

    public Mock oneOf(OneOf oneOf) {
        this.oneOf = oneOf;
        return this;
    }

    public Mock proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public Mock callbacks(List<Callback> callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    public Mock addCallback(Callback callback) {
        if (this.callbacks == null) {
            this.callbacks = new ArrayList<>();
        }
        this.callbacks.add(callback);
        return this;
    }

    public Mock ttl(long ttl) {
        this.ttl = ttl;
        return this;
    }

    public Mock useLimiter(int useLimiter) {
        this.useLimiter = useLimiter;
        return this;
    }

    public Mock useCounter(int useCounter) {
        this.useCounter = useCounter;
        return this;
    }

    public Mock priority(long priority) {
        this.priority = priority;
        return this;
    }

    public Mock tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    public Mock tags(String... tags) {
        this.tags = new ArrayList<>(Arrays.asList(tags));
        return this;
    }

    public Mock folderId(String folderId) {
        this.folderId = folderId;
        return this;
    }

    public Mock extract(Extract extract) {
        this.extract = extract;
        return this;
    }

    public Mock mockStore(Map<String, Object> mockStore) {
        this.mockStore = mockStore;
        return this;
    }

    // Getters

    public String getId() {
        return id;
    }

    public String getChainId() {
        return chainId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public String getServerName() {
        return serverName;
    }

    public HttpRequestContext getHttp() {
        return http;
    }

    public GrpcRequestContext getGrpc() {
        return grpc;
    }

    public MCPRequestContext getMcp() {
        return mcp;
    }

    public SocketRequestContext getSocket() {
        return socket;
    }

    public SoapRequestContext getSoap() {
        return soap;
    }

    public GraphQLRequestContext getGraphql() {
        return graphql;
    }

    public SSERequestContext getSse() {
        return sse;
    }

    public KafkaRequestContext getKafka() {
        return kafka;
    }

    public RabbitMQRequestContext getRabbitmq() {
        return rabbitmq;
    }

    public SmtpRequestContext getSmtp() {
        return smtp;
    }

    public ContentResponse getResponse() {
        return response;
    }

    public OneOf getOneOf() {
        return oneOf;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public List<Callback> getCallbacks() {
        return callbacks;
    }

    public Long getTtl() {
        return ttl;
    }

    public Integer getUseLimiter() {
        return useLimiter;
    }

    public Integer getUseCounter() {
        return useCounter;
    }

    public Long getPriority() {
        return priority;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getFolderId() {
        return folderId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getLastUse() {
        return lastUse;
    }

    public Long getExpireAt() {
        return expireAt;
    }

    public Long getClosedAt() {
        return closedAt;
    }

    public Extract getExtract() {
        return extract;
    }

    public Map<String, Object> getMockStore() {
        return mockStore;
    }

    @Override
    public String toString() {
        return "Mock{" +
                "id='" + id + '\'' +
                ", namespace='" + namespace + '\'' +
                ", protocol=" + protocol() +
                '}';
    }
}
