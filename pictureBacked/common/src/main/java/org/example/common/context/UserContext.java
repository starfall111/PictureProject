package context;


import entity.User;

/**
 * @author Zou
 * 用户上下文信息，用于线程中的使用
 */
public class UserContext {
    private final static ThreadLocal<User> USER_CONTEXT = new ThreadLocal<>();

    public static void set(User user){
        USER_CONTEXT.set(user);
    }

    public User get(){
        return USER_CONTEXT.get();
    }

    public static void clear(){
        USER_CONTEXT.remove();
    }

}
