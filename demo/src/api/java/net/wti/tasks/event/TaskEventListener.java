package net.wti.tasks.event;

/// Listener of arbitrary TaskEvents.
@FunctionalInterface
public interface TaskEventListener {
    void onEvent(TaskEvent evt);
}
