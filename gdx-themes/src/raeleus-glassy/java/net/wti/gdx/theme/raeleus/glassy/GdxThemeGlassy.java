package net.wti.gdx.theme.raeleus.glassy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.GdxTheme;

/// GdxThemeGlassy:
///
/// A libgdx skin from [Raeleus blog](https://ray3k.wordpress.com/artwork/glassy-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:37
public class GdxThemeGlassy implements GdxTheme {

    private final Skin skin;

    public GdxThemeGlassy() {
        skin = new Skin(Gdx.files.internal(getAssetPath() + "/glassy-ui.json"));
    }

    @Override
    public String getAssetPath() {
        return "cc-by-4/raeleus/glassy";
    }

    @Override
    public Skin getSkin() {
        return skin;
    }
}
