package net.wti.ui.view.api;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/// BaseViewTable
///
/// Convenience base class for table-based LibGDX views:
///  - Implements ViewComponent.
///  - Carries a reference to Skin.
///  - Establishes consistent default padding and alignment.
///
/// Subclasses are expected to override refresh() to rebuild their content.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 02:37
public abstract class BaseViewTable extends Table implements IsView {

    protected final Skin skin;

    protected BaseViewTable(final Skin skin) {
        super(skin);
        this.skin = skin;
        defaults().growX().pad(2, 6, 2, 6);
        top().left();
    }

    /// Default dispose implementation is a no-op. Subclasses may override.
    @Override
    public void dispose() {
        // no-op by default
    }
}
