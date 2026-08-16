package net.wti.quest.api;

///
/// QuestStatus:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 07/12/2025 @ 23:36
public enum QuestStatus {
    // The order of this enum is used for sorting non-deadline'd tasks.
    OVERDUE,
    ACTIVE,
    PAUSED,
    PARKED,
    CANCELLED,
    FAILED,
    READY, // ready to finish, but not marked done
    FINISHED,
    ARCHIVED
}
