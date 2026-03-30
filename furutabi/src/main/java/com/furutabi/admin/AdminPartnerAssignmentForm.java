package com.furutabi.admin;

public class AdminPartnerAssignmentForm {

    private String regionId;
    private Long partnerUserId;
    private String effectiveFrom;
    private String effectiveTo;
    private String reason;

    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    public Long getPartnerUserId() {
        return partnerUserId;
    }

    public void setPartnerUserId(Long partnerUserId) {
        this.partnerUserId = partnerUserId;
    }

    public String getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(String effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public String getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(String effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
