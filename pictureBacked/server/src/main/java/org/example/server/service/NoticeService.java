package org.example.server.service;

import java.util.concurrent.ExecutionException;

/**
 * @author Zou
 */

public interface NoticeService {

    void sendCode(Integer type,String account,String captchaVerifyParam) throws ExecutionException, InterruptedException;

    void checkPhoneOrEmail(Integer type, String account);

    //校验验证码
    void verityCode(String account, String code);
}
