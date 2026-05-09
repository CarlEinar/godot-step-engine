# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

> **IMPORTANT: Read `ROADMAP.md` at the start of every session.** It contains the full game design, all implementation phases, their completion status, the altitude milestone table, the satellite sprite list, and the background zone system. Use it as the primary source of truth for what has been built and what comes next.

## Session Rules

1. **Never start a new phase without asking the user for approval first.** Present what the phase involves and wait for a "yes" before writing any code.
2. **Token budget warning:** If context is running low (many tool calls, long responses, large files already read), warn the user before beginning a new phase. Suggest committing current work and resuming in a fresh session.
3. **Commit after each phase** so progress is never lost between sessions.
4. **Placeholder sprites are used until Phase 9.** Do not block implementation on final art — use colored rectangles or simple shapes as stand-ins.

## Tech Stack

- **Game Engine**: Godot 4.x
- **Primary Language**: GDScript
- **Game Type**: 2D (uses Godot's 2D rendering and physics)
- **Project Format**: Godot project files under `Godot_2D/`
- **Primary platform**: Mobile, android
- **Art style**: 8-bit pixel art

## Planning-First Workflow

**All feature requests and tasks must start with planning:**

1. **Initial Clarification Phase**: Before any code is written, a specialized Planning Agent should:
   - Ask the user clarifying questions about the feature/task
   - Understand scope, acceptance criteria, and constraints
   - Identify dependencies with existing game mechanics
   - Determine if research is needed (e.g., learning new Godot APIs, animation techniques, physics behaviors)

2. **User Involvement**: The planning phase is conversational—present options, explain trade-offs, and get explicit approval before proceeding to implementation.

3. **Research & Learning Phase**: If the task requires new skills or knowledge (e.g., implementing particle effects, 2D pathfinding, input handling), a specialized Research Agent should:
   - Study Godot documentation and GDScript patterns
   - Prototype solutions if needed
   - Report findings and recommended approaches to the user

4. **Implementation Planning**: Break down the feature into discrete, testable components:
   - Which scenes need modification or creation
   - Which scripts (nodes/autoloads) are involved
   - How the new code integrates with existing game systems
   - Testing strategy within the Godot editor

5. **Agent Assignment**: Clearly identify which agent will handle each phase (Planning, Research, Implementation, Testing).

## Agent Specialization

When working on this project, agents should specialize by task type:

- **Planning Agent**: Leads clarification conversations, breaks down features, identifies risks. Works with the user to refine scope.
- **Research Agent**: Dives into Godot docs, GDScript patterns, and 2D game design techniques. Learns and reports back.
- **Implementation Agent**: Writes code, creates/modifies scenes, handles debugging and integration.
- **Testing/Integration Agent**: Verifies gameplay, checks scene integration, and ensures no regressions.

## Godot & GDScript Essentials

### Project Structure

```
Godot_2D/
├── project.godot
├── icon.svg
├── addons/
│   └── step_engine/
│       ├── plugin.cfg
│       ├── plugin.gd
│       ├── core/          # StepManager.gd, StepCurrency.gd, StepSaveData.gd (Phase 2-4)
│       ├── ui/            # StepDisplay, CurrencyDisplay, SpendButton (Phase 5)
│       └── android/       # Compiled .aar plugin (Phase 1) — .gdignore prevents Godot import
└── demo/                  # Demo scene (Phase 6)
```

### Environment

- **Godot executable**: `D:\Godot\Godot_v4.6.2-stable_win64.exe\Godot_v4.6.2-stable_win64.exe`
- **Godot project root**: `D:\Claude_code\step game engine\Godot_2D`
- **Shell note**: Always invoke Godot from **bash** (not `cmd /c`) so stdout is captured correctly.

### Common Development Commands

- **Run the game**: Press `F5` in the editor
- **Run a specific scene**: Select a scene in the FileSystem and press `Ctrl+F5`
- **Reload scripts**: Scripts auto-reload on save, but scenes may need refresh
- **Export/build**: Use Project → Export in the editor (requires export templates for target platforms)
- **Debug**: Use the Debugger panel in the editor; add `print()` statements for GDScript logging


### GDScript Development Notes

- **Entry points**: Scenes use `_ready()` (initialization) and `_process(delta)` / `_physics_process(delta)` (per-frame updates)
- **Node signals**: Use `signal_name.emit()` to trigger callbacks; connect with `signal_name.connect(callable)`
- **Scene trees**: Understand parent-child relationships; use `get_parent()`, `get_child()`, `find_child()` to navigate
- **Input handling**: Use `_input()` or `_unhandled_input()` for keyboard/mouse; consider Input action names (configured in Project Settings)
- **Physics**: 2D physics use `CharacterBody2D`, `RigidBody2D`, and collision layers/masks for efficient interactions
- **Resources**: Textures, sounds, and scenes are loaded via paths (e.g., `preload("res://assets/sprites/player.png")`)

## Before Starting Any Task

When a new feature or bug fix is requested:

1. **Planning Agent** asks clarifying questions:
   - What does the player experience/see?
   - How does this interact with existing mechanics?
   - Any edge cases or special states to handle?
   - What's the minimum viable version?

2. **Planning Agent** proposes a breakdown:
   - Which scenes/scripts are involved
   - What new nodes or signals may be needed
   - Estimated complexity and dependencies

3. **User approves** the plan or suggests changes

4. **Research Agent** (if needed) investigates:
   - Relevant Godot APIs or GDScript patterns
   - Best practices for the feature type
   - Similar implementations or examples

5. **Implementation Agent** executes the plan with clear, testable steps

6. **Testing/Integration Agent** verifies in the editor

## Important Considerations

- **Scene instancing**: Reuse scenes for repeated objects (enemies, projectiles, UI elements)
- **Autoloads**: Use for game managers (score, audio, state) that persist across scenes
- **Collision detection**: Layer/mask setup is critical; verify in the Godot editor's Collision section
- **Performance**: 2D games can handle a lot, but monitor Node count and draw calls in the Debugger
- **Version control**: Godot project files (.godot, .godot.bak) are binary; use `.gitignore` for build artifacts and editor cache

## Game Development-Specific Workflow

When implementing gameplay features:

1. **Start in the editor**: Create scenes visually (drag nodes, arrange positions) before writing code
2. **Script one piece at a time**: Write a script for a single behavior, test it in play mode
3. **Use signals for communication**: Avoid tight coupling between systems; emit signals when events occur
4. **Iterate with the user**: Show results, ask if adjustments are needed (speed, timing, visual feedback)
5. **Test edge cases**: What happens when the player does something unexpected? Does the game handle it gracefully?

## Resources for Learning

- Official Godot 4 docs: https://docs.godotengine.org/en/stable/
- GDScript language reference: https://docs.godotengine.org/en/stable/getting_started/scripting/gdscript/
- 2D game tutorials in the docs
