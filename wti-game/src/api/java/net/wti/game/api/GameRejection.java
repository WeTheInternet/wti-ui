package net.wti.game.api;

/// Stable rejection codes produced by the reusable session boundary.
public final class GameRejection {

    public static final String INVALID_COMMAND_ID = "invalid-command-id";
    public static final String SESSION_MISMATCH = "session-mismatch";
    public static final String UNAUTHORIZED = "unauthorized";

    private GameRejection() {
    }
}
