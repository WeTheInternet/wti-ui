package net.wti.ui.quest.impl;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.wti.quest.api.ChecklistSnapshot;
import net.wti.ui.quest.api.ChecklistActionHandler;

/// Pure checklist renderer. Caller applies acknowledged snapshots and dispatches all setters
/// and refresh calls on the render thread. Rows are never removed optimistically.
public final class ChecklistView extends Table {
    private final ChecklistActionHandler handler;
    private ChecklistSnapshot snapshot;
    private boolean pending;
    private String error;

    public ChecklistView(final Skin skin, final ChecklistActionHandler handler) {
        super(skin);
        this.handler = handler;
        defaults().left().pad(8);
    }

    public void setSnapshot(final ChecklistSnapshot snapshot) { this.snapshot = snapshot; }
    public void setPending(final boolean pending) { this.pending = pending; }
    public void setError(final String error) { this.error = error; }

    /// Rebuild only from caller-provided state; membership and completion are separate.
    public void refresh() {
        clearChildren();
        if (snapshot == null) {
            statusRows();
            invalidateHierarchy();
            return;
        }
        final ChecklistSnapshot.Entry root = snapshot.getRoot();
        final Label title = new Label(root.getTitle() == null ? "Checklist" : root.getTitle(), getSkin());
        title.setWrap(true);
        add(title).growX().row();
        if (root.getDescription() != null && !root.getDescription().isEmpty()) {
            final Label description = new Label(root.getDescription(), getSkin());
            description.setWrap(true);
            add(description).growX().row();
        }
        for (final ChecklistSnapshot.Entry child : snapshot.getChildren()) {
            final Table row = new Table(getSkin());
            final String quantity = child.getQuantity() > 1 ? " × " + child.getQuantity() : "";
            final Label childTitle = new Label(child.getTitle() + quantity, getSkin());
            childTitle.setWrap(true);
            row.add(childTitle).growX().left().padRight(12);
            if (child.isComplete()) row.add(new Label("Done", getSkin()));
            else if (child.canComplete()) row.add(action("Complete", () -> handler.completeRequested(child.getLiveKey())));
            else row.add(new Label(child.isStarted() ? "Unavailable" : "Not started", getSkin()));
            add(row).growX().row();
        }
        if (snapshot.canStart()) add(action(root.isStarted() ? "Finish starting" : "Start checklist", () -> handler.startRequested())).row();
        else if (root.isComplete()) add(new Label("Checklist complete", getSkin())).row();
        else if (root.canComplete()) add(action("Complete checklist", () -> handler.completeRequested(root.getLiveKey()))).row();
        statusRows();
        invalidateHierarchy();
    }

    private void statusRows() {
        if (pending) add(new Label(snapshot == null ? "Loading…" : "Saving…", getSkin())).row();
        if (error != null && !error.isEmpty()) {
            final Label message = new Label(error, getSkin());
            message.setWrap(true);
            add(message).growX().row();
        }
    }

    private TextButton action(final String text, final Runnable request) {
        // Logical viewport units: keep the whole action touchable even with a small theme font.
        final TextButton button = new TextButton(text, getSkin()) {
            @Override public float getMinWidth() { return Math.max(48f, super.getMinWidth()); }
            @Override public float getMinHeight() { return Math.max(48f, super.getMinHeight()); }
        };
        button.setDisabled(pending || handler == null);
        button.addListener(new ChangeListener() {
            @Override public void changed(final ChangeEvent event, final Actor actor) {
                if (pending || handler == null) return;
                pending = true; // Suppress another intent until the controller supplies a terminal state.
                button.setDisabled(true);
                request.run();
            }
        });
        return button;
    }
}
