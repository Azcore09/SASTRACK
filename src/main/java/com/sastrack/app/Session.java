package com.sastrack.app;

import com.sastrack.model.User;

public final class Session {
    private static User currentUser;

    public static void login(User user) { currentUser = user; }
    public static User getCurrentUser() { return currentUser; }
    public static void logout() { currentUser = null; }

    private Session() {}
}
