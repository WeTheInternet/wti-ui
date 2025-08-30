package net.wti.tasks.event;

import net.wti.tasks.index.RunningRefreshQuery;

///
/// RefreshStartedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:04
public final class RefreshStartedEvent extends TaskEvent {
    private final RunningRefreshQuery operation;

    public RefreshStartedEvent(final RunningRefreshQuery operation) {
        this.operation = operation;
    }

    public RunningRefreshQuery getOperation() {
        return operation;
    }
}
