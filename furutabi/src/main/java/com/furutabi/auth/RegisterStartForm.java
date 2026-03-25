package com.furutabi.auth;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public class RegisterStartForm {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 100) private String nameKana;
    @NotNull @Past private LocalDate birthday;
    @NotBlank @Size(max = 20) private String gender;
    @NotBlank @Email @Size(max = 255) private String email;
    @NotBlank @Size(max = 30) private String phoneNumber;
    @NotBlank @Size(max = 255) private String address;
    @NotBlank @Size(max = 100) private String nickname;
    @NotBlank @Size(min = 8, max = 72) private String password;
    @NotBlank @Size(min = 8, max = 72) private String passwordConfirm;
    private boolean agreedToTerms;
    private boolean agreedToPrivacyPolicy;
    private boolean agreedToSmsNotice;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNameKana() { return nameKana; }
    public void setNameKana(String nameKana) { this.nameKana = nameKana; }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPasswordConfirm() { return passwordConfirm; }
    public void setPasswordConfirm(String passwordConfirm) { this.passwordConfirm = passwordConfirm; }
    public boolean isAgreedToTerms() { return agreedToTerms; }
    public void setAgreedToTerms(boolean agreedToTerms) { this.agreedToTerms = agreedToTerms; }
    public boolean isAgreedToPrivacyPolicy() { return agreedToPrivacyPolicy; }
    public void setAgreedToPrivacyPolicy(boolean agreedToPrivacyPolicy) { this.agreedToPrivacyPolicy = agreedToPrivacyPolicy; }
    public boolean isAgreedToSmsNotice() { return agreedToSmsNotice; }
    public void setAgreedToSmsNotice(boolean agreedToSmsNotice) { this.agreedToSmsNotice = agreedToSmsNotice; }
}
