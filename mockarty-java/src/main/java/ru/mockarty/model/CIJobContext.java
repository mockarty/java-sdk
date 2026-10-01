package ru.mockarty.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.util.*;

/** Frozen selected occurrences delivered privately to a CI job. */
public final class CIJobContext {
    public static final String ENVIRONMENT="MOCKARTY_CI_CONTEXT";
    private final JsonNode job;
    private final String raw;
    private CIJobContext(JsonNode job,String raw){this.job=job;this.raw=raw;}
    private static final ObjectMapper MAPPER=new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    public static CIJobContext load(String raw) {
        try {
            if(raw==null||raw.isEmpty()||raw.length()>(1<<20)||!raw.matches("[A-Za-z0-9_-]+")) throw CICallback.invalid();
            byte[] bytes=Base64.getUrlDecoder().decode(raw);
            if(!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(raw)) throw CICallback.invalid();
            String json=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
            JsonNode j;try(JsonParser parser=MAPPER.createParser(json)){j=MAPPER.readTree(parser);if(parser.nextToken()!=null)throw CICallback.invalid();}
            if(j==null||!j.isObject()||!keys(j,Set.of("format","namespace","token","launchId","groupId","attemptId","invocationId","items"))||!j.path("format").asText().equals("mockarty.native-ci-job/v1")||!text(j.path("namespace"),255)) throw CICallback.invalid();
            CIJobContext result=new CIJobContext(j,raw);result.callback().validate(true);
            JsonNode items=j.path("items");if(!items.isArray()||items.isEmpty()||items.size()>1000) throw CICallback.invalid();
            Set<String> seen=new HashSet<>();
            for(JsonNode i:items){
                if(!keys(i,Set.of("selector","itemUid","caseId","versionId","jobConfigId","groupId","attemptId"))) throw CICallback.invalid();
                for(String k:List.of("itemUid","caseId","versionId","jobConfigId"))if(!i.path(k).isTextual()||!CICallback.validUUID(i.path(k).asText()))throw CICallback.invalid();
                if(!i.path("groupId").equals(j.path("groupId"))||!i.path("attemptId").equals(j.path("attemptId"))||!seen.add(i.path("itemUid").asText()))throw CICallback.invalid();
                JsonNode selector=i.path("selector");if(!selector.isObject()||selector.size()==0||selector.size()>2)throw CICallback.invalid();
                Iterator<String> names=selector.fieldNames();while(names.hasNext()){String k=names.next();if(!k.equals("id")&&!k.equals("selector"))throw CICallback.invalid();JsonNode value=selector.get(k);if(!value.isTextual()||!value.asText().isEmpty()&&!text(value,k.equals("id")?512:2048))throw CICallback.invalid();}
                if(selector.path("id").asText().isEmpty()&&selector.path("selector").asText().isEmpty())throw CICallback.invalid();
            }
            return result;
        } catch(Exception e){throw CICallback.invalid();}
    }
    private static boolean keys(JsonNode n,Set<String> keys){if(!n.isObject()||n.size()!=keys.size())return false;Iterator<String> it=n.fieldNames();while(it.hasNext())if(!keys.contains(it.next()))return false;return true;}
    private static boolean text(JsonNode n,int limit){if(!n.isTextual())return false;String s=n.asText();return !s.isEmpty()&&s.equals(s.strip())&&s.getBytes(StandardCharsets.UTF_8).length<=limit&&s.codePoints().noneMatch(Character::isISOControl);}
    public String getNamespace(){return job.path("namespace").asText();}
    public CICallback callback(){return new CICallback(job.path("token").asText(),job.path("launchId").asText(),CICallback.ZERO,job.path("groupId").asText(),job.path("attemptId").asText(),job.path("invocationId").asText());}
    public ExternalRunRequest bindReport(ExternalRunRequest request,ObjectMapper mapper){
        JsonNode match=null;for(JsonNode item:job.path("items")){
            JsonNode s=item.path("selector");String id=s.path("id").asText();String full=s.path("selector").asText();
            if(!id.isEmpty()&&!id.equals(request.getExternalId())&&!id.equals(request.getTestCaseId()))continue;
            if(!full.isEmpty()&&!full.equals(request.getFullName()))continue;
            if(match!=null)throw CICallback.invalid();match=item;
        }
        if(match==null||request.getCaseId()!=null&&!request.getCaseId().isEmpty()&&!request.getCaseId().equals(match.path("caseId").asText()))throw CICallback.invalid();
        CICallback c=callback();CICallback selected=new CICallback(c.getToken(),c.getLaunchId(),match.path("itemUid").asText(),c.getGroupId(),c.getAttemptId(),c.getInvocationId());
        if(request.getCIContext()!=null&&!request.getCIContext().toWire(false).equals(selected.toWire(false)))throw CICallback.invalid();
        ObjectNode n=mapper.valueToTree(request);n.put("caseId",match.path("caseId").asText());n.set("ciContext",mapper.valueToTree(selected));n.remove(List.of("planId","planRunId"));n.put("autoCreate",false);n.put("claimCaseOwnership",false);
        return convert(mapper,n);
    }
    public static ExternalRunRequest prepare(String namespace,ExternalRunRequest request,ObjectMapper mapper,String raw){
        ExternalRunRequest out=request;if(raw!=null&&!raw.isEmpty()){CIJobContext ctx=load(raw);if(!ctx.getNamespace().equals(namespace))throw CICallback.invalid();out=ctx.bindReport(request,mapper);}
        CICallback cb=out.getCIContext();if(cb==null)return out;cb.validate(false);
        ObjectNode n=mapper.valueToTree(out);n.remove("ciContext");List<String> secrets=new ArrayList<>();secrets.add(cb.getToken());if(raw!=null&&!raw.isEmpty())secrets.add(raw);
        n=(ObjectNode)clean(n,secrets,0);n.set("ciContext",mapper.valueToTree(cb));return convert(mapper,n);
    }
    private static ExternalRunRequest convert(ObjectMapper mapper,JsonNode n){try{return mapper.treeToValue(n,ExternalRunRequest.class);}catch(Exception e){throw CICallback.invalid();}}
    private static JsonNode clean(JsonNode value,List<String> secrets,int depth){
        if(depth>64)throw CICallback.invalid();
        if(value.isObject()){
            if(value.path("name").asText().equalsIgnoreCase(ENVIRONMENT))return null;
            ObjectNode out=((ObjectNode)value).deepCopy();Iterator<String> keys=value.fieldNames();while(keys.hasNext()){
                String key=keys.next();if(key.equalsIgnoreCase(ENVIRONMENT)){out.remove(key);continue;}
                JsonNode item=value.get(key);
                if(key.equals("bodyB64")){byte[] bytes;try{bytes=Base64.getDecoder().decode(item.asText());}catch(Exception e){throw new IllegalArgumentException("invalid attachment encoding");}
                    String data=new String(bytes,StandardCharsets.ISO_8859_1);for(String s:secrets)if(data.contains(s))throw new IllegalArgumentException("attachment contains private CI capability");}
                JsonNode filtered=clean(item,secrets,depth+1);out.set(key,filtered==null?MAPPER.nullNode():filtered);
            }return out;
        }
        if(value.isArray()){ArrayNode out=MAPPER.createArrayNode();for(JsonNode item:value){JsonNode filtered=clean(item,secrets,depth+1);if(filtered!=null)out.add(filtered);}return out;}
        if(value.isTextual()){String s=value.asText();for(String secret:secrets)s=s.replace(secret,"[REDACTED]");return TextNode.valueOf(s);}return value;
    }
    public static Map<String,String> sanitizeEnvironment(Map<String,String> env){
        if(env==null)return null;String raw=System.getenv(ENVIRONMENT);List<String> secrets=new ArrayList<>();
        for(Map.Entry<String,String> e:env.entrySet())if(e.getKey().equalsIgnoreCase(ENVIRONMENT)&&e.getValue()!=null&&!e.getValue().isEmpty()){secrets.add(e.getValue());if(raw==null||raw.isEmpty())raw=e.getValue();}
        if(raw!=null&&!raw.isEmpty()){secrets.add(raw);try{String token=MAPPER.readTree(Base64.getUrlDecoder().decode(raw)).path("token").asText();if(!token.isEmpty())secrets.add(token);}catch(Exception ignored){/* The opaque private value is already excluded. */}}
        Map<String,String> out=new HashMap<>();for(Map.Entry<String,String> e:env.entrySet()){if(e.getKey().equalsIgnoreCase(ENVIRONMENT))continue;String value=e.getValue();if(value!=null)for(String secret:secrets)value=value.replace(secret,"[REDACTED]");out.put(e.getKey(),value);}return out;
    }
    @Override public String toString(){return "CIJobContext([REDACTED])";}
}
