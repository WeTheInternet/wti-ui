# Agent Guide

Use this file as a quick operating guide for LLM agents working in this repository. Read `README.md`, `REQUIREMENTS.md`, `STYLE-GUIDE.md`, `ROADMAP.md`, and the architecture notes before changing code.

## Repository topology lookup order

1. Start with `.xapi` files for authoritative module and platform declarations.
2. Use generated Gradle files when you need the concrete Gradle project name, source directories, or expanded dependency output.
3. Use `build/xindex` as a targeted machine index when it exists locally after generation.

## Targeted `build/xindex` lookup patterns

Do not broad-search the whole `build/xindex` tree when a project/module is known. Inspect the specific path first.

Path-side entries use:

```text
build/xindex/path/_<project>/<module-or-platform-module>/...
```

Concrete inspected example:

```text
build/xindex/path/_components/inventory/sources
```

In this checkout, that `sources` file contained one source root:

```text
/opt/wti-ui/components/src/inventory
```

The neighboring directory `build/xindex/path/_components/inventory` was listable through workspace tools, but no sibling files/directories were visible through that listing. Direct checks for common neighbor names such as dependency, output, resource, coordinate, project, and module files did not find additional files.

Coordinate-side entries may use:

```text
build/xindex/coord/<group>/<project-module>/...
```

The coordinate neighbor `build/xindex/coord/net.wti/components-inventory` was discoverable as a directory through workspace tools in this checkout, but no child files were visible and no `sources` file was found there.

## When to use each topology source

- Use `.xapi` files to understand or modify the intended project/module topology. They are the source of truth.
- Use generated Gradle files to diagnose the generated Gradle project path, generated source-set layout, or dependency expansion.
- Use `build/xindex` for fast targeted lookup from a known project/module coordinate to generated source roots or related machine-index entries. Treat it as generated diagnostic data, not as authoritative configuration.

If workspace tools cannot list/read the needed xindex directories, ask the user for targeted output instead of broad-searching, for example:

```bash
find build/xindex/path/_components/inventory -maxdepth 2 -type f -print
for f in build/xindex/path/_components/inventory/*; do echo "### $f"; sed -n '1,80p' "$f"; done
```
