package net.wti.game.spi;

import net.wti.game.api.GameCommand;

/// Immutable authority-side context for one first-seen command id.
public final class GameCommandContext<C extends GameCommand> {

    private final String sessionId;
    private final String actorId;
    private final long serverOrder;
    private final C command;

    public GameCommandContext(
            String sessionId,
            String actorId,
            long serverOrder,
            C command
    ) {
        this.sessionId = sessionId;
        this.actorId = actorId;
        this.serverOrder = serverOrder;
        this.command = command;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getActorId() {
        return actorId;
    }

    public long getServerOrder() {
        return serverOrder;
    }

    public C getCommand() {
        return command;
    }
}
