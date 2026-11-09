package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.quest.api.ChecklistSnapshot;
import net.wti.quest.api.QuestStatus;
import net.wti.ui.controls.focus.KeyboardFocusScope;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** One acknowledged goal and its display branches. Save states update existing actors;
 * only changed membership, wording or explicit expansion changes rebuild the layout. */
final class ChecklistGoalView extends Table {
    private final Consumer<String> complete;
    private final Map<String, CheckBox> boxes = new LinkedHashMap<>();
    private final Map<String, ChecklistSnapshot.Entry> entries = new LinkedHashMap<>();
    private final Map<String, Boolean> branches = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();
    private final Label status;
    private final CheckBox.CheckBoxStyle normalStyle, waitingStyle;
    private ChecklistSnapshot snapshot;
    private List<String> shape;
    private TextButton toggle, retry;
    private Label count, placeholder;
    private Cell<?> retryCell;
    private boolean expanded = true, pending, loading, retryShown;
    private String error;

    ChecklistGoalView(final Skin skin, final Consumer<String> complete) {
        super(skin); this.complete = complete;
        normalStyle = skin.get(CheckBox.CheckBoxStyle.class);
        waitingStyle = new CheckBox.CheckBoxStyle(normalStyle);
        // Disable interaction while preserving acknowledged content contrast, rather than dimming every task.
        waitingStyle.disabledFontColor = normalStyle.fontColor;
        waitingStyle.checkboxOnDisabled = normalStyle.checkboxOn;
        waitingStyle.checkboxOffDisabled = normalStyle.checkboxOff;
        status = new Label("", skin); status.setWrap(true);
        top().left();
    }

    void setSnapshot(final ChecklistSnapshot value) { snapshot = value; render(); }
    void setPending(final boolean value) { pending = value; render(); }
    void setLoading(final boolean value) { loading = value; render(); }
    void setError(final String value) { error = value; render(); }
    boolean isExpanded() { return expanded; }

    private void render() {
        final List<String> next = new ArrayList<>();
        entries.clear();
        if (snapshot != null) {
            entries.put(snapshot.getRoot().getDefinitionKey(), snapshot.getRoot());
            for (final ChecklistSnapshot.Entry entry : snapshot.getChildren()) entries.put(entry.getDefinitionKey(), entry);
            for (final ChecklistSnapshot.Entry entry : entries.values()) {
                next.add(entry.getDefinitionKey()); next.add(entry.getTitle());
            }
        }
        if (!next.equals(shape)) {
            shape = next;
            KeyboardFocusScope.preserveFocus(this, this::layoutRows);
        }
        updateState();
    }

    private void layoutRows() {
        clearChildren(); boxes.clear(); titles.clear(); toggle = null; retry = null; count = null; placeholder = null;
        if (snapshot == null) {
            placeholder = new Label("", getSkin()); placeholder.setWrap(true);
            add(placeholder).growX().minWidth(0).minHeight(48).row();
        } else if (snapshot.getChildren().isEmpty()) {
            add(check(snapshot.getRoot(), snapshot.getRoot().getTitle())).growX().minWidth(0).minHeight(48).row();
        } else {
            toggle = button("", "goal:" + snapshot.getRoot().getDefinitionKey(), () -> {
                expanded = !expanded; rebuild();
            });
            toggle.getLabel().setWrap(true);
            add(toggle).growX().minWidth(0).minHeight(48);
            count = new Label("", getSkin()); add(count).padLeft(12).row();
            if (expanded) add(members()).colspan(2).growX().minWidth(0).padLeft(12).row();
            retry = button("Finish goal", "finish:" + snapshot.getRoot().getDefinitionKey(), () -> {
                pending = true; updateState(); complete.accept(snapshot.getRoot().getLiveKey());
            });
            retry.setVisible(false); retryShown = false;
            retryCell = add(retry).colspan(2).height(0).minWidth(48).left(); row();
        }
        add(status).colspan(2).growX().minWidth(0).minHeight(24).padLeft(12).row();
    }

