package com.ray3k.sgx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.sgx.*;
import net.wti.ui.gdx.theme.CompositeGdxTheme;
import net.wti.ui.gdx.theme.GdxTheme;
import net.wti.ui.gdx.theme.UiDataBundle;
import net.wti.ui.view.responsive.FloatingPanel;
import net.wti.ui.view.responsive.FloatingPanelLayer;
import net.wti.ui.view.responsive.FloatingPanelStyle;
import net.wti.ui.view.responsive.ResponsiveAccordion;

/// SgxSample:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 02:51
public class SgxSample extends ApplicationAdapter {
    private Skin skin;
    private Stage stage;
    private FloatingPanelLayer floatingPanels;

    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(1280, 640);
        new Lwjgl3Application(new SgxSample(), config);
    }

    @Override
    public void create() {
        final GdxTheme theme = new CompositeGdxTheme(
                "cc-by-4/raeleus/sgx",
                new UiDataBundle(null, "cc-by-4/wti/common/wti-fonts.atlas"),
                new UiDataBundle(
                        "cc-by-4/wti/common/wti-common-ui.json",
                        "cc-by-4/wti/common/wti-common-ui.atlas"
                ),
                new UiDataBundle(
                        "cc-by-4/raeleus/sgx/sgx-ui.json",
                        "cc-by-4/raeleus/sgx/sgx-ui.atlas"
                ),
                new UiDataBundle(null, "cc-by-4/raeleus/sgx/sgx-fonts.atlas")
        );
        skin = theme.getSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Texture texture = new Texture(Gdx.files.internal(theme.getAssetPath() + "/background.png"));
        Image background = new Image(texture);
        background.setFillParent(true);
        stage.addActor(background);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        MenuButtonGroup menuButtonGroup = new MenuButtonGroup();
        Table menuBar = new Table(skin);
        menuBar.setBackground("file-menu-bar");
        root.add(menuBar).growX();

        final MenuButton fileMenuButton = new MenuButton("File", skin);
        fileMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(fileMenuButton);
        menuButtonGroup.add(fileMenuButton);
        fileMenuButton.setItems("Save", "Save As...", "Open...", "Exit");
        fileMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                System.out.println(fileMenuButton.getSelectedIndex() + " " + fileMenuButton.getSelectedItem());
            }
        });

        final MenuButton editMenuButton = new MenuButton("Edit", skin);
        editMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(editMenuButton);
        menuButtonGroup.add(editMenuButton);
        editMenuButton.setItems("Undo", "Redo", "Preferences...");
        editMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                System.out.println(editMenuButton.getSelectedIndex() + " " + editMenuButton.getSelectedItem());
            }
        });

        final MenuButton helpMenuButton = new MenuButton("Help", skin);
        helpMenuButton.getLabelCell().padLeft(5.0f).padRight(5.0f);
        menuBar.add(helpMenuButton).expandX().left();
        menuButtonGroup.add(helpMenuButton);
        helpMenuButton.setItems("About");
        helpMenuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                System.out.println(helpMenuButton.getSelectedIndex() + " " + helpMenuButton.getSelectedItem());
            }
        });

        root.row();
        Table table = new Table();
        root.add(table).padTop(5.0f).growX();

        table.add().expandX().width(300);

        Table subTable = new Table();
        subTable.defaults().space(5.0f);
        table.add(subTable);

        ImageButton imageButton = new ImageButton(skin);
        imageButton.setDisabled(true);
        subTable.add(imageButton);

        imageButton = new ImageButton(skin);
        imageButton.setDisabled(true);
        subTable.add(imageButton);

        imageButton = new ImageButton(skin);
        subTable.add(imageButton);

        imageButton = new ImageButton(skin);
        subTable.add(imageButton);

        Label label = new Label("SGX UI", skin, "title-white");
        label.setAlignment(Align.right);
        table.add(label).expandX().right().width(275).padRight(25.0f);

        root.row();
        table = new Table();
        root.add(table).padTop(5.0f);

        TextButton textButton = new TextButton("2", skin, "number");
        table.add(textButton);

        textButton = new TextButton("3", skin, "number");
        table.add(textButton);

        textButton = new TextButton("4", skin, "number");
        table.add(textButton);

        textButton = new TextButton("5", skin, "number");
        textButton.setDisabled(true);
        table.add(textButton);

        table.row();
        textButton = new TextButton("6", skin, "number");
        table.add(textButton);

        textButton = new TextButton("7", skin, "number");
        table.add(textButton);

        textButton = new TextButton("8", skin, "number");
        table.add(textButton);

        textButton = new TextButton("9", skin, "number");
        table.add(textButton);

        table.row();
        textButton = new TextButton("10", skin, "number");
        table.add(textButton);

        textButton = new TextButton("11", skin, "number");
        table.add(textButton);

        textButton = new TextButton("12", skin, "number");
        table.add(textButton);

        root.row();
        TabbedPane tabbedPane = new  TabbedPane(skin, Align.left);
        root.add(tabbedPane).padTop(10.0f);

        table = new Table();
        tabbedPane.addTab("Player", table);

        label = new Label("What is your name?", skin, "small");
        table.add(label);

        TextField textField = new TextField("", skin);
        table.add(textField);

        table.row();
        subTable = new Table();
        subTable.defaults().left();
        table.add(subTable).colspan(2);

        ImageTextButton checkBox = new ImageTextButton("Mute Audio", skin, "checkbox");
        subTable.add(checkBox).colspan(2);

        subTable.row();
        checkBox = new ImageTextButton("Play BGM", skin, "checkbox");
        checkBox.setDisabled(true);
        subTable.add(checkBox).colspan(2);

        table = new Table();
        tabbedPane.addTab("Options", table);

        root.row();
        table = new Table();
        table.defaults().space(35.0f).minWidth(150.0f);
        root.add(table).padTop(10.0f);

        final TextButton playButton = new TextButton("Play", skin);
        table.add(playButton);
        playButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent ce, Actor actor) {
                FileChooser fileChooser = new FileChooser("Choose a file...", skin, Gdx.files.local("/"));
                fileChooser.setFileNameEnabled(true);
                fileChooser.show(stage);
            }
        });

        textButton = new TextButton("Level Select", skin);
        textButton.setDisabled(true);
        table.add(textButton);

        textButton = new TextButton("Quit", skin);
        table.add(textButton);

        root.row();
        table = new Table();
        table.defaults().space(35.0f).minWidth(150.0f);
        root.add(table).padTop(10.0f);

        textButton = new TextButton("Play", skin, "emphasis");
        table.add(textButton);

        textButton = new TextButton("Level Select", skin, "emphasis");
        textButton.setDisabled(true);
        table.add(textButton);

        textButton = new TextButton("Quit", skin, "emphasis");
        table.add(textButton);

        root.row();
        table = new Table();
        table.defaults().space(35.0f).minWidth(150.0f);
        root.add(table).padTop(10.0f);

        textButton = new TextButton("Play", skin, "emphasis-colored");
        table.add(textButton);

        textButton = new TextButton("Level Select", skin, "emphasis-colored");
        textButton.setDisabled(true);
        table.add(textButton);

        textButton = new TextButton("Quit", skin, "emphasis-colored");
        table.add(textButton);

        root.row();
        Spinner spinner = new Spinner(0.0, 1.0, true, Spinner.Orientation.HORIZONTAL, skin);
        root.add(spinner).padTop(5.0f);

        root.row();
        SelectBox selectBox = new SelectBox(skin);
        selectBox.setItems("Easy Automatic", "Baby Mode", "Normal", "Difficult", "Hell");
        root.add(selectBox).padTop(5.0f).expandY().top();

        root.row();
        table = new Table();
        table.defaults().expandX();
        root.add(table).growX().padLeft(10.0f).padRight(10.0f).padBottom(5.0f);

        label = new Label("ray3k.wordpress.com", skin);
        table.add(label).left();

        label = new Label("ray3k.wordpress.com", skin, "medium");
        table.add(label);

        label = new Label("ray3k.wordpress.com", skin, "small");
        table.add(label).right();

        Window window = new Window("Settings", skin, "tool");
        stage.addActor(window);
        window.setSize(300.0f, 350.0f);
        window.setPosition(Gdx.graphics.getWidth() - 20.0f, Gdx.graphics.getHeight() / 2.0f, Align.right);
        window.getTitleLabel().setAlignment(Align.center);
        window.getTitleTable().getCells().first().padLeft(20.0f);

        Button button = new Button(skin, "close");
        window.getTitleTable().add(button);

        label = new Label("Preview", skin, "white");
        window.add(label);

        window.row();
        table = new Table();
        table.defaults().left();
        window.add(table).padTop(5.0f);

        ButtonGroup buttonGroup = new ButtonGroup();
        ImageTextButton radioButton = new ImageTextButton("Wireframe Display", skin, "radio");
        buttonGroup.add(radioButton);
        table.add(radioButton);

        table.row();
        radioButton = new ImageTextButton("Live Preview", skin, "radio");
        buttonGroup.add(radioButton);
        table.add(radioButton);

        table.row();
        radioButton = new ImageTextButton("Shaded Render", skin, "radio");
        buttonGroup.add(radioButton);
        table.add(radioButton);

        table.row();
        radioButton = new ImageTextButton("Interactive Test", skin, "radio");
        radioButton.setDisabled(true);
        buttonGroup.add(radioButton);
        table.add(radioButton);

        table.row();
        radioButton = new ImageTextButton("Final Render", skin, "radio");
        radioButton.setDisabled(true);
        buttonGroup.add(radioButton);
        table.add(radioButton);

        window.row();
        label = new Label("Volume", skin, "white");
        window.add(label).padTop(15.0f);

        window.row();
        Slider slider = new Slider(0, 100, 1, false, skin);
        slider.setValue(50.0f);
        window.add(slider).padTop(5.0f);

        window.row();
        label = new Label("Distance Field", skin, "white");
        window.add(label).padTop(15.0f);

        window.row();
        slider = new Slider(0, 100, 1, false, skin);
        slider.setValue(50.0f);
        slider.setDisabled(true);
        window.add(slider).padTop(5.0f);

        addFloatingPanelShowcase();
    }

    /// Adds an interactive showcase for the reusable WTI floating-panel host.
    private void addFloatingPanelShowcase() {
        final FloatingPanelStyle panelStyle = new FloatingPanelStyle();
        panelStyle.edgePadding = 12f;
        panelStyle.cascadeGap = 22f;
        panelStyle.minimumWidth = 250f;
        panelStyle.minimumHeight = 180f;
        panelStyle.titleContentGap = 4f;

        floatingPanels = new FloatingPanelLayer(panelStyle);
        floatingPanels.setBounds(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.addActor(floatingPanels);
        addFloatingPanel("Floating Controls", floatingControls(), panelStyle);
        addFloatingPanel("Floating Options", floatingOptions(), panelStyle);
    }

    private void addFloatingPanel(
            final String title,
            final Table content,
            final FloatingPanelStyle panelStyle
    ) {
        final Table titleBar = new Table(skin);
        titleBar.setBackground("file-menu-bar");
        titleBar.padTop(4f).padBottom(4f);
        titleBar.add(new Label(title, skin, "white")).growX().left().padLeft(8f);
        final TextButton minimize = new TextButton("-", panelStateButtonStyle(false));
        titleBar.add(minimize).size(34f, 35f).padRight(2f);
        final Button redock = new Button(skin, "close");
        titleBar.add(redock).size(28f).padRight(4f);

        final FloatingPanel panel = new FloatingPanel(
                titleBar,
                content,
                skin.get(ScrollPane.ScrollPaneStyle.class),
                panelStyle
        );
        panel.setResizable(true);
        panel.setResizeHandleBackground(skin.getDrawable("button-small"));
        panel.setPanelBackground(skin.get("tool", Window.WindowStyle.class).background);
        panel.setRedockAction(new Runnable() {
            @Override
            public void run() {
                floatingPanels.removePanel(panel);
            }
        });
        minimize.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panel.setMinimized(!panel.isMinimized());
                minimize.setText(panel.isMinimized() ? "+" : "-");
                minimize.setStyle(panelStateButtonStyle(panel.isMinimized()));
            }
        });
        redock.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                panel.redock();
            }
        });
        floatingPanels.addPanel(panel);
    }

    /// Binds the title state control directly to the registered SGX plus/minus regions.
    private TextButton.TextButtonStyle panelStateButtonStyle(final boolean minimized) {
        final TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = skin.getFont("font");
        style.fontColor = skin.getColor("font");
        style.overFontColor = skin.getColor("font-over");
        style.downFontColor = skin.getColor("font");
        style.disabledFontColor = skin.getColor("font-disabled");
        style.up = skin.getDrawable("button-small");
        style.down = skin.getDrawable("button-small-pressed");
        style.over = skin.getDrawable("button-small-over");
        style.disabled = skin.getDrawable("button-small-disabled");
        return style;
    }

    private Table floatingControls() {
        final Table content = new Table(skin);
        content.defaults().left().pad(5f);
        content.add(new Label("Normal SGX controls inside a reusable viewport.", skin, "small"))
                .colspan(2).growX().left();
        content.row();
        content.add(new CheckBox("Show grid", skin)).colspan(2).left();
        content.row();
        content.add(new CheckBox("Snap to tiles", skin)).colspan(2).left();
        content.row();
        content.add(new Label("Brush size", skin, "small"));
        Slider brush = new Slider(1f, 100f, 1f, false, skin);
        brush.setValue(40f);
        content.add(brush).width(180f);
        content.row();
        content.add(new Label("Opacity", skin, "small"));
        Slider opacity = new Slider(1f, 100f, 1f, false, skin);
        opacity.setValue(75f);
        content.add(opacity).width(180f);
        return content;
    }

    private Table floatingOptions() {
        final Table content = new Table(skin);
        content.defaults().left().pad(5f);
        content.add(new Label("Drag title bars; resize to test clamping.", skin, "small"))
                .colspan(2).growX().left();
        content.row();
        ButtonGroup group = new ButtonGroup();
        ImageTextButton normal = new ImageTextButton("Normal mode", skin, "radio");
        ImageTextButton preview = new ImageTextButton("Preview mode", skin, "radio");
        ImageTextButton disabled = new ImageTextButton("Unavailable mode", skin, "radio");
        disabled.setDisabled(true);
        group.add(normal, preview, disabled);
        content.add(normal).colspan(2).left();
        content.row();
        content.add(preview).colspan(2).left();
        content.row();
        content.add(disabled).colspan(2).left();
        content.row();
        content.add(new Label("More content demonstrates scrolling.", skin, "small"))
                .colspan(2).left();
        content.row();
        content.add(floatingAccordion()).colspan(2).growX().left();
        content.row();
        for (int i = 0; i < 8; i++) {
            content.add(new CheckBox("Option " + (i + 1), skin)).colspan(2).left();
            content.row();
        }
        return content;
    }

    /// Demonstrates the reusable multi-open accordion using the active SGX Skin.
    private ResponsiveAccordion floatingAccordion() {
        final ResponsiveAccordion accordion = new ResponsiveAccordion(skin);

        final Table display = new Table(skin);
        display.defaults().left().pad(3f);
        display.add(new CheckBox("Show grid", skin)).left().row();
        display.add(new CheckBox("Snap to tiles", skin)).left().row();
        final Table displayHeader = accordionHeader(accordion, "Display", "Display", true);
        accordion.addSection("Display", displayHeader, display);

        final Table tools = new Table(skin);
        tools.defaults().left().pad(3f);
        tools.add(new CheckBox("Live preview", skin)).left().row();
        tools.add(new CheckBox("Highlight selection", skin)).left().row();
        final Table toolsHeader = accordionHeader(accordion, "Tools", "Tools", false);
        accordion.addSection("Tools", toolsHeader, tools);
        accordion.expand("Display");
        return accordion;
    }

    /// Keeps the section state control inside the same styled header as its title.
    private Table accordionHeader(
            final ResponsiveAccordion accordion,
            final String key,
            final String title,
            final boolean expanded
    ) {
        final TextButton toggle = new TextButton(expanded ? "-" : "+", panelStateButtonStyle(expanded));
        toggle.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                accordion.toggle(key);
                final boolean nowExpanded = accordion.isExpanded(key);
                toggle.setText(nowExpanded ? "-" : "+");
                toggle.setStyle(panelStateButtonStyle(nowExpanded));
            }
        });
        toggle.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                event.stop();
            }
        });

        final Table header = new Table(skin);
        header.setBackground(skin.getDrawable("file-menu-bar"));
        header.add(new Label(title, skin, "white")).growX().center();
        header.add(toggle).size(29f, 27f).right().padRight(0f);
        header.setHeight(31f);
        return header;
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 0, 0, 1);
        Gdx.gl.glClear(GL31.GL_COLOR_BUFFER_BIT);

        stage.act();
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Keys.F5)) {
            dispose();
            create();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        stage.getViewport().update(width, height, true);
        if (floatingPanels != null) {
            floatingPanels.setBounds(0f, 0f, width, height);
            floatingPanels.clampPanels();
        }
    }

    @Override
    public void dispose() {
        skin.dispose();
        stage.dispose();
        floatingPanels = null;
    }
}
