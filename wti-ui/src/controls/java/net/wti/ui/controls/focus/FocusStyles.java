package net.wti.ui.controls.focus;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Per-scope focus presentation. Copies styles, never changes the caller's shared skin. */
final class FocusStyles {
    private Actor actor;
    private Object original;

    void clear() {
        if (actor instanceof Button) ((Button) actor).setStyle((Button.ButtonStyle) original);
        else if (actor instanceof SelectBox) ((SelectBox<?>) actor).setStyle((SelectBox.SelectBoxStyle) original);
        actor = null; original = null;
    }

    void show(final Actor target) {
        clear();
        if (target instanceof Button) {
            final Button button = (Button) target;
            final Button.ButtonStyle style = button.getStyle();
            final Button.ButtonStyle focused;
            if (style.getClass() == CheckBox.CheckBoxStyle.class) {
                focused = new CheckBox.CheckBoxStyle((CheckBox.CheckBoxStyle) style);
            }
            else if (style.getClass() == ImageTextButton.ImageTextButtonStyle.class) {
                focused = new ImageTextButton.ImageTextButtonStyle((ImageTextButton.ImageTextButtonStyle) style);
            }
            else if (style.getClass() == TextButton.TextButtonStyle.class) {
                focused = new TextButton.TextButtonStyle((TextButton.TextButtonStyle) style);
            }
            else if (style.getClass() == ImageButton.ImageButtonStyle.class) {
                focused = new ImageButton.ImageButtonStyle((ImageButton.ImageButtonStyle) style);
            }
            else if (style.getClass() == Button.ButtonStyle.class) focused = new Button.ButtonStyle(style);
            // Custom widgets can require extra style fields and reject a sliced base style.
            else return;
            if (style.over != null) focused.up = style.over;
            if (style.checkedOver != null) focused.checked = style.checkedOver;
            if (focused instanceof TextButton.TextButtonStyle) {
                final TextButton.TextButtonStyle text = (TextButton.TextButtonStyle) focused;
                text.fontColor = text.overFontColor == null ? Color.WHITE : text.overFontColor;
                text.disabledFontColor = text.fontColor;
            } else if (focused instanceof ImageTextButton.ImageTextButtonStyle) {
                final ImageTextButton.ImageTextButtonStyle text = (ImageTextButton.ImageTextButtonStyle) focused;
                text.fontColor = text.overFontColor == null ? Color.WHITE : text.overFontColor;
                text.disabledFontColor = text.fontColor;
            }
            actor = target; original = style; button.setStyle(focused);
        } else if (target instanceof SelectBox) {
            final SelectBox<?> box = (SelectBox<?>) target;
            final SelectBox.SelectBoxStyle style = box.getStyle();
            final SelectBox.SelectBoxStyle focused = new SelectBox.SelectBoxStyle(style);
            if (style.backgroundOpen != null) focused.background = style.backgroundOpen;
            focused.fontColor = Color.WHITE;
            actor = target; original = style; box.setStyle(focused);
        }
        // TextField already renders its cursor and focusedBackground from Stage keyboard focus.
    }
}
