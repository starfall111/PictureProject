package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author Zou
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserLoginDTO implements Serializable {

    // 0为账号登录 1为手机号登录 2为邮箱登录
    private Integer type;

    private String account;

    //密码
    private String password;

    //验证码
    private String verityCode;

    //验证码 or 密码 1为验证码 0为密码
    private Integer isVerityCode = 0;
}
