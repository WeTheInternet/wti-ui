package net.wti.ui.gdx.theme;

/// AbstractGdxTheme:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 23:26
public abstract class AbstractGdxTheme implements GdxTheme {

    private boolean landscape;

    @Override
    public boolean isLandscape() {
        return landscape;
    }

    @Override
    public void setLandscape(final boolean landscape) {
        this.landscape = landscape;
    }
}
