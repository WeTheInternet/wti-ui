package net.wti.quest.model.jre;

import net.wti.quest.api.QuestDefinition;
import net.wti.quest.api.ChildRef;
import net.wti.quest.model.impl.QuestDefinitionLoaderImpl;
import net.wti.quest.model.impl.QuestDefinitionSeeder;
import xapi.jre.model.ModelServiceJre;
import xapi.model.X_Model;
import xapi.model.api.ModelModule;
import xapi.model.tools.ModelSerializerDefault;
import xapi.util.api.RemovalHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/// Noninteractive, single-writer server seed tool. All paths/namespace are required explicitly.
/// Seed root contains real .xapi files under META-INF/models/qdef; no Java seed catalog exists.
public final class QuestDefinitionSeedMain {

    private QuestDefinitionSeedMain() {
    }

    public static void main(final String[] args) throws Exception {
        final int created = run(args, (ModelServiceJre) X_Model.getService());
        System.out.println("Acknowledged " + created + " new quest definitions; existing definitions were preserved.");
    }

    /// Uses the same JRE backend as the server, configured before any load/save. A caller embedding
    /// this startup step must run it before accepting writers; ModelService has no cross-process CAS.
    public static int run(final String[] args, final ModelServiceJre models) throws Exception {
        final Map<String, String> options = options(args);
        final Path seedRoot = Paths.get(options.get("seed-root")).toAbsolutePath().normalize();
        final Path modelDirectory = Paths.get(options.get("model-dir")).toFile().getCanonicalFile().toPath();
        final Path manifestFile = Paths.get(options.get("manifest-out")).toAbsolutePath().normalize();
        final Path manifestDestination = manifestFile.toFile().getCanonicalFile().toPath();
        component(options.get("namespace"), "namespace");
        if (!Files.isDirectory(seedRoot.resolve("META-INF/models/qdef"))) {
            throw new IllegalArgumentException("seed-root must contain META-INF/models/qdef");
        }
        if (manifestDestination.startsWith(modelDirectory) || modelDirectory.startsWith(manifestDestination)) {
            throw new IllegalArgumentException("Manifest output must be outside the model store");
        }
        if (Files.exists(modelDirectory, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(modelDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("model-dir must be a directory");
        }
        models.setRootDir(modelDirectory.toFile());
        final ModelModule module = QuestModelModule.create(models);
        // The established loader creates models through X_Model. When an isolated storage backend
        // is injected, register the same canonical types in that factory too; this performs no IO.
        if (models != X_Model.getService()) {
            QuestModelModule.create((ModelServiceJre) X_Model.getService());
        }
        final byte[] manifest = ModelSerializerDefault.serialize(module).getBytes(StandardCharsets.UTF_8);
        if (Files.exists(manifestFile, LinkOption.NOFOLLOW_LINKS)) requireIdenticalManifest(manifestFile, manifest);
        final RemovalHandler scope = ModelServiceJre.registerModule(module);
        boolean interrupted = false;
        try {
            final List<QuestDefinition> definitions = readDefinitions(seedRoot);
            if (definitions.isEmpty()) {
                throw new IllegalArgumentException("No quest definitions found under seed-root");
            }
            validateDestinations(modelDirectory, options.get("namespace"), definitions);
            final CompletableFuture<Integer> result = new CompletableFuture<>();
            new QuestDefinitionSeeder(models).seedMissing(options.get("namespace"), definitions,
                    SuccessHandler.handler(result::complete, result::completeExceptionally));
            // This startup step cannot cancel a ModelService operation. Returning on an observer
            // timeout/interrupt would let its producer keep creating records after reported failure.
            // Wait for the actual terminal callback, then restore the caller's interrupt status.
            final int created;
            int acknowledged;
            for (;;) {
                try {
                    acknowledged = result.get();
                    break;
                } catch (InterruptedException signal) {
                    interrupted = true;
                }
            }
            created = acknowledged;
            publishManifest(manifestFile, manifest);
            return created;
        } finally {
            scope.remove();
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static List<QuestDefinition> readDefinitions(final Path seedRoot) throws IOException {
        final List<QuestDefinition> definitions = new ArrayList<>();
        // Restrict resource lookup to the explicitly supplied root. Dependency/sample fixtures must
        // not become production seeds merely because a library happens to put them on the classpath.
        try (URLClassLoader resources = new URLClassLoader(new URL[] {seedRoot.toUri().toURL()}, null)) {
            new QuestDefinitionLoaderImpl() {
                @Override
                protected ClassLoader classLoader() {
                    return resources;
                }
            }.loadFromClasspath((resource, definition) -> definitions.add(definition));
        }
        return definitions;
    }

    /// Publish only complete bytes, without replacing an artifact created by another invocation.
    /// A hard link supplies atomic create-if-absent; ATOMIC_MOVE may replace an existing target.
    /// Filesystems without hard-link support fail visibly instead of weakening this guarantee.
    static void publishManifest(final Path file, final byte[] manifest) throws IOException {
        final Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        final Path staged = Files.createTempFile(parent, ".lifequest-manifest-", ".tmp");
        try {
            try (FileChannel output = FileChannel.open(staged, StandardOpenOption.WRITE)) {
                final ByteBuffer bytes = ByteBuffer.wrap(manifest);
                while (bytes.hasRemaining()) output.write(bytes);
                output.force(true);
            }
            try {
                Files.createLink(file, staged);
            } catch (java.nio.file.FileAlreadyExistsException existing) {
                requireIdenticalManifest(file, manifest);
            }
        } finally {
            Files.deleteIfExists(staged);
        }
    }

    static void requireIdenticalManifest(final Path file, final byte[] expected) throws IOException {
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("ModelModule artifact must be an existing regular file: " + file);
        }
        if (!Arrays.equals(Files.readAllBytes(file), expected)) {
            throw new IOException("Refusing to replace a different existing ModelModule artifact: " + file);
        }
    }

    private static void component(final String value, final String name) {
        if (value == null || value.trim().isEmpty() || ".".equals(value) || "..".equals(value)
                || value.indexOf('/') >= 0 || value.indexOf('\\') >= 0 || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(name + " must be one filesystem component, without path separators");
        }
    }

    private static void directoryComponent(final Path path, final Path store) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Seed destination must be a directory without a symlink: " + path);
        }
        if (!path.toFile().getCanonicalFile().toPath().startsWith(store)) {
            throw new IOException("Seed destination escapes the selected store: " + path);
        }
    }

    private static void validateDestinations(
            final Path store, final String namespace, final List<QuestDefinition> definitions
    ) throws IOException {
        final Path namespaceRoot = store.resolve(namespace);
        final Path bucket = namespaceRoot.resolve(QuestDefinition.MODEL_QUEST_DEFINITION);
        directoryComponent(namespaceRoot, store);
        directoryComponent(bucket, store);
        for (final QuestDefinition definition : definitions) {
            if (definition.getKey() == null) throw new IllegalArgumentException("Seed definition requires a key");
            final String id = definition.getKey().getId();
            component(id, "definition id");
            final Path destination = bucket.resolve(id);
            if (Files.isSymbolicLink(destination)
                    || !destination.toFile().getCanonicalFile().toPath().startsWith(bucket)) {
                throw new IOException("Seed record aliases a different destination: " + destination);
            }
            if (definition.getChildren() != null) {
                for (final ChildRef reference : definition.getChildren()) {
                    if (reference.getDefinitionKey() == null) {
                        throw new IllegalArgumentException("Seed ChildRef requires a definition key");
                    }
                    component(reference.getDefinitionKey().getId(), "ChildRef definition id");
                    if (!reference.getDefinitionKey().getNamespace().isEmpty()) {
                        component(reference.getDefinitionKey().getNamespace(), "ChildRef namespace");
                    }
                }
            }
        }
    }

    private static Map<String, String> options(final String[] args) {
        final Map<String, String> options = new HashMap<>();
        for (final String arg : args) {
            final int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 3) {
                throw new IllegalArgumentException("Expected --seed-root= --model-dir= --namespace= --manifest-out=");
            }
            final String name = arg.substring(2, equals);
            if (!Arrays.asList("seed-root", "model-dir", "namespace", "manifest-out").contains(name)
                    || options.put(name, arg.substring(equals + 1)) != null) {
                throw new IllegalArgumentException("Unknown or duplicate seed option: " + name);
            }
        }
        for (final String name : Arrays.asList("seed-root", "model-dir", "namespace", "manifest-out")) {
            if (!options.containsKey(name) || options.get(name).trim().isEmpty()) {
                throw new IllegalArgumentException("Missing explicit seed option: " + name);
            }
        }
        return options;
    }
}
