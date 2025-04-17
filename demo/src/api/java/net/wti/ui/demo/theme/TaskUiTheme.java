package net.wti.ui.demo.theme;

import net.wti.ui.gdx.theme.CompositeGdxTheme;
import net.wti.ui.gdx.theme.UiDataBundle;

///
/// TaskUiTheme:
///
/// A composite theme of the "stock" sgx-ui fram raeleus, and wti-specific add-ons.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 17/04/2025 @ 01:28
public class TaskUiTheme extends CompositeGdxTheme {
    public TaskUiTheme() {
        super(
                new UiDataBundle("cc-by-4/raeleus/sgx/sgx-ui.json", "cc-by-4/raeleus/sgx/sgx-ui.atlas"),
                new UiDataBundle("cc-by-4/wti/task-ui.json")
        );
    }
}
