package com.sastrack.data;

import com.sastrack.model.LostItem;
import com.sastrack.model.User;
import com.sastrack.model.Violation;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;

/**
 * In-memory sample data. This class is deliberately isolated (no JavaFX
 * controller code touches raw lists) so it can be swapped for a real
 * Oracle-backed repository layer later without changing the UI code.
 */
public final class DataStore {

    public static final ObservableList<User> USERS = FXCollections.observableArrayList(
            new User("j.delacruz", "admin123", "SAS Head"),
            new User("m.fernandez", "staff123", "SAS Staff")
    );

    public static final ObservableList<Violation> VIOLATIONS = FXCollections.observableArrayList(
            new Violation(1, "Miguel Santos", "24-00050", "Grade 10 - Rizal", "Minor Offense",
                    LocalDate.of(2026, 8, 10), "Pending", "2026-2027", "J. Dela Cruz", "Improper uniform."),
            new Violation(2, "Andrea Reyes", "23-00981", "Grade 12 - STEM B", "Minor Offense",
                    LocalDate.of(2026, 8, 11), "Resolved", "2026-2027", "M. Fernandez", "Late for first period, third instance."),
            new Violation(3, "Bea Villanueva", "24-00202", "Grade 11 - HUMSS A", "Serious Offense",
                    LocalDate.of(2026, 8, 12), "Pending", "2026-2027", "M. Fernandez", "Argued with subject teacher during class."),
            new Violation(4, "Carlos Ramirez", "25-00077", "Grade 9 - Bonifacio", "Serious Offense",
                    LocalDate.of(2026, 8, 14), "Under Review", "2026-2027", "J. Dela Cruz", "Involved in a physical altercation."),
            new Violation(5, "Nicole Torres", "24-00156", "Grade 12 - STEM C", "Minor Offense",
                    LocalDate.of(2026, 8, 15), "Pending", "2026-2027", "M. Fernandez", "Third tardy incident this month."),
            new Violation(6, "Patrick Tagle", "24-00301", "Grade 10 - Luna", "Major Offense",
                    LocalDate.of(2026, 8, 16), "Resolved", "2026-2027", "J. Dela Cruz", "Vandalism on classroom armchair."),
            new Violation(7, "Miguel Santos", "24-00050", "Grade 10 - Rizal", "Major Offense",
                    LocalDate.of(2025, 11, 3), "Resolved", "2025-2026", "J. Dela Cruz", "Cheating during a quarterly exam."),
            new Violation(8, "Andrea Reyes", "23-00981", "Grade 12 - STEM B", "Minor Offense",
                    LocalDate.of(2025, 9, 20), "Resolved", "2025-2026", "M. Fernandez", "Unauthorized use of phone in class."),
            new Violation(9, "Diego Ocampo", "23-00450", "Grade 11 - HUMSS B", "Serious Offense",
                    LocalDate.of(2025, 10, 14), "Resolved", "2025-2026", "J. Dela Cruz", "Bullying complaint from a classmate.")
    );

    public static final ObservableList<LostItem> ITEMS = FXCollections.observableArrayList(
            new LostItem(1, "Black Jansport backpack", "Bag", LocalDate.of(2026, 8, 5), "Canteen",
                    "Claimed", "Miguel Santos", "2026-2027"),
            new LostItem(2, "Casio scientific calculator", "Stationery", LocalDate.of(2026, 8, 7), "Room 204",
                    "Unclaimed", "", "2026-2027"),
            new LostItem(3, "Blue school ID lanyard", "ID / Documents", LocalDate.of(2026, 8, 9), "Gymnasium",
                    "Unclaimed", "", "2026-2027"),
            new LostItem(4, "Wired earphones (white)", "Electronics", LocalDate.of(2026, 8, 11), "Library",
                    "Claimed", "Andrea Reyes", "2026-2027"),
            new LostItem(5, "Grey school vest, size M", "Clothing", LocalDate.of(2026, 8, 13), "Covered court",
                    "Unclaimed", "", "2026-2027"),
            new LostItem(6, "Red umbrella", "Other", LocalDate.of(2025, 9, 2), "Main gate",
                    "Claimed", "Diego Ocampo", "2025-2026"),
            new LostItem(7, "Silver wristwatch", "Accessories", LocalDate.of(2025, 12, 1), "Chapel",
                    "Unclaimed", "", "2025-2026")
    );

    private static int nextViolationId = 10;
    private static int nextItemId = 8;

    public static int nextViolationId() { return nextViolationId++; }
    public static int nextItemId() { return nextItemId++; }

    private DataStore() {}
}
