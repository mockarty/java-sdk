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
 @Test void directoryFiltersOutsidersBeforeMedia()throws Exception{
  java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("selected-ci-dir-");
  java.nio.file.Files.writeString(dir.resolve("selected-result.json"),"{\"uuid\":\"selected\",\"fullName\":\"suite.test\",\"name\":\"case\",\"status\":\"passed\"}");
  java.nio.file.Files.writeString(dir.resolve("outsider-result.json"),"{\"uuid\":\"other\",\"fullName\":\"other.test\",\"name\":\"other\",\"status\":\"passed\",\"attachments\":[{\"name\":\"absent\",\"source\":\"missing.txt\"}]}");
  AtomicInteger calls=new AtomicInteger();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",e->{calls.incrementAndGet();JsonNode body=M.readTree(e.getRequestBody());assertEquals(ID,body.path("caseId").asText());assertFalse(body.path("autoCreate").asBoolean());byte[] bytes="{}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
  try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").build()){
   assertEquals(1,client.externalRuns().uploadAllureDirPrepared("qa",dir,envelope()).size());assertEquals(1,calls.get());
  }finally{server.stop(0);try(var files=java.nio.file.Files.list(dir)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(dir);}
 }
 @Test void directoryDuplicateRefusedBeforeHttp()throws Exception{
  java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("selected-ci-duplicate-");
  for(String name:List.of("a-result.json","b-result.json"))java.nio.file.Files.writeString(dir.resolve(name),"{\"uuid\":\"selected\",\"fullName\":\"suite.test\",\"name\":\"case\",\"status\":\"passed\"}");
  try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:1").namespace("qa").build()){
   assertThrows(IllegalArgumentException.class,()->client.externalRuns().uploadAllureDirPrepared("qa",dir,envelope()));
  }finally{try(var files=java.nio.file.Files.list(dir)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(dir);}
 }

 @Test void directoryAllSelectedMediaPreflight()throws Exception{
  var body=(com.fasterxml.jackson.databind.node.ObjectNode)M.readTree(Base64.getUrlDecoder().decode(envelope()));
  var items=(com.fasterxml.jackson.databind.node.ArrayNode)body.path("items");var second=items.get(0).deepCopy();
  ((com.fasterxml.jackson.databind.node.ObjectNode)second).put("itemUid","22222222-2222-4222-8222-222222222222");
  ((com.fasterxml.jackson.databind.node.ObjectNode)second.path("selector")).put("id","second").put("selector","suite.second");items.add(second);
  String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(M.writeValueAsBytes(body));
  java.nio.file.Path dir=java.nio.file.Files.createTempDirectory("selected-ci-preflight-");
  java.nio.file.Files.writeString(dir.resolve("a-result.json"),"{\"uuid\":\"selected\",\"fullName\":\"suite.test\",\"name\":\"first\",\"status\":\"passed\"}");
  java.nio.file.Files.writeString(dir.resolve("b-result.json"),"{\"uuid\":\"second\",\"fullName\":\"suite.second\",\"name\":\"second\",\"status\":\"passed\",\"attachments\":[{\"name\":\"secret\",\"source\":\"secret.txt\"}]}");
  java.nio.file.Files.writeString(dir.resolve("secret.txt"),body.path("token").asText());
  try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:1").namespace("qa").build()){
   assertThrows(IllegalArgumentException.class,()->client.externalRuns().uploadAllureDirPrepared("qa",dir,raw));
  }finally{try(var files=java.nio.file.Files.list(dir)){for(var file:files.toList())java.nio.file.Files.delete(file);}java.nio.file.Files.delete(dir);}
 }

 @Test void allSelectedBodyValidationBeforeHttp()throws Exception{
  for(String kind:List.of("status","step","inline")){
   ExternalRunRequest bad=new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed");
   switch(kind){case "status":bad.status("invalid");break;case "step":bad.steps(List.of(new ExternalStep(" ","passed")));break;case "inline":bad.attachments(List.of(new ExternalAttachment().name("large").bodyB64(Base64.getEncoder().encodeToString(new byte[(256<<10)+1]))));break;}
   AtomicInteger calls=new AtomicInteger();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/",e->{calls.incrementAndGet();byte[] bytes="{}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
   CIJobContext job=CIJobContext.load(envelope());ExternalRunRequest good=job.bindReport(new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed"),M);ExternalRunRequest pinnedBad=job.bindReport(bad,M);
   try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").build()){
    assertThrows(IllegalArgumentException.class,()->client.externalRuns().reportBatch("qa",List.of(good,pinnedBad)),kind);assertEquals(0,calls.get());
   }finally{server.stop(0);}
  }
 }

 @Test void batchSelectedStepLimit()throws Exception{
  for(int count:List.of(1000,1001)){
   AtomicInteger calls=new AtomicInteger();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/",e->{calls.incrementAndGet();byte[] bytes="{}".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,bytes.length);e.getResponseBody().write(bytes);e.close();});server.start();
   CIJobContext job=CIJobContext.load(envelope());ExternalRunRequest good=job.bindReport(new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed"),M);
   List<ExternalStep> steps=new ArrayList<>();for(int i=0;i<count;i++)steps.add(new ExternalStep("step","passed"));
   ExternalRunRequest bad=job.bindReport(new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed").steps(steps),M);
   try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").build()){
    if(count==1000){client.externalRuns().reportBatch("qa",List.of(good,bad));assertEquals(1,calls.get());}
    else{assertThrows(IllegalArgumentException.class,()->client.externalRuns().reportBatch("qa",List.of(good,bad)));assertEquals(0,calls.get());}
   }finally{server.stop(0);}
  }
 }

 @Test void exactDecodedInlineLimit()throws Exception{
  List<ExternalAttachment> attachments=new ArrayList<>();for(int i=0;i<4;i++)attachments.add(new ExternalAttachment().name(Integer.toString(i)).bodyB64(Base64.getEncoder().encodeToString(new byte[256<<10])));
  ExternalRunRequest request=new ExternalRunRequest().externalId("selected").fullName("suite.test").status("passed").attachments(attachments);
  request.metadata(Map.of("bodyB64","customer notes","nested",Map.of("bodyB64",attachments.get(0).getBodyB64())));
  ExternalRunRequest prepared=CIJobContext.prepare("qa",request,M,envelope());
  assertEquals(4,prepared.getAttachments().size());
  assertEquals("customer notes",prepared.getMetadata().get("bodyB64"));
  for(ExternalAttachment attachment:prepared.getAttachments())assertEquals(attachments.get(0).getBodyB64(),attachment.getBodyB64());
  assertFalse(M.writeValueAsString(prepared.getMetadata()).contains(CIJobContext.load(envelope()).callback().getToken()));
 }
}
