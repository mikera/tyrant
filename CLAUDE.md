# Claude Code Guidelines for Tyrant

## Project Overview

Tyrant is a classic graphical roguelike game, first written in 1997 and extended
intermittently ever since. It is a single-module Maven project written in Java,
using AWT for the UI and a custom 32x32 tileset.

## Build System

Maven project, Java 11 source/target. Single module producing a runnable jar with
main class `mikera.tyrant.QuestApplication`.

See [BUILD.md](BUILD.md) for build, test, run, CI and release details.

Quick start:

```bash
mvn clean install
java -jar target/tyrant-1.0.0-SNAPSHOT.jar
```

## Code Organisation

Flat package layout — most gameplay classes live directly in `mikera.tyrant`.

| Package | Purpose |
|---------|---------|
| `mikera.tyrant` | Gameplay classes (Being, Combat, Hero, Item, Spell, maps, screens, ...) |
| `mikera.tyrant.engine` | Core engine: `Thing`, `BaseObject`, `Map`, `Lib`, `Script`, `RPG` |
| `mikera.tyrant.author` | In-game authoring tools (Designer, MapMaker, ThingEditor, ThingMaker) |
| `mikera.tyrant.util` | Utilities (plug-in loader, metadata, text, exceptions) |

### Key engine concepts

- **`Thing`** (`engine/Thing.java`) — every object on a map is a `Thing`: monsters,
  items, scenery, the hero. `Thing` is `final` for JIT-friendliness and carries an
  arbitrary property map inherited from `BaseObject`. Mutable inventories live on
  `Thing` itself.
- **`Lib`** (`engine/Lib.java`) — object factory. `Lib.create("goblin")` spawns a
  named Thing. Selectors like `"[IsScroll]"` pick a random Thing matching a flag.
- **`Map`** (`engine/Map.java`) — a dungeon level. Things are chained per-square
  via `Thing.next`.
- **Scripts / events** — behaviour is attached via `Script` handlers and events
  (`Event`, `EventHandler`).

### Tests

JUnit 4. Tests mirror `src/main/java` under `src/test/java/mikera/tyrant/test`.
Performance-oriented harnesses live under `mikera.tyrant.perf`.

## Language and Style

- British English throughout (behaviour, colour, centre, organise, ...).
- Concise and precise — the codebase is old and terse; keep new code in that style.
- Prefer editing existing files over creating new ones.

## Running

The game is launched via `mikera.tyrant.QuestApplication`. An AWT `Frame` is
created and the `QuestApp` component handles everything from the main menu on.

Creating a file named `mikeradebug` in the working directory activates debug
cheats — see `QuestApplication.java`.

## Gotchas

- **AWT requires a display.** Some tests touch `QuestApp` and therefore need a
  graphical environment; CI uses `xvfb-run` to provide one. Don't try to mark
  these as headless.
- **Randomness in `Lib.create`.** Selectors like `"[IsScroll]"` return a random
  matching Thing, and some generators unpack into multiple flagged sub-items.
  Tests that count results should use `>= n` rather than `== n` when the input is
  a random selector.
- **`Thing` is intentionally `final`** — don't subclass it. Add behaviour via
  properties and `Script` handlers.

## Dependencies

- `net.mikera:mikera` and `net.mikera:mathz` — the author's own utility libraries
  (pulled from Clojars).
- `com.jgoodies:forms` — Swing form layout (used by authoring tools).
- `junit:junit:4.11` (test scope).

The parent POM is `net.mikera:mikera-pom:0.0.3`.

## Branch Strategy

- `develop` — active development (default branch).
- `master` — releases.
