package org.example.common.context;

import entity.User;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Zou
 * 用户上下文信息，用于线程中的使用
 */
@Slf4j
public class UserContext {
    private final static ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static void set(User user){
        log.info("当前登录用户={}",user);
        USER_CONTEXT.set(user);
    }

    public User get(){
        return USER_CONTEXT.get();
    }

    public static void clear(){
        USER_CONTEXT.remove();
    }

}
