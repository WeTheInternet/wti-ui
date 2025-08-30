package net.wti.ui.gdx.view;


import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import net.wti.ui.gdx.theme.GdxTheme;

import java.util.LinkedHashMap;
import java.util.Map;

/// Accordion
///
/// A tiny two-state accordion container for Scene2D where **only one section is
/// open at a time**. Designed for the Today view's portrait mode:
///
/// - Each section has a **TextButton header** (click to open)
/// - The open section's body is shown; others are hidden/disabled
/// - Minimal assumptions about styling—use your skin's `actionbar` style for
///   compact headers, or swap to any `TextButtonStyle` name you prefer.
///
/// ### API
/// - `addSection(key, content)`   → Register a titled section
/// - `openOnly(key)`              → Open one section and close others
///
/// ### Skin dependencies
/// - `TextButton` style name: `"actionbar"` (change below if needed)
///
/// ### Usage
/// ```java
/// Accordion acc = new Accordion(skin);
/// acc.addSection("Deadlines", deadlinesWidget);
/// acc.addSection("Goals", goalsWidget);
/// acc.openOnly("Deadlines");
/// root.add(acc).grow();
///```
///
/// This class is intentionally small and self-contained; extend or theme as you
/// like (e.g., add chevrons, selected colors, etc.).
///
/// Created by James X. Nelson (James@WeTheInter.net) and chatgpt on 27/08/2025 @ 05:00
public class AccordionPane extends Table {

    private final Map<String, Section> sections = new LinkedHashMap<>();
    private String openKey;
    private String headerStyle = "emphasis"; // change if you want a different header style
    private boolean disposed;

    public AccordionPane(GdxTheme theme) {
        super(theme.getSkin());
        top();
        defaults().growX();
    }

    /// Add a new accordion section. Call before `openOnly`.
    public void addSection(String key, Actor content) {
        if (sections.containsKey(key)) return;

        final Skin skin = getSkin();
        TextButton header = new TextButton(key, skin, headerStyle);
        header.getLabel().setAlignment(Align.center);
        header.setText(key);
        header.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                openOnly(key);
            }
        });

        Table body = new Table(skin);
        body.add(content).grow();

        Section s = new Section(header, body);
        sections.put(key, s);
        rebuild();
    }

    /// Open exactly one section (by key); others collapse.
    public void openOnly(String key) {
        openKey = key;
        rebuild();
    }

    /// Rebuild the accordion layout to reflect open/closed state.
    private void rebuild() {
        clearChildren();
        for (Map.Entry<String, Section> e : sections.entrySet()) {
            String key = e.getKey();
            Section s = e.getValue();
            s.header.setText(key);
            add(s.header).height(32).pad(2, 0, 0, 0).growX().row();
            boolean open = key.equals(openKey);
            s.body.setVisible(open);
            s.body.setTouchable(open ? Touchable.enabled : Touchable.disabled);

            if (open) add(s.body).grow().pad(2, 0, 8, 0).row();
        }
    }

    /// Optional: override the TextButton style used for headers.
    public void setHeaderStyle(String styleName) {
        this.headerStyle = styleName;
        // Relabel headers with the new style 
        for (Section s : sections.values()) {
            CharSequence title = s.header.getText();
            String name = s.header.getText().toString();
            s.header.remove();
            s.header = new TextButton(title.toString(), getSkin(), headerStyle);
            s.header.setText(name);
            s.header.getLabel().setAlignment(Align.left);
        }
        rebuild();
    }

    /// Internal record for a single section (header + body).
    private static class Section {
        TextButton header;
        final Table body;

        Section(TextButton header, Table body) {
            this.header = header;
            this.body = body;
        }
    }

}