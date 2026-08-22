package net.wti.ui.components.browser;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Disposable;

/// Optional detail host driven by browser selection.
///
/// The pane owns only its Scene2D child hierarchy. It performs no loading,
/// persistence, or domain mutation; callers provide already-available detail
/// actors from the selected entity snapshot.
public class EntityBrowserDetailPane<E> extends Table implements Disposable {

    private final EntityBrowserModel<E> model;
    private final EntityBrowserDetailRenderer<? super E> renderer;
    private final EntityBrowserListener<E> modelListener;

    private Actor emptyActor;
    private Actor currentDetail;

    /// Binds an optional detail host to one browser model.
    public EntityBrowserDetailPane(
            final EntityBrowserModel<E> model,
            final EntityBrowserDetailRenderer<? super E> renderer
    ) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer cannot be null");
        }
        this.model = model;
        this.renderer = renderer;
        this.modelListener = new EntityBrowserListener<E>() {
            @Override
            public void contentsChanged(final EntityBrowserModel<E> changedModel) {
                refreshDetail();
            }

            @Override
            public void selectionChanged(
                    final EntityBrowserModel<E> changedModel,
                    final String previousKey,
                    final String selectedKey,
                    final E selectedEntity
            ) {
                refreshDetail();
            }
        };
        model.addListener(modelListener);
        refreshDetail();
    }

    /// Sets caller-owned content shown when no entity is selected.
    public void setEmptyActor(final Actor emptyActor) {
        if (this.emptyActor != emptyActor) {
            this.emptyActor = emptyActor;
            refreshDetail();
        }
    }

    /// Returns the current caller-created detail actor, if any.
    public Actor getCurrentDetail() {
        return currentDetail;
    }

    /// Rebuilds detail content from the latest selected entity snapshot.
    public void refreshDetail() {
        clearChildren();
        currentDetail = null;
        final E selected = model.getSelectedEntity();
        if (selected == null) {
            if (emptyActor != null) {
                add(emptyActor).grow();
            }
            invalidateHierarchy();
            return;
        }
        final Actor detail = renderer.createDetail(selected, model.getSelectedKey());
        if (detail == null) {
            throw new IllegalStateException(
                    "Entity browser detail renderer returned null for key "
                            + model.getSelectedKey()
            );
        }
        currentDetail = detail;
        add(detail).grow();
        invalidateHierarchy();
    }

    /// Detaches the model listener and clears current presentation children.
    @Override
    public void dispose() {
        model.removeListener(modelListener);
        currentDetail = null;
        clearChildren();
    }
}
