package com.sastrack.controller;

import com.sastrack.app.Session;
import com.sastrack.data.DataStore;
import com.sastrack.model.LostItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class LostFoundController implements MainController.MainAware {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilterBox;
    @FXML private ComboBox<String> schoolYearFilterBox;
    @FXML private TableView<LostItem> table;
    @FXML private TableColumn<LostItem, String> colName;
    @FXML private TableColumn<LostItem, String> colPhoto;
    @FXML private TableColumn<LostItem, String> colCategory;
    @FXML private TableColumn<LostItem, LocalDate> colDate;
     @FXML private TableColumn<LostItem, String> colLocation;
     @FXML private TableColumn<LostItem, String> colStatus;
     @FXML private TableColumn<LostItem, String> colClaimant;
     @FXML private TableColumn<LostItem, String> colSchoolYear;
     @FXML private Button viewButton;
     @FXML private Button editButton;
     @FXML private Button deleteButton;

     private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
         colName.setCellValueFactory(new PropertyValueFactory<>("name"));
         colPhoto.setCellValueFactory(cellData -> Bindings.createStringBinding(
                 () -> cellData.getValue().getPhotoNames().isEmpty()
                         ? null
                         : cellData.getValue().getPhotoNames().get(0),
                 cellData.getValue().getPhotoNames()));
         colPhoto.setCellFactory(column -> new TableCell<>() {
             private final ImageView imageView = new ImageView();

             {
                 imageView.setFitWidth(72);
                 imageView.setFitHeight(72);
                 imageView.setPreserveRatio(true);
             }

             @Override
             protected void updateItem(String fileName, boolean empty) {
                 super.updateItem(fileName, empty);
                 setText(null);
                 if (empty || fileName == null) {
                     setGraphic(null);
                     return;
                 }
                 LostItem rowItem = getTableRow() == null ? null : getTableRow().getItem();
                 if (rowItem == null) {
                     setGraphic(null);
                     return;
                 }
                 Path photoPath = DataStore.getItemPhotoPath(rowItem.getId(), fileName);
                 if (!Files.isRegularFile(photoPath)) {
                     setText("Unavailable");
                     setGraphic(null);
                     return;
                 }
                 Image image = new Image(photoPath.toUri().toString(), 72, 72, true, true);
                 if (image.isError()) {
                     setText("Unavailable");
                     setGraphic(null);
                     return;
                 }
                 imageView.setImage(image);
                 setGraphic(imageView);
             }
         });
         colCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
         colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
         colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
         colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
         colClaimant.setCellValueFactory(new PropertyValueFactory<>("claimant"));
         colSchoolYear.setCellValueFactory(new PropertyValueFactory<>("schoolYear"));
         table.setFixedCellSize(84);
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

        statusFilterBox.setItems(FXCollections.observableArrayList("All", "Unclaimed", "Claimed"));
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

    @FXML
    private void handleEditSelected() {
        LostItem selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openItemDialog(selected);
        }
    }

    @FXML
    private void handleDeleteSelected() {
        LostItem selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (Session.getCurrentUser() == null || !Session.getCurrentUser().isAdmin()) {
            new Alert(Alert.AlertType.WARNING, "Only an SAS Head account can delete records.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Lost & Found Record");
        confirm.setHeaderText("Delete this lost-and-found record?");
        confirm.setContentText("This action cannot be undone.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                DataStore.ITEMS.remove(selected);
                try {
                    DataStore.deleteItemPhotos(selected);
                } catch (IOException e) {
                    showPhotoError("The item was deleted, but its photos could not be removed.", e);
                }
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
         LostItem selected = table.getSelectionModel().getSelectedItem();
         if (selected != null) {
             viewItemDialog(selected);
         }
     }

     @FXML
     private void handleAdd() {
         openItemDialog(null);
     }

     public void viewItemDialog(LostItem item) {
         Dialog<ButtonType> dialog = new Dialog<>();
         dialog.setTitle("View item record");
         dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
         dialog.getDialogPane().setPrefWidth(680);
         dialog.getDialogPane().setPrefHeight(450);

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
         Label headerTitle = new Label("Item Details");
         headerTitle.setStyle(
             "-fx-font-size: 18px;" +
             "-fx-font-weight: bold;" +
             "-fx-text-fill: white;"
         );
         Label headerSubtitle = new Label(item.getCategory() + " • " + (item.getStatus().equals("Claimed") ? "Claimed by " + item.getClaimant() : "Unclaimed"));
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
         addViewItemSection(contentBox, "Item Information", new String[][] {
             {"Item Name:", item.getName()},
             {"Category:", item.getCategory()}
         });

         addViewItemSection(contentBox, "Location & Timeline", new String[][] {
             {"Date Found:", item.getDate() == null ? "N/A" : item.getDate().toString()},
             {"Location:", item.getLocation()}
         });

         addViewItemSection(contentBox, "Status", new String[][] {
             {"Status:", item.getStatus()},
             {"Claimant:", item.getClaimant() == null || item.getClaimant().isEmpty() ? "N/A" : item.getClaimant()}
         });

         addViewItemSection(contentBox, "Organization", new String[][] {
             {"School Year:", item.getSchoolYear()}
         });

         addItemPhotos(contentBox, item);

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

     private void addViewItemSection(javafx.scene.layout.VBox parent, String sectionTitle, String[][] fields) {
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

     private void addItemPhotos(javafx.scene.layout.VBox parent, LostItem item) {
         javafx.scene.layout.VBox sectionBox = new javafx.scene.layout.VBox(12);
         sectionBox.setStyle("-fx-padding: 16 0 0 0;");
         Label sectionLabel = new Label("Photos");
         sectionLabel.setStyle(
                 "-fx-font-size: 12px;" +
                 "-fx-font-weight: bold;" +
                 "-fx-text-fill: #2f6fed;" +
                 "-fx-padding: 0 0 8 0;"
         );
         FlowPane gallery = new FlowPane(12, 12);
         if (item.getPhotoNames().isEmpty()) {
             gallery.getChildren().add(new Label("No photos attached."));
         } else {
             for (String fileName : item.getPhotoNames()) {
                 Path photoPath = DataStore.getItemPhotoPath(item.getId(), fileName);
                 if (!Files.isRegularFile(photoPath)) {
                     gallery.getChildren().add(new Label("Photo unavailable"));
                     continue;
                 }
                 Image image = new Image(photoPath.toUri().toString(), 280, 220, true, true);
                 if (image.isError()) {
                     gallery.getChildren().add(new Label("Photo unavailable"));
                     continue;
                 }
                 ImageView imageView = new ImageView(image);
                 imageView.setFitWidth(280);
                 imageView.setFitHeight(220);
                 imageView.setPreserveRatio(true);
                 gallery.getChildren().add(imageView);
             }
         }
         sectionBox.getChildren().addAll(sectionLabel, gallery);
         parent.getChildren().add(sectionBox);
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
        String defaultSchoolYear = existing == null ? currentSchoolYear() : existing.getSchoolYear();
        ComboBox<String> schoolYearBox = new ComboBox<>(schoolYearOptions(defaultSchoolYear));
        schoolYearBox.setValue(defaultSchoolYear);

        int itemId = existing == null ? DataStore.nextItemId() : existing.getId();
        ObservableList<String> photoNames = FXCollections.observableArrayList(
                existing == null ? List.of() : existing.getPhotoNames());
        List<String> stagedPhotoNames = new ArrayList<>();
        ListView<String> photoList = new ListView<>(photoNames);
        photoList.setPrefHeight(120);
        photoList.setCellFactory(list -> new ListCell<>() {
            private final ImageView thumbnail = new ImageView();
            private final Label label = new Label();
            private final HBox row = new HBox(10, thumbnail, label);

            {
                thumbnail.setFitWidth(56);
                thumbnail.setFitHeight(44);
                thumbnail.setPreserveRatio(true);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(String fileName, boolean empty) {
                super.updateItem(fileName, empty);
                if (empty || fileName == null) {
                    setGraphic(null);
                    return;
                }
                Path photoPath = DataStore.getItemPhotoPath(itemId, fileName);
                label.setText("Photo " + (getIndex() + 1));
                if (Files.isRegularFile(photoPath)) {
                    Image image = new Image(photoPath.toUri().toString(), 56, 44, true, true);
                    thumbnail.setImage(image.isError() ? null : image);
                } else {
                    thumbnail.setImage(null);
                }
                setGraphic(row);
            }
        });
        Button addPhotosButton = new Button("Add photos");
        Button removePhotoButton = new Button("Remove selected");
        removePhotoButton.disableProperty().bind(photoList.getSelectionModel().selectedItemProperty().isNull());
        addPhotosButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select item photos");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    "Image files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
            Window owner = dialog.getDialogPane().getScene().getWindow();
            List<File> selectedFiles = chooser.showOpenMultipleDialog(owner);
            if (selectedFiles == null) {
                return;
            }
            for (File file : selectedFiles) {
                try {
                    String fileName = DataStore.copyItemPhoto(itemId, file.toPath());
                    stagedPhotoNames.add(fileName);
                    photoNames.add(fileName);
                } catch (IOException e) {
                    showPhotoError("Unable to copy the selected photo.", e);
                }
            }
        });
        removePhotoButton.setOnAction(event -> {
            String selectedPhoto = photoList.getSelectionModel().getSelectedItem();
            if (selectedPhoto == null) {
                return;
            }
            if (stagedPhotoNames.contains(selectedPhoto)) {
                try {
                    DataStore.deleteItemPhoto(itemId, selectedPhoto);
                    stagedPhotoNames.remove(selectedPhoto);
                } catch (IOException e) {
                    showPhotoError("Unable to remove the selected photo.", e);
                    return;
                }
            }
            photoNames.remove(selectedPhoto);
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(14));
        int r = 0;
        grid.addRow(r++, new Label("Item name"), nameField);
        grid.addRow(r++, new Label("Category"), categoryBox);
        grid.addRow(r++, new Label("Date found"), datePicker);
        grid.addRow(r++, new Label("Location"), locationField);
        grid.addRow(r++, new Label("Status"), statusBox);
        grid.addRow(r++, new Label("Claimant"), claimantField);
        grid.addRow(r++, new Label("School year"), schoolYearBox);
        HBox photoActions = new HBox(8, addPhotosButton, removePhotoButton);
        VBox photoControls = new VBox(8, photoList, photoActions);
        grid.addRow(r, new Label("Photos"), photoControls);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType && !nameField.getText().trim().isEmpty()) {
            String selectedSchoolYear = schoolYearBox.getValue() == null ? currentSchoolYear() : schoolYearBox.getValue();
            if (!DataStore.SCHOOL_YEARS.contains(selectedSchoolYear)) {
                DataStore.SCHOOL_YEARS.add(selectedSchoolYear);
            }
            if (existing == null) {
                LostItem item = new LostItem(
                        itemId, nameField.getText().trim(), categoryBox.getValue(),
                        datePicker.getValue(), locationField.getText().trim(), statusBox.getValue(),
                        "Claimed".equals(statusBox.getValue()) ? claimantField.getText().trim() : "",
                        selectedSchoolYear);
                item.getPhotoNames().setAll(photoNames);
                DataStore.ITEMS.add(item);
            } else {
                List<String> removedPhotoNames = new ArrayList<>(existing.getPhotoNames());
                removedPhotoNames.removeAll(photoNames);
                existing.setStatus(statusBox.getValue());
                existing.setClaimant("Claimed".equals(statusBox.getValue()) ? claimantField.getText().trim() : "");
                existing.setSchoolYear(selectedSchoolYear);
                existing.getPhotoNames().setAll(photoNames);
                for (String removedPhotoName : removedPhotoNames) {
                    try {
                        DataStore.deleteItemPhoto(itemId, removedPhotoName);
                    } catch (IOException e) {
                        showPhotoError("The item was saved, but a removed photo could not be deleted.", e);
                    }
                }
            }
            stagedPhotoNames.clear();
            refresh();
        } else {
            cleanupStagedPhotos(itemId, stagedPhotoNames);
        }
    }

    private void cleanupStagedPhotos(int itemId, List<String> stagedPhotoNames) {
        for (String fileName : stagedPhotoNames) {
            try {
                DataStore.deleteItemPhoto(itemId, fileName);
            } catch (IOException e) {
                showPhotoError("Unable to clean up an unsaved photo.", e);
            }
        }
    }

    private void showPhotoError(String message, IOException exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Photo Error");
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());
        alert.showAndWait();
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
