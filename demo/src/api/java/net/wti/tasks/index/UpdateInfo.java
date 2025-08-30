package net.wti.tasks.index;

import xapi.model.api.ModelKey;

///
/// UpdateInfo
///
/// Captures minimal upsert state for a ModelTask:
///  - whether the task was newly added
///  - the last-updated timestamp from the task model
///  - the task key (optional convenience)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 28/08/2025 @ 00:07
public final class UpdateInfo {

    private final ModelKey key;
    private final boolean newlyAdded;
    private final long lastUpdated;

    public UpdateInfo(ModelKey key, boolean newlyAdded, long lastUpdated) {
        this.key = key;
        this.newlyAdded = newlyAdded;
        this.lastUpdated = lastUpdated;
    }

    public ModelKey getKey() {
        return key;
    }

    public boolean isNewlyAdded() {
        return newlyAdded;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "UpdateInfo{" +
                "key=" + key +
                ", newlyAdded=" + newlyAdded +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}
