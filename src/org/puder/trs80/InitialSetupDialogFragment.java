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
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;

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
        final String model1_rom_url = this.getString(R.string.model1_rom_url);
        final String model1_rom_file_in_zip = this.getString(R.string.model1_rom_file_in_zip);
        final String model3_rom_url = this.getString(R.string.model3_rom_url);
        final String model1_rom_filename = this.getString(R.string.model1_rom_filename);
        final String model3_rom_filename = this.getString(R.string.model3_rom_filename);
        final String model1_rom_success_msg = this.getString(R.string.model1_rom_success_msg);
        final String model1_rom_failure_msg = this.getString(R.string.model1_rom_failure_msg);
        final String model3_rom_success_msg = this.getString(R.string.model3_rom_success_msg);
        final String model3_rom_failure_msg = this.getString(R.string.model3_rom_failure_msg);

        File sdcard = Environment.getExternalStorageDirectory();
        final String dirName = sdcard.getAbsolutePath() + "/" + this.getString(R.string.trs80_dir) + "/";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        final ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(true);
        progressDialog.setMessage("Downloading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result;

                // Download Model 1 ROM
                try {
                    result = download(model1_rom_url, "1",
                                      true, model1_rom_file_in_zip,
                                      dirName + model1_rom_filename);
                } catch (IOException e) {
                    result = false;
                }

                if (result) {
                    createModel1Configuration();
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog.dismiss();
                            mainFrag.romDownloaded();
                            Toast.makeText(mainFrag.getApplicationContext(),
                                           model1_rom_success_msg,
                                           Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog.dismiss();
                            Toast.makeText(mainFrag.getApplicationContext(),
                                           model1_rom_failure_msg,
                                           Toast.LENGTH_LONG).show();
                        }
                    });
                }

                // Download Model 3 ROM
                try {
                    result = download(model3_rom_url, "3",
                                      false, "",
                                      dirName + model3_rom_filename);
                } catch (IOException e) {
                    result = false;
                }

                if (result) {
                    createModel3Configuration();
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            mainFrag.romDownloaded();
                            Toast.makeText(mainFrag.getApplicationContext(),
                                           model3_rom_success_msg,
                                           Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(mainFrag.getApplicationContext(),
                                           model3_rom_failure_msg,
                                           Toast.LENGTH_LONG).show();
                        }
                    });
                }

                // Calling this here seems to cause some exceptions,
                // so we call it in the run() methods of the final
                // ROM loader above, as it was when only the Model 3 ROM
                // loader code was implemented...
                //progressDialog.dismiss();
            }
        }).start();
    }

    private boolean download(String URL, String model,
                             boolean isZipped, String fileInZip,
                             String destFilePath) throws IOException {
        boolean result = false;

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

                        result = true;
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

            result = true;
        }

        in.close();

        if (result) {
            // Perhaps this belongs in caller?
            SharedPreferences sharedPrefs = this.mainFrag.getSharedPreferences(SettingsActivity.SHARED_PREF_NAME, Context.MODE_PRIVATE);
            Editor editor = sharedPrefs.edit();
            if (model.equals("1")) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL1, destFilePath);
            } else if (model.equals("3")) {
                editor.putString(SettingsActivity.CONF_ROM_MODEL3, destFilePath);
            }
            editor.commit();
        }

        return result;
    }

    private void createModel1Configuration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName("Model I - no disks");
        firstConfig.setModel(Hardware.MODEL1);
        firstConfig.save();
    }

    private void createModel3Configuration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName("Model III - no disks");
        firstConfig.setModel(Hardware.MODEL3);
        firstConfig.save();
    }
}
