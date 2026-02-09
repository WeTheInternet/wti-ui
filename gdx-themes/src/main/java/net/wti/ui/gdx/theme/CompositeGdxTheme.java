package net.wti.ui.gdx.theme;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/// CompositeGdxTheme
///
/// A fully modular GdxTheme that merges multiple skin bundles into a unified theme.
/// Loads each UiDataBundle's JSON and atlas into a single Skin, allowing clean separation of styles.
///
/// Supports:
/// 『 ✓ 』 Base skin from `sgx-ui.json`
/// 『 ✓ 』 Additional skins like `task-ui.json` merged afterward
/// 『 ✓ 』 Reuse of common styles while isolating component-specific styles
///
/// Usage Example:
/// ```java
/// GdxTheme theme = new CompositeGdxTheme(
///     new UiDataBundle("cc-by-4/raeleus/sgx/sgx-ui.json", "cc-by-4/raeleus/sgx/sgx-ui.atlas"),
///     new UiDataBundle("cc-by-4/wti/task-ui.json", "cc-by-4/wti/task-ui.atlas")
/// );
/// ```
///
/// Created by ChatGPT 4o and James X. Nelson (James@WeTheInter.net) on 2025-04-16 @ 22:32 CST
public class CompositeGdxTheme extends AbstractGdxTheme {

    private final Skin skin;
    private final String assetPath;

    /// Constructs a theme from multiple UiDataBundle inputs.
    /// @param bundles Vararg list of skin bundles to merge. The first bundle is treated as base.
    public CompositeGdxTheme(final String assetPath, UiDataBundle... bundles) {
        if (bundles == null || bundles.length == 0) {
            throw new IllegalArgumentException("At least one UiDataBundle must be provided.");
        }

        // Use the first skin + atlas as base skin
        UiDataBundle base = bundles[0];
        TextureAtlas baseAtlas = base.getAtlasOrNull();

        this.skin = (baseAtlas == null)
                ? new Skin()
                : new Skin(baseAtlas);
        final FileHandle json = base.getJsonHandleOrNull();
        if (json != null) {
            this.skin.load(json);
        }
        this.assetPath = assetPath == null ? base.getBasePath() : assetPath;

        // Merge remaining skins into the base skin instance
        for (int i = 1; i < bundles.length; i++) {
            UiDataBundle bundle = bundles[i];
            TextureAtlas maybeAtlas = bundle.getAtlasOrNull();
            if (maybeAtlas != null) {
                skin.addRegions(maybeAtlas);                // Add atlas regions
            }
            final FileHandle maybeJson = bundle.getJsonHandleOrNull();
            if (GdxTheme.isGlAvailable() && maybeJson != null) {
                skin.load(maybeJson);     // Load JSON styles into same Skin instance
            }
        }
    }

    /// @return Directory of the base skin (used for asset-relative lookups)
    @Override
    public String getAssetPath() {
        return assetPath;
    }

    /// @return Shared Skin instance containing all styles from merged bundles
    @Override
    public Skin getSkin() {
        return skin;
    }
}