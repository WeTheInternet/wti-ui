package net.wti.quest.model.impl;

import net.wti.quest.api.ChildRef;
import net.wti.quest.api.QuestDefinition;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.service.ModelService;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/// Seeds authored definitions through the existing model backend, without touching live progress.
/// Run as a single-writer startup step: ModelService does not expose an atomic create-if-absent.
/// A failed batch may have acknowledged earlier creates; retrying skips those stored definitions.
public final class QuestDefinitionSeeder {

    private final ModelService models;

    public QuestDefinitionSeeder(final ModelService models) {
        this.models = Objects.requireNonNull(models, "models");
        models.register(QuestDefinition.class);
        models.register(ChildRef.class);
    }

    /// The namespace is definition ownership, not the owner of future live checklist progress.
    /// Sources normally come from QuestDefinitionLoaderImpl. They are never mutated by seeding.
    /// The callback reports the number of acknowledged creates; an existing definition is untouched.
    public void seedMissing(
            final String namespace, final Iterable<QuestDefinition> sources,
            final SuccessHandler<Integer> callback
    ) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(callback, "callback");
        final List<QuestDefinition> definitions = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        try {
            for (final QuestDefinition definition : Objects.requireNonNull(sources, "sources")) {
                final ModelKey key = definition == null ? null : definition.getKey();
                if (key == null || !QuestDefinition.MODEL_QUEST_DEFINITION.equals(key.getKind())
                        || key.getId() == null || key.getId().trim().isEmpty() || key.getParent() != null) {
                    throw new IllegalArgumentException("Seed definitions require a stable, unparented qdef key");
                }
                if (!ids.add(key.getId())) {
                    throw new IllegalArgumentException("Duplicate seed definition: " + key.getId());
                }
                definitions.add(definition);
            }
        } catch (RuntimeException error) {
            ErrorHandler.delegateTo(callback).onError(error);
            return;
        }
        new SeedPass(namespace, definitions, callback).next();
    }

    private ModelKey qualifiedKey(final ModelKey key, final String namespace) {
        return models.newKey(namespace, key.getKind(), key.getId()).setKeyType(key.getKeyType());
    }

    private boolean sameKey(final ModelKey left, final ModelKey right) {
        // ModelKey.equals does not compare ancestry or keyType.
        return left != null && right != null && models.keyToString(left).equals(models.keyToString(right));
    }

    private final class SeedPass {
        private final String namespace;
        private final List<QuestDefinition> definitions;
        private final SuccessHandler<Integer> callback;
        private int index;
        private int created;
        private boolean finished;

        private SeedPass(
                final String namespace, final List<QuestDefinition> definitions, final SuccessHandler<Integer> callback
        ) {
            this.namespace = namespace;
            this.definitions = definitions;
            this.callback = callback;
        }

        private void next() {
            if (finished) return;
            if (index == definitions.size()) {
                finished = true;
                callback.onSuccess(created);
                return;
            }
            final QuestDefinition source = definitions.get(index++);
            final ModelKey key = qualifiedKey(source.getKey(), namespace);
            try {
                models.load(QuestDefinition.class, key, SuccessHandler.handler(existing -> {
                    if (existing == null || !sameKey(existing.getKey(), key)) {
                        fail(new IllegalStateException("Seed lookup returned an invalid identity"));
                    } else {
                        next();
                    }
                }, error -> {
                    if (error instanceof ModelNotFoundException && error.getCause() == null
                            && sameKey(((ModelNotFoundException) error).getModelKey(), key)) {
                        create(source, key);
                    } else {
                        fail(error);
                    }
                }));
            } catch (RuntimeException error) {
                fail(error);
            }
        }

        private void create(final QuestDefinition source, final ModelKey key) {
            if (finished) return;
            try {
                final QuestDefinition copy = models.deserialize(QuestDefinition.class,
                        new StringCharIterator(models.serialize(QuestDefinition.class, source).toSource()));
                copy.setKey(key);
                if (copy.getChildren() != null) {
                    for (final ChildRef reference : copy.getChildren()) {
                        final ModelKey target = reference.getDefinitionKey();
                        if (target == null || !QuestDefinition.MODEL_QUEST_DEFINITION.equals(target.getKind())
                                || target.getId() == null || target.getId().trim().isEmpty()) {
                            throw new IllegalArgumentException("Seed ChildRef requires a qdef key");
                        }
                        if (target.getNamespace().isEmpty()) {
                            if (target.getParent() != null) {
                                throw new IllegalArgumentException("Seed ChildRef target must be unparented");
                            }
                            reference.setDefinitionKey(qualifiedKey(target, namespace));
                        }
                    }
                }
                models.persist(copy, SuccessHandler.handler(saved -> {
                    if (saved == null || !sameKey(saved.getKey(), key)) {
                        fail(new IllegalStateException("Seed persist acknowledged an invalid identity"));
                    } else {
                        created++;
                        next();
                    }
                }, this::fail));
            } catch (RuntimeException error) {
                fail(error);
            }
        }

        private void fail(final Throwable error) {
            if (finished) return;
            finished = true;
            ErrorHandler.delegateTo(callback).onError(error);
        }
    }
}
