package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.wti.quest.api.ChecklistSnapshot;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Stable goal cards with direct completion checkboxes. Expansion is local view state only.
 * Callers supply acknowledged snapshots; a click never optimistically checks a task. */
public final class ChecklistTreeView extends Table {
    private final Map<String, ChecklistGoalView> goals = new LinkedHashMap<>();
    private final Consumer<String> complete;
    private boolean loading, fixedOrder;

    public ChecklistTreeView(final Skin skin, final Consumer<String> complete) {
        super(skin);
        this.complete = complete;
        top().left();
    }

    /** Initial/refresh reads must settle before the first visible goal can accept an intent. */
    public void setLoading(final boolean value) {
        loading = value;
        for (final ChecklistGoalView goal : goals.values()) goal.setLoading(loading);
    }

    /** Reserve every configured row before async reads. Reapplying the same order is a no-op. */
    public void setGoalOrder(final List<String> identities) {
        final List<String> order = new ArrayList<>(identities);
        if (order.contains(null) || order.contains("") || new LinkedHashSet<>(order).size() != order.size()) {
            throw new IllegalArgumentException("Distinct goal identities required");
        }
        fixedOrder = true;
        if (new ArrayList<>(goals.keySet()).equals(order)) return;
        final Stage stage = getStage();
        final Actor focused = stage == null ? null : stage.getKeyboardFocus();
        final Map<String, ChecklistGoalView> arranged = new LinkedHashMap<>();
        for (final String identity : order) {
            ChecklistGoalView goal = goals.get(identity);
            if (goal == null) { goal = new ChecklistGoalView(getSkin(), complete); goal.setLoading(loading); }
            arranged.put(identity, goal);
        }
        goals.clear(); goals.putAll(arranged); clearChildren();
        for (final ChecklistGoalView goal : goals.values()) add(goal).growX().padBottom(16).row();
        if (focused != null && focused.getStage() == stage) stage.setKeyboardFocus(focused);
    }

    public List<String> getGoalOrder() { return Collections.unmodifiableList(new ArrayList<>(goals.keySet())); }

    public void setGoal(final String identity, final ChecklistSnapshot snapshot) {
        ChecklistGoalView goal = goals.get(identity);
        if (goal == null) {
            if (fixedOrder) throw new IllegalArgumentException("Goal is outside the configured order");
            goal = new ChecklistGoalView(getSkin(), complete); goal.setLoading(loading); goals.put(identity, goal);
            add(goal).growX().padBottom(16).row();
        }
        goal.setSnapshot(snapshot);
    }

    public void setPending(final String identity, final boolean pending) {
        final ChecklistGoalView goal = goals.get(identity);
        if (goal != null) { goal.setPending(pending); }
    }

    public void setError(final String identity, final String error) {
        final ChecklistGoalView goal = goals.get(identity);
        if (goal != null) { goal.setError(error); }
    }

    public boolean hasGoal(final String identity) { return goals.containsKey(identity); }

    public boolean isExpanded(final String identity) { return goals.get(identity).isExpanded(); }

}
