package com.furutabi.app;

public class SupportRequestForm {

    private String requestType;
    private String relatedFeature;
    private String targetReference;
    private String body;
    private String replyPreference;

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRelatedFeature() {
        return relatedFeature;
    }

    public void setRelatedFeature(String relatedFeature) {
        this.relatedFeature = relatedFeature;
    }

    public String getTargetReference() {
        return targetReference;
    }

    public void setTargetReference(String targetReference) {
        this.targetReference = targetReference;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getReplyPreference() {
        return replyPreference;
    }

    public void setReplyPreference(String replyPreference) {
        this.replyPreference = replyPreference;
    }
}
