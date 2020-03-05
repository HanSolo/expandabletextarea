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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Scene;


/**
 * User: hansolo
 * Date: 2019-06-21
 * Time: 09:36
 */
public class Demo extends Application {
    private Label              header;
    private ExpandableTextArea expandableTextArea;
    private ToggleButton       toggleButton;
    private Button             exitButton;
    private HBox               buttonBox;


    @Override public void init() {
        final String text = "";
        header             = new Label("Header");
        expandableTextArea = new ExpandableTextArea(text, true, 3, 300, 50);
        toggleButton       = new ToggleButton("Not Expanded");
        exitButton         = new Button("Exit");
        buttonBox          = new HBox(5, toggleButton, exitButton);

        toggleButton.selectedProperty().addListener(o -> {
            if (toggleButton.isSelected()) {
                toggleButton.setText("Expanded");
                expandableTextArea.setExpandable(true);
            } else {
                toggleButton.setText("Not Expanded");
                expandableTextArea.setExpandable(false);
            }
        });
        exitButton.setOnAction(e -> stop());

        toggleButton.setSelected(expandableTextArea.isExpandable());
    }

    @Override public void start(Stage stage) {
        VBox pane = new VBox(10, header, expandableTextArea, buttonBox);
        pane.setPadding(new Insets(20));

        Scene scene = new Scene(pane, 400, 300);

        stage.setTitle("ExpandableTextArea");
        stage.setScene(scene);
        stage.show();

        expandableTextArea.setText("The process of learning the Java language can be a tough task for you, and they watch the videos for hours and hours to learn java programming before they proceed to the actual writing. Therefore, it is important to enjoy the process of learning, and you should have the patience to learn java programming. \n" +
                                   "You have to keep in mind that you can not understand everything for the first time. So, you have to figure out the possible and best ways to learn a programming language. Always try to neglect the negativity during your learning process so that you can easily concentrate on the learning methods.");
    }

    @Override public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
