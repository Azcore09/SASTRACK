package com.sastrack.controller;

import com.sastrack.app.MainApp;
import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Optional;

public class LoginController {

    @FXML private VBox loginCard;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void initialize() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(16);
        FadeTransition fade = new FadeTransition(Duration.millis(400), loginCard);
        fade.setToValue(1);
        Timeline slide = new Timeline(new KeyFrame(Duration.millis(400),
                new KeyValue(loginCard.translateYProperty(), 0, Interpolator.EASE_OUT)));
        fade.play();
        slide.play();
    }

    private void shakeOnError() {
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loginCard.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(loginCard.translateXProperty(), -8)),
                new KeyFrame(Duration.millis(120), new KeyValue(loginCard.translateXProperty(), 8)),
                new KeyFrame(Duration.millis(180), new KeyValue(loginCard.translateXProperty(), -6)),
                new KeyFrame(Duration.millis(240), new KeyValue(loginCard.translateXProperty(), 6)),
                new KeyFrame(Duration.millis(300), new KeyValue(loginCard.translateXProperty(), 0))
        );
        shake.play();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        Optional<User> match = DataStore.USERS.stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username) && u.getPassword().equals(password))
                .findFirst();

        if (match.isEmpty()) {
            showError("Invalid username or password.");
            return;
        }

        // Both SAS Head (admin) and SAS Staff can sign in. The role is
        // carried in the session so the rest of the app can check
        // Session.getCurrentUser().isAdmin() to gate admin-only actions
        // like deleting records or changing case status.
        User user = match.get();
        Session.login(user);
        try {
            MainApp.showMain();
        } catch (IOException e) {
            showError("Could not load the dashboard: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        shakeOnError();
    }
}
