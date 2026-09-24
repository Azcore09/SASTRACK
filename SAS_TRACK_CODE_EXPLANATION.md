# SAS-TRACK Code and Technology Explanation

## 1. What SAS-TRACK Is

SAS-TRACK is a desktop school management system. It helps the school manage:

- Student violation records
- Lost and found items
- School-year records
- User login and roles
- Dashboard statistics and charts
- Searching and filtering

The application is mainly written in Java and uses JavaFX for the user interface, FXML for screen layouts, CSS for design, SQLite for data storage, and Gradle for building and packaging the project.

## 2. Programming Languages and Technologies

### Java

Java is the main programming language. It controls what the application does, including:

- Starting the program
- Opening screens
- Checking login details
- Adding, editing, and deleting records
- Searching and filtering records
- Creating dashboard statistics
- Connecting to the database
- Checking user permissions

### JavaFX

JavaFX is a Java framework for creating desktop interfaces. It provides buttons, text fields, labels, tables, combo boxes, charts, dialogs, windows, and animations.

### FXML

FXML is an XML-based language used to describe JavaFX screen layouts. It defines where controls are placed and connects them to Java controller methods.

Example:

```xml
<Button text="SIGN IN" onAction="#handleLogin"/>
```

This creates a button labeled `SIGN IN` and runs the Java method `handleLogin` when it is clicked.

### CSS

CSS controls the appearance of the JavaFX application. It defines colors, fonts, spacing, borders, backgrounds, rounded corners, hover effects, table styles, and button styles.

The main CSS file is:

```text
src/main/resources/com/sastrack/css/app.css
```

### SQLite and SQL

SQLite is the local database used by the application. The database file is:

```text
data/sastrack.db
```

SQL commands create tables, read records, delete records, and insert records. Java connects to SQLite using the SQLite JDBC library.

### Gradle

Gradle manages the project. It downloads dependencies, compiles Java files, runs the application, runs tests, and packages the application.

The Gradle configuration is in:

```text
build.gradle
```

The project uses Java 17, JavaFX 21.0.2, SQLite JDBC, and JUnit.

## 3. Project Structure

### Java source folder

```text
src/main/java
```

This contains the Java application code.

### Resource folder

```text
src/main/resources
```

This contains FXML files, CSS files, logos, and images.

### Database folder

```text
data
```

This contains the SQLite database.

### Build folder

```text
build
```

This contains generated compiled and packaged files. Developers normally do not edit this folder manually.

## 4. Application Startup

### `Launcher.java`

`Launcher.java` contains the Java `main` method. It starts `MainApp`. A separate launcher helps avoid JavaFX runtime problems when the program is run from a packaged application.

### `MainApp.java`

`MainApp.java` is the main JavaFX application class. It:

1. Creates the main window.
2. Sets the window title.
3. Maximizes the window.
4. Shows the login screen.
5. Changes to the main screen after login.
6. Saves data when the application closes.

Important methods:

- `start()` starts JavaFX.
- `showLogin()` loads `login.fxml`.
- `showMain()` loads `main.fxml`.
- `stop()` saves current data before closing.

## 5. Login and User Roles

### `User.java`

This model represents a user. It stores:

- Username
- Password
- Role

The main roles are:

- `SAS Head`
- `SAS Staff`

The `isAdmin()` method checks whether the user is an SAS Head.

### `Session.java`

`Session.java` remembers the user who is currently logged in.

Important methods:

```java
Session.login(user);
Session.getCurrentUser();
Session.logout();
```

The session is used to display the logged-in user and restrict administrator-only actions such as deleting records.

### `LoginController.java`

This controller handles the login form. It:

1. Reads the username and password.
2. Searches the list of users.
3. Compares the entered details with the stored details.
4. Logs in the matching user.
5. Opens the main screen.
6. Shows an error when the details are incorrect.

The login screen also contains fade and shake animations.

## 6. Main Navigation

### `main.fxml`

This is the main layout after login. It contains:

- Left sidebar
- Top search bar
- Logged-in user label
- Main content area
- Logout button

### `MainController.java`

This controller handles navigation. It loads different FXML screens into the content area:

- Dashboard
- Violations
- Lost and Found
- Search results

It also controls:

