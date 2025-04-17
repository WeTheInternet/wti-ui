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
    public FileHandle getJsonHandle() {
        return Gdx.files.internal(jsonPath);
    }

    /// @return FileHandle for the atlas, or null if none provided or missing
    public TextureAtlas getAtlasOrNull() {
        if (atlasPath == null) return null;
        FileHandle file = Gdx.files.internal(atlasPath);
        return file.exists() ? new TextureAtlas(file) : null;
    }

    /// @return Asset path prefix (directory portion of the skin file)
    public String getBasePath() {
        int idx = jsonPath.lastIndexOf('/');
        return idx >= 0 ? jsonPath.substring(0, idx) : ".";
    }
}