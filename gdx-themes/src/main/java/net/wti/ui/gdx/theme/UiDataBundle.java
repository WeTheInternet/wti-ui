package net.wti.ui.gdx.theme;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;

/// UiDataBundle
///
/// Represents a skin JSON and an optional TextureAtlas file.
/// Used with CompositeGdxTheme to modularly compose UI styling assets.
///
/// Responsibilities:
/// 『 ✓ 』 Encapsulate both JSON + Atlas for a skin package
/// 『 ✓ 』 Provide access to FileHandles for use in theme loading
/// 『 ✓ 』 Support structured skin merging by defining a clear base path
/// Supports:
/// 『 ✓ 』 Optional `.atlas` file (null or missing okay)
/// 『 ✓ 』 File path resolution using libGDX `Gdx.files.internal()`
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:52 CST
public class UiDataBundle {

    public final String jsonPath;
    public final String atlasPath;

    public UiDataBundle(String jsonPath) {
        this(jsonPath, null);
    }

    public UiDataBundle(String jsonPath, String atlasPath) {
        this.jsonPath = jsonPath;
        this.atlasPath = atlasPath;
    }

    /// @return FileHandle for the skin JSON
    public FileHandle getJsonHandleOrNull() {
        if (jsonPath == null) {
            return null;
        }
        return Gdx.files.internal(jsonPath);
    }

    /// @return FileHandle for the atlas, or null if none provided or missing
    public TextureAtlas getAtlasOrNull() {
        if (atlasPath == null) return null;

        // Allow tests / servers to opt out explicitly.
        if (Boolean.getBoolean("wti.ui.theme.skipAtlases")) {
            return null;
        }

        // In true headless (no GL), attempting to load an atlas will NPE inside Texture creation.
        if (!GdxTheme.isGlAvailable()) {
            return null;
        }

        final FileHandle file = resolveAtlasFile(atlasPath);
        if (file == null || file.file() == null) {
            String extra = "";
            if (file != null) {
                extra = " ; " + file.file().getAbsolutePath() + " does not exist";
            }
            throw new IllegalStateException("Could not find file at " + atlasPath + extra);
        }

        try {
            return new TextureAtlas(file);
        } catch (NullPointerException e) {
            // libGDX can throw NPEs while loading when a page texture can't be resolved
            throw new IllegalStateException("Failed to create TextureAtlas from file: " + safePath(file), e);
        }
    }

    private static FileHandle resolveAtlasFile(final String path) {
        FileHandle internal = Gdx.files.internal(path);
        if (internal != null && internal.file().exists()) {
            // the default .exists() check is dumb.
            // It will fall-through from Internal to Classpath and return true
            // but then when you try to use the handle as Internal, it fails to resolve :-/
            return internal;
        }

        // In tests / jar-packaged assets, resources are often only available on the classpath.
        FileHandle cp = Gdx.files.classpath(path);
        if (cp != null && cp.exists()) {
            return cp;
        }

        return null;
    }

    private static String safePath(final FileHandle file) {
        try {
            return file.file().getAbsolutePath();
        } catch (Throwable ignored) {
            return String.valueOf(file);
        }
    }

    /// @return Asset path prefix (directory portion of the skin file)
    public String getBasePath() {
        String path = jsonPath;
        if (path == null) {
            path = atlasPath;
        }
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(0, idx) : ".";
    }
}