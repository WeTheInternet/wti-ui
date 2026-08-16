package net.wti.game.spi;

import net.wti.game.api.GameCommand;
import net.wti.game.api.GameCommandResult;

/// Game-specific authoritative mutation composed behind a session boundary.
public interface GameCommandHandler<C extends GameCommand, R extends GameCommandResult> {

    R handle(GameCommandContext<C> context);
}
