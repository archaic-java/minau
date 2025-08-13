# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Minau is a Java test runner that executes each test in its own virtual thread. It provides an implementation for test interfaces defined in `work.archaic.service.catalog`.

## Build System

The project uses Taskfile (task) for build automation. All build commands are defined in `Taskfile.yaml`.

### Essential Commands

- **Compile**: `task compile` - Compiles the main module
- **Lint**: `task lint` - Compiles with all linting checks enabled (-Xlint:all, -Xdoclint)
- **Test**: `task test` - Builds and runs tests using Minau test runner
- **Format**: `task format` - Formats all Java source files using Google Java Format
- **Run**: `task run` - Runs the main application for exploratory testing
- **Tags**: `task tags` - Generates ctags index for Java symbols

### Key Build Configuration

- **Java Version**: Release 24
- **Main Module**: `work.archaic.minau`
- **Test Module**: `work.archaic.minau.test`
- **Logging Module**: `work.archaic.jules`
- **Module Path**: `jar/bin` contains external dependencies (currently SLF4J)

## Architecture

### Module Structure

The project follows Java Platform Module System (JPMS):
- Module definition: `src/work.archaic.minau/module-info.java`
- Provides: `work.archaic.service.test` service
- Requires: `work.archaic.service.catalog`, `work.archaic.jules`

### Core Components

1. **Main.java**: Entry point that:
   - Enforces assertions are enabled (`-ea` flag required)
   - Parses command-line arguments (targets and output directory)
   - Orchestrates test discovery and execution

2. **ModuleScanner.java**: Discovers tests from specified modules

3. **TestExecutor.java**: Executes discovered tests (planned to use virtual threads)

4. **TestDescriptor.java**: Represents test metadata

5. **Reporter.java**: Handles test result reporting

6. **Result.java**: Tracks test execution statistics

### Logging Strategy

The project uses SLF4J for structured logging with specific conventions:
- **INFO**: Generic status (test start/end)
- **DEBUG**: Minau internal debugging
- **WARNING**: Recoverable error conditions with fallback
- **ERROR**: Unrecoverable errors

## Development Notes

### Current TODOs (from README)
1. Replace System.out/System.err with proper logger usage
2. Implement parallel test execution using virtual threads

### Testing Approach

Tests are discovered and executed through the Minau runner itself. The test module (`work.archaic.minau.test`) should be used for testing Minau functionality.

### Code Formatting

The project uses Google Java Format (v1.28.0) stored in `tools/`. Always run `task format` before committing code changes.

### Module Compilation

When compiling, note the typo in module-info.java line 3: `work.arachaic.jules` should likely be `work.archaic.jules`. This may need correction for successful compilation.