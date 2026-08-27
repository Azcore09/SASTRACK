package com.sastrack.controller;

import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.LostItem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeSet;

public class LostFoundController implements MainController.MainAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private ComboBox<String> schoolYearFilterBox;
    @FXML private TableView<LostItem> table;
    @FXML private TableColumn<LostItem, String> colName;
    @FXML private TableColumn<LostItem, String> colCategory;
    @FXML private TableColumn<LostItem, LocalDate> colDate;
    @FXML private TableColumn<LostItem, String> colLocation;
    @FXML private TableColumn<LostItem, String> colStatus;
    @FXML private TableColumn<LostItem, String> colClaimant;
    @FXML private TableColumn<LostItem, Void> colActions;

    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colClaimant.setCellValueFactory(new PropertyValueFactory<>("claimant"));
        addActionsColumn();

        statusFilterBox.setItems(FXCollections.observableArrayList("All", "Unclaimed", "Claimed"));
        statusFilterBox.getSelectionModel().selectFirst();

        TreeSet<String> years = new TreeSet<>(Comparator.reverseOrder());
        DataStore.ITEMS.forEach(i -> years.add(i.getSchoolYear()));
        schoolYearFilterBox.setItems(FXCollections.observableArrayList(years));
        schoolYearFilterBox.getItems().add(0, "All school years");
        schoolYearFilterBox.getSelectionModel().selectFirst();

        refresh();
    }

    public void applyStatusFilter(String status) {
        statusFilterBox.getSelectionModel().select(status == null ? "All" : status);
        refresh();
    }

    @FXML
    public void refresh() {
        String q = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String status = statusFilterBox.getValue();
        String year = schoolYearFilterBox.getValue();

        var filtered = DataStore.ITEMS.filtered(i ->
                (q.isEmpty() || i.getName().toLowerCase().contains(q) || i.getClaimant().toLowerCase().contains(q)) &&
                ("All".equals(status) || i.getStatus().equals(status)) &&
                (year == null || "All school years".equals(year) || i.getSchoolYear().equals(year))
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
                editBtn.setOnAction(e -> openItemDialog(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    if (Session.getCurrentUser() == null || !Session.getCurrentUser().isAdmin()) {
                        new Alert(Alert.AlertType.WARNING, "Only an SAS Head account can delete records.").showAndWait();
                        return;
                    }
                    DataStore.ITEMS.remove(getTableView().getItems().get(getIndex()));
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
        openItemDialog(null);
    }

    public void openItemDialog(LostItem existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Log found item" : "Edit item record");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField nameField = new TextField(existing == null ? "" : existing.getName());
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "Bag", "Stationery", "ID / Documents", "Electronics", "Clothing", "Accessories", "Other"));
        categoryBox.getSelectionModel().select(existing == null ? "Bag" : existing.getCategory());
        DatePicker datePicker = new DatePicker(existing == null ? LocalDate.now() : existing.getDate());
        TextField locationField = new TextField(existing == null ? "" : existing.getLocation());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Unclaimed", "Claimed"));
        statusBox.getSelectionModel().select(existing == null ? "Unclaimed" : existing.getStatus());
        TextField claimantField = new TextField(existing == null ? "" : existing.getClaimant());
        claimantField.setDisable(!"Claimed".equals(statusBox.getValue()));
        statusBox.setOnAction(e -> claimantField.setDisable(!"Claimed".equals(statusBox.getValue())));
        TextField schoolYearField = new TextField(existing == null ? currentSchoolYear() : existing.getSchoolYear());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(14));
        int r = 0;
        grid.addRow(r++, new Label("Item name"), nameField);
        grid.addRow(r++, new Label("Category"), categoryBox);
        grid.addRow(r++, new Label("Date found"), datePicker);
        grid.addRow(r++, new Label("Location"), locationField);
        grid.addRow(r++, new Label("Status"), statusBox);
        grid.addRow(r++, new Label("Claimant"), claimantField);
        grid.addRow(r++, new Label("School year"), schoolYearField);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            if (nameField.getText().trim().isEmpty()) return;
            if (existing == null) {
                DataStore.ITEMS.add(new LostItem(
                        DataStore.nextItemId(), nameField.getText().trim(), categoryBox.getValue(),
                        datePicker.getValue(), locationField.getText().trim(), statusBox.getValue(),
                        "Claimed".equals(statusBox.getValue()) ? claimantField.getText().trim() : "",
                        schoolYearField.getText().trim()));
            } else {
                existing.setStatus(statusBox.getValue());
                existing.setClaimant("Claimed".equals(statusBox.getValue()) ? claimantField.getText().trim() : "");
            }
            refresh();
        }
    }

    private String currentSchoolYear() {
        int y = LocalDate.now().getYear();
        return y + "-" + (y + 1);
    }
}
