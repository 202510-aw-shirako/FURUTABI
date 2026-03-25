package com.furutabi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterSmsForm {
    @NotBlank(message = "認証コードを入力してください。")
    @Pattern(regexp = "\\d{6}", message = "認証コードは6桁の数字で入力してください。")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
