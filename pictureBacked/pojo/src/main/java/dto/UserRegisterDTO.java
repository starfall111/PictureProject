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
public class UserRegisterDTO implements Serializable {

    //注册类型 0为账号注册 1为手机号注册 2为邮箱注册
    private Integer type;

    //账号/手机号/邮箱
    private String account;

    //密码
    private String password;

    //确认密码
    private String checkPassword;

    //验证码
    private String verityCode;
}
