package net.wti.tasks.index;


import net.wti.ui.demo.api.ModelTask;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.util.api.ErrorHandler;

import java.util.HashMap;
import java.util.Map;

///
/// RunningRefreshQuery
///
/// Consolidates the moving parts of an in-flight refresh:
/// - the ModelQuery being used
/// - the fail handler
/// - the latest success result (set before each processing step)
/// - a map of UpdateInfo per ModelKey
/// - the newestUpdatedTimestamp across all processed results so far
///
/// Created by James X. Nelson (James@WeTheInter.net) on 28/08/2025 @ 00:06
public final class RunningRefreshQuery {

    private final ModelQuery<ModelTask> query;
    private final ErrorHandler<? extends Throwable> failHandler;

    private final Map<ModelKey, UpdateInfo> updates = new HashMap<>();
    private long newestUpdatedTimestamp;

    private ModelQueryResult<ModelTask> success;

    public RunningRefreshQuery(ErrorHandler<? extends Throwable> failHandler) {
        this.query = new ModelQuery<>();
        this.failHandler = failHandler;
        this.newestUpdatedTimestamp = 0L;
    }

    public ModelQuery<ModelTask> getQuery() {
        return query;
    }

    public ErrorHandler<? extends Throwable> getFailHandler() {
        return failHandler;
    }

    public Map<ModelKey, UpdateInfo> getUpdates() {
        return updates;
    }

    public long getNewestUpdatedTimestamp() {
        return newestUpdatedTimestamp;
    }

    public void observeUpdatedTimestamp(long ts) {
        if (ts > newestUpdatedTimestamp) {
            newestUpdatedTimestamp = ts;
        }
    }

    public String getCursor() {
        return this.query.getCursor();
    }

    public void setCursor(final String cursor) {
        this.query.setCursor(cursor);
    }

    public ModelQueryResult<ModelTask> getSuccess() {
        return success;
    }

    public RunningRefreshQuery setSuccess(ModelQueryResult<ModelTask> success) {
        this.success = success;
        return this;
    }
}
