package com.sastrack.app;

/**
 * Do not extend Application here. Launching through a plain class avoids
 * the "JavaFX runtime components are missing" error when running from a
 * fat/shaded jar or via `./gradlew run`.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
