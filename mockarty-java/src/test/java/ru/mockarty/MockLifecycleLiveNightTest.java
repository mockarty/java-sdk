package ru.mockarty;

import org.junit.jupiter.api.Test;
import ru.mockarty.model.Mock;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MockLifecycleLiveNightTest {
 @Test void readsDeletedMockFromOwnedNativeStand() {
  String base=System.getenv("MOCKARTY_NIGHT_BASE"), key=System.getenv("MOCKARTY_NIGHT_KEY"), ns=System.getenv("MOCKARTY_NIGHT_NAMESPACE"), id=System.getenv("MOCKARTY_NIGHT_MOCK");
  assumeTrue(base!=null && key!=null && ns!=null && id!=null,"owned native fixture required");
  try (MockartyClient client=MockartyClient.builder().baseUrl(base).apiKey(key).namespace(ns).build()) {
   Mock mock=client.mocks().get(id);
   assertTrue(mock.getClosedAt()!=null && mock.getClosedAt()>0,"SDK lost live deletion marker");
  }
 }
}
