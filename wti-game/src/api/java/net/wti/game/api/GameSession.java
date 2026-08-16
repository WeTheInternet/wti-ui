package net.wti.game.api;

import xapi.fu.In1;

/// Asynchronous transport-neutral boundary for concrete commands and results.
///
/// `submit` only accepts work for later delivery. Implementations must not
/// invoke a handler or listener from the `submit` call stack.
public interface GameSession<C extends GameCommand, R extends GameCommandResult> {

    /// Returns the stable id of this session boundary.
    String getSessionId();

    /// Enqueues a concrete command without handling or notifying synchronously.
    void submit(C command);

    /// Adds a listener which receives concrete results in pump order.
    void addResultListener(In1<? super R> listener);

    /// Processes one explicit queue boundary and returns commands delivered.
    int pump();

    /// Returns commands currently waiting for a future pump boundary.
    int pendingCommands();
}
