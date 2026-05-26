```markdown
# Sokobot

Sokobot is an autonomous puzzle-solving application designed to navigate and solve Sokoban warehouse puzzles. It utilizes structured graph search algorithms, state representation engines, and optimization techniques to find efficient paths and push boxes to their designated target locations.

---

## Project Structure

The codebase is organized as a standard Java project package:

```text
sokobot/
├── src/
│   ├── gui/         # Graphical User Interface components (GameFrame, GamePanel, BotThread)
│   ├── main/        # Core execution entry point (Driver)
│   ├── reader/      # Level parser and map config handlers (FileReader, MapData)
│   └── solver/      # AI engine, search algorithms, state tracking (SokoBot)
├── .gitignore       # Prevents compiled bytecode (.class) from tracking
├── sokobot          # Run script execution file
└── README.md        # Project setup and contribution guidelines

```

---

## Requirements & Setup

### Prerequisites

* **Java Development Kit (JDK):** Version 11 or higher recommended.
* **Terminal/IDE:** VS Code, IntelliJ IDEA, or a configured PowerShell terminal.

### Compiling and Running via Terminal

To compile and execute the project from the root directory (`sokobot/`), use the following commands:

1. **Run a map level:**
Execute the run script followed by the specific map configuration file you want to load.

```powershell
   ./sokobot <map_name>

```

*Example:* `./sokobot map1` or `./sokobot maps/level1.txt` *(depending on your map folder layout).*

---

## Collaborator Workflow & Branching Policy

To maintain a stable, working codebase, direct commits to the `develop` branch are strictly prohibited. **All team members must develop features, trial solutions, or bug fixes on dedicated working branches.**

### Main Branch

* `develop`: The primary tracking branch holding the integration code where new components and trial solver features are gathered, run, and verified.

---

## Step-by-Step Development Guide

Follow these steps every time you work on a new feature or algorithm optimization:

### 1. Synchronize Local Files

Before starting any new work, update your local `develop` branch to prevent downstream merge conflicts.

```powershell
git checkout develop
git pull origin develop

```

### 2. Create a Working Branch

Create and switch to a descriptive branch name branching off from `develop` using a standard prefix (`feature/`, `fix/`, or `trial/`):

```powershell
# Format: git checkout -b <prefix>/<short-description>
git checkout -b trial/optimize-solver-heuristics

```

### 3. Implement and Stage Changes

As you develop inside VS Code, stage and commit logical milestones.

> **Important:** Do not commit compiled `.class` bytecode files. Ensure your root `.gitignore` file contains `*.class` before running Git tasks.

```powershell
# Stage a specific file
git add src/solver/SokoBot.java

# Or stage all source modifications safely
git add .

```

### 4. Commit Changes

Write concise, descriptive commit summaries:

```powershell
git commit -m "Implement Zobrist hashing for dead-state pruning"

```

### 5. Push Online and Open a Pull Request

Push your completed branch to the remote repository:

```powershell
git push -u origin trial/optimize-solver-heuristics

```

Once pushed:

1. Navigate to the repository on your Git host web client.
2. Open a **Pull Request (PR)**.
3. Ensure the target destination is set to **`develop`**.
4. Assign a teammate to review and run your solution before integration.

---

## Code Quality & Repository Rules

* **Atomic Commits:** Keep your commits small and focused on a single logical task.
* **Never commit generated binaries:** If you accidentally track any `.class` files, clean them from your directory structure and do not push them.
* **Stay Updated:** If your feature branch takes several days to build, periodically pull the remote `develop` branch into your active branch to integrate concurrent team updates:

```powershell
  git fetch origin
  git merge origin/develop

```

```

```
