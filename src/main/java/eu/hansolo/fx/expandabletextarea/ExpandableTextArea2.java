package eu.hansolo.fx.expandabletextarea;

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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ExpandableTextArea2 extends StackPane {
    private static final Pattern         LINEBREAK_PATTERN = Pattern.compile("\\R");
    private static final Matcher         LINEBREAK_MATCHER = LINEBREAK_PATTERN.matcher("");
    private static final Character       ENTER             = (char) 10;
    private              int             maxNoOfCharacters;
    private              double          lineHeight;
    private              Label           label;
    private              BooleanProperty fixedHeight;
    private              BooleanProperty expandable;
    private              IntegerProperty compactNoOfLines;
    private              IntegerProperty expandedNoOfLines;
    private              StackPane       labelPane;
    private              TextArea        textArea;
    private              TextAreaSkin    textAreaSkin;
    private              int             initialNoOfLines;
    private              BooleanBinding  showing;


    // ******************** Constructors *******************************
    public ExpandableTextArea2() {
        this("", true, false, 2, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text) {
        this(text, true, false, 2, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text, final int compactNoOfLines) {
        this(text, true, false, compactNoOfLines, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text, final boolean expandable, final int compactNoOfLines) {
        this(text, expandable, false, compactNoOfLines, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text, final boolean expandable, final int compactNoOfLines, final int maxNoOfCharacters) {
        this(text, expandable, false, compactNoOfLines, maxNoOfCharacters);
    }
    public ExpandableTextArea2(final String text, final boolean expandable, final boolean fixedHeight) {
        this(text, expandable, fixedHeight, 2, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text, final boolean expandable, final boolean fixedHeight, final int compactNoOfLines) {
        this(text, expandable, fixedHeight, compactNoOfLines, Integer.MAX_VALUE);
    }
    public ExpandableTextArea2(final String text, final boolean expandable, final boolean fixedHeight, final int compactNoOfLines, final int maxNoOfCharacters) {
        super();

        this.maxNoOfCharacters = clamp(5, Integer.MAX_VALUE, maxNoOfCharacters);
        this.lineHeight        = 17;
        this.fixedHeight       = new BooleanPropertyBase(fixedHeight) {
            @Override protected void invalidated() { updateHeight(textArea.getText()); }
            @Override public Object getBean() { return ExpandableTextArea2.this; }
            @Override public String getName() { return "fixedHeight"; }
        };
        this.expandable        = new BooleanPropertyBase(expandable) {
            @Override protected void invalidated() {
                updateHeight(textArea.getText());
                if (get()) {
                    setToExpandedHeight();
                    enableNode(textArea, true);
                } else {
                    setToFixedHeight();
                    enableNode(textArea, false);
                }
            }
            @Override public Object getBean() { return ExpandableTextArea2.this; }
            @Override public String getName() { return "expandable"; }
        };
        this.compactNoOfLines  = new IntegerPropertyBase(compactNoOfLines) {
            @Override protected void invalidated() { updateHeight(textArea.getText()); }
            @Override public Object getBean() { return ExpandableTextArea2.this; }
            @Override public String getName() { return "noOfRows"; }
        };
        this.expandedNoOfLines = new IntegerPropertyBase(1) {
            @Override public Object getBean() { return ExpandableTextArea2.this; }
            @Override public String getName() { return "expandedNoOfLines"; }
        };
        this.initialNoOfLines  = 1;

        initGraphics(text);
        registerListeners();
        setupBindings();
    }


    // ******************** Initialization ************************************
    private void initGraphics(final String text) {
        getStylesheets().add(ExpandableTextArea.class.getResource("expandable-text-area.css").toExternalForm());
        getStyleClass().add("expandable-text-area");

        setAlignment(Pos.TOP_LEFT);

        double compactedHeight = getCompactNoOfLines() * lineHeight;
        double expandedHeight  = getExpandedNoOfLines() * lineHeight;

        textArea = new TextArea(text);
        textArea.setMinHeight(lineHeight);
        textArea.setPrefHeight(compactedHeight);
        textArea.setMaxHeight(expandedHeight);
        textArea.setWrapText(true);
        textArea.setVisible(!isFixedHeight());

        textArea.setTextFormatter(new TextFormatter<String>(change -> {
            int noOfCharacters = change.getControlNewText().length();
            if (noOfCharacters >= maxNoOfCharacters) {
                String allowedText = change.getControlNewText().substring(0, maxNoOfCharacters - 1);
                change.setText(allowedText);
                change.setRange(0, change.getControlText().length());
            }
            return change;
        }));

        label = new Label(text);
        label.setAlignment(Pos.TOP_LEFT);
        label.setPrefWidth(Double.MAX_VALUE);
        label.setMinHeight(lineHeight);
        label.setPrefHeight(compactedHeight);
        label.setMaxHeight(expandedHeight);
        label.setWrapText(true);

        labelPane = new StackPane(label);
        labelPane.getStyleClass().add("label-pane");
        labelPane.setPrefWidth(Double.MAX_VALUE);
        labelPane.setAlignment(Pos.TOP_LEFT);
        labelPane.setPadding(new Insets(5, 6, 6, 9));

        getChildren().addAll(labelPane, textArea);
    }

    private void registerListeners() {
        textArea.widthProperty().addListener(o -> updateHeight(textArea.getText()));
        textArea.textProperty().addListener(o -> updateHeight(textArea.getText()));
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
                ScrollPane scrollPane = (ScrollPane)textArea.lookup(".scroll-pane");
                scrollPane.hbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.vbarPolicyProperty().setValue(ScrollPane.ScrollBarPolicy.NEVER);
            }
        });
    }


    // ******************** Methods *******************************************
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

    public void setInitialNoOfLines(final int initialNoOfLines) {
        this.initialNoOfLines = initialNoOfLines;
    }

    private void updateHeight(final String text) {
        if (null == textAreaSkin) { textAreaSkin = (TextAreaSkin) textArea.getSkin(); }
        if (isExpandable() && textAreaSkin != null) {
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
        double compactedHeight = getCompactNoOfLines() * lineHeight;
        label.setMaxHeight(compactedHeight);
        textArea.setMinHeight(compactedHeight);
        textArea.setMaxHeight(compactedHeight);
        textArea.setPrefHeight(compactedHeight);
        textArea.setPrefRowCount(getCompactNoOfLines());
        requestLayout();
    }

    private void setToExpandedHeight() {
        double expandedHeight = getExpandedNoOfLines() * lineHeight;
        if (getText().length() > 16 && (getExpandedNoOfLines() == 1 || initialNoOfLines > getExpandedNoOfLines())) {
            expandedHeight = initialNoOfLines * lineHeight;
        }
        label.setMaxHeight(expandedHeight);
        textArea.setPrefRowCount(getExpandedNoOfLines());
        textArea.setMinHeight(expandedHeight);
        textArea.setMaxHeight(expandedHeight);
        textArea.setPrefHeight(expandedHeight);
        requestLayout();
    }

    private void enableNode(final Node node, final boolean enable) {
        node.setVisible(enable);
        node.setManaged(enable);
    }
}
