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

import org.puder.trs80.keyboard.KeyboardManager;

public class Tutorial implements View.OnClickListener, Runnable {

    final private static int  KEY_DELAY = 180;
    final private static int  TUTORIAL_DELAY = 1000;
    final private static char DELAY_CHAR = '_';

    private KeyboardManager  keyboardManager;
    private View             tutorialRoot;
    private View             keyboardRoot;
    private View             keyboardSwitchView;
    private Step             currentStep;
    private String           currentCommand;
    private int              currentKeyStroke;


    private static class Step {
        public Step(int command, int description, long postCommandDelay) {
            this.command = TRS80Application.getAppContext().getString(command);
            this.description = TRS80Application.getAppContext().getString(description);
            this.postCommandDelay = postCommandDelay;
        }


        public String command;
        public String description;
        public long   postCommandDelay;
    }


    final private static Step[]   steps = new Step[] {
            new Step(R.string.tutorial_step_1_cmd, R.string.tutorial_step_1, 1000),
            new Step(R.string.tutorial_step_2_cmd, R.string.tutorial_step_2, 1000),
            new Step(R.string.tutorial_step_3_cmd, R.string.tutorial_step_3, 100),
            new Step(R.string.tutorial_step_4_cmd, R.string.tutorial_step_4, 100),
            new Step(R.string.tutorial_step_5_cmd, R.string.tutorial_step_5, 3500),
            new Step(R.string.tutorial_step_6_cmd, R.string.tutorial_step_6, 1800),
            new Step(R.string.tutorial_step_7_cmd, R.string.tutorial_step_7, 500),
            new Step(R.string.tutorial_step_8_cmd, R.string.tutorial_step_8, 0), };
    private TextView command;
    private TextView description;
    private Button   nextButton;
    private int      nextCommand;


    public Tutorial(KeyboardManager keyboardManager, View root) {
        this.keyboardManager = keyboardManager;
        XTRS.reset();
        XTRS.rewindCassette();
        tutorialRoot = root.findViewById(R.id.tutorial);
        keyboardRoot = root.findViewById(R.id.keyboard_container);
        keyboardSwitchView = root.findViewById(R.id.switch_keyboard);
        keyboardRoot.setVisibility(View.GONE);
        keyboardSwitchView.setVisibility(View.GONE);
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
        currentStep = steps[nextCommand++];
        String label = TRS80Application.getAppContext().getString(R.string.tutorial_next,
                nextCommand, steps.length);
        nextButton.setText(label);
        command.setText(currentStep.command.replaceAll(Character.toString(DELAY_CHAR), ""));
        description.setText(currentStep.description);
        currentCommand = currentStep.command + "\n";
        currentKeyStroke = 0;
    }

    private void showKeyboard() {
        keyboardSwitchView.setVisibility(View.VISIBLE);
        keyboardRoot.setVisibility(View.VISIBLE);
        keyboardRoot.requestLayout();
        keyboardRoot.invalidate();
    }

    @Override
    public void run() {
        if (currentKeyStroke == currentCommand.length()) {
            tutorialRoot.postDelayed(new Runnable() {
                @Override
                public void run() {
                    showNextCommand();
                }
            }, currentStep.postCommandDelay);
            return;
        }
        char ch = currentCommand.charAt(currentKeyStroke++);
        int delay = KEY_DELAY;
        if (ch == DELAY_CHAR) {
            delay = TUTORIAL_DELAY;
        } else {
            keyboardManager.injectKey(ch);
        }
        tutorialRoot.postDelayed(this, delay);
    }
}
