package com.ray3k.shade;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL31;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.shade.GdxThemeShade;
import net.wti.ui.gdx.theme.GdxTheme;

/// ShadeSample:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/artwork/shade-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 03:14
public class ShadeSample extends ApplicationAdapter {
    private Stage stage;
    private Skin skin;

    class Node extends Tree.Node<Node, String, Label> {
        public Node(String text) {
            super(new Label(text, skin));
            setValue(text);
        }
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(600, 750);
        new Lwjgl3Application(new ShadeSample(), config);
    }

    @Override
    public void create() {
        final GdxTheme theme = new GdxThemeShade();
        skin = theme.getSkin();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table rootTable = new Table();
        rootTable.setFillParent(true);
        stage.addActor(rootTable);

        rootTable.add(new Label("Shade UI", skin, "title")).colspan(3);
        rootTable.row();

        Table buttonsTable = new Table();
        buttonsTable.defaults().pad(2.0f);
        TextButton dialogButton = new TextButton("Dialog", skin);
        buttonsTable.add(dialogButton);
        buttonsTable.row();
        buttonsTable.add(new TextButton("Toggle", skin, "toggle"));
        buttonsTable.row();
        buttonsTable.add(new TextButton("Start Game", skin, "round"));
        buttonsTable.row();
        Table subTable = new Table();
        subTable.defaults().pad(2.0f);
        subTable.add(new Button(skin, "music"));
        subTable.add(new Button(skin, "sound"));
        buttonsTable.add(subTable);
        rootTable.add(buttonsTable);

        Table checksTable = new Table();
        checksTable.defaults().pad(2.0f);
        checksTable.add(new CheckBox(" Standard Checkbox", skin));
        checksTable.row();
        checksTable.add(new CheckBox(" Switch Checkbox", skin, "switch"));
        checksTable.row();
        ButtonGroup radioButtonGroup = new ButtonGroup();
        Array<Button> buttons = new Array<Button>();
        for (int i = 1; i <= 5; i++) {
            buttons.add(new TextButton("Selection " + Integer.toString(i), skin, "radio"));
            checksTable.add(buttons.peek());
            checksTable.row();
            radioButtonGroup.add(buttons.peek());
        }
        checksTable.add();
        rootTable.add(checksTable);

        Table inputTable = new Table();
        inputTable.defaults().pad(2.0f);
        inputTable.add(new Label("Login Page", skin, "title-plain")).colspan(2);
        inputTable.row();
        inputTable.add(new Label(" Username: ", skin, "subtitle"));
        TextField textField = new TextField("", skin);
        textField.setMessageText("Username or Email");
        textField.setFocusTraversal(true);
        inputTable.add(textField);
        inputTable.row();
        inputTable.add(new Label(" Password: ", skin, "subtitle"));
        textField = new TextField("", skin);
        textField.setMessageText("Password");
        textField.setFocusTraversal(true);
        textField.setPasswordMode(true);
        textField.setPasswordCharacter('*');
        inputTable.add(textField);
        inputTable.row();
        inputTable.add(new Label("Choose a Server:", skin)).colspan(2).padTop(10.0f);
        inputTable.row();
        SelectBox selectBox = new SelectBox<String>(skin);
        selectBox.setItems(new Object[]{"United States (East)", "United States (West)", "China", "Europe", "Australia", "India"});
        selectBox.setMaxListCount(4);
        inputTable.add(selectBox).colspan(2);
        rootTable.add(inputTable);

        rootTable.row();
        rootTable.add(new Touchpad(0, skin)).pad(5.0f).colspan(2);

        Tree tree = new Tree(skin);
        Node parentNode = new Node("Selection 1");
        tree.add(parentNode);
        for (int i = 2; i <= 4; i++) {
            Node node = new Node("Selection " + i);
            parentNode.add(node);
            parentNode = node;
        }
        tree.expandAll();
        tree.setPadding(2);
        rootTable.add(tree).fill().pad(5.0f);

        rootTable.row();
        Table progressTable = new Table();
        progressTable.defaults().pad(2.0f);
        Button leftButton = new Button(skin, "left");
        progressTable.add(leftButton);
        final ProgressBar progressBar = new ProgressBar(0, 100, 1, false, skin);
        progressBar.setValue(50);
        progressBar.setAnimateDuration(.3f);
        progressTable.add(progressBar);
        Button rightButton = new Button(skin, "right");
        progressTable.add(rightButton);
        rootTable.add(progressTable).padTop(5.0f).colspan(3);

        rootTable.row();
        Slider slider = new Slider(0, 100, 1, false, skin);
        slider.setValue(50);
        rootTable.add(slider).colspan(3).fillX().padTop(5.0f);

        rootTable.row();

        Window window = new Window("Window", skin);
        window.getTitleLabel().setAlignment(Align.center);
        ScrollPane scrollPane = new ScrollPane(new Label("ScrollPane. ScrollPane. ScrollPane.\nScrollPane. ScrollPane. ScrollPane.\nScrollPane. ScrollPane. ScrollPane.\nScrollPane. ScrollPane. ScrollPane.\nScrollPane. ScrollPane. ScrollPane.\n", skin), skin);
        scrollPane.setFadeScrollBars(false);
        ScrollPane androidScrollPane = new ScrollPane(new Label("Android ScrollPane. Android ScrollPane.\nAndroid ScrollPane. Android ScrollPane.\nAndroid ScrollPane. Android ScrollPane.\nAndroid ScrollPane. Android ScrollPane.\nAndroid ScrollPane. Android ScrollPane.\n", skin), skin, "android");
        SplitPane splitPane2 = new SplitPane(scrollPane, androidScrollPane, false, skin);


        Table infoTable = new Table();
        Label authorLabel = new Label("Created by Raymond \"Raeleus\" Buckley", skin, "error");
        authorLabel.setAlignment(Align.center);
        infoTable.add(authorLabel);
        infoTable.row();
        Label webLabel = new Label("Visit ray3k.com for more stuff!", skin, "optional");
        webLabel.setAlignment(Align.center);
        infoTable.add(webLabel);

        SplitPane splitPane = new SplitPane(infoTable, splitPane2, true, skin);
        window.add(splitPane);
        rootTable.add(window).colspan(3).padTop(5.0f);

        dialogButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                new Dialog("Shade UI", skin, "dialog") {
                    protected void result(Object object) {
                        System.out.println("Chosen: " + object);
                    }
                }.text("Are you sure?").button("Yes", true).button("No", false)
                        .key(Keys.ENTER, true).key(Keys.ESCAPE, false).show(stage).getTitleLabel().setAlignment(Align.center);
            }
        });

        leftButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                progressBar.setValue(progressBar.getValue() - 5.0f);
            }
        });

        rightButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                progressBar.setValue(progressBar.getValue() + 5.0f);
            }
        });
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL31.GL_COLOR_BUFFER_BIT);

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}