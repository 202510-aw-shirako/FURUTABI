package com.furutabi.app;

public class AppPrivacySettingsForm {

    private String profileVisibility = "PRIVATE";
    private String mapDefaultVisibility = "PRIVATE";
    private boolean receiveOperationNotice = true;
    private boolean receiveSecurityNotice = true;
    private boolean receiveBridgeContact = true;
    private boolean receiveLocalContact;
    private boolean receiveEmailNotice = true;
    private boolean receiveSmsNotice;

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public String getMapDefaultVisibility() {
        return mapDefaultVisibility;
    }

    public void setMapDefaultVisibility(String mapDefaultVisibility) {
        this.mapDefaultVisibility = mapDefaultVisibility;
    }

    public boolean isReceiveOperationNotice() {
        return receiveOperationNotice;
    }

    public void setReceiveOperationNotice(boolean receiveOperationNotice) {
        this.receiveOperationNotice = receiveOperationNotice;
    }

    public boolean isReceiveSecurityNotice() {
        return receiveSecurityNotice;
    }

    public void setReceiveSecurityNotice(boolean receiveSecurityNotice) {
        this.receiveSecurityNotice = receiveSecurityNotice;
    }

    public boolean isReceiveBridgeContact() {
        return receiveBridgeContact;
    }

    public void setReceiveBridgeContact(boolean receiveBridgeContact) {
        this.receiveBridgeContact = receiveBridgeContact;
    }

    public boolean isReceiveLocalContact() {
        return receiveLocalContact;
    }

    public void setReceiveLocalContact(boolean receiveLocalContact) {
        this.receiveLocalContact = receiveLocalContact;
    }

    public boolean isReceiveEmailNotice() {
        return receiveEmailNotice;
    }

    public void setReceiveEmailNotice(boolean receiveEmailNotice) {
        this.receiveEmailNotice = receiveEmailNotice;
    }

    public boolean isReceiveSmsNotice() {
        return receiveSmsNotice;
    }

    public void setReceiveSmsNotice(boolean receiveSmsNotice) {
        this.receiveSmsNotice = receiveSmsNotice;
    }
}
