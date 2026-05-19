package ru.mockarty.protocols.graphql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphQLOperationExtractTest {

    @Test
    void extractsNamedQuery() {
        assertEquals("GetUser",
            GraphQLClient.extractOperationName("query GetUser { user { id } }").orElse(null));
    }

    @Test
    void extractsNamedMutation() {
        assertEquals("CreateOrder",
            GraphQLClient.extractOperationName("mutation CreateOrder($id: ID!) { create(id: $id) }").orElse(null));
    }

    @Test
    void extractsSubscription() {
        assertEquals("OnUpdate",
            GraphQLClient.extractOperationName("subscription OnUpdate { update }").orElse(null));
    }

    @Test
    void anonymousQueryReturnsEmpty() {
        assertFalse(GraphQLClient.extractOperationName("{ ping }").isPresent());
    }

    @Test
    void emptyInputReturnsEmpty() {
        assertFalse(GraphQLClient.extractOperationName("").isPresent());
        assertFalse(GraphQLClient.extractOperationName(null).isPresent());
    }

    @Test
    void skipsLeadingCommentsAndBlankLines() {
        String q = "# comment\n\nquery GetUser { user { id } }";
        assertEquals("GetUser", GraphQLClient.extractOperationName(q).orElse(null));
    }

    @Test
    void stopsAtNonIdentifierChar() {
        // The name is followed by an open paren — extraction stops at '('.
        assertEquals("GetUser",
            GraphQLClient.extractOperationName("query GetUser($id: ID!) { user(id: $id) { id } }").orElse(null));
    }

    @Test
    void emptyClientUrlRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GraphQLClient(""));
    }

    @Test
    void emptyQueryRejected() {
        GraphQLClient c = new GraphQLClient("http://test");
        assertThrows(IllegalArgumentException.class, () -> c.execute(""));
    }
}
