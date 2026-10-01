package ru.mockarty.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Private selected result capability. Never place in logs or report metadata. */
public final class CICallback {
    private final String token, launchId, itemUid, groupId, attemptId, invocationId;
    public static final String ZERO = "00000000-0000-0000-0000-000000000000";
    @JsonCreator public CICallback(@JsonProperty("token") String token,
        @JsonProperty("launchId") String launchId, @JsonProperty("itemUid") String itemUid,
        @JsonProperty("groupId") String groupId, @JsonProperty("attemptId") String attemptId,
        @JsonProperty("invocationId") String invocationId) {
        this.token=token;this.launchId=launchId;this.itemUid=itemUid;
        this.groupId=groupId;this.attemptId=attemptId;this.invocationId=invocationId;
    }
    public String getToken(){return token;} public String getLaunchId(){return launchId;}
    public String getItemUid(){return itemUid;} public String getGroupId(){return groupId;}
    public String getAttemptId(){return attemptId;} public String getInvocationId(){return invocationId;}
    public static boolean validUUID(String value) {
        try {return value!=null&&!value.equals(ZERO)&&UUID.fromString(value).toString().equals(value);}
        catch(IllegalArgumentException e){return false;}
    }
    public void validate(boolean completion) {
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}")||!validUUID(launchId)||!validUUID(groupId)||!validUUID(attemptId)||!validUUID(invocationId)) throw invalid();
        try {byte[] bytes=Base64.getUrlDecoder().decode(token);
            if(bytes.length!=32||!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(token)) throw invalid();}
        catch(IllegalArgumentException e){throw invalid();}
        if(completion ? itemUid!=null&&!itemUid.isEmpty()&&!itemUid.equals(ZERO) : !validUUID(itemUid)) throw invalid();
    }
    public Map<String,String> toWire(boolean completion) {
        validate(completion);Map<String,String> out=new LinkedHashMap<>();
        out.put("token",token);out.put("launchId",launchId);out.put("itemUid",completion?ZERO:itemUid);
        out.put("groupId",groupId);out.put("attemptId",attemptId);out.put("invocationId",invocationId);return out;
    }
    static IllegalArgumentException invalid(){return new IllegalArgumentException("invalid or unmatched selected CI context");}
    @Override public String toString(){return "CICallback([REDACTED])";}
}
