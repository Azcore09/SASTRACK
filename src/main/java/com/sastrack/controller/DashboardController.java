package com.sastrack.controller;

import com.sastrack.data.DataStore;
import com.sastrack.model.LostItem;
import com.sastrack.model.Violation;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

public class DashboardController implements MainController.MainAware {

    @FXML private ComboBox<String> schoolYearCombo;
    @FXML private Button newSchoolYearBtn;
    @FXML private Button deleteSchoolYearBtn;
    @FXML private Label statTotal;
    @FXML private Label statItems;
    @FXML private Label statActive;
    @FXML private Label statResolved;
    @FXML private ToggleButton viewMonthToggle;
    @FXML private ToggleButton viewYearToggle;
    @FXML private BarChart<String, Number> offenseChart;
    @FXML private CategoryAxis xAxis;

    @FXML private VBox cardTotal;
    @FXML private VBox cardItems;
    @FXML private VBox cardActive;
    @FXML private VBox cardResolved;

    private MainController mainController;
    private boolean byMonth = true;

    private static final String[] TYPES = {"Minor Offense", "Major Offense", "Serious Offense"};

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        refreshSchoolYearOptions();

        viewMonthToggle.setSelected(true);
        viewMonthToggle.setOnAction(e -> { byMonth = true; viewYearToggle.setSelected(false); viewMonthToggle.setSelected(true); refresh(); });
        viewYearToggle.setOnAction(e -> { byMonth = false; viewMonthToggle.setSelected(false); viewYearToggle.setSelected(true); refresh(); });

        offenseChart.setCategoryGap(20);
        offenseChart.setBarGap(8);
        offenseChart.setLegendVisible(true);
        offenseChart.setAnimated(false);
        offenseChart.setMinHeight(360);
        offenseChart.setPrefHeight(380);
        offenseChart.setMaxWidth(Double.MAX_VALUE);
        xAxis.setTickLabelGap(8);

        attachDashboardCard(cardTotal, "Open all violations and lost & found records.");
        attachDashboardCard(cardItems, "View lost and found item entries for the selected school year.");
        attachDashboardCard(cardActive, "Review pending and under-review violation cases.");
        attachDashboardCard(cardResolved, "Check closed and resolved violation records.");

