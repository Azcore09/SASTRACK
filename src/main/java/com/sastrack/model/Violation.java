package com.sastrack.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class Violation {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty studentName = new SimpleStringProperty();
    private final StringProperty studentId = new SimpleStringProperty();
    private final StringProperty section = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();       // Specific catalog violation
    private final StringProperty offense = new SimpleStringProperty("N/A");
    private final StringProperty actionTaken = new SimpleStringProperty("N/A");
    private final IntegerProperty suspensionDays = new SimpleIntegerProperty();
    private final StringProperty sanction = new SimpleStringProperty("N/A");
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> completedDate = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();     // Pending / Under Review / Resolved
    private final StringProperty schoolYear = new SimpleStringProperty(); // e.g. "2026-2027"
    private final StringProperty reportedBy = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    public Violation(int id, String studentName, String studentId, String section, String type,
                      LocalDate date, String status, String schoolYear, String reportedBy, String description) {
        this(id, studentName, studentId, section, type, "N/A", "N/A", 0, "N/A",
                date, null, status, schoolYear, reportedBy, description);
    }

    public Violation(int id, String studentName, String studentId, String section, String type,
                     String offense, String actionTaken, int suspensionDays, String sanction,
                     LocalDate date, LocalDate completedDate, String status, String schoolYear,
                     String reportedBy, String description) {
        this.id.set(id);
        this.studentName.set(studentName);
        this.studentId.set(studentId);
        this.section.set(section);
        this.type.set(type);
        this.offense.set(offense);
        this.actionTaken.set(actionTaken);
        this.suspensionDays.set(suspensionDays);
        this.sanction.set(sanction);
        this.date.set(date);
        this.completedDate.set(completedDate);
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
    public void setSection(String v) { section.set(v); }
    public StringProperty sectionProperty() { return section; }

    public String getType() { return type.get(); }
    public void setType(String v) { type.set(v); }
    public StringProperty typeProperty() { return type; }

    public String getOffense() { return offense.get(); }
    public void setOffense(String v) { offense.set(v); }
    public StringProperty offenseProperty() { return offense; }

    public String getActionTaken() { return actionTaken.get(); }
    public void setActionTaken(String v) { actionTaken.set(v); }
    public StringProperty actionTakenProperty() { return actionTaken; }

    public int getSuspensionDays() { return suspensionDays.get(); }
    public void setSuspensionDays(int v) { suspensionDays.set(v); }
    public IntegerProperty suspensionDaysProperty() { return suspensionDays; }

    public String getSanction() { return sanction.get(); }
    public void setSanction(String v) { sanction.set(v); }
    public StringProperty sanctionProperty() { return sanction; }

    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate v) { date.set(v); }
    public ObjectProperty<LocalDate> dateProperty() { return date; }

    public LocalDate getCompletedDate() { return completedDate.get(); }
    public void setCompletedDate(LocalDate v) { completedDate.set(v); }
    public ObjectProperty<LocalDate> completedDateProperty() { return completedDate; }

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
