package com.sastrack.controller;

import com.sastrack.app.MainApp;
import com.sastrack.app.Session;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class MainController {

    @FXML private VBox sidebar;
    @FXML private Label brandLabel;
    @FXML private Button collapseBtn;

    @FXML private Button navDashboard;
    @FXML private Button navLostFoundHead;
    @FXML private VBox lostFoundSub;
    @FXML private Button navViolationsHead;
    @FXML private VBox violationsSub;
    @FXML private Button logoutBtn;

    @FXML private ComboBox<String> searchScopeBox;
    @FXML private TextField searchField;
    @FXML private Label userLabel;

    @FXML private StackPane contentArea;

    private boolean collapsed = false;

    @FXML
    public void initialize() {
        searchScopeBox.getItems().addAll("Violations", "Lost & Found");
        searchScopeBox.getSelectionModel().selectFirst();

        if (Session.getCurrentUser() != null) {
            userLabel.setText(Session.getCurrentUser().getUsername() + "  ·  " + Session.getCurrentUser().getRole());
        }

        showDashboard();
    }

    /* ---------------- Sidebar collapse ---------------- */
    @FXML
    private void toggleSidebar() {
        collapsed = !collapsed;
        double targetWidth = collapsed ? 64 : 220;

        if (collapsed) {
            // Collapse first (subnavs disappear immediately), width animates after.
            lostFoundSub.setVisible(false);
            lostFoundSub.setManaged(false);
            violationsSub.setVisible(false);
            violationsSub.setManaged(false);
            collapseBtn.setText("⟩");
        } else {
            collapseBtn.setText("⟨");
        }

        FadeTransition fadeLabel = new FadeTransition(Duration.millis(140), brandLabel);
        if (collapsed) {
            fadeLabel.setToValue(0);
            fadeLabel.setOnFinished(e -> { brandLabel.setVisible(false); brandLabel.setManaged(false); });
        } else {
            brandLabel.setVisible(true);
            brandLabel.setManaged(true);
            brandLabel.setOpacity(0);
            fadeLabel.setToValue(1);
        }
        fadeLabel.play();

        Timeline widthAnim = new Timeline(
                new KeyFrame(Duration.millis(180),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                        new KeyValue(sidebar.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH))
        );
        widthAnim.setOnFinished(e -> {
            if (collapsed) {
                setButtonIconOnly(navDashboard, "▦");
                setButtonIconOnly(navLostFoundHead, "🎒");
                setButtonIconOnly(navViolationsHead, "⚖");
                setButtonIconOnly(logoutBtn, "⏻");
            } else {
                navDashboard.setText("▦  Dashboard");
                navLostFoundHead.setText("🎒  Lost & Found");
                navViolationsHead.setText("⚖  Guidance Violations");
                logoutBtn.setText("⏻  Logout");
            }
        });
        widthAnim.play();
    }

    private void setButtonIconOnly(Button b, String icon) {
        b.setText(icon);
    }

    @FXML
    private void toggleLostFoundGroup() {
        if (collapsed) return; // expanding is disabled while collapsed; expand sidebar first
        boolean show = !lostFoundSub.isVisible();
        lostFoundSub.setVisible(show);
        lostFoundSub.setManaged(show);
    }

    @FXML
    private void toggleViolationsGroup() {
        if (collapsed) return;
        boolean show = !violationsSub.isVisible();
        violationsSub.setVisible(show);
        violationsSub.setManaged(show);
    }

    /* ---------------- View switching ---------------- */
    public void showDashboard() {
        DashboardController c = (DashboardController) loadView("dashboard.fxml");
    }

    public void showViolations(String typeFilter) {
        ViolationsController c = (ViolationsController) loadView("violations.fxml");
        if (c != null) c.applyTypeFilter(typeFilter);
    }

    public void showViolationsWithStatus(String statusGroup) {
        ViolationsController c = (ViolationsController) loadView("violations.fxml");
        if (c != null) c.applyStatusGroupFilter(statusGroup);
    }

    public void showLostFound(String statusFilter) {
        LostFoundController c = (LostFoundController) loadView("lostfound.fxml");
        if (c != null) c.applyStatusFilter(statusFilter);
    }

    private Object loadView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/sastrack/app/" + fxml));
            Parent view = loader.load();

            view.setOpacity(0);
            view.setTranslateY(10);
            contentArea.getChildren().setAll(view);

            FadeTransition fade = new FadeTransition(Duration.millis(220), view);
            fade.setFromValue(0);
            fade.setToValue(1);
            Timeline slide = new Timeline(new KeyFrame(Duration.millis(220),
                    new KeyValue(view.translateYProperty(), 0, Interpolator.EASE_OUT)));
            fade.play();
            slide.play();

            Object controller = loader.getController();
            if (controller instanceof MainAware ma) {
                ma.setMainController(this);
            }
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /* ---------------- Sidebar nav handlers ---------------- */
    @FXML private void showLostFoundAll() { showLostFound("All"); }
    @FXML private void showLostFoundUnclaimed() { showLostFound("Unclaimed"); }
    @FXML private void showLostFoundClaimed() { showLostFound("Claimed"); }
    @FXML private void logFoundItem() {
        LostFoundController c = (LostFoundController) loadView("lostfound.fxml");
        if (c != null) c.openItemDialog(null);
    }

    @FXML private void showViolationsAll() { showViolations("All"); }
    @FXML private void showViolationsMinor() { showViolations("Minor Offense"); }
    @FXML private void showViolationsMajor() { showViolations("Major Offense"); }
    @FXML private void showViolationsSerious() { showViolations("Serious Offense"); }
    @FXML private void addViolation() {
        ViolationsController c = (ViolationsController) loadView("violations.fxml");
        if (c != null) c.openViolationDialog(null);
    }

    /* ---------------- Search ---------------- */
    @FXML
    private void handleSearch() {
        String scope = searchScopeBox.getValue();
        String query = searchField.getText();
        SearchController c = (SearchController) loadView("search.fxml");
        if (c != null) c.runSearch(scope, query);
    }

    /* ---------------- Logout ---------------- */
    @FXML
    private void handleLogout() {
        Session.logout();
        try {
            MainApp.showLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Implemented by sub-view controllers that need to navigate elsewhere. */
    public interface MainAware {
        void setMainController(MainController mainController);
    }
}
