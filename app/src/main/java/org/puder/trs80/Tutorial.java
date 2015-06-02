/*
 * Copyright 2012-2013, Arno Puder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.puder.trs80;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class Tutorial implements View.OnClickListener, Runnable {

    final private static int KEY_DELAY = 180;

    private View             tutorialRoot;
    private View             keyboardRoot;
    private String           currentCommand;
    private int              currentKeyStroke;


    private static class Step {
        public Step(int command, int description) {
            this.command = TRS80Application.getAppContext().getString(command);
            this.description = TRS80Application.getAppContext().getString(description);
        }


        public String command;
        public String description;
    }


    final private static Step[]   steps = new Step[] {
            new Step(R.string.tutorial_step_1_cmd, R.string.tutorial_step_1),
            new Step(R.string.tutorial_step_2_cmd, R.string.tutorial_step_2),
            new Step(R.string.tutorial_step_3_cmd, R.string.tutorial_step_3),
            new Step(R.string.tutorial_step_4_cmd, R.string.tutorial_step_4),
            new Step(R.string.tutorial_step_5_cmd, R.string.tutorial_step_5),
            new Step(R.string.tutorial_step_6_cmd, R.string.tutorial_step_6),
            new Step(R.string.tutorial_step_7_cmd, R.string.tutorial_step_7),
            new Step(R.string.tutorial_step_8_cmd, R.string.tutorial_step_8), };
    private TextView command;
    private TextView description;
    private Button   nextButton;
    private int      nextCommand;


    public Tutorial(View tutorialRoot, View keyboardRoot) {
        XTRS.reset();
        XTRS.rewindCassette();
        this.tutorialRoot = tutorialRoot;
        this.keyboardRoot = keyboardRoot;
        keyboardRoot.setVisibility(View.GONE);
        nextButton = (Button) tutorialRoot.findViewById(R.id.tutorial_next);
        nextButton.setOnClickListener(this);
        ImageView buttonCancel = (ImageView) tutorialRoot.findViewById(R.id.tutorial_cancel);
        buttonCancel.setOnClickListener(this);
        command = (TextView) tutorialRoot.findViewById(R.id.tutorial_command);
        command.setTypeface(Fonts.getTypeface(Hardware.MODEL3));
        description = (TextView) tutorialRoot.findViewById(R.id.tutorial_description);
        nextCommand = 0;
    }

    public void show() {
        showNextCommand();
    }

    @Override
    public void onClick(View v) {
        tutorialRoot.setVisibility(View.GONE);
        if (v.getId() == R.id.tutorial_next) {
            tutorialRoot.postDelayed(this, KEY_DELAY);
        } else {
            showKeyboard();
        }
    }

    private void showNextCommand() {
        if (nextCommand >= steps.length) {
            showKeyboard();
            return;
        }
        tutorialRoot.setVisibility(View.VISIBLE);
        Step step = steps[nextCommand++];
        String label = TRS80Application.getAppContext().getString(R.string.tutorial_next,
                nextCommand, steps.length);
        nextButton.setText(label);
        command.setText(step.command);
        description.setText(step.description);
        currentCommand = step.command + "\n";
        currentKeyStroke = 0;
    }

    private void showKeyboard() {
        keyboardRoot.setVisibility(View.VISIBLE);
        keyboardRoot.requestLayout();
        keyboardRoot.invalidate();
    }

    @Override
    public void run() {
        if (currentKeyStroke == currentCommand.length()) {
            showNextCommand();
            return;
        }
        char ch = currentCommand.charAt(currentKeyStroke++);
        TRS80Application.getKeyboardManager().injectKey(ch);
        tutorialRoot.postDelayed(this, KEY_DELAY);
    }
}
