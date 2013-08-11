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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.io.IOUtils;
import org.puder.trs80.Hardware.Model;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class InitialSetupDialogFragment extends SherlockDialogFragment {
    final private static String PATH           = "/TRS-80/";

    final private static String URL_ROM_MODEL3 = "http://www.classiccmp.org/cpmarchives/trs80/Miscellany/Emulatrs/trs80-62/model3.rom";

    private MainFragment        mainFrag;

    public static InitialSetupDialogFragment newInstance(MainFragment mainFrag) {
        InitialSetupDialogFragment frag = new InitialSetupDialogFragment();
        frag.mainFrag = mainFrag;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.warning_icon)
                .setTitle(R.string.title_initial_setup)
                .setMessage(R.string.initial_setup)
                .setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        doInitialSetup();
                    }
                })
                .setNegativeButton(R.string.alert_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).create();
    }

    private void doInitialSetup() {
        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Downloading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    download();
                    createFirstConfiguration();
                    mainFrag.handler.post(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            mainFrag.romDownloaded();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download succeeded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (IOException e) {
                    mainFrag.handler.post(new Runnable() {

                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download failed!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void download() throws IOException {
        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(sdcard.getAbsolutePath() + PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fn = dir.getAbsolutePath() + "/model3.rom";
        File file = new File(fn);
        OutputStream out = new FileOutputStream(file);
        URL url = new URL(URL_ROM_MODEL3);
        URLConnection urlConnection = url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        IOUtils.copy(in, out);
        in.close();
        out.close();
        SharedPreferences sharedPrefs = this.mainFrag.getSharedPreferences(
                SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        Editor editor = sharedPrefs.edit();
        editor.putString(SettingsActivity.CONF_ROM_MODEL3, fn);
        editor.commit();
    }

    private void createFirstConfiguration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName("BASIC Interpreter");
        firstConfig.setModel(Model.MODEL3);
        firstConfig.save();
    }
}
