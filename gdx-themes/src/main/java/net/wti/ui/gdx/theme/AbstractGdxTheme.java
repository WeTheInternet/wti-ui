package net.wti.ui.gdx.theme;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;

/// AbstractGdxTheme:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 18/04/2025 @ 23:26
public abstract class AbstractGdxTheme implements GdxTheme {

    private boolean landscape;
    private final SetLike<In1<Boolean>> orientationChangeListeners = X_Jdk.setLinkedSynchronized();

    @Override
    public boolean isLandscape() {
        return landscape;
    }

    @Override
    public void setLandscape(final boolean landscape) {
        boolean isChanged = this.landscape != landscape;
        this.landscape = landscape;
        if (isChanged) {
            for (In1<Boolean> listener : orientationChangeListeners) {
                listener.in(landscape);
            }
        }
    }

    @Override
    public Do onOrientationChanged(final In1<ViewMode> callback) {
        final In1<Boolean> cb =  isLandscape -> callback.in(
                isLandscape ? ViewMode.landscape : ViewMode.portrait
        );
        orientationChangeListeners.add(cb);
        return () -> {
            orientationChangeListeners.remove(cb);
        };
    }
}
