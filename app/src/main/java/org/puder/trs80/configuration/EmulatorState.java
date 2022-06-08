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

package org.puder.trs80.configuration;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.protobuf.InvalidProtocolBufferException;

import org.puder.trs80.XTRS;
import org.puder.trs80.io.FileManager;
import org.puder.trs80.proto.NativeSystemState;
import org.retrostore.client.common.proto.SystemState;
import org.retrostore.client.common.proto.Trs80Model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Persists the state of an emulator session so it can be resumed later.
 */
public class EmulatorState {
    private static final String TAG = "EmulatorState";
    private static final String FILE_SCREENSHOT = "screenshot.png";
    private static final String FILE_STATE = "state";
    private static final String FILE_XRAY_STATE = "xray_state.pb";
    private static final String FILE_CASSETTE = "cassette.cas";

    private final FileManager fileManager;

    public static EmulatorState forConfigId(
            int configurationID, FileManager.Creator fileManagerCreator) throws IOException {
        FileManager fileManager = fileManagerCreator.createForAppSubDir(configurationID);
        fileManager.ensureNoMedia();
        return new EmulatorState(fileManager);
    }

    private EmulatorState(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    private String getStateFileName() {
        return fileManager.getAbsolutePathForFile(FILE_STATE);
    }

    public String getDefaultCassettePath() {
        return fileManager.getAbsolutePathForFile(FILE_CASSETTE);
    }

    public String getBasePath() {
        return fileManager.getAbsolutePathForFile("");
    }

    public void saveState() {
        XTRS.saveState(getStateFileName());
    }

    public void loadState() {
        XTRS.loadState(getStateFileName());
    }

    @SuppressLint("CheckResult")
    public Optional<SystemState> getSystemState() {
        if (!fileManager.hasFile(FILE_XRAY_STATE)) {
            return Optional.absent();
        }
        byte[] stateBytes = new byte[0];
        try {
            String absolutePathForFile = fileManager.getAbsolutePathForFile(FILE_XRAY_STATE);
            stateBytes = Files.toByteArray(new File(absolutePathForFile));
        } catch (IOException e) {
            Log.e(TAG, "Unable to load xray state file from file.", e);
            return Optional.absent();
        }
        NativeSystemState nativeState = null;
        try {
            nativeState = NativeSystemState.parseFrom(stateBytes);
        } catch (InvalidProtocolBufferException e) {
            Log.e(TAG, "Unable to parse xray state protocol buffer.", e);
            return Optional.absent();
        }

        SystemState.Builder rsState = SystemState.newBuilder();

        // Set model number.
        switch (nativeState.getModel()) {
            case MODEL_I:
                rsState.setModel(Trs80Model.MODEL_I);
            case MODEL_III:
                rsState.setModel(Trs80Model.MODEL_III);
                break;
            case MODEL_4:
                rsState.setModel(Trs80Model.MODEL_4);
                break;
            case MODEL_4P:
                rsState.setModel(Trs80Model.MODEL_4P);
                break;
            case UNKNOWN_MODEL:
                rsState.setModel(Trs80Model.UNKNOWN_MODEL);
                break;
            case UNRECOGNIZED:
                rsState.setModel(Trs80Model.UNRECOGNIZED);
                break;
        }

        // Set all registers.
        SystemState.Registers.Builder rsRegisters = SystemState.Registers.newBuilder()
                .setIx(nativeState.getRegisters().getIx())
                .setIy(nativeState.getRegisters().getIy())
                .setPc(nativeState.getRegisters().getPc())
                .setSp(nativeState.getRegisters().getSp())
                .setAf(nativeState.getRegisters().getAf())
                .setBc(nativeState.getRegisters().getBc())
                .setDe(nativeState.getRegisters().getDe())
                .setHl(nativeState.getRegisters().getHl())
                .setAfPrime(nativeState.getRegisters().getAfPrime())
                .setBcPrime(nativeState.getRegisters().getBcPrime())
                .setDePrime(nativeState.getRegisters().getDePrime())
                .setHlPrime(nativeState.getRegisters().getHlPrime())
                .setI(nativeState.getRegisters().getI())
                .setR1(nativeState.getRegisters().getR1())
                .setR2(nativeState.getRegisters().getR2());
        rsState.setRegisters(rsRegisters);

        // Add memory regions.
        for (NativeSystemState.MemoryRegion nativeMem : nativeState.getMemoryRegionsList()) {
            rsState.addMemoryRegions(SystemState.MemoryRegion
                    .newBuilder()
                    .setStart(nativeMem.getStart())
                    .setData(nativeMem.getData()));
        }

        return Optional.of(rsState.build());
    }

    public boolean hasState() {
        return fileManager.hasFile(FILE_STATE);
    }
    public boolean hasXrayState() {
        return fileManager.hasFile(FILE_XRAY_STATE);
    }

    public void saveScreenshot(Bitmap screenshot) {
        if (screenshot == null) {
            // Can happen when NotImplementedException is thrown
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // out = new FileOutputStream(getScreenshotFileName(configurationID));
            screenshot.compress(Bitmap.CompressFormat.PNG, 90, out);
            fileManager.writeFile(FILE_SCREENSHOT, out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (Throwable ignore) {
            }
        }
    }

    public Bitmap loadScreenshot() {
        Optional<byte[]> screenshot = fileManager.readFile(FILE_SCREENSHOT);
        if (!screenshot.isPresent()) {
            return null;
        }
        byte[] data = screenshot.get();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    public void deleteSavedState() {
        fileManager.deleteFile(FILE_STATE);
        fileManager.deleteFile(FILE_XRAY_STATE);
        fileManager.deleteFile(FILE_SCREENSHOT);
    }

    public void deleteAll() {
        fileManager.delete();
    }
}
