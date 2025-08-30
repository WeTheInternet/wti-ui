package net.wti.ui.view.panes;


import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.controls.focus.HoverScrollFocus;

///
/// ListView: Shows a header + vertically scrolling content list.
///
/// This panel ensures the inner ScrollPane gains scroll focus when the mouse is over it.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 30/08/2025 @ 03:56
public class ListView {

    private final Skin skin;
    private final String title;

    private final Table root = new Table();
    protected final Table content = new Table();
    private final ScrollPane scroller;

    public ListView(Skin skin, String title) {
        this.skin = skin;
        this.title = title;

        root.defaults().growX();

        Label header = new Label(title, skin, "task-recurrence-value");
        header.setAlignment(Align.left);

        Table head = new Table(skin);
        head.add(header).expandX().left().pad(4, 6, 4, 6);

        scroller = new ScrollPane(content, skin);
        scroller.setFadeScrollBars(false);
        scroller.setScrollingDisabled(true, false); // vertical-only

        // Give scroll focus to the inner pane when hovered
        HoverScrollFocus.attach(scroller);

        root.add(head).height(28).row();
        root.add(scroller).grow().padTop(4);
    }

    public Table container() { return root; }


    private Drawable safeBg(Skin skin) {
        if (skin.has("button", Drawable.class)) return skin.getDrawable("button");
        if (skin.has("panel-actionbar", Drawable.class)) return skin.getDrawable("panel-actionbar");
        return null;
    }

}

