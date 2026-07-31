/*
 * Copyright The TRS-80 App Authors
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

// GoogleApiClient and the Cast.CastApi surface it drives are deprecated in favour of CastContext,
// which this class predates. The deprecated MediaRouter.Callback overloads are kept because the
// replacements delegate to them.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package org.puder.trs80.cast

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.Cast
// `Cast.CastApi` names both a nested interface and the static field holding the singleton that
// implements it. Kotlin resolves the bare qualifier to the interface, so the field is aliased in.
import com.google.android.gms.cast.Cast.CastApi as castApi
import com.google.android.gms.cast.CastDevice
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
import com.google.android.gms.common.api.ResultCallback
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val TAG = "CastMessageSender"
private const val CAST_CHANNEL_NAME = "urn:x-cast:org.puder.trs80"

/**
 * Handles GoogleCast connections.
 */
class CastMessageSender private constructor(
    private val appContext: Context,
    private val chromecastAppId: String
) {
    private val mediaRouter: MediaRouter = MediaRouter.getInstance(appContext)

    /** The selector describing the Cast routes this app can talk to. */
    val routeSelector: MediaRouteSelector = MediaRouteSelector.Builder()
        .addControlCategory(CastMediaControlIntent.categoryForCast(chromecastAppId))
        .build()

    /** Guards [apiClient] and everything that is only valid while it is connected. */
    private val apiClientLock = ReentrantLock()

    private var apiClient: GoogleApiClient? = null
    private var selectedDevice: CastDevice? = null
    private var activeChannel: TRS80Channel? = null
    private var sessionId: String? = null
    private var waitingForReconnect = false
    private var applicationStarted = false

    /** Whether we're ready to send messages to the Cast receiver app. */
    var isReadyToSend = false
        private set

    /**
     * Listens to events from the cast connection such as status changes.
     */
    private val castClientListener = object : Cast.Listener() {
        override fun onApplicationStatusChanged() = Unit

        override fun onVolumeChanged() = Unit

        override fun onApplicationDisconnected(errorCode: Int) = teardown()
    }

    /**
     * Called back when the connection to the cast app either succeeded or failed. This hooks up
     * the custom channel so we can start the communication with the receiver app.
     */
    private val resultCallback = object : ResultCallback<Cast.ApplicationConnectionResult> {
        override fun onResult(result: Cast.ApplicationConnectionResult) {
            if (!result.status.isSuccess) {
                teardown()
                return
            }
            sessionId = result.sessionId
            applicationStarted = true

            val channel = TRS80Channel()
            activeChannel = channel
            apiClientLock.withLock {
                try {
                    val client = apiClient
                        ?: throw IOException("Cannot set up callbacks. No apiClient.")
                    castApi.setMessageReceivedCallbacks(client, channel.namespace, channel)
                    isReadyToSend = true
                } catch (e: IOException) {
                    activeChannel = null
                    Toast.makeText(
                        appContext, "Exception while creating channel" + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Exception while creating channel", e)
                }
            }
        }
    }

    /**
     * Callbacks to inform us about changes in the Play Services connection.
     */
    private val connectionCallbacks = object : ConnectionCallbacks {
        override fun onConnectionSuspended(cause: Int) {
            isReadyToSend = false
            Log.d(TAG, "onConnectionSuspended()")
            waitingForReconnect = true
        }

        override fun onConnected(connectionHint: Bundle?) {
            isReadyToSend = false
            Log.d(TAG, "Play Services connected.")

            if (waitingForReconnect) {
                waitingForReconnect = false
                // reconnectChannels();
                return
            }
            apiClientLock.withLock {
                try {
                    val client = apiClient
                        ?: throw IOException("Cannot launch application, apiClient is null.")
                    castApi.launchApplication(client, chromecastAppId, false)
                        .setResultCallback(resultCallback)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch application", e)
                }
            }
        }
    }

    /**
     * Listener that is called if a connection to Play Services could not be established. Common
     * reasons are that Play Services is not installed or too old. In this case we want to prompt
     * the user to install the latest version.
     */
    private val connectionFailedListener = object : OnConnectionFailedListener {
        override fun onConnectionFailed(result: ConnectionResult) {
            isReadyToSend = false
            Log.d(TAG, "onConnectionFailed")
            Toast.makeText(
                appContext, "ConnectionFailed. " + result.errorCode, Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Callback to inform us when the user has selected a cast device and the route is ready to be
     * used.
     */
    private val mediaRouterCallback = object : MediaRouter.Callback() {
        override fun onRouteSelected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            Log.d(TAG, "Route selected")
            val device = CastDevice.getFromBundle(route.extras)
            selectedDevice = device
            if (device == null) {
                Log.w(TAG, "Selected route carries no cast device.")
                return
            }

            val apiOptions = Cast.CastOptions.builder(device, castClientListener).build()
            apiClientLock.withLock {
                apiClient = GoogleApiClient.Builder(appContext)
                    .addApi(Cast.API, apiOptions)
                    .addConnectionCallbacks(connectionCallbacks)
                    .addOnConnectionFailedListener(connectionFailedListener)
                    .build()
                    .also { it.connect() }
                Log.d(TAG, "API Client connect() called.")
            }
        }

        override fun onRouteUnselected(router: MediaRouter, route: MediaRouter.RouteInfo) {
            Log.d(TAG, "Route unselected")
            teardown()
            selectedDevice = null
        }
    }

    /** Starts scanning for Cast receivers matching [routeSelector]. */
    fun start() = mediaRouter.addCallback(
        routeSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN
    )

    /** Stops scanning for Cast receivers. */
    fun stop() = mediaRouter.removeCallback(mediaRouterCallback)

    /**
     * Sends the given message to the receiver.
     *
     * @param wait whether to block until the receiver has acknowledged the message.
     */
    fun sendMessage(message: String, wait: Boolean) {
        if (!isReadyToSend) {
            Log.w(TAG, "Sender not ready to send, message discarded.")
            return
        }
        apiClientLock.withLock {
            val client = apiClient ?: return
            val channel = activeChannel ?: return
            val result = castApi.sendMessage(client, channel.namespace, message)
            if (wait) {
                result.await()
            }
        }
    }

    /**
     * Tears down the whole connection stack as cleanly as possible.
     */
    private fun teardown() {
        apiClientLock.withLock {
            isReadyToSend = false
            Log.d(TAG, "teardown")
            apiClient?.let { client ->
                if (applicationStarted) {
                    if (client.isConnected) {
                        try {
                            sessionId?.let { castApi.stopApplication(client, it) }
                            activeChannel?.let {
                                castApi.removeMessageReceivedCallbacks(client, it.namespace)
                                activeChannel = null
                            }
                        } catch (e: IOException) {
                            Log.e(TAG, "Exception while removing channel", e)
                        }
                        client.disconnect()
                    }
                    applicationStarted = false
                }
                apiClient = null
            }
            selectedDevice = null
            waitingForReconnect = false
            sessionId = null
        }
    }

    /**
     * Our custom cast channel which we can use to send and receive messages.
     */
    private class TRS80Channel : Cast.MessageReceivedCallback {
        val namespace = CAST_CHANNEL_NAME

        override fun onMessageReceived(
            castDevice: CastDevice,
            namespace: String,
            message: String
        ) {
            Log.d(TAG, "onMessageReceived: $message")
        }
    }

    companion object {
        private lateinit var instance: CastMessageSender

        /** Creates the process-wide instance that [get] hands out. */
        @JvmStatic
        fun initSingleton(chromecastAppId: String, context: Context) {
            instance = CastMessageSender(context, chromecastAppId)
        }

        /** Returns the singleton created by [initSingleton]. */
        @JvmStatic
        fun get(): CastMessageSender = instance
    }
}
