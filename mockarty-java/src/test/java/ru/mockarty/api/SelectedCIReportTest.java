package ru.mockarty.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.model.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class SelectedCIReportTest {
 static final String ID="11111111-1111-4111-8111-111111111111";
 static final ObjectMapper M=new ObjectMapper();
 static String envelope()throws Exception{
  Map<String,Object> item=Map.of("itemUid",ID,"caseId",ID,"versionId",ID,"groupId",ID,"attemptId",ID,"jobConfigId",ID,"selector",Map.of("id","selected","selector","suite.test"));
  Map<String,Object> data=Map.of("format","mockarty.native-ci-job/v1","namespace","qa","token",Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]),"items",List.of(item),"launchId",ID,"groupId",ID,"attemptId",ID,"invocationId",ID);
  return Base64.getUrlEncoder().withoutPadding().encodeToString(M.writeValueAsBytes(data));
 }
 @Test void exactPinnedReportAndMetadataClone()throws Exception{
  String raw=envelope();CIJobContext j=CIJobContext.load(raw);
  Map<String,Object> metadata=Map.of("environment",Map.of("MOCKARTY_CI_CONTEXT",raw,"safe","kept"),"stdout",raw);
  ExternalRunRequest original=new ExternalRunRequest().caseName("ordinary").autoCreate(true).externalId("selected").fullName("suite.test").status("passed").metadata(metadata);
  ExternalRunRequest out=CIJobContext.prepare("qa",original,M,raw);JsonNode wire=M.valueToTree(out);
  assertEquals(ID,wire.path("caseId").asText());assertEquals(ID,wire.path("ciContext").path("itemUid").asText());assertFalse(wire.path("autoCreate").asBoolean());
  assertFalse(wire.path("metadata").toString().contains(raw));assertFalse(wire.path("metadata").toString().contains("MOCKARTY_CI_CONTEXT"));assertTrue(original.isAutoCreate());assertEquals(metadata,original.getMetadata());
  assertFalse(j.toString().contains(j.callback().getToken()));assertFalse(j.callback().toString().contains(j.callback().getToken()));
 }
 @Test void unknownForeignMalformedAndAttachmentRefused()throws Exception{
  String raw=envelope();ExternalRunRequest r=new ExternalRunRequest().externalId("unknown").fullName("suite.test");
  assertThrows(IllegalArgumentException.class,()->CIJobContext.prepare("qa",r,M,raw));
  r.externalId("selected");assertThrows(IllegalArgumentException.class,()->CIJobContext.prepare("foreign",r,M,raw));
  assertThrows(IllegalArgumentException.class,()->CIJobContext.load(raw+"="));
  ExternalRunRequest secret=new ExternalRunRequest().externalId("selected").fullName("suite.test").attachments(List.of(new ExternalAttachment().name("log").bodyB64(Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)))));
  assertThrows(IllegalArgumentException.class,()->CIJobContext.prepare("qa",secret,M,raw));
 }
 @Test void completionAndResultExactlyOnePost()throws Exception{
  AtomicInteger calls=new AtomicInteger();AtomicReference<JsonNode> wire=new AtomicReference<>();AtomicReference<String> path=new AtomicReference<>();
  HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",e->{calls.incrementAndGet();path.set(e.getRequestURI().getPath());wire.set(M.readTree(e.getRequestBody()));byte[] b="{\"error\":\"unavailable\"}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(503,b.length);e.getResponseBody().write(b);e.close();});server.start();
  try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").maxRetries(3).build()){
   CIJobContext job=CIJobContext.load(envelope());assertThrows(Exception.class,()->client.externalRuns().completeGroup("qa",job.callback(),"failed"));assertEquals(1,calls.get());assertTrue(path.get().endsWith("/groups/"+ID+"/complete"));assertEquals(CICallback.ZERO,wire.get().path("callback").path("itemUid").asText());
   calls.set(0);ExternalRunRequest r=job.bindReport(new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed"),M);
   assertThrows(Exception.class,()->client.externalRuns().report("qa",r));assertEquals(1,calls.get());assertEquals(ID,wire.get().path("ciContext").path("itemUid").asText());
  }finally{server.stop(0);}
 }
 @Test void optionalSelectorStringsMatchDecoderParity()throws Exception{
  for(String key:List.of("id","selector")){
   com.fasterxml.jackson.databind.node.ObjectNode body=(com.fasterxml.jackson.databind.node.ObjectNode)M.readTree(Base64.getUrlDecoder().decode(envelope()));
   com.fasterxml.jackson.databind.node.ObjectNode selector=(com.fasterxml.jackson.databind.node.ObjectNode)body.path("items").get(0).path("selector");
   selector.put(key,"");String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(M.writeValueAsBytes(body));
   assertDoesNotThrow(()->CIJobContext.load(raw));
   selector.putNull(key);String invalid=Base64.getUrlEncoder().withoutPadding().encodeToString(M.writeValueAsBytes(body));
   assertThrows(IllegalArgumentException.class,()->CIJobContext.load(invalid));
  }
 }
}
