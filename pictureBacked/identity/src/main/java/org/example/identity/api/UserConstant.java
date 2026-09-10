package org.example.identity.api;

/**
 * @author Zou
 * 用户常用常量
 */
public interface UserConstant {

    String SALT = "a6f2c8e9d5b4g3cfe1f0b8d4cdd6f9e7";

    String REDIS_VERITY_CODE_KEY = "verity_code";

    String USER_LOGIN_STATE = "user_login";

    String ADMIN_AUTH_ROLE = "admin";

    String USER_AUTH_ROLE = "user";

    String DEFAULT_PASSWORD = "123456";

    String USER_ACCOUNT_FAILED = "userAccount";

    String USER_PHONE_FAILED = "userPhone";

    String USER_EMAIL_FAILED = "userEmail";
}
