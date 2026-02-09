package net.wti.ui.gdx.theme;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import xapi.fu.Do;
import xapi.fu.In1;

/// GdxTheme:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 00:56
public interface GdxTheme {

    static boolean isGlAvailable() {
        return Gdx.graphics != null && (Gdx.gl != null || Gdx.gl20 != null || Gdx.gl30 != null);
    }

    Skin getSkin();

    String getAssetPath();

    boolean isLandscape();

    void setLandscape(boolean landscape);

    Do onOrientationChanged(In1<ViewMode> callback);

}
