# Minau

Use JDK 25 and named JPMS modules. Build and launch with the checked-in `cmd/`
argument files; do not introduce Maven, Gradle or class-path fallbacks. Linked source
dependencies live in `lib/src`, deliberate binary modules in `lib/bin`, and generated
classes in ignored `out/`.

Read [README.md](README.md), then the foundation and relevant task links in
[the Minau project skill](skills/maintain-minau/SKILL.md) before changing the project.
Follow that file directly when the agent does not automatically discover local skills.
Use Archaic Java guidance when available; Minau's skill adds local responsibilities
and verification, and its contributor guide supplies the repository conventions.

Preserve both test-contract versions, discovery before execution, and test evidence
independent of application logging. The README owns the canonical commands; the
skill routes to behavior, source and regression checks. Keep detailed guidance in
those references rather than expanding this file.
