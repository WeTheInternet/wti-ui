package net.wti.gdx.theme.raeleus.sgx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.AbstractGdxTheme;

/// GdxThemeSgx:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:51
public class GdxThemeSgx extends AbstractGdxTheme {

    private final Skin skin;

    public GdxThemeSgx() {
        skin = new Skin(Gdx.files.internal(getAssetPath() + "/sgx-ui.json"));
    }

    @Override
    public String getAssetPath() {
        return "cc-by-4/raeleus/sgx";
    }

    @Override
    public Skin getSkin() {
        return skin;
    }
}
