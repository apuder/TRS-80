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

public class Model3 extends Hardware {

    final private static int     trsScreenCols    = 64;
    final private static int     trsScreenRows    = 16;
    final private static float   aspectRatio      = 3f;

    public Model3(Configuration conf, String romFile) {
        super(Hardware.MODEL3, conf, romFile);
    }

    @Override
    protected ScreenConfiguration getScreenConfiguration() {
        return new ScreenConfiguration(trsScreenCols, trsScreenRows, aspectRatio);
    }
}
