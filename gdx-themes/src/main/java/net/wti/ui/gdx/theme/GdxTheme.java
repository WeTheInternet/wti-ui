package net.wti.ui.gdx.theme;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import xapi.fu.Do;
import xapi.fu.In1;

/// GdxTheme:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 00:56
public interface GdxTheme {

    Skin getSkin();

    String getAssetPath();

    boolean isLandscape();

    void setLandscape(boolean landscape);

    Do onOrientationChanged(In1<ViewMode> callback);

}
