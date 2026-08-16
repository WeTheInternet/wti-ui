package net.wti.game.api;

import xapi.model.api.Model;

/// Transport-neutral request envelope shared by game-specific command models.
///
/// A command asks the authority to validate and apply an operation; it is not durable game
/// state. Concrete `@IsModel` commands extend this interface and add their typed payload
/// directly. The envelope deliberately does not contain a generic or heterogeneous payload
/// list.
public interface GameCommand extends Model {

    /// Stable idempotency key chosen by the command producer.
    String getCommandId();
    GameCommand setCommandId(String commandId);

    /// Session boundary to which this command is addressed.
    String getSessionId();
    GameCommand setSessionId(String sessionId);

    /// Actor on whose authority the command is requested.
    String getActorId();
    GameCommand setActorId(String actorId);

    /// Monotonic sequence assigned by this actor's client.
    long getClientSequence();
    GameCommand setClientSequence(long clientSequence);

    /// Aggregate revision against which the client formed the command.
    long getExpectedRevision();
    GameCommand setExpectedRevision(long expectedRevision);
}
