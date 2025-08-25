package net.wti.ui.inventory.api;

/// Img:
///
/// Abstraction over a graphical icon or sprite region.
/// Hides libGDX TextureRegion behind a simple interface for portability.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 02:34
public interface Img {

    /// @return width in pixels.
    float getWidth();

    /// @return height in pixels.
    float getHeight();

    /// @return the underlying libGDX TextureRegion (for rendering).
    com.badlogic.gdx.graphics.g2d.TextureRegion getRegion();
}
