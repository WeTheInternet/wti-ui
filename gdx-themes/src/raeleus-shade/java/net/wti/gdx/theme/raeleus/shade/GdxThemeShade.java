package net.wti.gdx.theme.raeleus.shade;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.GdxTheme;

/// GdxThemeShade:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 03:12
public class GdxThemeShade implements GdxTheme {

    private final Skin skin;

    public GdxThemeShade() {
        skin = new Skin(Gdx.files.internal(getAssetPath() + "/uiskin.json"));;
    }

    @Override
    public Skin getSkin() {
        return skin;
    }

    @Override
    public String getAssetPath() {
        return "cc-by-4/raeleus/shade";
    }
}
