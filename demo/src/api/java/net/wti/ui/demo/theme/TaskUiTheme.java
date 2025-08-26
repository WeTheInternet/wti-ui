package net.wti.ui.demo.theme;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager;
import com.badlogic.gdx.utils.GdxRuntimeException;
import net.wti.ui.components.SizingTextTooltip;
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
                "cc-by-4/raeleus/sgx",
                new UiDataBundle(null, "cc-by-4/wti/common/wti-fonts.atlas"),
                new UiDataBundle("cc-by-4/wti/common/wti-common-ui.json", "cc-by-4/wti/common/wti-common-ui.atlas"),
                new UiDataBundle("cc-by-4/raeleus/sgx/sgx-ui.json", "cc-by-4/raeleus/sgx/sgx-ui.atlas"),
                new UiDataBundle("cc-by-4/wti/task-ui.json", "cc-by-4/raeleus/sgx/sgx-fonts.atlas")
        );
    }


    /** Configure global tooltip behavior once per app. */
    public void applyTooltipDefaults() {
        TooltipManager tm = TooltipManager.getInstance();
        tm.initialTime = 0.35f; // delay before showing
        tm.subsequentTime = 0.10f; // re-show quickly as you move between widgets
        tm.resetTime = 0.10f; // debounce closing
        tm.maxWidth = SizingTextTooltip.MAX_W;         // avoid super-wide tooltips
        tm.edgeDistance = 8f;       // don't hug the edge of the window
        tm.hideAll();
        // Optional:
    }


    /** Ensure the tooltip style exists and references the common drawable. */
    public TextTooltip.TextTooltipStyle tooltipStyle() {
        final Skin skin = getSkin();
        if (!skin.has("tooltip-default", TextTooltip.TextTooltipStyle.class)) {
            throw new GdxRuntimeException("Missing style: tooltip-default (expected in sgx-ui.json or task-ui.json)");
        }
        return skin.get("tooltip-default", TextTooltip.TextTooltipStyle.class);
    }
}
