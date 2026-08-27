package com.sastrack.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("SAS-TRACK");
        showLogin();
        stage.show();
    }

    public static void showLogin() throws IOException {
        Parent root = FXMLLoader.load(MainApp.class.getResource("/com/sastrack/app/login.fxml"));
        Scene scene = new Scene(root, 980, 650);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(760);
        primaryStage.setMinHeight(520);
    }

    public static void showMain() throws IOException {
        Parent root = FXMLLoader.load(MainApp.class.getResource("/com/sastrack/app/main.fxml"));
        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(MainApp.class.getResource("/com/sastrack/css/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
