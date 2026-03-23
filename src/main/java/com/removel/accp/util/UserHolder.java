package com.removel.accp.util;

import com.removel.accp.model.entity.User;

public class UserHolder {
    private static final ThreadLocal<User> userHolder = new ThreadLocal<User>();

    public static User getUser() {
        return userHolder.get();
    }

    public static void setUser(User user) {
        userHolder.set(user);
    }

    public static void removeUser() {
        userHolder.remove();
    }
}
