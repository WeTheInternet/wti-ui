package net.wti.ui.sample;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/// AbstractSampleApp
///
/// Generic LibGDX JRE application base:
///  - Creates a Stage with a ScreenViewport.
///  - Loads a default Skin (customizable).
///  - Delegates content creation to subclasses.
///  - Handles basic render loop and disposal.
///
/// This class is intended for "sample apps" that demonstrate UI components.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/12/2025 @ 03:35
public abstract class AbstractSampleApp extends ApplicationAdapter {

    protected Stage stage;
    protected Skin skin;

    @Override
    public void create() {
        stage = new Stage(new ScreenViewport());
        skin = createSkin();

        final InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(multiplexer);

        createContent(stage, skin);
    }

    /// Subclasses create their UI here.
    protected abstract void createContent(Stage stage, Skin skin);

    /// Override if you want a custom skin loader.
    protected Skin createSkin() {
        return new Skin(Gdx.files.internal("uiskin.json"));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        final float delta = Gdx.graphics.getDeltaTime();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(final int width, final int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
    }
}