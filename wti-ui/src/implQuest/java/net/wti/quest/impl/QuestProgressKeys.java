package net.wti.quest.impl;

import net.wti.quest.api.LiveQuest;
import net.wti.quest.api.QuestDefinition;
import net.wti.time.api.ModelDay;
import xapi.model.api.Model;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.service.ModelService;
import xapi.fu.In1;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/// Full-key checks and callback boundaries shared by the explicit checklist services.
final class QuestProgressKeys {
    private QuestProgressKeys() {}

    static void current(final BooleanSupplier current) {
        if (!current.getAsBoolean()) throw new CancellationException("Checklist run is no longer current");
    }

    static String encoded(final ModelService models, final ModelKey key) {
        if (key == null) throw new IllegalArgumentException("A complete model key is required");
        final Set<ModelKey> seen = Collections.newSetFromMap(new IdentityHashMap<ModelKey, Boolean>());
        final String namespace = key.getNamespace();
        for (ModelKey part = key; part != null; part = part.getParent()) {
            if (!seen.add(part) || !part.isComplete() || part.getKind() == null
                    || !Objects.equals(namespace, part.getNamespace())) {
                throw new IllegalArgumentException("Incomplete, cyclic or inconsistent model key");
            }
        }
        return models.keyToString(key);
    }

    static ModelKey copy(final ModelService models, final ModelKey key) {
        return models.keyFromString(encoded(models, key));
    }

    static boolean same(final ModelService models, final ModelKey a, final ModelKey b) {
        return a == null || b == null ? a == b : encoded(models, a).equals(encoded(models, b));
    }

    static ModelKey day(final ModelService models, final ModelDay day) {
        if (day == null || day.getDayNum() == null) throw new IllegalArgumentException("Explicit day and owner key required");
        final ModelKey key = copy(models, day.getKey());
        if (!ModelDay.MODEL_DAY.equals(key.getKind()) || key.getKeyType() != ModelKey.KEY_TYPE_LONG
                || !String.valueOf(day.getDayNum()).equals(key.getId())) {
            throw new IllegalArgumentException("Day key must match the supplied day number and long key type");
        }
        return key;
    }

    static void definition(final ModelService models, final ModelKey key) {
        encoded(models, key);
        if (!QuestDefinition.MODEL_QUEST_DEFINITION.equals(key.getKind())) {
            throw new IllegalArgumentException("Expected quest definition key");
        }
    }

    static void live(final ModelService models, final LiveQuest live, final ModelKey key,
                     final ModelKey day, final int dayNum, final ModelKey definition) {
        if (live == null || !same(models, key, live.getKey())
                || !same(models, day, live.getParentDayKey()) || !Objects.equals(dayNum, live.getDayIndex())
                || !same(models, definition, live.getSourceDefinitionKey())
                || !Objects.equals(key.getId(), live.getLiveKey()) || live.getSourceRuleKey() != null) {
            throw new IllegalStateException("Existing checklist progress has different identity or lineage");
        }
    }

    static <M extends Model> void optional(final ModelService models, final Class<M> type,
                                          final ModelKey key, final SuccessHandler<M> callback) {
        final SuccessHandler<M> once = once(callback);
        try {
            models.load(type, key, SuccessHandler.handler(value -> {
                try {
                    if (value == null || !same(models, key, value.getKey())) {
                        fail(once, new IllegalStateException("Load returned a different model identity"));
                    } else once.onSuccess(value);
                } catch (final Throwable failure) { fail(once, failure); }
            }, error -> {
                try {
                    if (error instanceof ModelNotFoundException && error.getCause() == null
                            && same(models, key, ((ModelNotFoundException) error).getModelKey())) {
                        once.onSuccess(null);
                    } else fail(once, error);
                } catch (final Throwable failure) { fail(once, failure); }
            }));
        } catch (final Throwable failure) { fail(once, failure); }
    }

    /** Availability is configuration, not health/negotiation. Pin this choice for one start. */
    static ModelServiceWithCreateIfAbsent creator(final ModelService models) {
        if (!(models instanceof ModelServiceWithCreateIfAbsent)) return null;
        final ModelServiceWithCreateIfAbsent capable = (ModelServiceWithCreateIfAbsent) models;
        return capable.supportsCreateIfAbsent() ? capable : null;
    }

    /** A selected conditional provider never downgrades after an uncertain or failed request. */
    static <M extends Model> void createMissing(final ModelService models, final ModelServiceWithCreateIfAbsent creator,
                                               final M model, final SuccessHandler<M> callback) {
        if (creator == null) { save(models, model, callback); return; }
        final SuccessHandler<M> once = once(callback);
        try {
            final String expected = encoded(models, model.getKey());
            final String type = model.getType();
            creator.createIfAbsent(model, SuccessHandler.handler(result -> {
                try {
                    final M acknowledged = result == null ? null : result.getModel();
                    if (acknowledged == null || !expected.equals(encoded(models, acknowledged.getKey()))
                            || !type.equals(acknowledged.getType())) {
                        throw new IllegalStateException("Conditional create acknowledged a different model identity");
                    }
                    once.onSuccess(acknowledged);
                } catch (final Throwable failure) { fail(once, failure); }
            }, error -> fail(once, error)));
        } catch (final Throwable failure) { fail(once, failure); }
    }

    static <M extends Model> void save(final ModelService models, final M model,
                                      final SuccessHandler<M> callback) {
        final SuccessHandler<M> once = once(callback);
        final String expected = encoded(models, model.getKey());
        try {
            models.persist(model, SuccessHandler.handler(saved -> {
                try {
                    if (saved == null || !expected.equals(encoded(models, saved.getKey()))) {
                        fail(once, new IllegalStateException("Persist acknowledged a different model identity"));
                    } else once.onSuccess(saved);
                } catch (final Throwable failure) { fail(once, failure); }
            }, error -> fail(once, error)));
        } catch (final Throwable failure) { fail(once, failure); }
    }

    static <T> SuccessHandler<T> once(final SuccessHandler<T> callback) {
        Objects.requireNonNull(callback, "callback");
        final AtomicBoolean done = new AtomicBoolean();
        return SuccessHandler.handler(value -> {
            if (done.compareAndSet(false, true)) callback.onSuccess(value);
        }, error -> {
            if (done.compareAndSet(false, true)) fail(callback, error);
        });
    }

    static <T> SuccessHandler<T> then(final SuccessHandler<?> terminal, final In1<T> continuation) {
        return SuccessHandler.handler(value -> {
            try { continuation.in(value); }
            catch (final Throwable failure) { fail(terminal, failure); }
        }, error -> fail(terminal, error));
    }

    static void fail(final SuccessHandler<?> callback, final Throwable error) {
        if (callback instanceof ErrorHandler) ((ErrorHandler) callback).onError(error);
        else throw new IllegalStateException("An error-aware callback is required", error);
    }
}
