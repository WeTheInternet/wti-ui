package net.wti.ui.gdx.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import java.util.ArrayList;
import java.util.List;

/// MultiFontLabel:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 23/04/2025 @ 14:41
public class MultiFontLabel extends Table {

    public static class Span {
        public final String text;
        public final BitmapFont font;
        public final Color color;

        public Span(String text, BitmapFont font, Color color) {
            this.text = text;
            this.font = font;
            this.color = color;
        }
    }

    private final List<Span> spans = new ArrayList<>();
    private final Skin skin;

    public MultiFontLabel(Skin skin) {
        this.skin = skin;
        this.defaults().left();
    }

    public void setText(List<Span> text) {
        this.spans.clear();
        this.clearChildren();

        for (Span span : text) {
            // need to pull this from the skin
            TextField.TextFieldStyle style = new TextField.TextFieldStyle();
            style.font = span.font;
            style.fontColor = span.color;
            TextField field = new TextField(span.text, style);
            field.setDisabled(true);
            field.setTouchable(Touchable.enabled);
            field.setFocusTraversal(false);
            field.setCursorPosition(0);
            field.setBlinkTime(0);
            this.add(field);
        }

        this.row();
    }

    public void clearText() {
        spans.clear();
        clearChildren();
    }
}
