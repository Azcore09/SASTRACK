package com.sastrack.controller;

import com.sastrack.data.DataStore;
import com.sastrack.model.SearchResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class SearchController implements MainController.MainAware {

    @FXML private Label titleLabel;
    @FXML private TableView<SearchResult> table;
    @FXML private TableColumn<SearchResult, String> colKind;
    @FXML private TableColumn<SearchResult, String> colRef;
    @FXML private TableColumn<SearchResult, String> colDetail;
    @FXML private TableColumn<SearchResult, String> colDate;
    @FXML private TableColumn<SearchResult, String> colStatus;

    private MainController mainController;

    @Override
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @FXML
    public void initialize() {
        colKind.setCellValueFactory(new PropertyValueFactory<>("kind"));
        colRef.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colDetail.setCellValueFactory(new PropertyValueFactory<>("detail"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    public void runSearch(String scope, String rawQuery) {
        String q = rawQuery == null ? "" : rawQuery.trim().toLowerCase();
        titleLabel.setText("Search results — " + scope + (q.isEmpty() ? "" : " · \"" + rawQuery.trim() + "\""));

        var results = FXCollections.<SearchResult>observableArrayList();

        if ("Violations".equals(scope)) {
            DataStore.VIOLATIONS.stream()
                    .filter(v -> q.isEmpty() || v.getStudentName().toLowerCase().contains(q) || v.getStudentId().toLowerCase().contains(q))
                    .forEach(v -> results.add(new SearchResult("Violation",
                            v.getStudentName() + " (" + v.getStudentId() + ")", v.getType(),
                            v.getDate().toString(), v.getStatus())));
        } else {
            DataStore.ITEMS.stream()
                    .filter(i -> q.isEmpty() || i.getName().toLowerCase().contains(q) || i.getClaimant().toLowerCase().contains(q))
                    .forEach(i -> results.add(new SearchResult("Lost & Found",
                            i.getName(), i.getLocation(), i.getDate().toString(), i.getStatus())));
        }

        table.setItems(results);
    }
}
