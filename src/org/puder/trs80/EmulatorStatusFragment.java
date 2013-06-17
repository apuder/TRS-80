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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class EmulatorStatusFragment extends SherlockFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.emulator_status, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add("Stop").setIcon(R.drawable.stop_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add("Play").setIcon(R.drawable.play_icon)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("Stop".equals(item.getTitle())) {
            doStop();
            return true;
        }
        if ("Play".equals(item.getTitle())) {
            doPlay();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (this.isVisible()) {
            if (isVisibleToUser) {
                updateView();
            }
        }
    }

    private void doStop() {
        Configuration conf = TRS80Application.getCurrentConfiguration();
        if (conf == null) {
            showNoEmuRunningAlertDialog();
            return;
        }

        Configuration config = TRS80Application.getCurrentConfiguration();
        String msg = getActivity().getString(R.string.alert_dialog_confirm_stop_emu,
                config.getName());
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);
        builder.setMessage(msg);
        builder.setIcon(R.drawable.warning_icon);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                TRS80Application.setCurrentConfiguration(null);
                updateView();
            }

        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doPlay() {
        Configuration conf = TRS80Application.getCurrentConfiguration();
        if (conf == null) {
            showNoEmuRunningAlertDialog();
            return;
        }
        Intent i = new Intent(getActivity(), EmulatorActivity.class);
        startActivityForResult(i, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateView();
    }

    private void showNoEmuRunningAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.app_name);
        builder.setMessage(R.string.emulator_not_running);
        builder.setIcon(R.drawable.warning_icon);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }

        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateView() {
        boolean isRunning = TRS80Application.getCurrentConfiguration() != null;
        View runningView = getView().findViewById(R.id.emulator_running);
        View notRunningView = getView().findViewById(R.id.emulator_not_running);
        if (!isRunning) {
            notRunningView.setVisibility(View.VISIBLE);
            runningView.setVisibility(View.GONE);
            return;
        }

        notRunningView.setVisibility(View.GONE);
        runningView.setVisibility(View.VISIBLE);
        updateScreenshot();
    }

    private void updateScreenshot() {
        Bitmap screenshot = TRS80Application.getScreenshot();
        if (screenshot != null) {
            ImageView img = (ImageView) getView().findViewById(R.id.screenshot);
            img.setImageBitmap(screenshot);
        }

        Configuration conf = TRS80Application.getCurrentConfiguration();
        TextView nameLabel = (TextView) getView().findViewById(R.id.current_configuration_name);
        nameLabel.setText(conf == null ? "-" : conf.getName());
    }
}
