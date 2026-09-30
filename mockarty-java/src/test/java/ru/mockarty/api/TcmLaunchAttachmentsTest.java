package ru.mockarty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import ru.mockarty.MockartyClient;
import ru.mockarty.exception.MockartyException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TcmLaunchAttachmentsTest {
 @Test void allPagesAndCorruptResponses() throws Exception {
  for(String mode:List.of("ok","duplicate","empty-more","changing-total","bad-offset","foreign-owner","oversized-total","missing-meta","missing-snapshot","changed-snapshot","request-cap")) {
   AtomicInteger calls=new AtomicInteger();ObjectMapper mapper=new ObjectMapper();
   HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/",exchange->{
    calls.incrementAndGet();String query=exchange.getRequestURI().getQuery();int offset=0;
    for(String part:query.split("&")){if(part.startsWith("offset="))offset=Integer.parseInt(part.substring(7));}
    int total=mode.equals("request-cap")?101:2;
    if(mode.equals("changing-total")&&offset>0)total=3;
    if(mode.equals("oversized-total"))total=10001;
    Map<String,Object> item=Map.of("id",mode.equals("duplicate")?"same":"a"+offset,"namespace","qa","parentKind","test_plan_run","parentId",mode.equals("foreign-owner")?"foreign":"run");
    List<Map<String,Object>> items=mode.equals("empty-more")?List.of():List.of(item);
    Map<String,Object> page=new HashMap<>(Map.of("items",items,"total",total,"offset",mode.equals("bad-offset")?1:offset,"limit",500,"snapshot","a".repeat(64),"hasMore",offset+items.size()<total));
    if(mode.equals("missing-meta"))page.remove("total");
    if(mode.equals("missing-snapshot"))page.remove("snapshot");
    if(mode.equals("changed-snapshot")&&offset>0)page.put("snapshot","b".repeat(64));
    byte[] data=mapper.writeValueAsBytes(page);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,data.length);
    try(var out=exchange.getResponseBody()){out.write(data);}
   });server.start();
   try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:"+server.getAddress().getPort()).namespace("qa").build()){
    if(mode.equals("ok")){assertEquals(2,client.tcm().listAttachments(null,"test_plan_run","run").size());assertEquals(2,calls.get());}
    else{assertThrows(MockartyException.class,()->client.tcm().listAttachments(null,"test_plan_run","run"),mode);}
    if(mode.equals("request-cap"))assertEquals(100,calls.get());
   }finally{server.stop(0);}
  }
 }
 @Test void interruptedListingMakesNoRequest(){
  try(MockartyClient client=MockartyClient.builder().baseUrl("http://127.0.0.1:1").namespace("qa").build()){
   Thread.currentThread().interrupt();
   assertThrows(MockartyException.class,()->client.tcm().listAttachments(null,"test_plan_run","run"));
  }finally{Thread.interrupted();}
 }
}
