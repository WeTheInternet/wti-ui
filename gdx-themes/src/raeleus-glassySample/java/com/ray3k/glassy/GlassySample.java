package com.ray3k.glassy;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import net.wti.gdx.theme.raeleus.glassy.GdxThemeGlassy;


/// GlassySample:
///
/// Adapted from example zip from [Raeleus Blog](https://ray3k.wordpress.com/artwork/glassy-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 01:59
public class GlassySample extends ApplicationAdapter {
    private SpriteBatch spriteBatch;
    private Stage stage;
    private Skin skin;

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setWindowedMode(800, 800);
        new Lwjgl3Application(new GlassySample(), config);
    }

    class Node extends Tree.Node<Node, String, Label> {
        public Node(String text) {
            super(new Label(text, skin, "black"));
            setValue(text);
        }
    }

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        final GdxThemeGlassy theme = new GdxThemeGlassy();
        skin = theme.getSkin();
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(new Label("Glassy UI", skin, "big")).colspan(3);

        root.row();
        Table table = new Table();
        table.defaults().pad(10.0f);
        table.add(new TextButton("Story", skin));
        table.row();
        table.add(new TextButton("Options", skin));
        table.row();
        table.add(new TextButton("Quit", skin));
        root.add(table).expandX();

        table = new Table();
        table.defaults().pad(10.0f);
        table.add(new TextButton("Story", skin, "small"));
        table.row();
        table.add(new TextButton("Options", skin, "small"));
        table.row();
        table.add(new TextButton("Quit", skin, "small"));
        root.add(table).expandX();

        table = new Table();
        table.add(new Label("Difficulty", skin)).colspan(2);
        table.row();
        SelectBox selectBox = new SelectBox(skin);
        selectBox.setItems("Easy", "Difficult", "Extreme");
        table.add(selectBox).colspan(2);
        table.row();
        table.add(new Label("Name: ", skin)).padTop(15.0f);
        table.add(new TextField("Nameo", skin)).padTop(15.0f);
        root.add(table).expandX();

        root.row();
        root.add(new Label("reticulating splines...", skin)).colspan(3).padTop(5.0f);
        root.row();
        final ProgressBar progressBar = new ProgressBar(0, 100.0f, 1, false, skin);
        progressBar.setValue(50.0f);
        progressBar.setAnimateDuration(.2f);
        root.add(progressBar).colspan(3).growX();

        root.row();
        root.add(new Label("VOLUME", skin)).colspan(3).padTop(5.0f);
        root.row();
        final Slider slider = new Slider(0.0f, 100.0f, 1.0f, false, skin);
        slider.setValue(50.0f);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                progressBar.setValue(slider.getValue());
            }
        });
        root.add(slider).colspan(3).growX().expandY().top();

        Window window = new Window("Inventory", skin);
        table = new Table();
        window.add(table);
        window.setSize(400.0f, 200.0f);
        window.setPosition(stage.getWidth() / 2.0f, 25.0f, Align.bottom);
        stage.addActor(window);

        Tree<Tree.Node<Tree.Node, String, Label>, String> tree = new Tree<>(skin);

        Tree.Node parent = new Node("Backpack");
        tree.add(parent);
        Tree.Node child = new Node("Kitty Snacks");
        parent.add(child);
        child = new Node("Dripping Bastard Sword of Misfortune");
        parent.add(child);
        child = new Node("Redeeming Pencil of Noteworthiness");
        parent.add(child);
        parent = new Node("Belt");
        tree.add(parent);
        child = new Node("Soda Brand Soda");
        parent.add(child);
        child = new Node("Bundle of Pocket Lint");
        parent.add(child);
        child = new Node("Horadric Dodecahedron");
        parent.add(child);
        parent = child;
        child = new Node("Void Boogers");
        parent.add(child);
        child = new Node("Void Boogers");
        parent.add(child);
        child = new Node("Void Boogers");
        parent.add(child);

        ScrollPane scrollPane = new ScrollPane(tree, skin);
        scrollPane.setFadeScrollBars(false);

        table = new Table(skin);
        table.setBackground("black");
        table.defaults().expandX().left().padLeft(10.0f);
        table.add(new CheckBox("CheckBox", skin));
        table.row();
        table.add(new CheckBox("CheckBox", skin));
        table.row();
        ButtonGroup<CheckBox> buttonGroup = new ButtonGroup<CheckBox>();
        CheckBox checkBox = new CheckBox("Radio Button", skin, "radio");
        buttonGroup.add(checkBox);
        table.add(checkBox);
        table.row();
        checkBox = new CheckBox("Radio Button", skin, "radio");
        buttonGroup.add(checkBox);
        table.add(checkBox);

        SplitPane splitPane = new SplitPane(scrollPane, table, false, skin);
        window.add(splitPane).grow();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(.5f, .5f, .5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        stage.dispose();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        super.resize(width, height);
    }
}
