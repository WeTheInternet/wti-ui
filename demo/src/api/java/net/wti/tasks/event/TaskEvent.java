package net.wti.tasks.event;

/// Base event type; all concrete events extend this.
public abstract class TaskEvent {
    public final long whenMillis = System.currentTimeMillis();
}
