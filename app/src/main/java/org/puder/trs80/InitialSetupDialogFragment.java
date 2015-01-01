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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.widget.Toast;

public class InitialSetupDialogFragment extends DialogFragment {

    public interface DownloadCompletionListener {
        public void onDownloadCompleted();
    }

    DownloadCompletionListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (DownloadCompletionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DownloadCompletionListener");
        }
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
        final String model1_rom_url = this.getString(R.string.model1_rom_url);
        final String model1_rom_file_in_zip = this.getString(R.string.model1_rom_file_in_zip);
        final String model3_rom_url = this.getString(R.string.model3_rom_url);
        final String model1_rom_filename = this.getString(R.string.model1_rom_filename);
        final String model3_rom_filename = this.getString(R.string.model3_rom_filename);

        File sdcard = Environment.getExternalStorageDirectory();
        final String dirName = sdcard.getAbsolutePath() + "/" + this.getString(R.string.trs80_dir)
                + "/";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(true);
        progressDialog.setMessage(this.getString(R.string.downloading));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok;

                if (!ROMs.hasModel1ROM()) {
                    // Download Model 1 ROM
                    try {
                        ok = download(model1_rom_url, Hardware.MODEL1, true,
                                model1_rom_file_in_zip, dirName + model1_rom_filename);
                    } catch (IOException e) {
                        ok = false;
                    }
                    if (ok) {
                        createModel1Configuration();
                    }
                }

                if (!ROMs.hasModel3ROM()) {
                    // Download Model 3 ROM
                    try {
                        ok = download(model3_rom_url, Hardware.MODEL3, false, "", dirName
                                + model3_rom_filename);
                    } catch (IOException e) {
                        ok = false;
                    }
                    if (ok) {
                        createModel3Configuration();
                    }
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            progressDialog.dismiss();
                            if (ROMs.hasROMs()) {
                                Toast.makeText(TRS80Application.getAppContext(),
                                        R.string.roms_downlaod_success_msg, Toast.LENGTH_LONG)
                                        .show();
                                listener.onDownloadCompleted();
                            } else {
                                Toast.makeText(TRS80Application.getAppContext(),
                                        R.string.roms_download_failure_msg, Toast.LENGTH_LONG)
                                        .show();
                            }
                        } catch (IllegalArgumentException ex) {
                            /*
                             * If the Activity has gone away before
                             * progressDialog.dismiss() is called, we will get
                             * this exception. Simply ignore it.
                             */
                        }
                    }
                });
            }
        }).start();
    }

    private boolean download(String URL, int model, boolean isZipped, String fileInZip,
            String destFilePath) throws IOException {
        boolean ok = false;

        URL url = new URL(URL);
        URLConnection urlConnection = url.openConnection();
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());

        if (isZipped) {
            ZipInputStream zis = new ZipInputStream(in);
            try {
                ZipEntry ze;
                while ((ze = zis.getNextEntry()) != null) {
                    if (ze.getName().equals(fileInZip)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int count;
                        while ((count = zis.read(buffer)) != -1) {
                            baos.write(buffer, 0, count);
                        }
                        byte[] bytes = baos.toByteArray();

                        File destFile = new File(destFilePath);
                        OutputStream out = new FileOutputStream(destFile);
                        out.write(bytes);
                        out.close();

                        ok = true;
                        break;
                    }
                }
            } finally {
                zis.close();
            }
        } else {
            File destFile = new File(destFilePath);
            OutputStream out = new FileOutputStream(destFile);
            IOUtils.copy(in, out);
            out.close();

            ok = true;
        }

        in.close();

        if (ok) {
            // Perhaps this belongs in caller?
            String pref = null;
            switch (model) {
            case Hardware.MODEL1:
                pref = SettingsActivity.CONF_ROM_MODEL1;
                break;
            case Hardware.MODEL3:
                pref = SettingsActivity.CONF_ROM_MODEL3;
                break;
            }

            SharedPreferences sharedPrefs = TRS80Application.getAppContext().getSharedPreferences(
                    SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            Editor editor = sharedPrefs.edit();
            editor.putString(pref, destFilePath);
            editor.commit();
        }

        return ok;
    }

    private void createModel1Configuration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName(TRS80Application.getAppContext().getString(R.string.config_name_model1));
        firstConfig.setModel(Hardware.MODEL1);
        firstConfig.save();
    }

    private void createModel3Configuration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName(TRS80Application.getAppContext().getString(R.string.config_name_model3));
        firstConfig.setModel(Hardware.MODEL3);
        firstConfig.save();
    }
}
