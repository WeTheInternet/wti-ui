package net.wti.gdx.theme.raeleus.sgx;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.utils.Align;

import java.math.BigDecimal;

/// Spinner:
///
/// Adapted from theme example zip from [Raeleus blog](https://ray3k.wordpress.com/sgx-ui-skin-for-libgdx)
///
/// Created by James X. Nelson (James@WeTheInter.net) on 13/02/2025 @ 03:00
public class Spinner extends Table {
    private BigDecimal value;
    private Double minimum;
    private Double maximum;
    private boolean usingMinimum, usingMaximum;
    private BigDecimal increment;
    private boolean rounding;
    private TextField textField;
    private Button buttonMinus;
    private Button buttonPlus;
    private Actor transversalNext, transversalPrevious;
    public enum Orientation {
        HORIZONTAL, HORIZONTAL_FLIPPED, VERTICAL, VERTICAL_FLIPPED, RIGHT_STACK, LEFT_STACK
    }
    private Orientation orientation;
    private SpinnerStyle style;

    public Spinner(double value, double increment, boolean round, Orientation orientation, SpinnerStyle style) {
        this.value = BigDecimal.valueOf(value);
        rounding = round;
        this.orientation = orientation;
        usingMinimum = false;
        usingMaximum = false;
        this.increment = BigDecimal.valueOf(increment);
        this.style = style;

        addWidgets();
    }

    public Spinner(double value, double increment, boolean round, Orientation orientation, Skin skin, String style) {
        this(value, increment, round, orientation, skin.get(style, SpinnerStyle.class));
    }

    public Spinner(double value, double increment, boolean round, Orientation orientation, Skin skin) {
        this(value, increment, round, orientation, skin, "default");
    }

