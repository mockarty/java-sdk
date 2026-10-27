package ru.mockarty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.mockarty.model.MissionAnswerRequest;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MissionAnswerIdentityTest {
 @Test void capturedFieldsSerializeWithoutLegacyAnswer() throws Exception {
  var request=new MissionAnswerRequest().questionId("A").revision(2).generation(3).text("answer A")
   .selected(List.of("staging")).entity(new MissionAnswerRequest.Entity().kind("environment").id("env-A").namespace("team-A")).idempotencyKey("retry-A");
  request.validate();
  var body=new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(request));
  assertEquals("A",body.path("questionId").asText());assertEquals(2,body.path("revision").asLong());
  assertEquals(3,body.path("generation").asLong());assertEquals("answer A",body.path("text").asText());
  assertEquals("staging",body.path("selected").get(0).asText());assertEquals("team-A",body.path("entity").path("namespace").asText());
  assertEquals("retry-A",body.path("idempotencyKey").asText());assertFalse(body.has("answer"));
 }
 @Test void capturedIdentityValidation() {
  for(var request:List.of(new MissionAnswerRequest().text("missing identity"),new MissionAnswerRequest().questionId("A").text("missing revision"),
   new MissionAnswerRequest().questionId("A").revision(1).generation(-1).text("negative"),
   new MissionAnswerRequest().questionId("A").revision(1).text("typed").answer("legacy"),
   new MissionAnswerRequest().questionId("A").revision(1),
   new MissionAnswerRequest().questionId("A").revision(1).selected(List.of("one"," one ")),
   new MissionAnswerRequest().questionId("A").revision(1).text("я".repeat(4001)),
   new MissionAnswerRequest().questionId("A").revision(1).entity(new MissionAnswerRequest.Entity().kind("environment")))) {
    assertThrows(IllegalArgumentException.class,request::validate);
  }
  assertDoesNotThrow(()->new MissionAnswerRequest().answer("legacy").validate());
  assertDoesNotThrow(()->new MissionAnswerRequest().questionId("A").revision(1).text("я".repeat(4000)).validate());
 }
}
