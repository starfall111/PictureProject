package org.example.pojo.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户换绑手机号/邮箱 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBindAccountDTO implements Serializable {

    // 1=手机号, 2=邮箱
    private Integer type;

    // 手机号或邮箱
    private String account;

    // 验证码
    private String verificationCode;
}
