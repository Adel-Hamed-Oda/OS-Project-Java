# OS Project Java
## Expandability be damned, Unleash the spaghetti

## Setup

This project uses JavaFX and expects the environment variable JAVAFX_HOME to point to the JavaFX SDK root directory.

Example:
- Correct: C:/JavaFX/javafx-sdk-17.0.15
- Not correct: C:/JavaFX/javafx-sdk-17.0.15/lib

### 1) Install prerequisites

- JDK 17 or newer
- JavaFX SDK 17+

### 2) Set JAVAFX_HOME

Windows PowerShell (current terminal session):

```powershell
$env:JAVAFX_HOME = "C:/JavaFX/javafx-sdk-17.0.15"
```

Windows (persist for future terminals):

```powershell
[System.Environment]::SetEnvironmentVariable("JAVAFX_HOME", "C:/JavaFX/javafx-sdk-17.0.15", "User")
```

macOS or Linux (bash/zsh):

```bash
export JAVAFX_HOME="/path/to/javafx-sdk-17"
```

### 3) Verify VS Code workspace settings

The workspace settings in [.vscode/settings.json](.vscode/settings.json) are portable and reference:
- ${env:JAVAFX_HOME}
- ${workspaceFolder}

## Run

- Open this folder in VS Code.
- Ensure JAVAFX_HOME is set.
- Run [src/Dashboard.java](src/Dashboard.java) from VS Code, or build/run using your Java extension workflow.

## Notes

- JavaFX JARs can be placed under [lib](lib) and are auto-detected by the workspace settings.
- If JavaFX classes are not found, confirm JAVAFX_HOME points to the SDK root and restart VS Code.