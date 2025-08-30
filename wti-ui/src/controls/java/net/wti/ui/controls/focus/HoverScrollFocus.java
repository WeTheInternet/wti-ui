package net.wti.ui.controls.focus;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;

///
/// HoverScrollFocus: A utility class to handle automatic scroll focus management for ScrollPane components.
///
/// This class attaches input listeners to a ScrollPane to automatically set scroll focus when the mouse
/// enters or moves within the pane's boundaries. This enables seamless scrolling interactions without
/// requiring explicit clicks to gain focus.
///
/// Usage:
/// ```
/// ScrollPane pane = new ScrollPane(content);
/// HoverScrollFocus.attach(pane);
///```
///
/// The attached listeners will:
/// - Set scroll focus when the mouse enters the pane
/// - Maintain scroll focus when the mouse moves within the pane
/// - Handle cases where the pane might have lost focus while the mouse is still over it
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/08/2025 @ 04:21
public final class HoverScrollFocus {

    private HoverScrollFocus() {
    }

    public static void attach(ScrollPane pane) {
        pane.addListener(new InputListener() {

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                Stage s = pane.getStage();
                if (s != null) s.setScrollFocus(pane);
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                Stage s = pane.getStage();
                if (s != null && s.getScrollFocus() != pane) s.setScrollFocus(pane);
                return false;
            }
        });
    }
}