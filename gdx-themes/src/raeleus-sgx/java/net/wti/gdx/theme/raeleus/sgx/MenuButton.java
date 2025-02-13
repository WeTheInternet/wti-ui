package net.wti.gdx.theme.raeleus.sgx;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import net.wti.gdx.theme.raeleus.sgx.MenuList.MenuListStyle;

/// MenuButton:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:54
public class MenuButton <T> extends TextButton {
    private MenuList<T> menuList;
    private MenuButtonStyle style;
    private Vector2 menuListPosition;
    private StageHideListener hideListener;
    private MenuButtonGroup menuButtonGroup;


    public MenuButton(String text, Skin skin) {
        this(text, skin, "default");
    }

    public MenuButton(String text, Skin skin, String styleName) {
        this(text, skin.get(styleName, MenuButtonStyle.class));
        setSkin(skin);
    }

    public MenuButton(String text, MenuButtonStyle style) {
        super(text, style);
        setStyle(style);

        menuList = new MenuList(style.menuListStyle);

        menuList.addCaptureListener(new MenuList.MenuListListener() {
            @Override
            public void menuClicked() {
                fire(new MenuButtonEvent());
                setChecked(false);
            }
        });

        menuListPosition = new Vector2();

        addListener(new MbGroupInputListener(this));

        addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                if (isChecked() && getStage() != null) {
                    localToStageCoordinates(menuListPosition.set(0.0f, 0.0f));
                    menuList.show(menuListPosition, getStage());

                    if (menuButtonGroup != null) {
                        menuButtonGroup.check((MenuButton) actor);
                    }

                    hideListener = new StageHideListener((MenuButton) actor, getStage());
                    getStage().addListener(hideListener);
                } else {
                    menuList.hide();
                    if (menuButtonGroup != null) {
                        menuButtonGroup.uncheckAll();
                    }
                }
            }
        });
    }

    public MenuList getMenuList() {
        return menuList;
    }

    public MenuButtonGroup getMenuButtonGroup() {
        return menuButtonGroup;
    }

    public void setMenuButtonGroup(MenuButtonGroup menuButtonGroup) {
        this.menuButtonGroup = menuButtonGroup;
    }

    public Array<T> getItems() {
        return menuList.getItems();
    }

    public void setItems(Array<T> newItems) {
        if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.");
        menuList.setItems(newItems);
    }

    public void setItems(T... newItems) {
        if (newItems == null) throw new IllegalArgumentException("newItems cannot be null.");
        menuList.setItems(newItems);
    }

    public void clearItems() {
        menuList.clearItems();
    }

    public Array<String> getShortcuts() {
        return menuList.getShortcuts();
    }

    public void setShortcuts(Array<String> shortcuts) {
        if (shortcuts == null) throw new IllegalArgumentException("shortcuts cannot be null.");
        menuList.setShortcuts(shortcuts);
    }

    public void setShortcuts(String... shortcuts) {
        if (shortcuts == null) throw new IllegalArgumentException("shortcuts cannot be null.");
        menuList.setShortcuts(shortcuts);
    }

    public void clearShortcuts() {
        menuList.clearItems();
    }

    public Array<TextButton> getButtons() {
        return menuList.getButtons();
    }

    public int getSelectedIndex() {
        return menuList.getSelectedIndex();
    }

    public T getSelectedItem() {
        return menuList.getSelectedItem();
    }

    public void setDisabled(int index, boolean disabled) {
        menuList.setDisabled(index, disabled);
    }

    public void setDisabled(T item, boolean disabled) {
        menuList.setDisabled(item, disabled);
    }

    public void updateContents() {
        menuList.updateContents();
    }

    @Override
    public void setStyle(ButtonStyle style) {
        if (style == null) {
            throw new NullPointerException("style cannot be null");
        }
        if (!(style instanceof MenuButtonStyle)) {
            throw new IllegalArgumentException("style must be a MenuButtonStyle.");
        }
        super.setStyle(style);
        this.style = (MenuButtonStyle) style;
        if (menuList != null) {
            menuList.setStyle(this.style.menuListStyle);
        }
    }

    @Override
    public MenuButtonStyle getStyle() {
        return style;
    }

    public static class MenuButtonStyle extends TextButtonStyle {
        public MenuListStyle menuListStyle;
    }

    private static class MbGroupInputListener extends InputListener {
        final private MenuButton menuButton;

        public MbGroupInputListener(MenuButton menuButton) {
            this.menuButton = menuButton;
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (menuButton.menuButtonGroup != null
                    && menuButton.menuButtonGroup.getSelected() != null) {
                menuButton.menuButtonGroup.check(menuButton);
            }
        }

    }

    private static class StageHideListener extends InputListener {
        private final MenuButton menuButton;
        private final Stage stage;

        public StageHideListener(MenuButton menuButton, Stage stage) {
            this.menuButton = menuButton;
            this.stage = stage;
        }

        @Override
        public boolean keyDown(InputEvent event, int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                menuButton.setChecked(false);
                stage.removeListener(this);
            }
            return false;
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer,
                                 int button) {
            Actor target = event.getTarget();
            if (menuButton.isAscendantOf(target) || menuButton.getMenuList().isAscendantOf(target)) {
                return false;
            } else {
                menuButton.setChecked(false);
                stage.removeListener(this);
                return false;
            }
        }
    }

    public static class MenuButtonEvent extends Event {
    }

    public static abstract class MenuButtonListener implements EventListener {

        @Override
        public boolean handle(Event event) {
            if (event instanceof MenuButtonEvent) {
                menuClicked();
                return true;
            } else {
                return false;
            }
        }

        public abstract void menuClicked();
    }
}