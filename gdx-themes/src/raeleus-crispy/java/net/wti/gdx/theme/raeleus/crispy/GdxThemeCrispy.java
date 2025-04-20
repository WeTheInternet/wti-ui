package net.wti.gdx.theme.raeleus.crispy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.AbstractGdxTheme;

/// GdxThemeCrispy:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:48
public class GdxThemeCrispy extends AbstractGdxTheme {
    private final Skin skin;

    public GdxThemeCrispy() {
        skin = new Skin(Gdx.files.internal(getAssetPath() + "/clean-crispy-ui.json"));
    }

    @Override
    public String getAssetPath() {
        return "cc-by-4/raeleus/crispy";
    }

    @Override
    public Skin getSkin() {
        return skin;
    }
}
