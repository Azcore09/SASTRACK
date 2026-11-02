package com.sastrack.data;

import com.sastrack.model.LostItem;
import com.sastrack.model.User;
import com.sastrack.model.Violation;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DataStore {

    private static final Path DB_PATH = Paths.get(System.getProperty("user.dir"), "data", "sastrack.db");
    private static final Path ITEM_PHOTOS_PATH = DB_PATH.getParent().resolve("lost-found-images");

    public static final ObservableList<User> USERS = FXCollections.observableArrayList(
            new User("admin", "admin123", "SAS Head"),
            new User("staff", "staff123", "SAS Staff")
    );
    public static final ObservableList<String> SCHOOL_YEARS = FXCollections.observableArrayList();
    public static final ObservableList<Violation> VIOLATIONS = FXCollections.observableArrayList();
    public static final ObservableList<LostItem> ITEMS = FXCollections.observableArrayList();

    static {
        initializeDatabase();
        loadSchoolYears();
        loadViolations();
        loadItems();
        attachPersistence();
    }

    public static void initializeSyncSupport() {
        ensureSyncColumns();
    }

    private static void ensureSyncColumns() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            ensureSyncColumnsForTable(statement, "violations");
            ensureSyncColumnsForTable(statement, "lost_items");
            ensureSyncColumnsForTable(statement, "school_years");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to add sync metadata columns to SQLite tables.", e);
        }
    }

    private static void ensureSyncColumnsForTable(Statement statement, String tableName) throws SQLException {
        boolean hasCreatedAt = false;
        boolean hasUpdatedAt = false;
        boolean hasDeleted = false;
        boolean hasSyncStatus = false;

        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (columns.next()) {
                String name = columns.getString("name");
                if ("created_at".equals(name)) hasCreatedAt = true;
                if ("updated_at".equals(name)) hasUpdatedAt = true;
                if ("deleted".equals(name)) hasDeleted = true;
                if ("sync_status".equals(name)) hasSyncStatus = true;
            }
        }

        if (!hasCreatedAt) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN created_at TEXT NOT NULL DEFAULT ''");
        }
        if (!hasUpdatedAt) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN updated_at TEXT NOT NULL DEFAULT ''");
        }
        if (!hasDeleted) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
        }
        if (!hasSyncStatus) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'synced'");
        }
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
            statement.execute("CREATE TABLE IF NOT EXISTS violations (id INTEGER PRIMARY KEY, student_name TEXT NOT NULL, student_id TEXT NOT NULL, section TEXT NOT NULL, type TEXT NOT NULL, offense TEXT NOT NULL DEFAULT 'N/A', action_taken TEXT NOT NULL DEFAULT 'N/A', suspension_days INTEGER NOT NULL DEFAULT 0, sanction TEXT NOT NULL DEFAULT 'N/A', date TEXT NOT NULL, conference_date TEXT, completed_date TEXT, status TEXT NOT NULL, school_year TEXT NOT NULL, reported_by TEXT NOT NULL, remarks TEXT NOT NULL DEFAULT '')");
            ensureSyncColumnsForTable(statement, "violations");
            addViolationColumn(statement, "offense", "TEXT NOT NULL DEFAULT 'N/A'");
            addViolationColumn(statement, "action_taken", "TEXT NOT NULL DEFAULT 'N/A'");
            addViolationColumn(statement, "suspension_days", "INTEGER NOT NULL DEFAULT 0");
            addViolationColumn(statement, "sanction", "TEXT NOT NULL DEFAULT 'N/A'");
            addViolationColumn(statement, "completed_date", "TEXT");
            addViolationColumn(statement, "conference_date", "TEXT");
            addViolationColumn(statement, "remarks", "TEXT NOT NULL DEFAULT ''");
            ensureViolationRemarksColumn(statement);
            statement.execute("CREATE TABLE IF NOT EXISTS lost_items (id INTEGER PRIMARY KEY, name TEXT NOT NULL, category TEXT NOT NULL, date TEXT NOT NULL, location TEXT NOT NULL, status TEXT NOT NULL, claimant TEXT NOT NULL, school_year TEXT NOT NULL)");
            ensureSyncColumnsForTable(statement, "lost_items");
            statement.execute("CREATE TABLE IF NOT EXISTS lost_item_photos (item_id INTEGER NOT NULL, position INTEGER NOT NULL, file_name TEXT NOT NULL, PRIMARY KEY (item_id, position))");
            statement.execute("CREATE TABLE IF NOT EXISTS school_years (year TEXT PRIMARY KEY NOT NULL)");
            ensureSyncColumnsForTable(statement, "school_years");
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to initialize the SQLite database for SAS-TRACK.", e);
        }
    }

    private static void ensureViolationRemarksColumn(Statement statement) throws SQLException {
        boolean hasRemarks = false;
        boolean hasDescription = false;
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(violations)")) {
            while (columns.next()) {
                String name = columns.getString("name");
                if ("remarks".equals(name)) hasRemarks = true;
                if ("description".equals(name)) hasDescription = true;
            }
        }
        if (!hasRemarks && hasDescription) {
            statement.execute("ALTER TABLE violations RENAME TO violations_old");
            statement.execute("CREATE TABLE violations (id INTEGER PRIMARY KEY, student_name TEXT NOT NULL, student_id TEXT NOT NULL, section TEXT NOT NULL, type TEXT NOT NULL, offense TEXT NOT NULL DEFAULT 'N/A', action_taken TEXT NOT NULL DEFAULT 'N/A', suspension_days INTEGER NOT NULL DEFAULT 0, sanction TEXT NOT NULL DEFAULT 'N/A', date TEXT NOT NULL, conference_date TEXT, completed_date TEXT, status TEXT NOT NULL, school_year TEXT NOT NULL, reported_by TEXT NOT NULL, remarks TEXT NOT NULL DEFAULT '')");
            statement.execute("INSERT INTO violations (id, student_name, student_id, section, type, offense, action_taken, suspension_days, sanction, date, conference_date, completed_date, status, school_year, reported_by, remarks) SELECT id, student_name, student_id, section, type, offense, action_taken, suspension_days, sanction, date, NULL, completed_date, status, school_year, reported_by, description FROM violations_old");
            statement.execute("DROP TABLE violations_old");
        } else if (!hasRemarks) {
            addViolationColumn(statement, "remarks", "TEXT NOT NULL DEFAULT ''");
        }
    }

    private static void loadSchoolYears() {
        SCHOOL_YEARS.clear();
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT year FROM school_years ORDER BY year DESC")) {
            while (rs.next()) {
                SCHOOL_YEARS.add(rs.getString("year"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load school year data from SQLite.", e);
        }
    }

    private static void addViolationColumn(Statement statement, String column, String definition) throws SQLException {
        try (ResultSet columns = statement.executeQuery("PRAGMA table_info(violations)")) {
            while (columns.next()) {
                if (column.equals(columns.getString("name"))) return;
            }
        }
        statement.execute("ALTER TABLE violations ADD COLUMN " + column + " " + definition);
    }

    // Explicitly persist current in-memory lists to the database. Called on application shutdown.
    public static void saveAll() {
        // Persist current state; individual persist methods are private but callable from within the class.
        persistViolations();
        persistItems();
        persistSchoolYears();
    }

    private static void loadViolations() {
        VIOLATIONS.clear();
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT id, student_name, student_id, section, type, offense, action_taken, suspension_days, sanction, date, conference_date, completed_date, status, school_year, reported_by, remarks FROM violations ORDER BY id ASC")) {
            while (rs.next()) {
                Violation violation = new Violation(
                        rs.getInt("id"),
                        rs.getString("student_name"),
                        rs.getString("student_id"),
                        rs.getString("section"),
                        rs.getString("type"),
                        rs.getString("offense"),
                        rs.getString("action_taken"),
                        rs.getInt("suspension_days"),
                        rs.getString("sanction"),
                        LocalDate.parse(rs.getString("date")),
                        rs.getString("conference_date") == null || rs.getString("conference_date").isEmpty() ? null : LocalDate.parse(rs.getString("conference_date")),
                        rs.getString("completed_date") == null || rs.getString("completed_date").isEmpty() ? null : LocalDate.parse(rs.getString("completed_date")),
                        rs.getString("status"),
                        rs.getString("school_year"),
                        rs.getString("reported_by"),
                        rs.getString("remarks")
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
        Map<Integer, List<String>> photoNamesByItem = new HashMap<>();
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT item_id, file_name FROM lost_item_photos ORDER BY item_id ASC, position ASC")) {
            while (rs.next()) {
                photoNamesByItem.computeIfAbsent(rs.getInt("item_id"), ignored -> new ArrayList<>())
                        .add(rs.getString("file_name"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load lost item photos from SQLite.", e);
        }

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
                item.getPhotoNames().setAll(photoNamesByItem.getOrDefault(item.getId(), List.of()));
                attachLostItemListeners(item);
                ITEMS.add(item);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load lost item data from SQLite.", e);
        }
    }

    private static void attachPersistence() {
        SCHOOL_YEARS.addListener((ListChangeListener<String>) change -> persistSchoolYears());
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

    private static void persistSchoolYears() {
        try (Connection connection = connect();
             PreparedStatement delete = connection.prepareStatement("DELETE FROM school_years");
             PreparedStatement insert = connection.prepareStatement("INSERT INTO school_years (year) VALUES (?)")) {
            delete.executeUpdate();
            for (String year : SCHOOL_YEARS) {
                insert.setString(1, year);
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist school year data to SQLite.", e);
        }
    }

    private static void attachViolationListeners(Violation violation) {
        violation.idProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.studentNameProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.studentIdProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.sectionProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.typeProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.offenseProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.actionTakenProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.suspensionDaysProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.sanctionProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.dateProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.conferenceDateProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.completedDateProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.statusProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.schoolYearProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.reportedByProperty().addListener((obs, oldValue, newValue) -> persistViolations());
        violation.remarksProperty().addListener((obs, oldValue, newValue) -> persistViolations());
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
        item.getPhotoNames().addListener((ListChangeListener<String>) change -> persistItems());
    }

    private static void persistViolations() {
        try (Connection connection = connect(); PreparedStatement delete = connection.prepareStatement("DELETE FROM violations");
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO violations (id, student_name, student_id, section, type, offense, action_taken, suspension_days, sanction, date, conference_date, completed_date, status, school_year, reported_by, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            delete.executeUpdate();
            for (Violation violation : VIOLATIONS) {
                insert.setInt(1, violation.getId());
                insert.setString(2, violation.getStudentName());
                insert.setString(3, violation.getStudentId());
                insert.setString(4, violation.getSection());
                insert.setString(5, violation.getType());
                insert.setString(6, violation.getOffense());
                insert.setString(7, violation.getActionTaken());
                insert.setInt(8, violation.getSuspensionDays());
                insert.setString(9, violation.getSanction());
                insert.setString(10, violation.getDate().toString());
                insert.setString(11, violation.getConferenceDate() == null ? null : violation.getConferenceDate().toString());
                insert.setString(12, violation.getCompletedDate() == null ? null : violation.getCompletedDate().toString());
                insert.setString(13, violation.getStatus());
                insert.setString(14, violation.getSchoolYear());
                insert.setString(15, violation.getReportedBy());
                insert.setString(16, violation.getRemarks());
                insert.addBatch();
            }
            insert.executeBatch();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist violation data to SQLite.", e);
        }
    }

    private static void persistItems() {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deletePhotos = connection.prepareStatement("DELETE FROM lost_item_photos");
                 PreparedStatement deleteItems = connection.prepareStatement("DELETE FROM lost_items");
                 PreparedStatement insertItem = connection.prepareStatement(
                         "INSERT INTO lost_items (id, name, category, date, location, status, claimant, school_year) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                 PreparedStatement insertPhoto = connection.prepareStatement(
                         "INSERT INTO lost_item_photos (item_id, position, file_name) VALUES (?, ?, ?)")) {
                deletePhotos.executeUpdate();
                deleteItems.executeUpdate();
                for (LostItem item : ITEMS) {
                    insertItem.setInt(1, item.getId());
                    insertItem.setString(2, item.getName());
                    insertItem.setString(3, item.getCategory());
                    insertItem.setString(4, item.getDate().toString());
                    insertItem.setString(5, item.getLocation());
                    insertItem.setString(6, item.getStatus());
                    insertItem.setString(7, item.getClaimant());
                    insertItem.setString(8, item.getSchoolYear());
                    insertItem.addBatch();

                    for (int position = 0; position < item.getPhotoNames().size(); position++) {
                        insertPhoto.setInt(1, item.getId());
                        insertPhoto.setInt(2, position);
                        insertPhoto.setString(3, item.getPhotoNames().get(position));
                        insertPhoto.addBatch();
                    }
                }
                insertItem.executeBatch();
                insertPhoto.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to persist lost item data to SQLite.", e);
        }
    }

    public static String copyItemPhoto(int itemId, Path source) throws IOException {
        String originalName = source.getFileName().toString();
        int extensionIndex = originalName.lastIndexOf('.');
        String extension = extensionIndex < 0 ? "" : originalName.substring(extensionIndex).toLowerCase(Locale.ROOT);
        if (!List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp").contains(extension)) {
            extension = "";
        }
        String fileName = UUID.randomUUID() + extension;
        Path itemDirectory = ITEM_PHOTOS_PATH.resolve(Integer.toString(itemId));
        Files.createDirectories(itemDirectory);
        Files.copy(source, itemDirectory.resolve(fileName));
        return fileName;
    }

    public static Path getItemPhotoPath(int itemId, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Invalid lost item photo file name.");
        }
        Path filePath = Paths.get(fileName);
        if (filePath.isAbsolute() || filePath.getNameCount() != 1
                || ".".equals(fileName) || "..".equals(fileName) || fileName.contains(":")) {
            throw new IllegalArgumentException("Invalid lost item photo file name.");
        }
        return ITEM_PHOTOS_PATH.resolve(Integer.toString(itemId)).resolve(fileName);
    }

    public static void deleteItemPhoto(int itemId, String fileName) throws IOException {
        Path photoPath = getItemPhotoPath(itemId, fileName);
        Files.deleteIfExists(photoPath);
        try {
            Files.deleteIfExists(photoPath.getParent());
        } catch (DirectoryNotEmptyException ignored) {
            // Other photos for this item still use the folder.
        }
    }

    public static void deleteItemPhotos(LostItem item) throws IOException {
        for (String fileName : item.getPhotoNames()) {
            deleteItemPhoto(item.getId(), fileName);
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
