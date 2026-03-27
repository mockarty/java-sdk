// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the MIT License. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;
import ru.mockarty.builder.MockBuilder;
import ru.mockarty.model.Mock;
import ru.mockarty.model.MockFolder;
import ru.mockarty.model.Page;
import ru.mockarty.model.Tag;

import java.util.List;
import java.util.Map;

/**
 * Tag and folder management examples demonstrating organization
 * of mocks through tagging and hierarchical folder structures.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>Creating and listing tags</li>
 *   <li>Folder CRUD and hierarchical organization</li>
 *   <li>Batch tag updates for multiple mocks</li>
 *   <li>Moving mocks between folders</li>
 *   <li>Filtering mocks by tags</li>
 * </ul>
 */
public class TagsAndFoldersExample {

    public static void main(String[] args) {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://localhost:5770")
                .apiKey("your-api-key")
                .namespace("sandbox")
                .build()) {

            tagManagement(client);
            folderManagement(client);
            batchTagOperations(client);
            organizeWithFolders(client);
            filterByTags(client);
        }
    }

    /**
     * Create and list tags for organizing mocks.
     */
    static void tagManagement(MockartyClient client) {
        System.out.println("=== Tag Management ===");

        // Create tags
        Tag userTag = client.tags().create("users");
        System.out.println("Created tag: " + userTag.getName());

        Tag authTag = client.tags().create("authentication");
        System.out.println("Created tag: " + authTag.getName());

        Tag v2Tag = client.tags().create("v2");
        System.out.println("Created tag: " + v2Tag.getName());

        client.tags().create("critical-path");
        client.tags().create("integration-test");
        client.tags().create("smoke-test");

        // List all tags
        List<Tag> tags = client.tags().list();
        System.out.println("Total tags: " + tags.size());
        for (Tag tag : tags) {
            System.out.println("  - " + tag.getName());
        }
    }

    /**
     * Create, update, and organize folder hierarchies.
     */
    static void folderManagement(MockartyClient client) {
        System.out.println("\n=== Folder Management ===");

        // Create root folders
        MockFolder apiFolder = client.folders().create(
                new MockFolder().name("API Services").description("All API service mocks")
        );
        System.out.println("Created folder: " + apiFolder.getName() + " (id: " + apiFolder.getId() + ")");

        MockFolder integrationFolder = client.folders().create(
                new MockFolder().name("Integration Tests").description("Mocks for integration testing")
        );
        System.out.println("Created folder: " + integrationFolder.getName());

        // Create sub-folders
        MockFolder usersFolder = client.folders().create(
                new MockFolder()
                        .name("User Service")
                        .description("User-related mocks")
                        .parentId(apiFolder.getId())
        );
        System.out.println("Created sub-folder: " + usersFolder.getName());

        MockFolder ordersFolder = client.folders().create(
                new MockFolder()
                        .name("Order Service")
                        .description("Order-related mocks")
                        .parentId(apiFolder.getId())
        );
        System.out.println("Created sub-folder: " + ordersFolder.getName());

        MockFolder paymentsFolder = client.folders().create(
                new MockFolder()
                        .name("Payment Service")
                        .description("Payment-related mocks")
                        .parentId(apiFolder.getId())
        );
        System.out.println("Created sub-folder: " + paymentsFolder.getName());

        // List all folders
        List<MockFolder> folders = client.folders().list();
        System.out.println("Total folders: " + folders.size());
        for (MockFolder folder : folders) {
            String indent = folder.getParentId() != null ? "    " : "  ";
            System.out.println(indent + "- " + folder.getName() + " (id: " + folder.getId() + ")");
        }

        // Update a folder
        MockFolder updated = client.folders().update(usersFolder.getId(),
                new MockFolder()
                        .name("User Service (v2)")
                        .description("Updated user service mocks for v2 API")
        );
        System.out.println("Updated folder: " + updated.getName());

        // Move a folder under a new parent
        client.folders().move(paymentsFolder.getId(), integrationFolder.getId());
        System.out.println("Moved 'Payment Service' under 'Integration Tests'");

        // Move to root (no parent)
        // client.folders().move(paymentsFolder.getId(), null);
    }

    /**
     * Batch update tags for multiple mocks at once.
     */
    static void batchTagOperations(MockartyClient client) {
        System.out.println("\n=== Batch Tag Operations ===");

        // Create some mocks first
        Mock mock1 = MockBuilder.http("/api/tag-demo/1", "GET")
                .id("tag-demo-1")
                .tags("demo")
                .respond(200, Map.of("item", 1))
                .build();

        Mock mock2 = MockBuilder.http("/api/tag-demo/2", "GET")
                .id("tag-demo-2")
                .tags("demo")
                .respond(200, Map.of("item", 2))
                .build();

        Mock mock3 = MockBuilder.http("/api/tag-demo/3", "GET")
                .id("tag-demo-3")
                .tags("demo")
                .respond(200, Map.of("item", 3))
                .build();

        client.mocks().create(mock1);
        client.mocks().create(mock2);
        client.mocks().create(mock3);
        System.out.println("Created 3 demo mocks");

        // Batch update tags for all three mocks
        client.mocks().batchUpdateTags(
                List.of("tag-demo-1", "tag-demo-2", "tag-demo-3"),
                List.of("users", "v2", "critical-path")
        );
        System.out.println("Applied tags [users, v2, critical-path] to 3 mocks");

        // Verify tags were applied
        Mock verified = client.mocks().get("tag-demo-1");
        System.out.println("Mock 'tag-demo-1' tags: " + verified.getTags());

        // Clean up
        client.mocks().delete("tag-demo-1");
        client.mocks().delete("tag-demo-2");
        client.mocks().delete("tag-demo-3");
        System.out.println("Cleaned up demo mocks");
    }

    /**
     * Organize mocks into folders programmatically.
     */
    static void organizeWithFolders(MockartyClient client) {
        System.out.println("\n=== Organize with Folders ===");

        List<MockFolder> folders = client.folders().list();
        if (folders.isEmpty()) {
            System.out.println("No folders available for organization");
            return;
        }

        String targetFolderId = folders.get(0).getId();

        // Create mocks and move them to a folder
        Mock userMock = MockBuilder.http("/api/users", "GET")
                .id("folder-user-list")
                .tags("users")
                .respond(200, Map.of("users", List.of()))
                .build();

        Mock userDetailMock = MockBuilder.http("/api/users/:id", "GET")
                .id("folder-user-detail")
                .tags("users")
                .respond(200, Map.of("id", "$.pathParam.id", "name", "$.fake.FirstName"))
                .build();

        client.mocks().create(userMock);
        client.mocks().create(userDetailMock);

        // Move mocks to the folder
        client.mocks().moveToFolder(
                List.of("folder-user-list", "folder-user-detail"),
                targetFolderId
        );
        System.out.println("Moved 2 mocks to folder: " + folders.get(0).getName());

        // Clean up
        client.mocks().delete("folder-user-list");
        client.mocks().delete("folder-user-detail");
    }

    /**
     * Filter mocks by tags for targeted operations.
     */
    static void filterByTags(MockartyClient client) {
        System.out.println("\n=== Filter by Tags ===");

        // List mocks filtered by tags
        Page<Mock> userMocks = client.mocks().list(
                "sandbox",
                List.of("users"),       // Filter by tag
                null,                    // No search text
                0,                       // Offset
                50                       // Limit
        );
        System.out.println("Mocks tagged 'users': " + userMocks.getTotal());

        // Filter by multiple tags (AND logic)
        Page<Mock> criticalV2 = client.mocks().list(
                "sandbox",
                List.of("critical-path", "v2"),
                null,
                0,
                50
        );
        System.out.println("Mocks tagged 'critical-path' + 'v2': " + criticalV2.getTotal());

        // Search with text filter
        Page<Mock> searchResult = client.mocks().list(
                "sandbox",
                null,
                "user",                  // Search for "user" in mock IDs/routes
                0,
                50
        );
        System.out.println("Mocks matching 'user': " + searchResult.getTotal());
    }
}
