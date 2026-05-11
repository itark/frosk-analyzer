---
name: restart-frosk
description: Restart the frosk-analyzer Spring Boot application. Use this skill whenever the user types or says "restart frosk", "frosk restart", "kick frosk", "bounce frosk", "reload frosk", or anything that asks to stop and start the frosk-analyzer app — even casual phrasings. Stops any running instance, activates the project's pinned Java version via SDKMAN (`sdk env`), and starts a fresh run with `mvn spring-boot:run` in the user's Terminal.
---

# Restart Frosk

This skill restarts the frosk-analyzer Spring Boot app on the user's Mac. The work happens on the user's machine, so you must use **computer-use** tools — not the workspace bash sandbox, which runs in a different filesystem and cannot see or kill the user's processes.

## What "restart" means here

Frosk binds to a port (Spring Boot default 8080), so you cannot have two instances running. A restart is therefore: stop any running instance → activate the right Java toolchain → start fresh.

The project ships a single helper script that does all of this in one shot:

- **`/Users/fredrikmoller/itark/git/frosk-analyzer/restart-frosk.command`**

That `.command` file is a bash script that on macOS opens in Terminal when launched. It:

1. `cd`s to the project,
2. kills any running `java … frosk-analyzer` process and frees port 8080,
3. sources SDKMAN and runs `sdk env` (which reads `.sdkmanrc` → Java `21.0.6-librca`),
4. prints the active `java --version`,
5. `exec`s `mvn spring-boot:run` so Ctrl+C in that Terminal stops the app.

So your job is small: get macOS to launch that file. Don't reinvent the workflow inside the skill — the script is the source of truth.

## How to launch it

Use computer-use. Native macOS apps require it; the workspace bash sandbox is a different machine.

### Step 1 — request access

Call `request_access` with `["Terminal", "Finder"]` before any other computer-use action. You may not need Finder, but request it up front so you don't have to interrupt the flow if the primary path fails.

### Step 2 — launch the .command file

Try these in order. Stop at the first one that works.

**Path A — `open_application` with the file path (preferred).**
On macOS, `open_application` can accept a file path; the OS opens the file with its default handler, and `.command` files are handled by Terminal. So:

```
open_application(path_or_name="/Users/fredrikmoller/itark/git/frosk-analyzer/restart-frosk.command")
```

If this brings up a Terminal window that starts running the script, you're done — skip to Step 3.

**Path B — Finder double-click (fallback).**
If Path A errors or nothing happens after a screenshot:

1. `open_application("Finder")`
2. Press `Cmd+Shift+G` (Go to Folder), type `/Users/fredrikmoller/itark/git/frosk-analyzer`, press Return. Finder is full-tier so typing and key presses work here.
3. Find `restart-frosk.command` in the listing and `double_click` it. macOS will open it in Terminal.

Do **not** try to type into Terminal directly. Terminal is a tier-"click" app — `type`, `key`, `right_click`, and modifier-clicks are blocked. That's why we launch a `.command` file instead of opening Terminal and trying to paste commands.

### Step 3 — confirm it's starting

Take a `screenshot`. You're looking for a Terminal window showing the script's output — the "==> Stopping…", "==> Loading SDKMAN…", and eventually `mvn spring-boot:run` Maven output. Spring Boot's banner usually appears within 10–30 seconds; you don't need to wait for it before reporting back, but a screenshot showing the script has begun is a good confirmation.

If you see compilation errors or `BUILD FAILURE`, surface that to the user verbatim — don't try to fix it from inside this skill.

## Edge cases

- **`restart-frosk.command` is missing.** This skill assumes the script exists at the path above. If it doesn't, tell the user — don't silently fall back to running raw commands (Terminal's tier blocks typing, so you can't recover that way anyway). The script lives in the repo and is checked in alongside this skill.
- **SDKMAN not installed.** The script will print a message pointing at https://sdkman.io and exit. Surface that to the user.
- **Port 8080 still occupied.** The script tries `lsof -ti tcp:8080 | xargs kill -9` as a backstop. If `mvn spring-boot:run` still complains about the port, something outside the frosk java process is holding it (Docker, another service). Tell the user — don't start hunting it down inside this skill.
- **Multiple Terminal windows already open.** That's fine. The `.command` file always opens a fresh Terminal window of its own; you don't have to find or reuse an existing session.

## Why a skill and not just a shell command

The user works in Cowork mode, where typing "restart frosk" should Just Work. A skill gives Claude a clear, single way to do this — no improvising with Spotlight, no risk of pasting into the wrong window, no second-guessing which Java version to use. The actual work is delegated to a checked-in shell script so the behavior stays identical whether triggered from this skill, double-clicked from Finder, or run from a real terminal.