    private Table members() {
        final Table body = new Table(getSkin());
        final List<ChecklistSnapshot.Entry> children = snapshot.getChildren();
        for (int i = 0; i < children.size();) {
            final ChecklistSnapshot.Entry first = children.get(i);
            final String prefix = prefix(first.getTitle());
            int end = i + 1;
            if (!prefix.isEmpty()) while (end < children.size() && prefix.equals(prefix(children.get(end).getTitle()))) end++;
            if (end == i + 1) {
                body.add(check(first, first.getTitle())).growX().minWidth(0).minHeight(48).padTop(4).row();
            } else {
                // Contiguous legacy "Parent: item" labels become display branches in source order.
                // This neither edits definitions nor invents nested progress/assignment identities.
                final String branch = first.getDefinitionKey();
                final boolean open = branches.getOrDefault(branch, true);
                final TextButton header = button((open ? "-  " : "+  ") + prefix, "branch:" + branch,
                        () -> { branches.put(branch, !branches.getOrDefault(branch, true)); rebuild(); });
                header.getLabel().setWrap(true);
                body.add(header).growX().minWidth(0).minHeight(48).padTop(4).row();
                if (open) {
                    final Table nested = new Table(getSkin());
                    for (int j = i; j < end; j++) {
                        final ChecklistSnapshot.Entry entry = children.get(j);
                        nested.add(check(entry, entry.getTitle().substring(entry.getTitle().indexOf(": ") + 2)))
                                .growX().minWidth(0).minHeight(48).padTop(4).row();
                    }
                    body.add(nested).growX().minWidth(0).padLeft(20).row();
                }
            }
            i = end;
        }
        return body;
    }

    private static String prefix(final String title) {
        if (title == null) return "";
        final int colon = title.indexOf(": ");
        return colon > 0 && colon + 2 < title.length() ? title.substring(0, colon).trim() : "";
    }

    private void rebuild() { shape = null; render(); }

    private CheckBox check(final ChecklistSnapshot.Entry entry, final String title) {
        final String key = entry.getDefinitionKey();
        final CheckBox box = new CheckBox("", getSkin());
        box.setName("task:" + key); box.getLabel().setWrap(true);
        box.getLabelCell().growX().minWidth(0).padLeft(12); box.left();
        box.setProgrammaticChangeEvents(false);
        boxes.put(key, box); titles.put(key, title);
        box.addListener(new ChangeListener() {
            @Override public void changed(final ChangeEvent event, final Actor actor) {
                final ChecklistSnapshot.Entry current = entries.get(key);
                box.setChecked(current.isComplete());
                if (box.isDisabled()) return;
                pending = true; updateState(); complete.accept(current.getLiveKey());
            }
        });
        return box;
    }

    private void updateState() {
        if (placeholder != null) placeholder.setText(error == null ? "Loading goal..." : "Goal unavailable");
        if (snapshot != null) {
            if (toggle != null) {
                toggle.setText((expanded ? "-  " : "+  ") + snapshot.getRoot().getTitle());
                int done = 0;
                for (final ChecklistSnapshot.Entry entry : snapshot.getChildren()) if (entry.isComplete()) done++;
                count.setText(done + "/" + snapshot.getChildren().size());
            }
            for (final Map.Entry<String, CheckBox> row : boxes.entrySet()) {
                final ChecklistSnapshot.Entry entry = entries.get(row.getKey());
                final CheckBox box = row.getValue();
                final CheckBox.CheckBoxStyle style = (pending || loading) && !entry.isComplete() ? waitingStyle : normalStyle;
                if (box.getStyle() != style) box.setStyle(style);
                box.setText(titles.get(row.getKey()) + (entry.getQuantity() > 1 ? " x " + entry.getQuantity() : ""));
                box.setChecked(entry.isComplete());
                final boolean active = entry.getStatus() == null || entry.getStatus() == QuestStatus.ACTIVE;
                box.setDisabled(loading || complete == null || pending || entry.isComplete()
                        || entry.getLiveKey() == null || !(entry.canComplete() || snapshot.canStart() && active));
            }
            if (retry != null) {
                final boolean visible = snapshot.getRoot().canComplete() && !snapshot.getRoot().isComplete();
                if (visible != retryShown) {
                    retryShown = visible; retry.setVisible(visible); retryCell.height(visible ? 48 : 0); invalidateHierarchy();
                }
                retry.setDisabled(!visible || loading || pending || complete == null);
            }
        }
        status.setText(error != null ? error : pending ? "Saving..." : loading && snapshot != null ? "Refreshing..." : "");
    }

    private TextButton button(final String title, final String name, final Runnable action) {
        final TextButton button = new TextButton(title, getSkin()); button.setName(name);
        button.addListener(new ChangeListener() {
            @Override public void changed(final ChangeEvent event, final Actor actor) { if (!button.isDisabled()) action.run(); }
        });
        return button;
    }
}
