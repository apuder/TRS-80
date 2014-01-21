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
    final private static String PATH           = "/TRS-80/";

    final private static String URL_ROM_MODEL1 = "http://www.classic-computers.org.nz/system-80/s80-roms.zip";
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
        File sdcard = Environment.getExternalStorageDirectory();
        final String dirName = sdcard.getAbsolutePath() + PATH;
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
                    String destFilePath = dirName + "model1.rom";
                    result = download(URL_ROM_MODEL1, "1",
                                      true, "trs80model1.rom", destFilePath);
                } catch (IOException e) {
                    result = false;
                }

                if (result) {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog.dismiss();
                            mainFrag.romDownloaded();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download of Model 1 ROM succeeded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //progressDialog.dismiss();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download of Model 1 ROM failed!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }

                // Download Model 3 ROM
                try {
                    String destFilePath = dirName + "model3.rom";
                    result = download(URL_ROM_MODEL3, "3",
                                      false, "", destFilePath);

                    createFirstConfiguration();
                } catch (IOException e) {
                    result = false;
                }

                if (result) {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            mainFrag.romDownloaded();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download of Model 3 ROM succeeded!",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    mainFrag.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            Toast.makeText(mainFrag.getApplicationContext(), "Download of Model 3 ROM failed!",
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

    private void createFirstConfiguration() {
        ConfigurationBackup firstConfig = new ConfigurationBackup(Configuration.newConfiguration());
        firstConfig.setName("BASIC Interpreter");
        firstConfig.setModel(Hardware.MODEL3);
        firstConfig.save();
    }
}
