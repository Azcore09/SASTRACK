package com.sastrack.controller;

import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.Violation;
import com.sastrack.model.ViolationCatalog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;

public class ViolationsController implements MainController.MainAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sectionFilterBox;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private ComboBox<String> schoolYearFilterBox;
    @FXML private TableView<Violation> table;
    @FXML private TableColumn<Violation, String> colName;
    @FXML private TableColumn<Violation, String> colSection;
    @FXML private TableColumn<Violation, String> colType;
    @FXML private TableColumn<Violation, String> colOffense;
    @FXML private TableColumn<Violation, String> colActionTaken;
    @FXML private TableColumn<Violation, Number> colSuspensionDays;
    @FXML private TableColumn<Violation, String> colSanction;
    @FXML private TableColumn<Violation, LocalDate> colDate;
    @FXML private TableColumn<Violation, LocalDate> colCompletedDate;
    @FXML private TableColumn<Violation, String> colStatus;
    @FXML private TableColumn<Violation, String> colYear;
    @FXML private TableColumn<Violation, String> colBy;
    @FXML private Button editButton;
    @FXML private Button deleteButton;

    private MainController mainController;
    private String statusGroupFilter = null; // "Active" / "Resolved" / null

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colSection.setCellValueFactory(new PropertyValueFactory<>("section"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colOffense.setCellValueFactory(new PropertyValueFactory<>("offense"));
        colActionTaken.setCellValueFactory(new PropertyValueFactory<>("actionTaken"));
        colSuspensionDays.setCellValueFactory(new PropertyValueFactory<>("suspensionDays"));
        colSanction.setCellValueFactory(new PropertyValueFactory<>("sanction"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colCompletedDate.setCellValueFactory(new PropertyValueFactory<>("completedDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
        colBy.setCellValueFactory(new PropertyValueFactory<>("reportedBy"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            editButton.setDisable(!hasSelection);
            deleteButton.setDisable(!hasSelection);
        });
        editButton.setDisable(true);
        deleteButton.setDisable(true);

        sectionFilterBox.setItems(FXCollections.observableArrayList("N/A"));
        sectionFilterBox.getItems().addAll(ViolationCatalog.sections());
        sectionFilterBox.getSelectionModel().selectFirst();

        statusFilterBox.setItems(FXCollections.observableArrayList("All statuses", "Pending", "Under Review", "Resolved"));
        statusFilterBox.getSelectionModel().selectFirst();

        TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
        DataStore.VIOLATIONS.forEach(v -> years.add(v.getSchoolYear()));
        schoolYearFilterBox.setItems(FXCollections.observableArrayList(years));
        schoolYearFilterBox.getItems().add(0, "All school years");
        schoolYearFilterBox.getSelectionModel().selectFirst();

        refresh();
    }

    public void applySectionFilter(String section) {
        sectionFilterBox.getSelectionModel().select(section == null || section.equals("All")
                ? "N/A" : section);
        refresh();
    }

    public void applyStatusGroupFilter(String group) {
        this.statusGroupFilter = group;
        refresh();
    }

    @FXML
    public void refresh() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String section = sectionFilterBox.getValue();
        String status = statusFilterBox.getValue();
        String year = schoolYearFilterBox.getValue();

        var filtered = DataStore.VIOLATIONS.filtered(v ->
                (q.isEmpty() || v.getStudentName().toLowerCase().contains(q) || v.getStudentId().toLowerCase().contains(q)) &&
                ("N/A".equals(section) || v.getSection().equals(section)) &&
                ("All statuses".equals(status) || v.getStatus().equals(status)) &&
                (year == null || "All school years".equals(year) || v.getSchoolYear().equals(year)) &&
                (statusGroupFilter == null
                        || ("Active".equals(statusGroupFilter) && !"Resolved".equals(v.getStatus()))
                        || ("Resolved".equals(statusGroupFilter) && "Resolved".equals(v.getStatus())))
        );
        table.setItems(filtered);
    }

    @FXML
    private void handleEditSelected() {
        Violation selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openViolationDialog(selected);
        }
    }

    @FXML
    private void handleDeleteSelected() {
        Violation selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (Session.getCurrentUser() == null || !Session.getCurrentUser().isAdmin()) {
            new Alert(Alert.AlertType.WARNING, "Only an SAS Head account can delete records.").showAndWait();
            return;
        }
        DataStore.VIOLATIONS.remove(selected);
        refresh();
    }

    @FXML
    private void handleUnselect() {
        table.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleAdd() {
        openViolationDialog(null);
    }

    public void openViolationDialog(Violation existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add violation record" : "Edit violation record");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nameField = new TextField(existing == null ? "" : existing.getStudentName());
        TextField sidField = new TextField(existing == null ? "" : existing.getStudentId());
        ComboBox<String> sectionBox = new ComboBox<>(FXCollections.observableArrayList("N/A"));
        sectionBox.getItems().addAll(ViolationCatalog.sections());
        sectionBox.setMaxWidth(Double.MAX_VALUE);
        if (existing != null && ViolationCatalog.sections().contains(existing.getSection())) {
            sectionBox.setValue(existing.getSection());
        } else {
            sectionBox.getSelectionModel().selectFirst();
        }
        ComboBox<String> violationBox = new ComboBox<>();
        violationBox.setMaxWidth(Double.MAX_VALUE);
        violationBox.setItems(FXCollections.observableArrayList(ViolationCatalog.allViolations()));
        if (existing != null && ViolationCatalog.allViolations().contains(existing.getType())) {
            violationBox.setValue(existing.getType());
        }
        final boolean[] updatingSelection = {false};
        Runnable updateViolations = () -> {
            String selectedSection = sectionBox.getValue();
            boolean sectionSelected = selectedSection != null && !"N/A".equals(selectedSection);
            violationBox.setItems(FXCollections.observableArrayList(
                    sectionSelected ? ViolationCatalog.violationsFor(selectedSection)
                            : ViolationCatalog.allViolations()));
            if (existing != null && selectedSection != null && (selectedSection.equals(existing.getSection())
                    || (!sectionSelected && ViolationCatalog.allViolations().contains(existing.getType())))
                    && ViolationCatalog.allViolations().contains(existing.getType())) {
                violationBox.setValue(existing.getType());
            } else if (violationBox.getValue() == null
                    || !violationBox.getItems().contains(violationBox.getValue())) {
                violationBox.getSelectionModel().clearSelection();
            }
        };
        sectionBox.setOnAction(e -> updateViolations.run());
        violationBox.setOnAction(e -> {
            if (updatingSelection[0] || violationBox.getValue() == null) return;
            String selectedSection = sectionBox.getValue();
            if ("N/A".equals(selectedSection)) {
                String matchingSection = ViolationCatalog.sectionFor(violationBox.getValue());
                if (matchingSection != null) {
                    updatingSelection[0] = true;
                    sectionBox.setValue(matchingSection);
                    updatingSelection[0] = false;
                }
            }
        });
        updateViolations.run();

        DatePicker datePicker = new DatePicker(existing == null ? LocalDate.now() : existing.getDate());
        DatePicker completedDatePicker = new DatePicker(existing == null ? null : existing.getCompletedDate());
        ComboBox<String> offenseBox = new ComboBox<>(FXCollections.observableArrayList(
                "N/A", "1st Offense", "2nd Offense", "3rd Offense", "4th Offense", "5th Offense"));
        offenseBox.setEditable(false);
        offenseBox.setValue(existing == null ? "N/A" : existing.getOffense());
        ComboBox<String> actionBox = new ComboBox<>(FXCollections.observableArrayList(
                "N/A", "Official Warning", "Dismissal"));
        actionBox.setValue(existing == null ? "N/A" : existing.getActionTaken());
        TextField suspensionField = new TextField(existing == null || existing.getSuspensionDays() == 0
                ? "" : String.valueOf(existing.getSuspensionDays()));
        suspensionField.setPromptText("Optional non-negative number of days");
        ComboBox<String> sanctionBox = new ComboBox<>(FXCollections.observableArrayList(
                "N/A", "NI in Report Card", "Confiscation of the Item", "Disciplinary Probation",
                "Automatic zero equivalent in the tests/outputs", "Restitution"));
        sanctionBox.setValue(existing == null ? "N/A" : existing.getSanction());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Pending", "Under Review", "Resolved"));
        statusBox.getSelectionModel().select(existing == null ? "Pending" : existing.getStatus());
        TextField schoolYearField = new TextField(existing == null ? currentSchoolYear() : existing.getSchoolYear());
        TextArea descArea = new TextArea(existing == null ? "" : existing.getDescription());
        descArea.setPrefRowCount(3);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(14));
        int r = 0;
        grid.addRow(r++, new Label("Student name"), nameField);
        grid.addRow(r++, new Label("Student ID"), sidField);
        grid.addRow(r++, new Label("Section"), sectionBox);
        grid.addRow(r++, new Label("Violation"), violationBox);
        grid.addRow(r++, new Label("Offense"), offenseBox);
        grid.addRow(r++, new Label("Action taken"), actionBox);
        grid.addRow(r++, new Label("Days of suspension"), suspensionField);
        grid.addRow(r++, new Label("Sanction"), sanctionBox);
        grid.addRow(r++, new Label("Date reported"), datePicker);
        grid.addRow(r++, new Label("Date completed"), completedDatePicker);
        grid.addRow(r++, new Label("Status"), statusBox);
        grid.addRow(r++, new Label("School year"), schoolYearField);
        grid.addRow(r++, new Label("Description"), descArea);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (nameField.getText().trim().isEmpty() || datePicker.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Student name and date reported are required.").showAndWait();
                return;
            }
            String selectedSection = sectionBox.getValue();
            String selectedViolation = violationBox.getValue();
            if (selectedSection == null || "N/A".equals(selectedSection) || selectedViolation == null) {
                new Alert(Alert.AlertType.WARNING, "Select a section and violation.").showAndWait();
                return;
            }
            int suspensionDays;
            try {
                String rawDays = suspensionField.getText().trim();
                suspensionDays = rawDays.isEmpty() ? 0 : Integer.parseInt(rawDays);
                if (suspensionDays < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Days of suspension must be a non-negative whole number.").showAndWait();
                return;
            }
            if (existing == null) {
                DataStore.VIOLATIONS.add(new Violation(
                        DataStore.nextViolationId(), nameField.getText().trim(), sidField.getText().trim(),
                        selectedSection, selectedViolation, offenseBox.getValue(), actionBox.getValue(),
                        suspensionDays, sanctionBox.getValue(), datePicker.getValue(), completedDatePicker.getValue(),
                        statusBox.getValue(), schoolYearField.getText().trim(),
                        Session.getCurrentUser() == null ? "" : Session.getCurrentUser().getUsername(),
                        descArea.getText().trim()));
            } else {
                existing.setSection(selectedSection);
                existing.setType(selectedViolation);
                existing.setOffense(offenseBox.getValue());
                existing.setActionTaken(actionBox.getValue());
                existing.setSuspensionDays(suspensionDays);
                existing.setSanction(sanctionBox.getValue());
                existing.setCompletedDate(completedDatePicker.getValue());
                existing.setStatus(statusBox.getValue());
                existing.setDate(datePicker.getValue());
            }
            refresh();
        }
    }

    private String currentSchoolYear() {
        int y = LocalDate.now().getYear();
        return y + "-" + (y + 1);
    }
}
