---
name: maintain-minau
description: "Understand, maintain, extend, diagnose, or review the work.archaic.minau Java test runner. Use for changes to suite discovery, v01/v02 execution, case registration, CLI selection, failure trails, reporting, regression fixtures, or Minau documentation. Complements Archaic Java with repository-specific guidance."
---

# Maintain Minau

Use this as the shared starting point for human contributors and coding agents.
Read the foundation, choose the relevant task below, then follow that reference's
source links. Load other references only when the change crosses their boundaries.
For a first test or the repository commands, start with the [README](../../README.md).

## Establish the foundation

- Use a full JDK 25, named JPMS modules and the checked-in `cmd/` argument files.
  Read applicable `AGENTS.md` instructions and inspect the worktree before editing.
- Keep tests dependent on service-catalog contracts rather than runner classes.
  Preserve existing v01 annotated suites alongside v02 registered cases.
- Validate all requested modules before constructing suites or executing cases.
  Keep discovery failures distinct from construction/registration and case failures.
- Preserve independent concurrent case execution, registration identity, and failure
  evidence. Keep test trails independent of application logging.
- Use Archaic Java for shared conventions when available; use this repository for
  local mechanics. Read the contributor guide's conventions when that skill is
  unavailable. Do not require humans to install an agent tool to access the rules.

## Choose the task

| Task | Read | Check before finishing |
|---|---|---|
| Write a suite or design case fixtures | [Writing tests](references/writing-tests.md) | Immutable inputs, per-case resources, inline assertions and module access. |
| Configure modules or change discovery | [Discovery](references/discovery.md) | Exploded modules and JARs, access/linkage diagnostics, and no execution after discovery failure. |
| Run/list tests or change CLI selection | [CLI](references/cli.md) | Listing, duplicate ordinals, invalid selections, exit codes and excluded constructors. |
| Investigate evidence or change reporting | [Diagnostics](references/diagnostics.md) | Trail ownership/bounds, discarded success evidence, original failures and ordered reports. |
| Change execution/lifecycle or extend Minau's own tests | [Contributing](references/contributing.md) and [Writing tests](references/writing-tests.md) | v01 lifecycle, v02 registration isolation, concurrency and real CLI regression coverage. |
| Change a service boundary, set up the checkout, or review a change | [Contributing](references/contributing.md) | Owning module, contract compatibility, dependencies and canonical verification. |
| Update documentation | [Contributing: documentation ownership](references/contributing.md#keep-documentation-authoritative) | One authoritative home per fact, working links and guidance matching source. |

## Complete the change

For implementation changes, run the README's compile, integration-test and example
commands in that order. Follow the owning reference's regression guidance. For
documentation-only changes, follow the contributor guide's documentation checks.
Inspect the diff and report actual verification results and any limitations.
Update the owning reference with changed behavior; keep this entry point compact.
