package net.wti.game.api;

import xapi.model.api.Model;

/// Transport-neutral receipt envelope shared by game-specific result models.
///
/// A result reports an authority's outcome, event, or delta; it is not itself durable game
/// state. A concrete result may add a typed event, batch, or aggregate snapshot. The common
/// API intentionally avoids a polymorphic `ModelList` payload. Results may be retained
/// separately for audit or replay without becoming part of the canonical state graph.
public interface GameCommandResult extends Model {

    /// Command id to which this result belongs.
    String getCommandId();
    GameCommandResult setCommandId(String commandId);

    /// True when authoritative mutation was accepted.
    boolean getAccepted();
    GameCommandResult setAccepted(boolean accepted);

    /// Stable machine-readable outcome or rejection code.
    String getReasonCode();
    GameCommandResult setReasonCode(String reasonCode);

    /// Order assigned when the authority first processes this command id.
    long getServerOrder();
    GameCommandResult setServerOrder(long serverOrder);

    /// Aggregate revision after the authoritative outcome.
    long getResultingRevision();
    GameCommandResult setResultingRevision(long resultingRevision);
}
