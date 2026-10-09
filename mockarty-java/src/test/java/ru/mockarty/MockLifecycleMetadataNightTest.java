package ru.mockarty;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.mockarty.model.Mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockLifecycleMetadataNightTest {
 @Test void readsGetMetadataAndPreservesExplicitRoot() throws Exception {
  ObjectMapper mapper=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
  String[] rows={"{\"_meta\":{\"closedAt\":1700000000}}","{\"closedAt\":12,\"_meta\":{\"closedAt\":20}}","{\"_meta\":{\"closedAt\":20},\"closedAt\":12}","{\"closedAt\":0,\"_meta\":{\"closedAt\":20}}","{\"closedAt\":null,\"_meta\":{\"closedAt\":20}}"};
  long[] wants={1700000000,12,12,0,20};
  for(int i=0;i<rows.length;i++) assertEquals(wants[i],mapper.readValue(rows[i],Mock.class).getClosedAt());
  Mock decoded = mapper.readValue(rows[0], Mock.class);
  assertFalse(mapper.writeValueAsString(decoded).contains("_meta"));
 }
}
