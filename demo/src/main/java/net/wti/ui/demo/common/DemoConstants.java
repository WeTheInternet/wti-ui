package net.wti.ui.demo.common;

import net.wti.ui.demo.i18n.Messages;
import xapi.inject.X_Inject;

/// DemoConstants:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 19/08/2025 @ 21:55
public interface DemoConstants {
    Messages MESSAGES = X_Inject.singleton(Messages.class);
    float MAX_WIDTH = 1024; // After 1024, pad the edges
    //    private static final String GLYPH_INFINITY = "\uD800\uDD85";
    String GLYPH_INFINITY = "∞";
    String GLYPH_FINISH   = "✓";
    String GLYPH_DEFER    = "⌚";
    String GLYPH_CANCEL   = "✕";
    String GLYPH_EDIT     = "🛠";
    String GLYPH_EXPAND   = "+"; // down arrows; consider ▼ or ⯯
    String GLYPH_COLLAPSE = "-"; // down arrows; consider ▲ or ⯯
    String GLYPH_DELETE     = "🗑";
    String GLYPH_START     = "▶";
}
