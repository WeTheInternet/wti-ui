package net.wti.ui.demo.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.demo.api.ModelSettings;
import net.wti.ui.form.WtiForm;
import net.wti.ui.gdx.theme.GdxTheme;

/// SettingsPanel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 23:42
public class SettingsPanel extends WtiForm {
    public SettingsPanel(final GdxTheme theme) {
        super(theme);

        ModelSettings settings = ModelSettings.INSTANCE;
        final Skin skin = theme.getSkin();

        addIntegerField(settings, "defaultHour", "Default Hour")
                .addValidator(val -> {
                    if (val > 26 || val < 0) {
                        return "Invalid hour " + val + " must be between 0-26";
                    }
                    return null;
                });

        addIntegerField(settings, "defaultMinute", "Default Minute")
                .addValidator(val -> {
                    if (val > 59 || val < 0) {
                        return "Invalid minute " + val + " must be between 0-59";
                    }
                    return null;
                });

    }
}
