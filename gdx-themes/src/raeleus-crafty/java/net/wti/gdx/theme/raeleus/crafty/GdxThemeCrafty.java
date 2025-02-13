package net.wti.gdx.theme.raeleus.crafty;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.wti.ui.gdx.theme.GdxTheme;

/// GdxThemeCrafty:
///
/// A ligbdx skin from [Raeleus blog](https://ray3k.wordpress.com/craftacular-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:25
public class GdxThemeCrafty implements GdxTheme {

    private final Skin skin;

    public GdxThemeCrafty() {
        skin = new Skin(Gdx.files.internal(getAssetPath() + "/craftacular-ui.json"));
    }

    @Override
    public String getAssetPath() {
        return "cc-by-4/raeleus/crafty";
    }

    @Override
    public Skin getSkin() {
        return skin;
    }
}
