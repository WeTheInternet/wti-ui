package net.wti.ui.api;

/// GfxConstants:
///
/// A handy place to store simple constants for use in UI-related code
///
/// Created by James X. Nelson (James@WeTheInter.net) on 04/07/2025 @ 03:36
public interface GfxConstants {
    float TINY = 1f / ( 1 << 16 ); // a very small value after which we discard remaining fractions
}