        refresh();
    }

    private void refreshSchoolYearOptions() {
        TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
        DataStore.VIOLATIONS.forEach(v -> years.add(v.getSchoolYear()));
        DataStore.ITEMS.forEach(i -> years.add(i.getSchoolYear()));

        schoolYearCombo.getItems().setAll(years);
        if (schoolYearCombo.getItems().isEmpty()) {
            schoolYearCombo.setValue(null);
            schoolYearCombo.setDisable(true);
            deleteSchoolYearBtn.setDisable(true);
            return;
        }

        schoolYearCombo.setDisable(false);
        deleteSchoolYearBtn.setDisable(false);
        if (schoolYearCombo.getValue() == null || !schoolYearCombo.getItems().contains(schoolYearCombo.getValue())) {
            schoolYearCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void addSchoolYear() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add School Year");
        dialog.setHeaderText("Create a new school year");
        dialog.setContentText("School year:");
        dialog.getEditor().setPromptText("2026-2027");
        dialog.getEditor().setText("");

        dialog.showAndWait().ifPresent(input -> {
            String year = input == null ? "" : input.trim();
            if (year.isEmpty()) {
                return;
            }
            if (!year.matches("\\d{4}-\\d{4}")) {
                new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Please enter a school year in the format YYYY-YYYY.").showAndWait();
                return;
            }
            if (schoolYearCombo.getItems().contains(year)) {
                schoolYearCombo.getSelectionModel().select(year);
                refresh();
                return;
            }

            List<String> items = new java.util.ArrayList<>(schoolYearCombo.getItems());
            items.add(year);
            items.sort(Comparator.reverseOrder());
            schoolYearCombo.getItems().setAll(items);
            schoolYearCombo.getSelectionModel().select(year);
            refresh();
        });
    }

    @FXML
    private void deleteSchoolYear() {
        String selected = schoolYearCombo.getValue();
        if (selected == null || selected.isBlank()) {
            return;
        }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete School Year");
        confirm.setHeaderText("Delete school year " + selected + "?");
        confirm.setContentText("This will remove all matching records from the dashboard data for this school year.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                DataStore.VIOLATIONS.removeIf(v -> selected.equals(v.getSchoolYear()));
                DataStore.ITEMS.removeIf(i -> selected.equals(i.getSchoolYear()));
                refreshSchoolYearOptions();
                refresh();
            }
        });
    }

    /** Small scale-up animation so stat cards feel clickable on hover. */
    private void attachDashboardCard(Node card, String description) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(120), card);
        grow.setToX(1.03);
        grow.setToY(1.03);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(120), card);
        shrink.setToX(1);
        shrink.setToY(1);

        Tooltip tooltip = new Tooltip(description);
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(220);
        tooltip.setShowDelay(Duration.millis(120));
        Tooltip.install(card, tooltip);

        card.setOnMouseEntered(e -> { shrink.stop(); grow.playFromStart(); });
        card.setOnMouseExited(e -> { grow.stop(); shrink.playFromStart(); });
    }

    @FXML
    public void refresh() {
        String year = schoolYearCombo.getValue();
        if (year == null) return;

        long itemCount = DataStore.ITEMS.stream().filter(i -> i.getSchoolYear().equals(year)).count();
        long violationCount = DataStore.VIOLATIONS.stream().filter(v -> v.getSchoolYear().equals(year)).count();
        long active = DataStore.VIOLATIONS.stream().filter(v -> v.getSchoolYear().equals(year) && !"Resolved".equals(v.getStatus())).count();
        long resolved = DataStore.VIOLATIONS.stream().filter(v -> v.getSchoolYear().equals(year) && "Resolved".equals(v.getStatus())).count();

        statTotal.setText(String.valueOf(itemCount + violationCount));
        statItems.setText(String.valueOf(itemCount));
        statActive.setText(String.valueOf(active));
        statResolved.setText(String.valueOf(resolved));

        buildChart(year);
    }

    private void buildChart(String year) {
        offenseChart.getData().clear();
        xAxis.setLabel(byMonth ? "Month (" + year + ")" : "School year");

        Map<String, XYChart.Series<String, Number>> seriesByType = new LinkedHashMap<>();
        for (String type : TYPES) {
            XYChart.Series<String, Number> s = new XYChart.Series<>();
            s.setName(type.replace(" Offense", ""));
            seriesByType.put(type, s);
        }

        if (byMonth) {
            String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
            for (String m : months) {
                counts.put(m, new LinkedHashMap<>());
                for (String type : TYPES) {
                    counts.get(m).put(type, 0);
                }
            }
            for (Violation v : DataStore.VIOLATIONS) {
                if (!v.getSchoolYear().equals(year)) continue;
                String monthKey = v.getDate().getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                String monthLabel = monthKey.substring(0, 3);
                if (!counts.containsKey(monthLabel)) {
                    counts.put(monthLabel, new LinkedHashMap<>());
                    for (String type : TYPES) {
                        counts.get(monthLabel).put(type, 0);
                    }
                }
                counts.get(monthLabel).merge(v.getType(), 1, Integer::sum);
            }
            for (String m : months) {
                for (String type : TYPES) {
                    int c = counts.getOrDefault(m, new LinkedHashMap<>()).getOrDefault(type, 0);
                    seriesByType.get(type).getData().add(new XYChart.Data<>(m, c));
                }
            }
        } else {
            TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
            DataStore.VIOLATIONS.forEach(v -> years.add(v.getSchoolYear()));
            Map<String, Map<String, Integer>> counts = new LinkedHashMap<>();
            for (String y : years) {
                counts.put(y, new LinkedHashMap<>());
                for (String type : TYPES) {
                    counts.get(y).put(type, 0);
                }
            }
            for (Violation v : DataStore.VIOLATIONS) {
                counts.computeIfAbsent(v.getSchoolYear(), ignored -> new LinkedHashMap<>());
                counts.get(v.getSchoolYear()).merge(v.getType(), 1, Integer::sum);
            }
            for (String y : years) {
                for (String type : TYPES) {
                    int c = counts.getOrDefault(y, new LinkedHashMap<>()).getOrDefault(type, 0);
                    seriesByType.get(type).getData().add(new XYChart.Data<>(y, c));
                }
            }
        }

        offenseChart.getData().addAll(seriesByType.values());
    }

    @FXML private void goToAllViolations() { if (mainController != null) mainController.showViolations("All"); }
    @FXML private void goToAllItems() { if (mainController != null) mainController.showLostFound("All"); }
    @FXML private void goToActiveViolations() { if (mainController != null) mainController.showViolationsWithStatus("Active"); }
    @FXML private void goToResolvedViolations() { if (mainController != null) mainController.showViolationsWithStatus("Resolved"); }
}
