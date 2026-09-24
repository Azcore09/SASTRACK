package com.sastrack.controller;

import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.Violation;
import com.sastrack.model.ViolationCatalog;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

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
     @FXML private Button viewButton;
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
             viewButton.setDisable(!hasSelection);
             editButton.setDisable(!hasSelection);
             deleteButton.setDisable(!hasSelection);
         });
         viewButton.setDisable(true);
         editButton.setDisable(true);
         deleteButton.setDisable(true);

        sectionFilterBox.setItems(FXCollections.observableArrayList("N/A"));
        sectionFilterBox.getItems().addAll(ViolationCatalog.sections());
        sectionFilterBox.getSelectionModel().selectFirst();

        statusFilterBox.setItems(FXCollections.observableArrayList("All statuses", "Pending", "Under Review", "Resolved"));
        statusFilterBox.getSelectionModel().selectFirst();

        TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
        DataStore.SCHOOL_YEARS.forEach(year -> {
            if (year != null && !year.trim().isEmpty()) {
                years.add(year);
            }
        });
        years.add(currentSchoolYear());
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Violation");
        confirm.setHeaderText("Delete this violation record?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                DataStore.VIOLATIONS.remove(selected);
                refresh();
            }
        });
    }

     @FXML
     private void handleUnselect() {
         table.getSelectionModel().clearSelection();
     }

     @FXML
     private void handleView() {
         Violation selected = table.getSelectionModel().getSelectedItem();
         if (selected != null) {
             viewViolationDialog(selected);
         }
     }

     @FXML
     private void handleAdd() {
         openViolationDialog(null);
     }

     public void viewViolationDialog(Violation violation) {
         Dialog<ButtonType> dialog = new Dialog<>();
         dialog.setTitle("View violation record");
         dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
         dialog.getDialogPane().setPrefWidth(720);
         dialog.getDialogPane().setPrefHeight(600);

         // Apply theme styling to dialog
         dialog.getDialogPane().setStyle(
             "-fx-background-color: #f5f6fa;" +
             "-fx-padding: 0;"
         );

         // Create header panel
         javafx.scene.layout.VBox headerBox = new javafx.scene.layout.VBox();
         headerBox.setStyle(
             "-fx-background-color: linear-gradient(to right, #2f6fed, #1b4fb7);" +
             "-fx-padding: 20 24;" +
             "-fx-spacing: 4;"
         );
         Label headerTitle = new Label("Violation Details");
         headerTitle.setStyle(
             "-fx-font-size: 18px;" +
             "-fx-font-weight: bold;" +
             "-fx-text-fill: white;"
         );
         Label headerSubtitle = new Label(violation.getStudentName() + " • " + violation.getSection());
         headerSubtitle.setStyle(
             "-fx-font-size: 12px;" +
             "-fx-text-fill: rgba(255, 255, 255, 0.85);"
         );
         headerBox.getChildren().addAll(headerTitle, headerSubtitle);

         // Create content scrollpane
         javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox();
         contentBox.setStyle(
             "-fx-background-color: white;" +
             "-fx-padding: 24;" +
             "-fx-spacing: 0;"
         );

         // Add fields grouped by section
         addViewSection(contentBox, "Student Information", new String[][] {
             {"Student Name:", violation.getStudentName()},
             {"Student ID:", violation.getStudentId()},
             {"Section:", violation.getSection()}
         });

         addViewSection(contentBox, "Violation Details", new String[][] {
             {"Violation Type:", violation.getType()},
             {"Offense:", violation.getOffense()},
             {"Action Taken:", violation.getActionTaken()}
         });

         addViewSection(contentBox, "Sanctions & Consequences", new String[][] {
             {"Days of Suspension:", String.valueOf(violation.getSuspensionDays())},
             {"Sanction(s):", violation.getSanction()}
         });

         addViewSection(contentBox, "Dates & Status", new String[][] {
             {"Date Reported:", violation.getDate() == null ? "N/A" : violation.getDate().toString()},
             {"Conference Date:", violation.getConferenceDate() == null ? "N/A" : violation.getConferenceDate().toString()},
             {"Date Completed:", violation.getCompletedDate() == null ? "N/A" : violation.getCompletedDate().toString()},
             {"Status:", violation.getStatus()},
             {"School Year:", violation.getSchoolYear()}
         });

         addViewSection(contentBox, "Additional Information", new String[][] {
             {"Reported By:", violation.getReportedBy()}
         });

         if (violation.getRemarks() != null && !violation.getRemarks().isEmpty()) {
             javafx.scene.layout.VBox descBox = new javafx.scene.layout.VBox();
             descBox.setStyle("-fx-spacing: 6; -fx-padding: 16 0 0 0;");
             Label descLabelHeader = new Label("Remarks");
             descLabelHeader.setStyle(
                 "-fx-font-weight: bold;" +
                 "-fx-text-fill: #0e1b3d;" +
                 "-fx-font-size: 12px;"
             );
             Label descContent = new Label(violation.getRemarks());
             descContent.setStyle(
                 "-fx-text-fill: #475a73;" +
                 "-fx-font-size: 11px;" +
                 "-fx-wrap-text: true;"
             );
             descBox.getChildren().addAll(descLabelHeader, descContent);
             contentBox.getChildren().add(descBox);
         }

         ScrollPane scrollPane = new ScrollPane(contentBox);
         scrollPane.setStyle("-fx-background-color: white; -fx-padding: 0;");
         scrollPane.setFitToWidth(true);

         javafx.scene.layout.VBox mainBox = new javafx.scene.layout.VBox();
         mainBox.setStyle("-fx-spacing: 0; -fx-padding: 0;");
         mainBox.getChildren().addAll(headerBox, scrollPane);
         javafx.scene.layout.VBox.setVgrow(scrollPane, javafx.scene.layout.Priority.ALWAYS);

         dialog.getDialogPane().setContent(mainBox);
         dialog.showAndWait();
     }

     private void addViewSection(javafx.scene.layout.VBox parent, String sectionTitle, String[][] fields) {
         javafx.scene.layout.VBox sectionBox = new javafx.scene.layout.VBox();
         sectionBox.setStyle(
             "-fx-padding: 16 0 0 0;" +
             "-fx-spacing: 12;"
         );

         Label sectionLabel = new Label(sectionTitle);
         sectionLabel.setStyle(
             "-fx-font-size: 12px;" +
             "-fx-font-weight: bold;" +
             "-fx-text-fill: #2f6fed;" +
             "-fx-padding: 0 0 8 0;"
         );

         javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
         grid.setStyle(
             "-fx-hgap: 16;" +
             "-fx-vgap: 10;" +
             "-fx-padding: 0;"
         );

         for (int i = 0; i < fields.length; i++) {
             Label keyLabel = new Label(fields[i][0]);
             keyLabel.setStyle(
                 "-fx-font-size: 11px;" +
                 "-fx-font-weight: bold;" +
                 "-fx-text-fill: #9499a8;" +
                 "-fx-min-width: 140px;"
             );

             Label valueLabel = new Label(fields[i][1]);
             valueLabel.setStyle(
                 "-fx-font-size: 11px;" +
                 "-fx-text-fill: #0e1b3d;" +
                 "-fx-wrap-text: true;"
             );

             grid.addRow(i, keyLabel, valueLabel);
         }

         sectionBox.getChildren().addAll(sectionLabel, grid);
         parent.getChildren().add(sectionBox);
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
             sectionBox.setValue("N/A");
         }
         ComboBox<String> violationBox = new ComboBox<>();
         violationBox.setMaxWidth(Double.MAX_VALUE);
         violationBox.setItems(FXCollections.observableArrayList("N/A"));
         violationBox.getItems().addAll(ViolationCatalog.allViolations());
         if (existing != null && ViolationCatalog.allViolations().contains(existing.getType())) {
             violationBox.setValue(existing.getType());
         } else {
             violationBox.setValue("N/A");
         }
         final boolean[] updatingSelection = {false};
         Runnable updateViolations = () -> {
             String selectedSection = sectionBox.getValue();
             boolean sectionSelected = selectedSection != null && !"N/A".equals(selectedSection);
             var availableViolations = FXCollections.observableArrayList("N/A");
             availableViolations.addAll(sectionSelected
                     ? ViolationCatalog.violationsFor(selectedSection)
                     : ViolationCatalog.allViolations());
             violationBox.setItems(availableViolations);
             if (existing != null && selectedSection != null && (selectedSection.equals(existing.getSection())
                     || (!sectionSelected && ViolationCatalog.allViolations().contains(existing.getType())))
                     && ViolationCatalog.allViolations().contains(existing.getType())) {
                 violationBox.setValue(existing.getType());
             } else if (!violationBox.getItems().contains(violationBox.getValue())) {
                 violationBox.setValue("N/A");
             }
         };
         sectionBox.setOnAction(e -> updateViolations.run());
         violationBox.setOnAction(e -> {
             if (updatingSelection[0] || violationBox.getValue() == null
                     || "N/A".equals(violationBox.getValue())) return;
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
         DatePicker conferenceDatePicker = new DatePicker(existing == null ? null : existing.getConferenceDate());
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
         
         // Multi-select sanctions using CheckBoxes
         String[] sanctionOptions = {"NI in Report Card", "Confiscation of the Item", "Disciplinary Probation",
                 "Automatic zero equivalent in the tests/outputs", "Restitution"};
         java.util.Map<String, CheckBox> sanctionCheckboxes = new java.util.HashMap<>();
         VBox sanctionBox = new VBox(5);
         
         // Parse existing sanctions (comma-separated string)
         java.util.Set<String> selectedSanctions = new java.util.HashSet<>();
         if (existing != null && existing.getSanction() != null && !"N/A".equals(existing.getSanction())) {
             String[] parts = existing.getSanction().split(",");
             for (String part : parts) {
                 selectedSanctions.add(part.trim());
             }
         }
         
         // Create checkboxes for each sanction option
         for (String sanction : sanctionOptions) {
             CheckBox cb = new CheckBox(sanction);
             cb.setSelected(selectedSanctions.contains(sanction));
             sanctionCheckboxes.put(sanction, cb);
             sanctionBox.getChildren().add(cb);
         }
         sanctionBox.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 3; -fx-padding: 8;");
         sanctionBox.setPrefHeight(150);
         
         ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Pending", "Under Review", "Resolved"));
         statusBox.getSelectionModel().select(existing == null ? "Pending" : existing.getStatus());
         String defaultSchoolYear = existing == null ? currentSchoolYear() : existing.getSchoolYear();
         ComboBox<String> schoolYearBox = new ComboBox<>(schoolYearOptions(defaultSchoolYear));
         schoolYearBox.setValue(defaultSchoolYear);
         TextArea descArea = new TextArea(existing == null ? "" : existing.getDescription());
         descArea.setPrefRowCount(3);

         TextArea remarksArea = new TextArea(existing == null ? "" : existing.getRemarks());
         remarksArea.setPrefRowCount(3);

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
         grid.addRow(r++, new Label("Sanction(s)"), sanctionBox);
         grid.addRow(r++, new Label("Date reported"), datePicker);
         grid.addRow(r++, new Label("Conference date"), conferenceDatePicker);
         grid.addRow(r++, new Label("Date completed"), completedDatePicker);
         grid.addRow(r++, new Label("Status"), statusBox);
         grid.addRow(r++, new Label("School year"), schoolYearBox);
         grid.addRow(r++, new Label("Remarks"), remarksArea);
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

             // Collect selected sanctions from checkboxes
             java.util.List<String> selectedSanctionsList = new java.util.ArrayList<>();
             for (java.util.Map.Entry<String, CheckBox> entry : sanctionCheckboxes.entrySet()) {
                 if (entry.getValue().isSelected()) {
                     selectedSanctionsList.add(entry.getKey());
                 }
             }
             String sanctionsString = selectedSanctionsList.isEmpty() ? "N/A" : String.join(", ", selectedSanctionsList);
             String selectedSchoolYear = schoolYearBox.getValue() == null ? currentSchoolYear() : schoolYearBox.getValue();

             if (!DataStore.SCHOOL_YEARS.contains(selectedSchoolYear)) {
                 DataStore.SCHOOL_YEARS.add(selectedSchoolYear);
             }

             if (existing == null) {
                 DataStore.VIOLATIONS.add(new Violation(
                         DataStore.nextViolationId(), nameField.getText().trim(), sidField.getText().trim(),
                         selectedSection, selectedViolation, offenseBox.getValue(), actionBox.getValue(),
                         suspensionDays, sanctionsString, datePicker.getValue(), conferenceDatePicker.getValue(),
                         completedDatePicker.getValue(), statusBox.getValue(), selectedSchoolYear,
                         Session.getCurrentUser() == null ? "" : Session.getCurrentUser().getUsername(),
                         remarksArea.getText().trim()));
             } else {
                 existing.setStudentName(nameField.getText().trim());
                 existing.setStudentId(sidField.getText().trim());
                 existing.setSection(selectedSection);
                 existing.setType(selectedViolation);
                 existing.setOffense(offenseBox.getValue());
                 existing.setActionTaken(actionBox.getValue());
                 existing.setSuspensionDays(suspensionDays);
                 existing.setSanction(sanctionsString);
                 existing.setConferenceDate(conferenceDatePicker.getValue());
                 existing.setCompletedDate(completedDatePicker.getValue());
                 existing.setStatus(statusBox.getValue());
                 existing.setDate(datePicker.getValue());
                 existing.setSchoolYear(selectedSchoolYear);
                 existing.setRemarks(remarksArea.getText().trim());
             }
             refresh();
         }
     }

      private ObservableList<String> schoolYearOptions(String preferredValue) {
          TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
          DataStore.SCHOOL_YEARS.forEach(year -> {
              if (year != null && !year.trim().isEmpty()) {
                  years.add(year);
              }
          });
          years.add(currentSchoolYear());
          if (preferredValue != null && !preferredValue.trim().isEmpty()) {
              years.add(preferredValue);
          }
          return FXCollections.observableArrayList(years);
      }

      private String currentSchoolYear() {
          int y = LocalDate.now().getYear();
          return y + "-" + (y + 1);
      }

}
