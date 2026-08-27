package com.sastrack.controller;

import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.Violation;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public class ViolationsController implements MainController.MainAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilterBox;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private ComboBox<String> schoolYearFilterBox;
    @FXML private TableView<Violation> table;
    @FXML private TableColumn<Violation, String> colName;
    @FXML private TableColumn<Violation, String> colSection;
    @FXML private TableColumn<Violation, String> colType;
    @FXML private TableColumn<Violation, LocalDate> colDate;
    @FXML private TableColumn<Violation, String> colStatus;
    @FXML private TableColumn<Violation, String> colYear;
    @FXML private TableColumn<Violation, String> colBy;
    @FXML private TableColumn<Violation, Void> colActions;

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
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colYear.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
        colBy.setCellValueFactory(new PropertyValueFactory<>("reportedBy"));
        addActionsColumn();

        typeFilterBox.setItems(FXCollections.observableArrayList("All", "Minor Offense", "Major Offense", "Serious Offense"));
        typeFilterBox.getSelectionModel().selectFirst();

        statusFilterBox.setItems(FXCollections.observableArrayList("All statuses", "Pending", "Under Review", "Resolved"));
        statusFilterBox.getSelectionModel().selectFirst();

        TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
        DataStore.VIOLATIONS.forEach(v -> years.add(v.getSchoolYear()));
        schoolYearFilterBox.setItems(FXCollections.observableArrayList(years));
        schoolYearFilterBox.getItems().add(0, "All school years");
        schoolYearFilterBox.getSelectionModel().selectFirst();

        refresh();
    }

    public void applyTypeFilter(String type) {
        typeFilterBox.getSelectionModel().select(type == null || type.equals("All") ? "All" : type);
        refresh();
    }

    public void applyStatusGroupFilter(String group) {
        this.statusGroupFilter = group;
        refresh();
    }

    @FXML
    public void refresh() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String type = typeFilterBox.getValue();
        String status = statusFilterBox.getValue();
        String year = schoolYearFilterBox.getValue();

        var filtered = DataStore.VIOLATIONS.filtered(v ->
                (q.isEmpty() || v.getStudentName().toLowerCase().contains(q) || v.getStudentId().toLowerCase().contains(q)) &&
                ("All".equals(type) || v.getType().equals(type)) &&
                ("All statuses".equals(status) || v.getStatus().equals(status)) &&
                (year == null || "All school years".equals(year) || v.getSchoolYear().equals(year)) &&
                (statusGroupFilter == null
                        || ("Active".equals(statusGroupFilter) && !"Resolved".equals(v.getStatus()))
                        || ("Resolved".equals(statusGroupFilter) && "Resolved".equals(v.getStatus())))
        );
        table.setItems(filtered);
    }

    private void addActionsColumn() {
        colActions.setCellValueFactory(cd -> new SimpleObjectProperty<>());
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, delBtn);
            {
                editBtn.getStyleClass().add("btn-outline-small");
                delBtn.getStyleClass().add("btn-outline-small");
                editBtn.setOnAction(e -> openViolationDialog(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    if (Session.getCurrentUser() == null || !Session.getCurrentUser().isAdmin()) {
                        new Alert(Alert.AlertType.WARNING, "Only an SAS Head account can delete records.").showAndWait();
                        return;
                    }
                    DataStore.VIOLATIONS.remove(getTableView().getItems().get(getIndex()));
                    refresh();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
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
        TextField sectionField = new TextField(existing == null ? "" : existing.getSection());

        ToggleGroup offenseGroup = new ToggleGroup();
        Map<String, ToggleButton> offenseButtons = new LinkedHashMap<>();
        String defaultOffense = existing == null ? "Minor Offense" : existing.getType();
        offenseButtons.put("Minor Offense", createSeverityButton("Minor", "Minor Offense", "minor", "Wrong hair color, prohibited earrings, improper uniform, tardiness, minor teasing, minor disruption"));
        offenseButtons.put("Major Offense", createSeverityButton("Major", "Major Offense", "major", "Fighting, bullying, threats, vandalism, cheating, possession/use of vape, repeated disobedience"));
        offenseButtons.put("Serious Offense", createSeverityButton("Serious", "Serious Offense", "serious", "Severe violence, serious threats, sexual harassment, serious injury, selling/distributing vape, severe bullying"));

        HBox typeToggle = new HBox(0);
        typeToggle.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        typeToggle.setPrefWidth(270);
        typeToggle.setStyle("-fx-border-color: #dfe3ed; -fx-border-radius: 10; -fx-background-radius: 10; -fx-background-color: white;");

        offenseButtons.forEach((offenseType, button) -> {
            button.setToggleGroup(offenseGroup);
            button.setUserData(offenseType);
            button.setSelected(offenseType.equals(defaultOffense));
            HBox.setHgrow(button, Priority.ALWAYS);
            typeToggle.getChildren().add(button);
        });

        DatePicker datePicker = new DatePicker(existing == null ? LocalDate.now() : existing.getDate());
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
        grid.addRow(r++, new Label("Section"), sectionField);
        grid.addRow(r++, new Label("Offense type"), typeToggle);
        grid.addRow(r++, new Label("Date"), datePicker);
        grid.addRow(r++, new Label("Status"), statusBox);
        grid.addRow(r++, new Label("School year"), schoolYearField);
        grid.addRow(r++, new Label("Description"), descArea);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (nameField.getText().trim().isEmpty()) return;
            String selectedType = offenseButtons.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("Minor Offense");
            if (existing == null) {
                DataStore.VIOLATIONS.add(new Violation(
                        DataStore.nextViolationId(), nameField.getText().trim(), sidField.getText().trim(),
                        sectionField.getText().trim(), selectedType, datePicker.getValue(),
                        statusBox.getValue(), schoolYearField.getText().trim(),
                        Session.getCurrentUser() == null ? "" : Session.getCurrentUser().getUsername(),
                        descArea.getText().trim()));
            } else {
                existing.setType(selectedType);
                existing.setStatus(statusBox.getValue());
            }
            refresh();
        }
    }

    private ToggleButton createSeverityButton(String shortLabel, String offenseType, String tone, String examples) {
        ToggleButton button = new ToggleButton(shortLabel);
        button.getStyleClass().addAll("offense-severity", tone);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinHeight(32);
        button.setTooltip(new Tooltip(shortenIfNeeded(examples)));
        button.setOnMouseEntered(e -> button.setTooltip(new Tooltip(shortenIfNeeded(examples))));
        button.setUserData(offenseType);
        return button;
    }

    private String shortenIfNeeded(String text) {
        if (text == null) return "";
        if (text.length() <= 112) return text;
        return text.substring(0, 109).trim() + "...";
    }

    private String currentSchoolYear() {
        int y = LocalDate.now().getYear();
        return y + "-" + (y + 1);
    }
}
