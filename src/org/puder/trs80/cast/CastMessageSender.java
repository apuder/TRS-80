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

import java.io.IOException;

import android.content.BroadcastReceiver.PendingResult;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

/**
 * Handles GoogleCast connections.
 */
public class CastMessageSender {

    private static final String           TAG                 = "CastMessageSender";
    private static final String           CAST_CHANNEL_NAME   = "urn:x-cast:org.puder.trs80";
    private MediaRouteSelector            routeSelector;
    private MediaRouter                   mediaRouter;
    private final MediaRouterCallbackImpl mediaRouterCallback = new MediaRouterCallbackImpl();
    private CastDevice                    selectedDevice;
    private GoogleApiClient               apiClient;
    private boolean                       waitingForReconnect;
    private boolean                       applicationStarted;
    private String                        chromecastAppId;
    private String                        sessionId;
    private TRS80Channel                  activeChannel;
    private Context                       appContext;
    private boolean                       readyToSend;

    private static CastMessageSender      instance;

    public static void initSingleton(String chromcastAppId, Context context) {
        instance = (new CastMessageSender(context)).init(chromcastAppId);
    }

    public static CastMessageSender get() {
        return instance;
    }

    public CastMessageSender(Context appContext) {
        this.appContext = appContext;
    }

    public CastMessageSender init(String chromcastAppId) {
        chromecastAppId = chromcastAppId;
        mediaRouter = MediaRouter.getInstance(appContext);
        routeSelector = new MediaRouteSelector.Builder().addControlCategory(
                CastMediaControlIntent.categoryForCast(chromecastAppId)).build();
        return this;
    }

    public void start() {
        mediaRouter.addCallback(routeSelector, mediaRouterCallback,
                MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
    }

    public void stop() {
        mediaRouter.removeCallback(mediaRouterCallback);
    }

    /**
     * Returns whether we're ready to send messages to the Cast receiver app.
     */
    public boolean isReadyToSend() {
        return readyToSend;
    }

    /** Send the given message to the receiver. */
    public void sendMessage(String message, boolean wait) {
        if (!readyToSend) {
            Log.w(TAG, "Sender not ready to send, message discarded.");
            return;
        }
        com.google.android.gms.common.api.PendingResult<Status> result = Cast.CastApi.sendMessage(
                apiClient, activeChannel.getNamespace(), message);
        if (wait) {
            result.await();
        }
    }

    public MediaRouteSelector getRouteSelector() {
        return routeSelector;
    }

    /**
     * Our custom cast channel which we can use to send and receive messages.
     */
    private static class TRS80Channel implements Cast.MessageReceivedCallback {
        public String getNamespace() {
            return CAST_CHANNEL_NAME;
        }

        @Override
        public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
            Log.d(TAG, "onMessageReceived: " + message);
        }
    }

    /**
     * Listens to event from the cast connection such as status changes.
     */
    private class CastClientListener extends Cast.Listener {
        @Override
        public void onApplicationStatusChanged() {
//            if (apiClient != null) {
//                Log.d(TAG,
//                        "onApplicationStatusChanged: "
//                                + Cast.CastApi.getApplicationStatus(apiClient));
//            }
        }

        @Override
        public void onVolumeChanged() {
//            if (apiClient != null) {
//                Log.d(TAG, "onVolumeChanged: " + Cast.CastApi.getVolume(apiClient));
//            }
        }

        @Override
        public void onApplicationDisconnected(int errorCode) {
            teardown();
        }
    }

    private final Cast.Listener castClientListener = new CastClientListener();

    /**
     * Callback to inform us when the user has selected a cast device and the
     * route is ready to be used.
     */
    private class MediaRouterCallbackImpl extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "Route selected");
            selectedDevice = CastDevice.getFromBundle(route.getExtras());

            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(selectedDevice,
                    castClientListener);

            apiClient = new GoogleApiClient.Builder(appContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener).build();

            apiClient.connect();
            Log.d(TAG, "API Client connect() called.");
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo route) {
            Log.d(TAG, "Route unselected");
            teardown();
            selectedDevice = null;
        }
    }

    /**
     * Called back when the connection to the cast app either succeeded or
     * failed. This hooks up the custom channel so we can start the
     * communication with the receiver app.
     */
    private class CastResultCallback implements ResultCallback<Cast.ApplicationConnectionResult> {
        @Override
        public void onResult(Cast.ApplicationConnectionResult result) {
            Status status = result.getStatus();
            if (!status.isSuccess()) {
                teardown();
                return;
            }
            ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
            sessionId = result.getSessionId();
            String applicationStatus = result.getApplicationStatus();
            boolean wasLaunched = result.getWasLaunched();
            applicationStarted = true;

            activeChannel = new TRS80Channel();
            try {
                Cast.CastApi.setMessageReceivedCallbacks(apiClient, activeChannel.getNamespace(),
                        activeChannel);
                readyToSend = true;
            } catch (IOException e) {
                activeChannel = null;
                String message = "Exception while creating channel" + e.getMessage();
                Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Exception while creating channel", e);
            }
        }
    }

    private final ResultCallback<Cast.ApplicationConnectionResult> resultCallback = new CastResultCallback();

    /**
     * Callbacks to inform us about changes in the Play Services connection.
     */
    private class GmsConnectionCallbacks implements ConnectionCallbacks {
        @Override
        public void onConnectionSuspended(int cause) {
            readyToSend = false;
            Log.d(TAG, "onConnectionSuspended()");
            waitingForReconnect = true;
        }

        @Override
        public void onConnected(Bundle connectionHint) {
            readyToSend = false;
            Log.d(TAG, "Play Services connected.");

            if (waitingForReconnect) {
                waitingForReconnect = false;
                // reconnectChannels();
            } else {
                try {
                    Cast.CastApi.launchApplication(apiClient, chromecastAppId, false)
                            .setResultCallback(resultCallback);

                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch application", e);
                }
            }
        }
    }

    private final ConnectionCallbacks connectionCallbacks = new GmsConnectionCallbacks();

    /**
     * Listener that is called if a connection to Play Services could not be
     * established. Common reasons are that Play Services is not installed or
     * too old. In this case we want to prompt the user to install the latest
     * version.
     */
    private class GmsConnectionFailedListener implements OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            readyToSend = false;
            Log.d(TAG, "onConnectionFailed");
            // try
            // {
            Toast.makeText(appContext, "ConnectionFailed. " + result.getErrorCode(),
                    Toast.LENGTH_SHORT).show();

            // result.startResolutionForResult(activity, 23);
            // }
            // catch
            // (SendIntentException e)
            // {
            // e.printStackTrace();
            // Toast.makeText(appContext, e.getMessage(),
            // Toast.LENGTH_SHORT).show();
            // }
        }
    }

    private final OnConnectionFailedListener connectionFailedListener = new GmsConnectionFailedListener();

    /**
     * Tears down the whole connection stack as cleanly as possible.
     */
    private void teardown() {
        readyToSend = false;
        Log.d(TAG, "teardown");
        if (apiClient != null) {
            if (applicationStarted) {
                if (apiClient.isConnected()) {
                    try {
                        Cast.CastApi.stopApplication(apiClient, sessionId);
                        if (activeChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(apiClient,
                                    activeChannel.getNamespace());
                            activeChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
                    }
                    apiClient.disconnect();
                }
                applicationStarted = false;
            }
            apiClient = null;
        }
        selectedDevice = null;
        waitingForReconnect = false;
        sessionId = null;
    }
}
