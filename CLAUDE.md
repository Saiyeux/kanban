# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a **Kanban Task Management Android Application** built with Kotlin using modern Android development practices. The app provides a full-featured kanban board for task management with drag-and-drop functionality, time tracking, and progress monitoring.

### Core Features
- **Task Management**: Create, edit, delete, and move tasks between columns
- **Kanban Board**: Visual board with customizable columns (To Do, In Progress, Testing, Done)
- **Time Tracking**: Start/stop time tracking for tasks with elapsed time display  
- **Priority System**: Color-coded priority levels (Low, Medium, High, Urgent)
- **Progress Monitoring**: Track estimated vs actual time spent on tasks
- **Cross-Column Movement**: Move tasks between different status columns

### Architecture
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL + KAPT for Room
- **UI Framework**: Android Views with View Binding
- **Architecture Pattern**: MVVM with LiveData and ViewModel
- **Database**: Room with SQLite for local data persistence
- **Async Programming**: Kotlin Coroutines and Flow
- **Minimum SDK**: 34 (Android 14)
- **Target SDK**: 36 (Android 15 compatible)

### Key Dependencies
- **Room Database**: Local data persistence with relationships
- **Kotlin Coroutines**: Asynchronous programming
- **AndroidX Lifecycle**: ViewModel, LiveData for reactive UI
- **Material Design Components**: Modern UI components
- **RecyclerView**: Efficient list rendering for tasks
- **Fragment KTX**: Modern fragment management

## Development Commands

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run all tests
./gradlew check
```

### Code Quality
```bash
# Run lint checks
./gradlew lint

# Run lint and generate report
./gradlew lintDebug
```

### Installation
```bash
# Install debug APK on connected device/emulator
./gradlew installDebug
```

## Project Structure

### Main Source Structure
- `app/src/main/java/com/example/kanban/` - Main Kotlin source code
  - `KanbanApplication.kt` - Application class with dependency injection setup
  - `MainActivity.kt` - Entry point hosting KanbanFragment
  - `data/` - Data layer with Room database
    - `entity/` - Room entities (Task, KanbanColumn, TimeLog)
    - `dao/` - Data Access Objects for database operations
    - `database/` - Room database setup and configuration
    - `repository/` - Repository pattern for data access
    - `converter/` - Type converters for Room (LocalDateTime)
  - `ui/` - UI layer organized by feature
    - `kanban/` - Main kanban board (Fragment + ViewModel)
    - `adapter/` - RecyclerView adapters (TaskAdapter)
    - `dialog/` - Dialog fragments (CreateTaskDialog, TaskDetailDialog)
    - `drag/` - Drag and drop helper classes
- `app/src/main/res/` - Android resources
  - `layout/` - XML layouts for fragments, dialogs, and items
  - `drawable/` - Vector icons and graphics
  - `values/` - Strings, colors, dimensions, themes

### Database Schema
- **tasks** table: Task entities with foreign key to columns
- **kanban_columns** table: Column definitions with positions and colors
- **time_logs** table: Time tracking entries linked to tasks
- **Relationships**: Tasks belong to columns, time logs belong to tasks

### Build Configuration
- `build.gradle.kts` (project level) - Root build configuration
- `app/build.gradle.kts` - App module with Room and Coroutines
- `gradle/libs.versions.toml` - Version catalog with latest dependencies
- `gradle.properties` - Gradle properties and JVM settings

## Development Notes

- **MVVM Architecture**: ViewModels manage UI state, Repository handles data
- **Room Database**: Automatic database creation with default columns on first run
- **Time Tracking**: Background coroutine updates elapsed time display
- **Reactive UI**: LiveData observers update UI automatically when data changes
- **Material Design 3**: Uses latest Material Design components and theming
- **Type Safety**: View Binding eliminates findViewById calls
- **Error Handling**: Database operations wrapped in try-catch with proper error states