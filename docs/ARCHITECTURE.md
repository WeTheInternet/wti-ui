# Architecture

This repository uses XAPI schema files to describe logical projects, modules, and platform variants. Gradle topology is generated from those schemas.

## Topology sources

Use these sources in this order:

1. `.xapi` schema files are authoritative. Edit and trust these for intended modules, platforms, source sets, and dependencies.
2. Generated `*.gradle` files are diagnostic output. Read them when you need the concrete Gradle project name, source directories, Java/Groovy source-set expansion, or generated dependency declarations.
3. `build/xindex` is a generated machine index. Use it for targeted lookup when present locally, then confirm meaning against `.xapi` and generated Gradle files.

## `build/xindex` targeted lookup

Avoid broad searches over the whole generated index. If you already know the project/module, inspect the direct path-side entry:

```text
build/xindex/path/_<project>/<module-or-platform-module>/...
```

For example, inspect inventory sources at:

```text
build/xindex/path/_components/inventory/sources
```

In this checkout, that file contained:

```text
/opt/wti-ui/components/src/inventory
```

That means the xindex `sources` entry maps the known `components` / `inventory` module to its source root. The neighboring directory `build/xindex/path/_components/inventory` was listable through workspace tools, but no sibling files or directories were visible from that listing. Targeted reads for likely neighbor names such as dependency/dependencies, outputs, classes, resources, generated, coord/coords, project, and module were not present.

A coordinate-side lookup may also exist with this pattern:

```text
build/xindex/coord/<group>/<project-module>/...
```

The coordinate-side neighbor inspected here was:

```text
build/xindex/coord/net.wti/components-inventory
```

It was discoverable as a directory, but no child files were visible through workspace tools and `build/xindex/coord/net.wti/components-inventory/sources` was not present.

## Choosing xindex, `.xapi`, or generated Gradle files

- Use `build/xindex` when you need a fast answer for a known generated module, such as “where are the sources for `components` inventory?”
- Use `.xapi` files when you need the authoritative declaration or when changing module topology.
- Use generated Gradle files when you need the exact Gradle task/project name, generated source directories, or dependency expansion used by Gradle.
- If generated files disagree, treat `.xapi` as authoritative and consider `build/xindex`/generated Gradle stale until regenerated.

If workspace file tools cannot inspect a targeted xindex directory, request narrow command output instead of running a broad search, for example:

```bash
find build/xindex/path/_components/inventory -maxdepth 2 -type f -print
for f in build/xindex/path/_components/inventory/*; do echo "### $f"; sed -n '1,80p' "$f"; done
```
