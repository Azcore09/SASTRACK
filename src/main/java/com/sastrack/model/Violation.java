package com.sastrack.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Violation {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty studentName = new SimpleStringProperty();
    private final StringProperty studentId = new SimpleStringProperty();
    private final StringProperty section = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();       // Minor / Major / Serious
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();     // Pending / Under Review / Resolved
    private final StringProperty schoolYear = new SimpleStringProperty(); // e.g. "2026-2027"
    private final StringProperty reportedBy = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    public Violation(int id, String studentName, String studentId, String section, String type,
                      LocalDate date, String status, String schoolYear, String reportedBy, String description) {
        this.id.set(id);
        this.studentName.set(studentName);
        this.studentId.set(studentId);
        this.section.set(section);
        this.type.set(type);
        this.date.set(date);
        this.status.set(status);
        this.schoolYear.set(schoolYear);
        this.reportedBy.set(reportedBy);
        this.description.set(description);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }

    public String getStudentName() { return studentName.get(); }
    public StringProperty studentNameProperty() { return studentName; }

    public String getStudentId() { return studentId.get(); }
    public StringProperty studentIdProperty() { return studentId; }

    public String getSection() { return section.get(); }
    public StringProperty sectionProperty() { return section; }

    public String getType() { return type.get(); }
    public void setType(String v) { type.set(v); }
    public StringProperty typeProperty() { return type; }

    public LocalDate getDate() { return date.get(); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public String getStatus() { return status.get(); }
    public void setStatus(String v) { status.set(v); }
    public StringProperty statusProperty() { return status; }

    public String getSchoolYear() { return schoolYear.get(); }
    public StringProperty schoolYearProperty() { return schoolYear; }

    public String getReportedBy() { return reportedBy.get(); }
    public StringProperty reportedByProperty() { return reportedBy; }

    public String getDescription() { return description.get(); }
    public StringProperty descriptionProperty() { return description; }
}
