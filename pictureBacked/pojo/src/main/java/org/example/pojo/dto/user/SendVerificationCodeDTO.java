package org.example.pojo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationCodeDTO implements Serializable {
    private Integer type;
    private String account;
    private String captchaVerifyParam;
}
