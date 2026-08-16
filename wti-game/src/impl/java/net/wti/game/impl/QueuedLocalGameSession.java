package net.wti.game.impl;

import net.wti.game.api.GameCommand;
import net.wti.game.api.GameCommandResult;
import net.wti.game.api.GameRejection;
import net.wti.game.api.GameSession;
import net.wti.game.spi.GameCommandAuthorizer;
import net.wti.game.spi.GameCommandContext;
import net.wti.game.spi.GameCommandHandler;
import net.wti.game.spi.GameCommandResultFactory;
import xapi.fu.In1;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Deterministic queued in-process implementation of `GameSession`.
///
/// Each pump snapshots the number of pending commands and processes exactly
/// that boundary FIFO. Commands submitted by handlers or listeners wait for the
/// next pump. Pumping is independent from any simulation clock, and reentrant
/// pumping is rejected.
///
/// First-seen results, including rejections, are retained in insertion order up
/// to a configured positive limit. A duplicate command id within that window
/// republishes the original result object without authorization or mutation.
/// Once evicted, an old id is treated as new; callers must size retention for
/// their retry horizon and must not mutate a result after it is published.
public final class QueuedLocalGameSession<
        C extends GameCommand,
        R extends GameCommandResult
        > implements GameSession<C, R> {

    private final String sessionId;
    private final int retainedResults;
    private final GameCommandAuthorizer<C> authorizer;
    private final GameCommandHandler<C, R> handler;
    private final GameCommandResultFactory<C, R> resultFactory;
    private final Deque<C> commands;
    private final List<In1<? super R>> listeners;
    private final Map<String, R> resultsByCommandId;

    private long serverOrder;
    private boolean pumping;

    public QueuedLocalGameSession(
            String sessionId,
            int retainedResults,
            GameCommandAuthorizer<C> authorizer,
            GameCommandHandler<C, R> handler,
            GameCommandResultFactory<C, R> resultFactory
    ) {
        if (sessionId == null || sessionId.isEmpty()) {
            throw new IllegalArgumentException("sessionId must not be empty");
        }
        if (retainedResults < 1) {
            throw new IllegalArgumentException("retainedResults must be positive");
        }
        if (authorizer == null || handler == null || resultFactory == null) {
            throw new IllegalArgumentException("session collaborators must not be null");
        }
        this.sessionId = sessionId;
        this.retainedResults = retainedResults;
        this.authorizer = authorizer;
        this.handler = handler;
        this.resultFactory = resultFactory;
        commands = new ArrayDeque<>();
        listeners = new ArrayList<>();
        resultsByCommandId = new LinkedHashMap<>();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public void submit(C command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        commands.addLast(command);
    }

    @Override
    public void addResultListener(In1<? super R> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
    }

    @Override
    public int pump() {
        if (pumping) {
            throw new IllegalStateException("pump must not be called reentrantly");
        }
        pumping = true;
        try {
            final int boundary = commands.size();
            for (int i = 0; i < boundary; i++) {
                process(commands.removeFirst());
            }
            return boundary;
        } finally {
            pumping = false;
        }
    }

    @Override
    public int pendingCommands() {
        return commands.size();
    }

    private void process(C command) {
        final String commandId = command.getCommandId();
        if (commandId != null && !commandId.isEmpty()) {
            final R cached = resultsByCommandId.get(commandId);
            if (cached != null) {
                publish(cached);
                return;
            }
        }

        final long order = ++serverOrder;
        final GameCommandContext<C> context = new GameCommandContext<>(
                sessionId,
                command.getActorId(),
                order,
                command
        );

        final String rejection;
        if (commandId == null || commandId.isEmpty()) {
            rejection = GameRejection.INVALID_COMMAND_ID;
        } else if (!sessionId.equals(command.getSessionId())) {
            rejection = GameRejection.SESSION_MISMATCH;
        } else {
            final String authorization = authorizer.rejectionReason(context);
            rejection = authorization == null || authorization.isEmpty()
                    ? null
                    : authorization;
        }

        final R result = rejection == null
                ? requireResult(handler.handle(context))
                : requireResult(resultFactory.rejected(context, rejection));
        result.setCommandId(commandId);
        result.setServerOrder(order);
        if (rejection != null) {
            // The boundary owns rejection semantics; the factory only supplies
            // the consumer's concrete result type and aggregate revision.
            result.setAccepted(false);
            result.setReasonCode(rejection);
        }

        if (commandId != null && !commandId.isEmpty()) {
            retain(commandId, result);
        }
        publish(result);
    }

    private R requireResult(R result) {
        if (result == null) {
            throw new IllegalStateException("handler and result factory must return a result");
        }
        return result;
    }

    private void retain(String commandId, R result) {
        while (resultsByCommandId.size() >= retainedResults) {
            final Iterator<String> ids = resultsByCommandId.keySet().iterator();
            ids.next();
            ids.remove();
        }
        resultsByCommandId.put(commandId, result);
    }

    private void publish(R result) {
        final List<In1<? super R>> boundary = new ArrayList<>(listeners);
        for (In1<? super R> listener : boundary) {
            listener.in(result);
        }
    }
}
