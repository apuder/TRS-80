/*
 * Copyright 2014, Sascha Haeberling
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

package org.puder.trs80.cast;

import java.util.Locale;

/**
 * A remote display channel that controls a remote screen using the google cast
 * protocol.
 */
public class RemoteCastScreen implements RemoteDisplayChannel {
    private static final int        TYPE_START_SESSION      = 1;
    private static final int        TYPE_END_SESSION        = 2;
    private static final int        TYPE_SEND_SCREEN_BUFFER = 3;
    private static final int        TYPE_SEND_CONFIGURATION = 4;
    private static final String     MESSAGE_FORMAT          = "%d:%s";
    private static final String     SCREEN_BUFFER_FORMAT    = "%s:%s";

    private static RemoteCastScreen instance;

    private final CastMessageSender sender;


    public static void initSingleton(CastMessageSender sender) {
        instance = new RemoteCastScreen(sender);
    }

    public static RemoteCastScreen get() {
        return instance;
    }

    private RemoteCastScreen(CastMessageSender sender) {
        if (sender == null) {
            throw new IllegalArgumentException("Null sender not allowed.");
        }
        this.sender = sender;
    }

    @Override
    public void startSession() {
        sendMessage(TYPE_START_SESSION, null);
    }

    @Override
    public void endSession() {
        sendMessage(TYPE_END_SESSION, null);
    }

    @Override
    public void sendScreenBuffer(boolean expandedMode, String buffer) {
        sendMessage(TYPE_SEND_SCREEN_BUFFER,
                String.format(Locale.US, SCREEN_BUFFER_FORMAT, expandedMode ? "1" : "0", buffer));
    }

    @Override
    public void sendConfiguration(int foregroundColor, int backgroundColor) {
        String fgColor = colorToWebFormat(foregroundColor);
        String bgColor = colorToWebFormat(backgroundColor);
        sendMessage(TYPE_SEND_CONFIGURATION, fgColor + ':' + bgColor);
    }

    private void sendMessage(int type, Object payload) {
        if (!sender.isReadyToSend()) {
            return;
        }
        this.sender.sendMessage(createMessage(type, payload));
    }

    private static String createMessage(int type, Object payload) {
        return String.format(Locale.US, MESSAGE_FORMAT, type,
                payload == null ? "" : payload.toString());
    }

    private static String colorToWebFormat(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }
}
