/*
 * Copyright (c) 2020 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.expandabletextarea;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.IntegerPropertyBase;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExpandableTextArea extends VBox {
    private static final Pattern         LINEBREAK_PATTERN = Pattern.compile("\\R");
    private static final Matcher         LINEBREAK_MATCHER = LINEBREAK_PATTERN.matcher("");
    private static final Character       ENTER             = (char) 10;
    private              String          limitationText    = "characters left";
    private              int             maxNoOfCharacters;
    private              int             characterThreshold;
    private              double          lineHeight;
    private              Label           label;
    private              BooleanProperty fixedHeight;
    private              BooleanProperty expandable;
    private              IntegerProperty compactNoOfLines;
    private              IntegerProperty expandedNoOfLines;
    private              StackPane       labelPane;
    private              TextArea        textArea;
    private              TextAreaSkin    textAreaSkin;
    private              Label           limitationLabel;
    private              int             initialNoOfLines;
    private              BooleanBinding  showing;


    // ******************** Constructors *******************************
    public ExpandableTextArea() {
        this("", true, false, 2, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text) {
        this(text, true, false, 2, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text, final int compactNoOfLines) {
        this(text, true, false, compactNoOfLines, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text, final boolean expandable, final int compactNoOfLines) {
        this(text, expandable, false, compactNoOfLines, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text, final boolean expandable, final int compactNoOfLines, final int maxNoOfCharacters, final int characterThreshold) {
        this(text, expandable, false, compactNoOfLines, maxNoOfCharacters, characterThreshold);
    }
    public ExpandableTextArea(final String text, final boolean expandable, final boolean fixedHeight) {
        this(text, expandable, fixedHeight, 2, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text, final boolean expandable, final boolean fixedHeight, final int compactNoOfLines) {
        this(text, expandable, fixedHeight, compactNoOfLines, Integer.MAX_VALUE, -1);
    }
    public ExpandableTextArea(final String text, final boolean expandable, final boolean fixedHeight, final int compactNoOfLines, final int maxNoOfCharacters, final int characterThreshold) {
        super();

        this.maxNoOfCharacters  = clamp(5, Integer.MAX_VALUE, maxNoOfCharacters);
        this.characterThreshold = characterThreshold;
        this.lineHeight         = 17;
        this.fixedHeight        = new BooleanPropertyBase(fixedHeight) {
            @Override protected void invalidated() { if (get()) { updateHeight(textArea.getText()); } }
            @Override public Object getBean() { return ExpandableTextArea.this; }
            @Override public String getName() { return "fixedHeight"; }
        };
        this.expandable         = new BooleanPropertyBase(expandable) {
            @Override protected void invalidated() {
                if (isFixedHeight()) {
                    updateHeight(textArea.getText());
                    if (get()) {
                        setToExpandedHeight();
                        enableNode(textArea, true);
                        enableNode(limitationLabel, textArea.getText().length() >= (maxNoOfCharacters - characterThreshold - 1));
                    } else {
                        setToFixedHeight();
                        enableNode(textArea, false);
                        enableNode(limitationLabel, false);
                    }
                }
            }
            @Override public Object getBean() { return ExpandableTextArea.this; }
            @Override public String getName() { return "expandable"; }
        };
        this.compactNoOfLines   = new IntegerPropertyBase(compactNoOfLines) {
            @Override protected void invalidated() { updateHeight(textArea.getText()); }
            @Override public Object getBean() { return ExpandableTextArea.this; }
            @Override public String getName() { return "noOfRows"; }
        };
        this.expandedNoOfLines  = new IntegerPropertyBase(1) {
            @Override public Object getBean() { return ExpandableTextArea.this; }
            @Override public String getName() { return "expandedNoOfLines"; }
        };
        this.initialNoOfLines   = 1;

        initGraphics(text);
        registerListeners();
        setupBindings();
    }


    // ******************** Initialization ************************************
    private void initGraphics(final String text) {
        getStylesheets().add(ExpandableTextArea.class.getResource("expandable-text-area.css").toExternalForm());
        getStyleClass().add("expandable-text-area");

        setAlignment(Pos.TOP_LEFT);

        textArea = new TextArea(text);
        if (isFixedHeight()) {
            textArea.setPrefHeight(getCompactNoOfLines() * lineHeight);
        }
        textArea.setWrapText(true);
        textArea.setVisible(!isFixedHeight());

        textArea.setTextFormatter(new TextFormatter<String>(change -> {
            int noOfCharacters = change.getControlNewText().length();
            if (noOfCharacters >= maxNoOfCharacters) {
                String allowedText = change.getControlNewText().substring(0, maxNoOfCharacters - 1);
                change.setText(allowedText);
                change.setRange(0, change.getControlText().length());
            }
            if (Integer.MAX_VALUE != maxNoOfCharacters) {
                enableNode(limitationLabel, noOfCharacters >= (maxNoOfCharacters - characterThreshold - 1));
            }
            return change;
        }));

        label = new Label(text);
        label.setAlignment(Pos.TOP_LEFT);
        label.setPrefWidth(Double.MAX_VALUE);
        label.setPrefHeight(getCompactNoOfLines() * lineHeight);
        label.setWrapText(true);

        labelPane = new StackPane(label);
        labelPane.getStyleClass().add("label-pane");
        labelPane.setPrefWidth(Double.MAX_VALUE);
        labelPane.setAlignment(Pos.TOP_LEFT);
        labelPane.setPadding(new Insets(5, 6, 6, 9));

        StackPane pane = new StackPane(labelPane, textArea);
        pane.getStyleClass().add("text-area-pane");

        limitationLabel = new Label();
        limitationLabel.getStyleClass().add("limitation-label");
        limitationLabel.setFont(Font.font(10));
        limitationLabel.setAlignment(Pos.CENTER_RIGHT);

        enableNode(limitationLabel, maxNoOfCharacters != Integer.MAX_VALUE);

        setSpacing(5);
        getChildren().addAll(pane, limitationLabel);
    }

    private void registerListeners() {
        textArea.widthProperty().addListener(o -> updateHeight(textArea.getText()));
        textArea.textProperty().addListener(o -> {
            Platform.runLater(() -> limitationLabel.setText((maxNoOfCharacters - textArea.getText().length() - 1) + " " + limitationText));
            updateHeight(textArea.getText());
        });
        label.heightProperty().addListener(o -> updateHeight(textArea.getText()));
    }

    private void setupBindings() {
        textArea.mouseTransparentProperty().bind(mouseTransparentProperty());

        label.prefWidthProperty().bind(textArea.widthProperty());
        label.textProperty().bind(new StringBinding() {
            {
                bind(textArea.textProperty());
            }

            @Override protected String computeValue() {
                if (null != textArea.getText() && textArea.getText().length() > 0) {
                    if (!((Character) textArea.getText().charAt(textArea.getText().length() - 1)).equals(ENTER)) {
                        return textArea.getText() + ENTER;
                    }
                }
                return textArea.getText();
            }
        });

        // Binding the container width/height to the TextArea width.
        labelPane.maxWidthProperty().bind(textArea.widthProperty());

        limitationLabel.prefWidthProperty().bind(textArea.widthProperty());

        if (null != getScene()) {
            initShowing();
        } else {
            sceneProperty().addListener((o1, ov1, nv1) -> {
                if (null == nv1) { return; }
                if (null != getScene().getWindow()) {
                    initShowing();
                } else {
                    sceneProperty().get().windowProperty().addListener((o2, ov2, nv2) -> {
                        if (null == nv2) { return; }
                        initShowing();
                    });
                }
            });
        }
    }

    private void initShowing() {
        showing = Bindings.createBooleanBinding(() -> {
            if (getScene() != null && getScene().getWindow() != null) {
                return getScene().getWindow().isShowing();
            } else {
                return false;
            }
        }, sceneProperty(), getScene().windowProperty(), getScene().getWindow().showingProperty());

        showing.addListener(ob -> {
            if (showing.get()) {
                if (isFixedHeight()) {
                    if (isExpandable()) { setToExpandedHeight(); } else { setToFixedHeight(); }
                    ScrollPane scrollPane = (ScrollPane) textArea.lookup(".scroll-pane");
                    scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);
                    scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);
                }
            }
        });
    }


    // ******************** Public Methods ************************************
    public boolean isFixedHeight() { return fixedHeight.get(); }
    public void setFixedHeight(final boolean fixedHeight) { this.fixedHeight.set(fixedHeight); }
    public BooleanProperty fixedHeightProperty() { return fixedHeight; }

    public boolean isExpandable() { return expandable.get(); }
    public void setExpandable(final boolean expandable) { this.expandable.set(expandable); }
    public BooleanProperty expandableProperty() { return expandable; }

    public int getCompactNoOfLines() { return compactNoOfLines.get(); }
    public void setCompactNoOfLines(final int compactNoOfLines) { this.compactNoOfLines.set(compactNoOfLines); }
    public IntegerProperty compactNoOfLinesProperty() { return compactNoOfLines; }

    public TextArea getTextArea() { return textArea; }

    public String getText() { return textArea.getText(); }
    public void setText(final String text) { textArea.setText(text); }
    public StringProperty textProperty() { return textArea.textProperty(); }

    public boolean isEditable() { return textArea.isEditable(); }
    public void setEditable(final boolean editable) { textArea.setEditable(editable); }
    public BooleanProperty editableProperty() { return textArea.editableProperty(); }

    public int getExpandedNoOfLines() { return expandedNoOfLines.get(); }
    public ReadOnlyIntegerProperty expandedNoOfLinesProperty() { return expandedNoOfLines; }

    public int getMaxNoOfCharacters() { return maxNoOfCharacters; }
    public void setMaxNoOfCharacters(final int maxNoOfCharacters) {
        this.maxNoOfCharacters = clamp(5, Integer.MAX_VALUE, maxNoOfCharacters);
        if (Integer.MAX_VALUE == this.maxNoOfCharacters) {
            characterThreshold = -1;
        }
        enableNode(limitationLabel, this.maxNoOfCharacters != Integer.MAX_VALUE);
    }

    public int getCharacterThreshold() { return characterThreshold; }
    public void setCharacterThreshold(final int characterThreshold) { this.characterThreshold = Integer.MAX_VALUE == maxNoOfCharacters ? -1 : clamp(0, maxNoOfCharacters, characterThreshold); }

    public String getLimitationText() { return limitationText; }
    public void setLimitationText(final String limitationText) { this.limitationText = null == limitationText ? "" : limitationText; }

    public void setInitialNoOfLines(final int initialNoOfLines) {
        this.initialNoOfLines = initialNoOfLines;
    }

    public int getNoOfLines() {
        if (null == textAreaSkin) { textAreaSkin = (TextAreaSkin) textArea.getSkin(); }
        int noOfLines = getCompactNoOfLines();
        if (textAreaSkin != null) {
            int textLength = textArea.getText().length();
            if (textLength >= 1) {
                Rectangle2D startBounds;
                Rectangle2D endBounds;
                try {
                    startBounds = textAreaSkin.getCharacterBounds(1);
                    endBounds   = textAreaSkin.getCharacterBounds(textLength);
                } catch (IndexOutOfBoundsException e) {
                    startBounds = new Rectangle2D(textAreaSkin.getCaretBounds().getMinX(), textAreaSkin.getCaretBounds().getMinX(), textAreaSkin.getCaretBounds().getWidth(), textAreaSkin.getCaretBounds().getHeight());
                    endBounds   = textAreaSkin.getCharacterBounds(textLength);
                }
                if (null != startBounds && null != endBounds) {
                    double lineHeight = endBounds.getHeight();
                    noOfLines = (clamp(1, Integer.MAX_VALUE, (int) ((endBounds.getMaxY() - (null == startBounds ? 0 : startBounds.getMinY())) / (lineHeight - 2))));
                }
            }
        }
        return noOfLines;
    }


    // ******************** Private Methods ***********************************
    private void updateHeight(final String text) {
        if (null == textAreaSkin) { textAreaSkin = (TextAreaSkin) textArea.getSkin(); }
        if (isExpandable()) {
            int textLength = text.length();
            if (textLength < 1) {
                expandedNoOfLines.set(1);
            } else {
                Rectangle2D startBounds;
                Rectangle2D endBounds;
                try {
                    startBounds = textAreaSkin.getCharacterBounds(1);
                    endBounds   = textAreaSkin.getCharacterBounds(textLength);
                } catch (IndexOutOfBoundsException e) {
                    startBounds = new Rectangle2D(textAreaSkin.getCaretBounds().getMinX(), textAreaSkin.getCaretBounds().getMinX(), textAreaSkin.getCaretBounds().getWidth(), textAreaSkin.getCaretBounds().getHeight());
                    endBounds   = textAreaSkin.getCharacterBounds(textLength);
                }
                if (null == startBounds || null == endBounds) {
                    expandedNoOfLines.set(1);
                } else {
                    lineHeight = endBounds.getHeight();
                    expandedNoOfLines.set(clamp(1, Integer.MAX_VALUE, (int) ((endBounds.getMaxY() - (null == startBounds ? 0 : startBounds.getMinY())) / (lineHeight - 2))));
                }
            }
            LINEBREAK_MATCHER.reset(text);
            int lineBreaks = 0;
            while (LINEBREAK_MATCHER.find()) { lineBreaks++; }
            if (getExpandedNoOfLines() == lineBreaks) { expandedNoOfLines.set(getExpandedNoOfLines() + 1); }
            setToExpandedHeight();
        }
    }

    private int clamp(final int min, final int max, final int value) {
        if (value < min) { return min; }
        if (value > max) { return max; }
        return value;
    }

    private void setToFixedHeight() {
        double height = getCompactNoOfLines() * lineHeight;
        label.setMaxHeight(height);
        textArea.setMinHeight(height);
        textArea.setMaxHeight(height);
        textArea.setPrefHeight(height);
        textArea.setPrefRowCount(getCompactNoOfLines());
        requestLayout();
    }

    private void setToExpandedHeight() {
        double height = getExpandedNoOfLines() * lineHeight;
        if (getText().length() > 16 && getExpandedNoOfLines() == 1) {
            height = initialNoOfLines * lineHeight;
        }
        label.setMaxHeight(height);
        textArea.setMinHeight(height);
        textArea.setMaxHeight(height);
        textArea.setPrefHeight(height);
        textArea.setPrefRowCount(getExpandedNoOfLines());
        requestLayout();
    }

    private void enableNode(final Node node, final boolean enable) {
        node.setVisible(enable);
        node.setManaged(enable);
    }
}
