// Copyright (c) 2026 Mockarty. All rights reserved.
// Licensed under the Mockarty SDK License Agreement. See LICENSE file for details.

package ru.mockarty.examples;

import ru.mockarty.MockartyClient;

public final class ExperienceExample {
    private ExperienceExample() {}

    public static void main(String[] args) throws Exception {
        try (MockartyClient client = MockartyClient.create()) {
            client.experience().search("payment retry").getResults()
                    .forEach(item -> System.out.println(item.getKind() + ": " + item.getText()));
            client.experience().listReview("candidate", 20, null).getItems()
                    .forEach(item -> System.out.println("review " + item.getState() + " " + item.getId()
                            + " v" + item.getVersion() + ": " + item.getSource()));
        }
    }
}
