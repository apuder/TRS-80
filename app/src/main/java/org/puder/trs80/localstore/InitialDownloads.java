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

package org.puder.trs80.localstore;

import org.puder.trs80.Hardware;
import org.puder.trs80.InitialSetupDialogFragment;

/**
 * ROMs and entries to be downloaded initially when the app starts up.
 */
public class InitialDownloads {

    public static Download[] get() {
        return new Download[]{
                new Download(true, Hardware.MODEL1, null,
                        "http://www.classic-computers.org.nz/system-80/s80-roms.zip",
                        "trs80model1.rom", "model1.rom"),
            /*
            // Defunct download
            new Download(
                    true,
                    Hardware.MODEL3,
                    null,
                    "http://www.classiccmp
                    .org/cpmarchives/trs80/Miscellany/Emulatrs/trs80-62/model3.rom",
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
                    "http://www.classiccmp
                    .org/cpmarchives/trs80/Software/Model%201/N/NEWDOS-80%20v2.0%20(19xx)
                    (Apparat%20Inc)%5bDSK%5d%5bMaster%5d.zip",
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
                    "http://www.classiccmp
                    .org/cpmarchives/trs80/Software/Model%20III/NEWDOS-80%20v2.0%20(19xx)
                    (Apparat%20Inc)%5bDSK%5d.zip",
                    "NEWDOS80.DSK", "newdos80-model3.dsk"),
                     */
                new Download(
                        false,
                        Hardware.MODEL3,
                        "Model III - NEWDOS/80",
                        "http://www.manmrk.net/tutorials/TRS80/Software/newdos/nd80v2d.zip",
                        "nd80ira.dsk", "newdos80-model3.dsk"),
        };
    }

    public static class Download {
        public boolean isROM;
        public int model;
        public String configurationName;
        public String url;
        public String fileInZip;
        public String destinationFilename;


        public Download(boolean isROM, int model, String configurationName, String url,
                        String fileInZip, String destinationFilename) {
            this.isROM = isROM;
            this.model = model;
            this.configurationName = configurationName;
            this.url = url;
            this.fileInZip = fileInZip;
            this.destinationFilename = destinationFilename;
        }
    }
}
