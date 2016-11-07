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
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.view.View;

import org.apache.commons.io.IOUtils;

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

public class InitialSetupDialogFragment extends DialogFragment {

    public interface DownloadCompletionListener {
        void onDownloadCompleted();
    }


    static private class Download {
        public boolean isROM;
        public int     model;
        public String  configurationName;
        public String  url;
        public String  fileInZip;
        public String  destinationPath;


        public Download(boolean isROM, int model, String configurationName, String url,
                String fileInZip, String destinationPath) {
            this.isROM = isROM;
            this.model = model;
            this.configurationName = configurationName;
            this.url = url;
            this.fileInZip = fileInZip;
            this.destinationPath = destinationPath;
        }
    }


    static private Download            downloads[]     = {
            new Download(true, Hardware.MODEL1, null,
                    "http://www.classic-computers.org.nz/system-80/s80-roms.zip",
                    "trs80model1.rom", "model1.rom"),
            /*
            // Defunct download
            new Download(
                    true,
                    Hardware.MODEL3,
                    null,
                    "http://www.classiccmp.org/cpmarchives/trs80/Miscellany/Emulatrs/trs80-62/model3.rom",
                    null, "model3.rom"),
                    */
            new Download(
                    true,
                    Hardware.MODEL3,
                    null,
                    "https://github.com/lkesteloot/trs80/raw/master/roms/model3.rom",
                    null, "model3.rom"),
            /*
            new Download(false, Hardware.MODEL1, "Model I - LDOS",
                    "http://www.tim-mann.org/trs80/ld1-531.zip", "ld1-531.dsk", "ldos-model1.dsk"),
                    */
            /*
            new Download(
                    false,
                    Hardware.MODEL1,
                    "Model I - NEWDOS/80",
                    "http://www.classiccmp.org/cpmarchives/trs80/Software/Model%201/N/NEWDOS-80%20v2.0%20(19xx)(Apparat%20Inc)%5bDSK%5d%5bMaster%5d.zip",
                    "ND80MST.DSK", "newdos80-model1.dsk"),
                    */
            new Download(
                    false,
                    Hardware.MODEL1,
                    "Model I - NEWDOS/80",
                    "http://www.manmrk.net/tutorials/TRS80/Software/newdos/nd80v2m1.zip",
                    "ND80MST.DSK", "newdos80-model1.dsk"),
            /*
            new Download(false, Hardware.MODEL3, "Model III - LDOS",
                    "http://www.tim-mann.org/trs80/ld3-531.zip", "ld3-531.dsk", "ldos-model3.dsk"),
                    */
            /*
            new Download(
                    false,
                    Hardware.MODEL3,
                    "Model III - NEWDOS/80",
                    "http://www.classiccmp.org/cpmarchives/trs80/Software/Model%20III/NEWDOS-80%20v2.0%20(19xx)(Apparat%20Inc)%5bDSK%5d.zip",
                    "NEWDOS80.DSK", "newdos80-model3.dsk"),
                     */
            new Download(
                    false,
                    Hardware.MODEL3,
                    "Model III - NEWDOS/80",
                    "http://www.manmrk.net/tutorials/TRS80/Software/newdos/nd80v2d.zip",
                    "nd80ira.dsk", "newdos80-model3.dsk"),
    };

    private DownloadCompletionListener listener;
    private int                        downloadCounter = 0;


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
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setCancelable(false);
        progressDialog
                .setMessage(getString(R.string.downloading, downloadCounter, downloads.length));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        return progressDialog;
    }

    @Override
    public void onDestroyView() {
        /*
         * https://code.google.com/p/android/issues/detail?id=17423
         */
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setCancelable(false);
        setRetainInstance(true);

        File sdcard = Environment.getExternalStorageDirectory();
        final String dirName = sdcard.getAbsolutePath() + "/" + this.getString(R.string.trs80_dir)
                + "/";
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        new AsyncTask<Void, Integer, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                SharedPreferences sharedPrefs = TRS80Application.getAppContext()
                        .getSharedPreferences(SettingsActivity.SHARED_PREF_NAME,
                                Context.MODE_PRIVATE);
                Editor editor = sharedPrefs.edit();
                for (int i = 0; i < downloads.length; i++) {
                    downloadCounter = i + 1;
                    publishProgress(downloadCounter);
                    Download download = downloads[i];
                    String url = download.url;
                    boolean isZipped = download.fileInZip != null;
                    String fileInZip = download.fileInZip;
                    String destFilePath = dirName + download.destinationPath;
                    if (!download.isROM && new File(destFilePath).exists()) {
                        // Image file already exists. Skip
                        continue;
                    }
                    boolean ok = download(url, isZipped, fileInZip, destFilePath);
                    if (!ok) {
                        continue;
                    }
                    if (download.isROM) {
                        String key = null;
                        switch (download.model) {
                        case Hardware.MODEL1:
                            key = SettingsActivity.CONF_ROM_MODEL1;
                            break;
                        case Hardware.MODEL3:
                            key = SettingsActivity.CONF_ROM_MODEL3;
                            break;
                        case Hardware.MODEL4:
                            key = SettingsActivity.CONF_ROM_MODEL4;
                            break;
                        case Hardware.MODEL4P:
                            key = SettingsActivity.CONF_ROM_MODEL4P;
                            break;
                        }
                        editor.putString(key, destFilePath);
                    } else {
                        ConfigurationBackup newConfig = new ConfigurationBackup(
                                Configuration.newConfiguration());
                        newConfig.setName(download.configurationName);
                        newConfig.setModel(download.model);
                        newConfig.setDiskPath(0, destFilePath);
                        newConfig.save();
                    }
                }
                editor.apply();
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                ((ProgressDialog) getDialog()).setMessage(getString(R.string.downloading,
                        progress[0], downloads.length));
            }

            @Override
            protected void onPostExecute(Void result) {
                dismissAllowingStateLoss();
                View root = getActivity().findViewById(R.id.main);
                if (ROMs.hasROMs()) {
                    listener.onDownloadCompleted();
                } else {
                    Snackbar.make(root, R.string.roms_download_failure_msg,
                            Snackbar.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    private boolean download(String URL, boolean isZipped, String fileInZip, String destFilePath) {
        boolean ok = false;

        try {
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
        } catch (IOException e) {
            ok = false;
        }

        return ok;
    }
}
