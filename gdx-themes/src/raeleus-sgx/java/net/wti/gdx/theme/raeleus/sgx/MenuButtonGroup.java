package net.wti.gdx.theme.raeleus.sgx;

import com.badlogic.gdx.utils.Array;

/// MenuButtonGroup:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:53
public class MenuButtonGroup <T extends MenuButton> {
    private final Array<T> buttons;
    private T selected;

    public MenuButtonGroup() {
        buttons = new Array<T>();
    }

    public void add(T button) {
        if (button == null) throw new IllegalArgumentException("button cannot be null.");
        buttons.add(button);
        button.setMenuButtonGroup(this);
    }

    public Array<T> getButtons() {
        return buttons;
    }

    public void check(T button) {
        if (selected == null || !selected.equals(button)) {
            if (selected != null) selected.setChecked(false);
            button.setChecked(true);
            selected = button;
        }
    }

    public void uncheckAll() {
        for (MenuButton button : buttons) {
            if (button.isChecked()) button.setChecked(false);
        }

        selected = null;
    }

    public T getSelected() {
        return selected;
    }
}