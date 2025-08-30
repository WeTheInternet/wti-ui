package net.wti.tasks.event;

///
/// RefreshFinishedEvent:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 27/08/2025 @ 07:03
public final class RefreshFinishedEvent extends TaskEvent {
    public final int total;

    public RefreshFinishedEvent(int total) {
        this.total = total;
    }
}
