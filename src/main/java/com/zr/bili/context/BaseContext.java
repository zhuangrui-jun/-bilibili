package com.zr.bili.context;

public class BaseContext {

    private static ThreadLocal<Long> userIdThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> usernameThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        userIdThreadLocal.set(id);
    }

    public static Long getCurrentId() {
        return userIdThreadLocal.get();
    }

    public static void setCurrentUsername(String username) {
        usernameThreadLocal.set(username);
    }

    public static String getCurrentUsername() {
        return usernameThreadLocal.get();
    }

    public static void removeCurrentId() {
        userIdThreadLocal.remove();
    }

    public static void removeCurrentUsername() {
        usernameThreadLocal.remove();
    }

    /**
     * 清除当前线程的所有上下文信息
     */
    public static void removeAll() {
        userIdThreadLocal.remove();
        usernameThreadLocal.remove();
    }

}