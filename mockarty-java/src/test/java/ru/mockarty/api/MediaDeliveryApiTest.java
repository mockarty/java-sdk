package ru.mockarty.api;

import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaDeliveryApiTest {
    @Test
    void pathsAreEncodedAndGuessesAreRejected() throws Exception {
        try (MockartyClient client = MockartyClient.builder()
                .baseUrl("http://127.0.0.1:1").apiKey("mk_test").namespace("team a").build()) {
            MediaDeliveryApi api = client.mediaDelivery();
            var base = MediaDeliveryApi.class.getDeclaredMethod("base", String.class);
            base.setAccessible(true);
            var query = MediaDeliveryApi.class.getDeclaredMethod("namespaceQuery");
            query.setAccessible(true);
            assertEquals("/api/v1/transcribe/jobs", base.invoke(api, "transcribe"));
            assertEquals("?namespace=team%20a", query.invoke(api));
            assertThrows(IllegalArgumentException.class,
                    () -> api.reconcile("tts", "job-1", "runner-1", "maybe"));
        }
    }
}
