package net.wti.game.spi;

import net.wti.game.api.GameCommand;

/// Explicit authorization hook composed by the owning game.
///
/// Return `null` to authorize. Return a stable non-empty reason code to reject.
/// Connection to a session never implies ownership of the requested actor.
public interface GameCommandAuthorizer<C extends GameCommand> {

    String rejectionReason(GameCommandContext<C> context);
}