- Sidebar collapse and expansion
- Lost and Found submenu
- Violations submenu
- Search
- Logout

When the sidebar is collapsed, text labels are hidden and icons remain visible.

## 7. Data Model Classes

Model classes describe the information stored by the system.

### `Violation.java`

This represents a student violation. It contains:

- ID
- Student name
- Student ID
- Section
- Violation type
- Offense number
- Action taken
- Suspension days
- Sanction
- Date reported
- Date completed
- Status
- School year
- Reported by
- Description

Possible statuses include:

- Pending
- Under Review
- Resolved

The class uses JavaFX properties. These allow the interface and database listeners to notice when values change.

### `LostItem.java`

This represents a lost and found record. It contains:

- ID
- Item name
- Category
- Date found
- Location
- Status
- Claimant
- School year

Possible statuses are `Unclaimed` and `Claimed`.

### `SearchResult.java`

This is a simplified model used by the search results table. It contains:

- Record type
- Reference
- Details
- Date
- Status

### `ViolationCatalog.java`

This stores the official violation sections and the violations under each section. It helps ensure that users select a valid violation for the selected section.

It can:

- Return all sections
- Return violations for one section
- Return all violations
- Find the section belonging to a violation

## 8. Database and `DataStore.java`

`DataStore.java` is responsible for storing and loading application data. It manages:

- Users
- School years
- Violations
- Lost and found items
- SQLite connections
- Loading records
- Saving records

It contains observable lists:

```java
USERS
SCHOOL_YEARS
VIOLATIONS
ITEMS
```

An `ObservableList` can notify the program when records are added or removed.

### Database tables

The application creates these tables:

#### `violations`

Stores student violation information such as the student, violation type, status, dates, and school year.

#### `lost_items`

Stores item name, category, location, status, claimant, and school year.

#### `school_years`

Stores school-year values such as `2025-2026`.

### Loading data

When the application starts, `DataStore`:

1. Creates the database folder if needed.
2. Creates missing database tables.
3. Loads school years.
4. Loads violations.
5. Loads lost and found records.
6. Places the records into observable lists.

### Saving data

When a record is added, deleted, or changed, persistence listeners save the information to SQLite. For example, when the student name property changes, the violation data is saved.

When the application closes, `DataStore.saveAll()` saves all current data.

## 9. Dashboard

### `DashboardController.java`

The dashboard displays:

- Total records
- Lost and found count
- Active violations
- Resolved cases
- Violation chart
- School-year filter

The statistics are calculated by filtering records for the selected school year and counting the results.

### Dashboard cards

The cards are clickable. They open the related records:

- All records
- Lost and found items
- Active violations
- Resolved violations

The cards also grow slightly when the mouse moves over them.

### Chart

The chart can display violations:

- By month for the selected school year
- By year across all school years

The chart uses JavaFX `BarChart`, `CategoryAxis`, and chart series classes.

### School-year management

The dashboard allows the user to:

- Select a school year
- Add a school year
- Delete a school year

Deleting a school year also removes records belonging to that year after confirmation.

## 10. Violations Page

### `ViolationsController.java`

This controller manages the violations screen. It supports:

- Viewing violation records
- Searching by student name or ID
- Filtering by section
- Filtering by status
- Filtering by school year
- Adding violations
- Editing violations
- Deleting violations
- Unselecting a table row

### Violation table

The table uses `PropertyValueFactory` to connect each column to a property in `Violation.java`.

For example:

```java
colName.setCellValueFactory(
        new PropertyValueFactory<>("studentName"));
```

This tells the table to display the student name in that column.

### Adding a violation

The add dialog:

1. Collects student information.
2. Lets the user select a section.
3. Updates the violation choices based on the section.
4. Validates required fields.
5. Creates a new `Violation` object.
6. Adds it to the observable list.
7. Saves it to the database.

Student name, section, violation, and date reported are required.

### Editing a violation

When a row is selected and edited:

1. Existing values are placed in a dialog.
2. The user changes the values.
3. The system validates the input.
4. The existing object is updated.
5. Property listeners save the changes.
6. The table is refreshed.

### Deleting a violation

Only an SAS Head account can delete a violation. The system checks the current session, asks for confirmation, removes the record, and saves the updated list.

## 11. Lost and Found Page