    private void addWidgets() {
        buttonMinus = new Button(style.buttonMinusStyle);
        buttonPlus = new Button(style.buttonPlusStyle);
        textField = new TextField("", style.textFieldStyle) {
            @Override
            public void next(boolean up) {
                if (up) {
                    if (transversalPrevious != null) {
                        getStage().setKeyboardFocus(transversalPrevious);
                        if (transversalPrevious instanceof TextField) {
                            ((TextField) transversalPrevious).selectAll();
                        }
                    } else {
                        super.next(up);
                    }
                } else {
                    if (transversalNext != null) {
                        getStage().setKeyboardFocus(transversalNext);
                        if (transversalNext instanceof TextField) {
                            ((TextField) transversalNext).selectAll();
                        }
                    } else {
                        super.next(up);
                    }
                }
            }

        };

        textField.setAlignment(Align.center);

        textField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Keys.UP) {
                    addValue();
                    fire(new ChangeListener.ChangeEvent());
                } else if (keycode == Keys.DOWN) {
                    subtractValue();
                    fire(new ChangeListener.ChangeEvent());
                }
                return false;
            }
        });

        textField.setTextFieldFilter(new TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(TextField textField1, char c) {
                boolean returnValue = false;
                if ((c >= 48 && c <= 57) || c == 45 || (!rounding && c == 46)) {
                    String text = textField1.getText();
                    if (textField1.getCursorPosition() <= text.length()) {
                        text = text.substring(0, textField1.getCursorPosition());
                        text += c + textField1.getText().substring(textField1.getCursorPosition());
                    }
                    if (text.matches("-?\\d*\\.?\\d*")) {
                        returnValue = true;
                    }
                }
                return returnValue;
            }
        });
        updateText();

        if (null != orientation) switch (orientation) {
            case HORIZONTAL:
                add(buttonMinus);
                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();
                add(buttonPlus);
                break;
            case HORIZONTAL_FLIPPED:
                add(buttonPlus);
                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();
                add(buttonMinus);
                break;
            case VERTICAL:
                add(buttonPlus);
                row();
                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();
                row();
                add(buttonMinus);
                break;
            case VERTICAL_FLIPPED:
                add(buttonMinus);
                row();
                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();
                row();
                add(buttonPlus);
                break;
            case RIGHT_STACK:
            {
                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();

                VerticalGroup group = new VerticalGroup();
                add(group);

                group.addActor(buttonPlus);
                group.addActor(buttonMinus);
                break;
            }
            case LEFT_STACK:
            {
                VerticalGroup group = new VerticalGroup();
                add(group);

                group.addActor(buttonPlus);
                group.addActor(buttonMinus);

                add(textField).prefWidth(35.0f).minWidth(35.0f).growX();
                break;
            }
            default:
                break;
        }

        buttonMinus.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                subtractValue();
            }
        });

        buttonPlus.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                addValue();
            }
        });

        textField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                Spinner parent = (Spinner) actor;
                String text = textField.getText();

                if (text.matches("-?\\d+\\.?\\d*")) {
                    double value = Double.parseDouble(text);
                    parent.value = BigDecimal.valueOf(value);
                } else {
                    parent.value = BigDecimal.valueOf(0);
                }
            }
        });

        textField.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
                Spinner parent = (Spinner) textField.getParent();
                if (usingMinimum && parent.value.doubleValue() < parent.minimum) {
                    parent.value = BigDecimal.valueOf(parent.minimum);
                }
                if (usingMaximum && parent.value.doubleValue() > parent.maximum) {
                    parent.value = BigDecimal.valueOf(parent.maximum);
                }
                parent.updateText();
            }

        });

        final Spinner spinner = this;
        addCaptureListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                event.setTarget(spinner);
            }
        });
    }

    private void subtractValue() {
        value = value.subtract(increment);
        if (usingMinimum && value.doubleValue() < minimum) {
            value = BigDecimal.valueOf(minimum);
        }
        if (usingMaximum && value.doubleValue() > maximum) {
            value = BigDecimal.valueOf(maximum);
        }
        updateText();
    }

    private void addValue() {
        value = value.add(increment);
        if (usingMinimum && value.doubleValue() < minimum) {
            value = BigDecimal.valueOf(minimum);
        }
        if (usingMaximum && value.doubleValue() > maximum) {
            value = BigDecimal.valueOf(maximum);
        }
        updateText();
    }

    public double getValue() {
        return value.doubleValue();
    }

    public void setValue(double value) {
        setValue(value, true);
    }

    public double getMinimum() {
        return minimum;
    }

    public void setMinimum(double minimum) {
        this.minimum = minimum;
        usingMinimum = true;
    }

    public double getMaximum() {
        return maximum;
    }

    public void setMaximum(double maximum) {
        this.maximum = maximum;
        usingMaximum = true;
    }

    public void clearMinMax() {
        usingMinimum = false;
        usingMaximum = false;
    }

    private void setValue(double value, boolean updateText) {
        this.value = BigDecimal.valueOf(value);
        if (updateText) {
            updateText();
        }
    }

    private void updateText() {
        int pos = textField.getCursorPosition();
        int startLength = textField.getText().length();
        if (rounding) {
            textField.setText(Integer.toString((int) MathUtils.round(value.floatValue())));
        } else {
            textField.setText(value.toString());
        }
        int length = textField.getText().length();

        pos += length - startLength;
        textField.setCursorPosition(Math.min(pos, length));
    }

    static public class SpinnerStyle {
        public ButtonStyle buttonMinusStyle, buttonPlusStyle;
        public TextFieldStyle textFieldStyle;

        public SpinnerStyle() {

        }

        public SpinnerStyle(ButtonStyle buttonMinusStyle, ButtonStyle buttonPlusStyle, TextFieldStyle textFieldStyle) {
            this.buttonMinusStyle = buttonMinusStyle;
            this.buttonPlusStyle = buttonPlusStyle;
            this.textFieldStyle = textFieldStyle;
        }

        public SpinnerStyle(SpinnerStyle style) {
            buttonMinusStyle = style.buttonMinusStyle;
            buttonPlusStyle = style.buttonPlusStyle;
            textFieldStyle = style.textFieldStyle;
        }
    }

    public TextField getTextField() {
        return textField;
    }

    public Button getButtonMinus() {
        return buttonMinus;
    }

    public Button getButtonPlus() {
        return buttonPlus;
    }

    public Actor getTransversalNext() {
        return transversalNext;
    }

    public void setTransversalNext(Actor transversalNext) {
        this.transversalNext = transversalNext;
    }

    public Actor getTransversalPrevious() {
        return transversalPrevious;
    }

    public void setTransversalPrevious(Actor transversalPrevious) {
        this.transversalPrevious = transversalPrevious;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;

        clear();
        addWidgets();
    }

    public SpinnerStyle getStyle() {
        return style;
    }

    public void setStyle(SpinnerStyle style) {
        this.style = style;

        clear();
        addWidgets();
    }
}