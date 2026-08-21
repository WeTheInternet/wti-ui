package net.wti.ui.components.browser;

/// Toolkit-neutral presentation state for one currently rendered browser cell.
public final class EntityBrowserCellState {

    private final boolean selected;
    private final boolean keyboardCurrent;

    /// Captures selection and keyboard-current state for one render update.
    public EntityBrowserCellState(
            final boolean selected,
            final boolean keyboardCurrent
    ) {
        this.selected = selected;
        this.keyboardCurrent = keyboardCurrent;
    }

    /// Returns true when this cell owns browser selection.
    public boolean isSelected() {
        return selected;
    }

    /// True when this selected cell is current in a keyboard-focused browser.
    public boolean isKeyboardCurrent() {
        return keyboardCurrent;
    }
}