### `LostFoundController.java`

This controller manages lost and found records. It supports:

- Viewing items
- Searching by item name or claimant
- Filtering by status
- Filtering by school year
- Adding items
- Editing items
- Deleting items

When an item is unclaimed, the claimant field is disabled. When it is claimed, the claimant field becomes available.

Only an SAS Head account can delete an item.

## 12. Search

### `SearchController.java`

The main search bar allows the user to select either:

- Violations
- Lost & Found

For violations, the system searches student name and student ID.

For lost and found, it searches item name and claimant.

The results are placed into `SearchResult` objects and displayed in a table.

## 13. FXML Files

### `login.fxml`

Defines the login screen, school branding, username field, password field, sign-in button, and error label.

### `main.fxml`

Defines the sidebar, top bar, search controls, user label, logout button, and content area.

### `dashboard.fxml`

Defines the dashboard cards, school-year controls, and violation chart.

### `violations.fxml`

Defines the violation search field, filters, buttons, and violations table.

### `lostfound.fxml`

Defines the lost and found search field, filters, buttons, and items table.

### `search.fxml`

Defines the search results table.

## 14. CSS Design

`app.css` provides the visual design of the system. It controls:

- Login background and branding
- Blue and dark navy color scheme
- Sidebar appearance
- Button styles
- Hover and pressed effects
- Table row colors
- Dashboard cards
- Charts
- Text sizes
- Borders and spacing

The main button styles are:

- `btn-primary` for main actions
- `btn-outline` for normal actions
- `btn-danger` for delete actions

## 15. Gradle Configuration

`build.gradle` configures the project as a Java application. It:

- Applies the application plugin.
- Adds the JavaFX plugin.
- Sets Java 17.
- Adds JavaFX modules.
- Adds SQLite JDBC.
- Adds JUnit for tests.
- Defines the application launcher.
- Creates packaged application files.

The application can be run with:

```text
gradlew run
```

The application image can be packaged with:

```text
gradlew jpackageAppImage
```

## 16. Complete Data Flow

The general data flow is:

```text
User action
    ↓
FXML control
    ↓
Controller method
    ↓
Model object or DataStore list
    ↓
SQLite database
```

For example, editing a violation works as follows:

1. The user clicks `Edit selected`.
2. The FXML screen calls the controller method.
3. The controller opens the edit dialog.
4. The user changes the student name.
5. The controller updates the `Violation` object.
6. The JavaFX property listener detects the change.
7. `DataStore` saves the records to SQLite.
8. The table refreshes and displays the new name.

## 17. Complete User Flow

1. The application opens.
2. The login screen appears.
3. The user enters a username and password.
4. The system checks the credentials.
5. The user is logged in.
6. The dashboard appears.
7. The user selects a school year.
8. The user views statistics and charts.
9. The user opens Violations or Lost & Found.
10. The user searches or filters records.
11. The user adds or edits a record.
12. The record is stored in memory and saved to SQLite.
13. The user logs out or closes the program.
14. The records remain in `data/sastrack.db`.

## 18. Current Limitations

The current system has these limitations:

1. Passwords are stored as plain text in the source code.
2. Demo users are hard-coded.
3. SQLite is used instead of a shared server database.
4. The system is designed for local desktop use.
5. Database persistence rewrites the table instead of updating only one row.
6. Some lost and found fields are not fully editable.
7. There is no advanced user-management screen.
8. There are no audit logs.
9. Automated tests are limited.

## 19. Future Improvements

Possible future improvements include:

- Hashing passwords
- Adding user management
- Connecting to Oracle or MySQL
- Adding audit logs
- Exporting reports to PDF or Excel
- Adding backups and restore functions
- Improving database update operations
- Adding more validation
- Adding more automated tests
- Creating a web or mobile version

## 20. Simple Capstone Explanation

SAS-TRACK is a Java-based desktop school management system developed using JavaFX. FXML is used to arrange the user interface, while CSS controls the visual design. Java controllers handle login, navigation, searching, filtering, adding, editing, and deleting records. Model classes represent users, student violations, and lost and found items. SQLite is used as the local database for permanent storage. JavaFX observable properties allow the interface and database-saving functions to respond when record values change. Gradle manages dependencies, compilation, testing, and application packaging.
