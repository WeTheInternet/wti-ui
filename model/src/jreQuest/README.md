# Quest seed launcher

Build `:model-jreQuest:installDist` and distribute its complete installation.
`seedQuestDefinitions` selects module flags from its configured Java launcher;
installed launchers inspect the actual Java selected by JAVA_HOME or PATH.
Java8 gets no module option; Java9+ opens java.lang.invoke for the existing XApi
runtime. Compilation remains Java8. Arguments remain literal, including paths
with spaces and punctuation; no global JAVA_OPTS change is needed.

Owning distribution tests build on Java17 and execute the installed Linux script
with both Java8 and Java17, seed five real XApi definitions then preserve their
bytes on a zero-insert rerun, and verify the packaged metadata reader codeSource.
Windows template selection is implemented but Windows execution is unverified;
Linux probes do not establish Windows acceptance.
