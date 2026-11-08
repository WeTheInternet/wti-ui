# Style Guide (Assistant Preferences)

## Documentation & comments
- Prefer `///`-style Markdown docs for:
    - class / interface headers
    - public/protected methods
    - non-trivial private methods (when behavior is subtle)
- Also include:
    - Javadoc blocks (`///`) where tools or conventions expect them (public API, overrides when helpful)
    - in-code comments for “why”, edge cases, invariants, and non-obvious choices (not for restating obvious code)

## Language & compatibility
- Implementation code: **Java** only, and must be **Java 8 compatible**
    - No `var`, records, text blocks, sealed types
    - No pattern matching `instanceof`, switch expressions, `Stream.toList()`, etc.
    - Avoid newer java.time conveniences if they require >8 (java.time itself is OK in 8)
- Tests: **Spock + Groovy** only
    - No JUnit tests unless explicitly requested
    - Keep test fixtures minimal; prefer readability

## Control flow and helper types
- Always use braces for `if`, `else`, and `for` bodies, including one-line guards and nested loops. Expand the body onto separate lines so control flow stays readable.
- Prefer descriptive top-level helper types over short-named nested classes. Keep implementation-only helpers package-private.
- Use compact stable names for persisted model kinds and identifiers; descriptive Java class names and local path variables need not be shortened.

## Behavioral expectations
- When generating code:
    - include both docs (`///` and/or Javadoc) and targeted inline comments
    - keep production code free of headless/test branching unless explicitly requested
- If a user request conflicts with this spec (e.g., asks for Java 17 features, JUnit, Kotlin):
    - explicitly warn that it deviates
    - propose the closest compliant alternative
    - offer to update the style guide/spec if the deviation is intentional

## Output conventions
- Provide file-scoped patches/snippets when editing existing files.
- Keep changes minimal and localized; avoid drive-by refactors unless requested.
