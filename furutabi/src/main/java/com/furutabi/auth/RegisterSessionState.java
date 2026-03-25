package com.furutabi.auth;

import java.io.Serializable;

public class RegisterSessionState implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String email;
    private final String phoneNumber;
    private boolean smsVerified;
    private boolean profileCompleted;
    private boolean profileSkipped;
    private String latestSmsCode;

    public RegisterSessionState(long userId, String email, String phoneNumber, String latestSmsCode) {
        this.userId = userId;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.latestSmsCode = latestSmsCode;
    }

    public long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isSmsVerified() { return smsVerified; }
    public void setSmsVerified(boolean smsVerified) { this.smsVerified = smsVerified; }
    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }
    public boolean isProfileSkipped() { return profileSkipped; }
    public void setProfileSkipped(boolean profileSkipped) { this.profileSkipped = profileSkipped; }
    public String getLatestSmsCode() { return latestSmsCode; }
    public void setLatestSmsCode(String latestSmsCode) { this.latestSmsCode = latestSmsCode; }
}
