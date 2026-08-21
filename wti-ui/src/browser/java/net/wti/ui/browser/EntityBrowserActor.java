package net.wti.ui.browser;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Disposable;

import java.util.LinkedHashMap;
import java.util.Map;

/// Scene2D composition for EntityBrowserModel.
///
/// The actor lays out caller-rendered content as a configurable list/grid,
/// converts clicks and keys into selection/activation intents, and contains no
/// domain mutation, modal placement, routing, or close policy. Mutate this
/// actor and its bound model on the libGDX render thread in real backends.
public class EntityBrowserActor<E> extends Table implements Disposable {

    private final EntityBrowserModel<E> model;
    private final EntityBrowserCellRenderer<? super E> renderer;
    private final Table pageTable;
    private final Map<String, Actor> cellsByKey;
    private final EntityBrowserListener<E> modelListener;

    private Actor emptyActor;
    private int columns;

    /// Creates a browser over one model using caller-owned cell content.
    public EntityBrowserActor(
            final EntityBrowserModel<E> model,
            final EntityBrowserCellRenderer<? super E> renderer,
            final int columns
    ) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }
        requireColumns(columns);
        this.model = model;
        this.renderer = renderer;
        this.columns = columns;
        this.pageTable = new Table();
        this.cellsByKey = new LinkedHashMap<>();
        this.modelListener = new EntityBrowserListener<E>() {
            @Override
            public void contentsChanged(final EntityBrowserModel<E> changedModel) {
                refresh();
            }

            @Override
            public void selectionChanged(
                    final EntityBrowserModel<E> changedModel,
                    final String previousKey,
                    final String selectedKey,
                    final E selectedEntity
            ) {
                updateCellStates();
            }
        };

        setTouchable(Touchable.enabled);
        add(pageTable).grow();
        installInput();
        model.addListener(modelListener);
        refresh();
    }

    /// Returns the renderer-neutral model bound to this actor.
    public EntityBrowserModel<E> getModel() {
        return model;
    }

    /// Returns the internal bounded-page layout table.
    public Table getPageTable() {
        return pageTable;
    }

    /// Returns the positive list/grid column count.
    public int getColumns() {
        return columns;
    }

    /// Switches between one-column list and multi-column grid layout.
    public void setColumns(final int columns) {
        requireColumns(columns);
        if (this.columns != columns) {
            this.columns = columns;
            refresh();
        }
    }

    /// Sets optional caller-owned content shown for an empty result.
    public void setEmptyActor(final Actor emptyActor) {
        if (this.emptyActor != emptyActor) {
            this.emptyActor = emptyActor;
            refresh();
        }
    }

    /// Returns the current caller-owned empty actor, if any.
    public Actor getEmptyActor() {
        return emptyActor;
    }

    /// Exposes a current-page cell for inspection and presentation composition.
    public Actor getCellActor(final String stableKey) {
        return cellsByKey.get(stableKey);
    }

    /// Rebuilds only the bounded current page from caller-provided actors.
    public void refresh() {
        pageTable.clear();
        cellsByKey.clear();
        if (model.isEmpty()) {
            if (emptyActor != null) {
                pageTable.add(emptyActor).grow();
            }
            invalidateHierarchy();
            return;
        }

        int column = 0;
        for (final E entity : model.getPageEntries()) {
            final String stableKey = model.keyOf(entity);
            final EntityBrowserCellState state = stateFor(stableKey);
            final Actor cell = renderer.createCell(entity, stableKey, state);
            if (cell == null) {
                throw new IllegalStateException(
                        "Entity browser renderer returned null for key " + stableKey
                );
            }
            attachSelection(cell, stableKey);
            cellsByKey.put(stableKey, cell);
            pageTable.add(cell).grow();
            column++;
            if (column == columns) {
                pageTable.row();
                column = 0;
            }
        }
        invalidateHierarchy();
    }

    /// Requests keyboard focus without imposing application-level restoration.
    public boolean requestBrowserFocus() {
        final Stage stage = getStage();
        if (stage == null) {
            return false;
        }
        stage.setKeyboardFocus(this);
        updateCellStates();
        return true;
    }

    /// Pushes current selection/focus state into every visible caller cell.
    protected void updateCellStates() {
        for (final Map.Entry<String, Actor> entry : cellsByKey.entrySet()) {
            final String stableKey = entry.getKey();
            final E entity = model.getEntry(stableKey);
            if (entity != null) {
                renderer.updateCell(
                        entry.getValue(),
                        entity,
                        stableKey,
                        stateFor(stableKey)
                );
            }
        }
    }

    private void installInput() {
        addListener(new InputListener() {
            @Override
            public boolean keyDown(final InputEvent event, final int keycode) {
                switch (keycode) {
                    case Keys.LEFT:
                        return model.moveSelection(EntityBrowserDirection.LEFT, columns);
                    case Keys.RIGHT:
                        return model.moveSelection(EntityBrowserDirection.RIGHT, columns);
                    case Keys.UP:
                        return model.moveSelection(EntityBrowserDirection.UP, columns);
                    case Keys.DOWN:
                        return model.moveSelection(EntityBrowserDirection.DOWN, columns);
                    case Keys.HOME:
                        return model.moveSelection(EntityBrowserDirection.HOME, columns);
                    case Keys.END:
                        return model.moveSelection(EntityBrowserDirection.END, columns);
                    case Keys.PAGE_UP:
                        return model.previousPage();
                    case Keys.PAGE_DOWN:
                        return model.nextPage();
                    case Keys.ENTER:
                    case Keys.NUMPAD_ENTER:
                        return model.activateSelection();
                    default:
                        return false;
                }
            }
        });
        addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(
                    final FocusEvent event,
                    final Actor actor,
                    final boolean focused
            ) {
                updateCellStates();
            }
        });
    }

    private void attachSelection(final Actor cell, final String stableKey) {
        cell.addListener(new ClickListener() {
            @Override
            public void clicked(final InputEvent event, final float x, final float y) {
                model.setSelectedKey(stableKey);
                requestBrowserFocus();
            }
        });
    }

    private EntityBrowserCellState stateFor(final String stableKey) {
        final boolean selected = stableKey.equals(model.getSelectedKey());
        final Stage stage = getStage();
        final boolean keyboardCurrent =
                selected && stage != null && stage.getKeyboardFocus() == this;
        return new EntityBrowserCellState(selected, keyboardCurrent);
    }

    private static void requireColumns(final int columns) {
        if (columns <= 0) {
            throw new IllegalArgumentException("columns must be greater than zero");
        }
    }

    /// Detaches model listeners and clears this actor hierarchy.
    @Override
    public void dispose() {
        model.removeListener(modelListener);
        cellsByKey.clear();
        clear();
    }
}
