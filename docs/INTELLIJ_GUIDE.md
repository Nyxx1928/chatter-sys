# Welcome to IntelliJ IDEA (from VS Code)

Coming from VS Code, the first thing you'll notice is that IntelliJ IDEA is an **IDE (Integrated Development Environment)**, not just a text editor. While VS Code is modular and lightweight, IntelliJ comes "batteries-included" with deep understanding of your code.

## 1. Project Structure & Architecture

In IntelliJ, your workspace is called a **Project**. Within a project, you can have one or more **Modules** (small sub-projects).

### Current Folder Structure (Standard Maven)
Your project follows the standard Maven layout:
- **`.idea/`**: This folder contains IntelliJ-specific settings (run configurations, code styles, etc.). In VS Code, this was `.vscode/`. **Do not delete this.**
- **`pom.xml`**: The heart of your project. It defines dependencies, build plugins, and project metadata. Equivalent to `package.json` in Node.js.
- **`src/main/java/`**: Your production Java source code.
    - `org/example/`: This is your **package** structure. In Java, folders match packages.
- **`src/main/resources/`**: (If created) Non-code files like config files, images, or SQL scripts.
- **`src/test/java/`**: (If created) Your unit tests.
- **`target/`**: The output folder where Maven puts compiled `.class` files and packaged `.jar` files. Equivalent to `dist/` or `build/`.

### Suggested Folder Structure for Future
As your project grows, consider this clean architecture:
```text
my-project/
├── .idea/              # IDE settings
├── src/
│   ├── main/
│   │   ├── java/       # Production code
│   │   │   └── com/yourname/app/
│   │   │       ├── controller/  # Web/API entry points
│   │   │       ├── service/     # Business logic
│   │   │       ├── repository/  # Database access
│   │   │       └── model/       # Data classes
│   │   └── resources/  # Configs (application.properties, SQL)
│   └── test/           # Mirror of main/java for tests
├── pom.xml             # Maven configuration
└── README.md
```

---

## 2. The Database Tool

IntelliJ has a powerful built-in Database client. You don't need external tools like DBeaver or TablePlus.

### How to use it:
1. **Open the Tool Window**: On the far right edge of the IDE, click the **Database** icon.
2. **Add a Source**: Click the **+** (New) button -> **Data Source**. Choose your DB (MySQL, PostgreSQL, SQLite, etc.).
3. **Download Drivers**: If IntelliJ shows "Missing driver files," click the blue **Download** link in the setup window.
4. **Connect**: Enter your credentials and click "Test Connection".
5. **Querying**: Right-click your connection -> **Query Console**. You can write SQL here and run it with `Ctrl+Enter` (Cmd+Enter on Mac).
6. **GUI Editing**: Double-click a table to see a spreadsheet-like view where you can edit data directly.

---

## 3. Maven Mastery

Maven handles two main things: **Dependencies** (libraries) and **Lifecycle** (build/test/package).

### Managing Dependencies
To add a library (like Jackson for JSON or JUnit for testing):
1. Find the dependency on [Maven Central](https://mvnrepository.com/).
2. Paste it into your `pom.xml` inside a `<dependencies>` block.
3. **Crucial**: After editing `pom.xml`, click the **"Load Maven Changes"** icon (a small floating icon with a blue circle) or open the Maven Tool Window and click the Refresh icon.

### The Maven Tool Window
Look at the right sidebar (near Database). The Maven window shows:
- **Lifecycle**: 
    - `clean`: Deletes the `target` folder.
    - `compile`: Compiles your code.
    - `test`: Runs your unit tests.
    - `package`: Bundles your code into a `.jar` file.
- **Dependencies**: A tree view of everything your project relies on.

---

## 4. Pro Tips for VS Code Switchers

- **Search Everything**: Press `Shift` twice. This is your Command Palette.
- **Context Actions**: Press `Alt+Enter`. This is the "magic fix" button for errors and suggestions.
- **No Manual Save**: IntelliJ saves automatically.
- **Project Structure**: Press `Ctrl+Alt+Shift+S` to manage SDKs (JDK versions) and Modules.
- **Terminal**: `Alt+F12` opens the built-in terminal.

Happy coding!
