/*
 * Copyright 2017, Sascha Haeberling
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

package org.puder.trs80.io;

import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads files and optionally extracts entries from a ZIP file.
 */
public class FileDownloader {
    private static final String TAG = "FileDownloader";

    /**
     * Download the given URL and if needed, extracts a file from the ZIP file.
     *
     * @param urlStr    the URL to download.
     * @param fileInZip if the URL is a ZIP file, specify which file to extract from it.
     * @return The file contents or absent, if the file could not be downloaded or extracted.
     */
    public Optional<byte[]> download(String urlStr, String fileInZip) {
        byte[] fileContent = null;
        try {
            java.net.URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            if (fileInZip != null) {
                ZipInputStream zis = new ZipInputStream(in);
                try {
                    ZipEntry ze;
                    while ((ze = zis.getNextEntry()) != null) {
                        if (ze.getName().equals(fileInZip)) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            ByteStreams.copy(zis, baos);
                            fileContent = baos.toByteArray();
                            break;
                        }
                    }
                } finally {
                    zis.close();
                }
            } else {
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                IOUtils.copy(in, byteArray);
                byteArray.close();
                fileContent = byteArray.toByteArray();
            }
            in.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not load data", e);
        }
        return Optional.fromNullable(fileContent);
    }
}
