package com.furutabi.auth;

public class RegisterProfileForm {
    private String bio;
    private String region;
    private String interestRegion;
    private String visitHistory;
    private String careNote;
    private String withChildren;
    private String foodNote;
    private String relationNote;
    private boolean receiveOperationNotice = true;
    private boolean receiveSecurityNotice = true;
    private boolean receiveBridgeContact = true;
    private boolean receiveLocalContact;
    private boolean receiveEmailNotice = true;
    private boolean receiveSmsNotice;

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getInterestRegion() { return interestRegion; }
    public void setInterestRegion(String interestRegion) { this.interestRegion = interestRegion; }
    public String getVisitHistory() { return visitHistory; }
    public void setVisitHistory(String visitHistory) { this.visitHistory = visitHistory; }
    public String getCareNote() { return careNote; }
    public void setCareNote(String careNote) { this.careNote = careNote; }
    public String getWithChildren() { return withChildren; }
    public void setWithChildren(String withChildren) { this.withChildren = withChildren; }
    public String getFoodNote() { return foodNote; }
    public void setFoodNote(String foodNote) { this.foodNote = foodNote; }
    public String getRelationNote() { return relationNote; }
    public void setRelationNote(String relationNote) { this.relationNote = relationNote; }
    public boolean isReceiveOperationNotice() { return receiveOperationNotice; }
    public void setReceiveOperationNotice(boolean receiveOperationNotice) { this.receiveOperationNotice = receiveOperationNotice; }
    public boolean isReceiveSecurityNotice() { return receiveSecurityNotice; }
    public void setReceiveSecurityNotice(boolean receiveSecurityNotice) { this.receiveSecurityNotice = receiveSecurityNotice; }
    public boolean isReceiveBridgeContact() { return receiveBridgeContact; }
    public void setReceiveBridgeContact(boolean receiveBridgeContact) { this.receiveBridgeContact = receiveBridgeContact; }
    public boolean isReceiveLocalContact() { return receiveLocalContact; }
    public void setReceiveLocalContact(boolean receiveLocalContact) { this.receiveLocalContact = receiveLocalContact; }
    public boolean isReceiveEmailNotice() { return receiveEmailNotice; }
    public void setReceiveEmailNotice(boolean receiveEmailNotice) { this.receiveEmailNotice = receiveEmailNotice; }
    public boolean isReceiveSmsNotice() { return receiveSmsNotice; }
    public void setReceiveSmsNotice(boolean receiveSmsNotice) { this.receiveSmsNotice = receiveSmsNotice; }
}
