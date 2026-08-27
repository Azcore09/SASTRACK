package com.sastrack.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class LostItem {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty category = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final StringProperty location = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty(); // Unclaimed / Claimed
    private final StringProperty claimant = new SimpleStringProperty();
    private final StringProperty schoolYear = new SimpleStringProperty();

    public LostItem(int id, String name, String category, LocalDate date, String location,
                     String status, String claimant, String schoolYear) {
        this.id.set(id);
        this.name.set(name);
        this.category.set(category);
        this.date.set(date);
        this.location.set(location);
        this.status.set(status);
        this.claimant.set(claimant == null ? "" : claimant);
        this.schoolYear.set(schoolYear);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }

    public LocalDate getDate() { return date.get(); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public String getLocation() { return location.get(); }
    public StringProperty locationProperty() { return location; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }

    public String getClaimant() { return claimant.get(); }
    public void setClaimant(String v) { claimant.set(v); }
    public StringProperty claimantProperty() { return claimant; }

    public String getSchoolYear() { return schoolYear.get(); }
    public StringProperty schoolYearProperty() { return schoolYear; }
}
