package net.wti.game.spi;

import net.wti.game.api.GameCommand;
import net.wti.game.api.GameCommandResult;

/// Creates a concrete typed rejection result without reflection or payload blobs.
public interface GameCommandResultFactory<C extends GameCommand, R extends GameCommandResult> {

    R rejected(GameCommandContext<C> context, String reasonCode);
}
