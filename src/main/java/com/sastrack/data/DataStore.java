package com.sastrack.data;

import com.sastrack.model.LostItem;
import com.sastrack.model.User;
import com.sastrack.model.Violation;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public final class DataStore {

    private static final Path DB_PATH = Paths.get(System.getProperty("user.dir"), "data", "sastrack.db");

    public static final ObservableList<User> USERS = FXCollections.observableArrayList(
            new User("j.delacruz", "admin123", "SAS Head"),
            new User("m.fernandez", "staff123", "SAS Staff")
    );
    public static final ObservableList<Violation> VIOLATIONS = FXCollections.observableArrayList();
    public static final ObservableList<LostItem> ITEMS = FXCollections.observableArrayList();

    static {
        initializeDatabase();
        loadViolations();
        loadItems();
        attachPersistence();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH.toAbsolutePath());
    }

    private static void initializeDatabase() {
        try {
            Files.createDirectories(DB_PATH.getParent());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create local data directory for SAS-TRACK.", e);
        }

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS violations (id INTEGER PRIMARY KEY, student_name TEXT NOT NULL, student_id TEXT NOT NULL, section TEXT NOT NULL, type TEXT NOT NULL, date TEXT NOT NULL, status TEXT NOT NULL, school_year TEXT NOT NULL, reported_by TEXT NOT NULL, description TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS lost_items (id INTEGER PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, date TEXT NOT NULL, location TEXT NOT NULL, status TEXT NOT NULL, claimant TEXT NOT NULL, school_year TEXT NOT NULL)");

            statement.execute("DELETE FROM violations");
            statement.execute("DELETE FROM lost_items");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize the SQLite database for SAS-TRACK.", e);
        }
    }

    private static void loadViolations() {
        VIOLATIONS.clear();
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, student_name, student_id, section, type, date, status, school_year, reported_by, description FROM violations ORDER BY id ASC")) {
            while (rs.next()) {
                Violation violation = new Violation(
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getString("student_id"),
                        rs.getString("section"),
                        rs.getString("type"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getString("status"),
                        rs.getString("school_year"),
                        rs.getString("reported_by"),
                        rs.getString("description")
                );
                attachViolationListeners(violation);
                VIOLATIONS.add(violation);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load violation data from SQLite.", e);
        }
    }

    private static void loadItems() {
        ITEMS.clear();
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, name, category, date, location, status, claimant, school_year FROM lost_items ORDER BY id ASC")) {
            while (rs.next()) {
                LostItem item = new LostItem(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getString("location"),
                        rs.getString("status"),
                        rs.getString("claimant"),
                        rs.getString("school_year")
                );
                attachLostItemListeners(item);
                ITEMS.add(item);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load lost item data from SQLite.", e);
        }
    }

    private static void attachPersistence() {
        VIOLATIONS.addListener((ListChangeListener<Violation>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Violation violation : change.getAddedSubList()) {
                        attachViolationListeners(violation);
                    }
                }
                if (change.wasAdded() || change.wasRemoved()) {
                    persistViolations();
                }
            }
        });

        ITEMS.addListener((ListChangeListener<LostItem>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (LostItem item : change.getAddedSubList()) {
                        attachLostItemListeners(item);
                    }
                }
                if (change.wasAdded() || change.wasRemoved()) {
                    persistItems();
                }
            }
        });
    }

    private static void attachViolationListeners(Violation violation) {
        violation.idProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.studentNameProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.studentIdProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.sectionProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.typeProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.dateProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.statusProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.schoolYearProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.reportedByProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.descriptionProperty().addListener((obs, oldValue, newValue) -> persistViolations());
    }

    private static void attachLostItemListeners(LostItem item) {
        item.idProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.nameProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.categoryProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.dateProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.locationProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.statusProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.claimantProperty().addListener((obs, oldValue, newValue) -> persistItems());
        item.schoolYearProperty().addListener((obs, oldValue, newValue) -> persistItems());
    }

    private static void persistViolations() {
        try (Connection connection = connect(); PreparedStatement delete = connection.prepareStatement("DELETE FROM violations");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO violations (id, student_name, student_id, section, type, date, status, school_year, reported_by, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            delete.executeUpdate();
            for (Violation violation : VIOLATIONS) {
                insert.setInt(1, violation.getId());
                insert.setString(2, violation.getStudentName());
                insert.setString(3, violation.getStudentId());
                insert.setString(4, violation.getSection());
                insert.setString(5, violation.getType());
                insert.setString(6, violation.getDate().toString());
                insert.setString(7, violation.getStatus());
                insert.setString(8, violation.getSchoolYear());
                insert.setString(9, violation.getReportedBy());
                insert.setString(10, violation.getDescription());
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist violation data to SQLite.", e);
        }
    }

    private static void persistItems() {
        try (Connection connection = connect(); PreparedStatement delete = connection.prepareStatement("DELETE FROM lost_items");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO lost_items (id, name, category, date, location, status, claimant, school_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            delete.executeUpdate();
            for (LostItem item : ITEMS) {
                insert.setInt(1, item.getId());
                insert.setString(2, item.getName());
                insert.setString(3, item.getCategory());
                insert.setString(4, item.getDate().toString());
                insert.setString(5, item.getLocation());
                insert.setString(6, item.getStatus());
                insert.setString(7, item.getClaimant());
                insert.setString(8, item.getSchoolYear());
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist lost item data to SQLite.", e);
        }
    }

    public static int nextViolationId() {
        return nextId("violations");
    }

    public static int nextItemId() {
        return nextId("lost_items");
    }

    private static int nextId(String tableName) {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt("next_id");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to determine the next ID for SQLite table " + tableName + ".", e);
        }
        return 1;
    }

    private DataStore() {}
}
