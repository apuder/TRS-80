package org.puder.trs80;

import java.io.IOException;

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

public class CastMessageSender {

	private static final String TAG = "CastMessageSender";
	private MediaRouteSelector routeSelector;
	private MediaRouter mediaRouter;
	private final MediaRouterCallbackImpl mediaRouterCallback = new MediaRouterCallbackImpl();
	private CastDevice selectedDevice;
	private GoogleApiClient apiClient;
	private boolean waitingForReconnect;
	private boolean applicationStarted;
	private String chromecastAppId;
	private String sessionId;
	private TRS80Channel activeChannel;
	private Context appContext;

	public CastMessageSender(Context appContext) {
		this.appContext = appContext;
	}

	public CastMessageSender init(String chromcastAppId) {
		chromecastAppId = chromcastAppId;
		mediaRouter = MediaRouter.getInstance(appContext);
		routeSelector = new MediaRouteSelector.Builder().addControlCategory(
				CastMediaControlIntent.categoryForCast(chromecastAppId))
				.build();
		return this;
	}

	public void start() {
		mediaRouter.addCallback(routeSelector, mediaRouterCallback,
				MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
	}

	public void stop() {
		mediaRouter.removeCallback(mediaRouterCallback);
	}

	public void sendMessage(String message) {
		Cast.CastApi.sendMessage(apiClient, activeChannel.getNamespace(),
				message);
	}

	public MediaRouteSelector getRouteSelector() {
		return routeSelector;
	}

	private static class TRS80Channel implements Cast.MessageReceivedCallback {
		public String getNamespace() {
			return "urn:x-cast:org.puder.trs80";
		}

		@Override
		public void onMessageReceived(CastDevice castDevice, String namespace,
				String message) {
			Log.d(TAG, "onMessageReceived: " + message);
		}
	}

	Cast.Listener castClientListener = new Cast.Listener() {
		@Override
		public void onApplicationStatusChanged() {
			if (apiClient != null) {
				Log.d(TAG,
						"onApplicationStatusChanged: "
								+ Cast.CastApi.getApplicationStatus(apiClient));
			}
		}

		@Override
		public void onVolumeChanged() {
			if (apiClient != null) {
				Log.d(TAG,
						"onVolumeChanged: " + Cast.CastApi.getVolume(apiClient));
			}
		}

		@Override
		public void onApplicationDisconnected(int errorCode) {
			teardown();
		}
	};

	private class MediaRouterCallbackImpl extends MediaRouter.Callback {
		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo route) {
			Log.d(TAG, "Route selected");
			selectedDevice = CastDevice.getFromBundle(route.getExtras());

			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
					.builder(selectedDevice, castClientListener);

			apiClient = new GoogleApiClient.Builder(appContext)
					.addApi(Cast.API, apiOptionsBuilder.build())
					.addConnectionCallbacks(connectionCallbacks)
					.addOnConnectionFailedListener(connectionFailedListener)
					.build();

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

	ResultCallback<Cast.ApplicationConnectionResult> resultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
		@Override
		public void onResult(Cast.ApplicationConnectionResult result) {
			Status status = result.getStatus();
			if (!status.isSuccess()) {
				teardown();
				return;
			}
			ApplicationMetadata applicationMetadata = result
					.getApplicationMetadata();
			sessionId = result.getSessionId();
			String applicationStatus = result.getApplicationStatus();
			boolean wasLaunched = result.getWasLaunched();
			applicationStarted = true;

			activeChannel = new TRS80Channel();
			try {
				Cast.CastApi.setMessageReceivedCallbacks(apiClient,
						activeChannel.getNamespace(), activeChannel);
			} catch (IOException e) {
				activeChannel = null;
				String message = "Exception while creating channel"
						+ e.getMessage();
				Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Exception while creating channel", e);
			}
		}
	};

	private final ConnectionCallbacks connectionCallbacks = new ConnectionCallbacks() {

		@Override
		public void onConnectionSuspended(int cause) {
			Log.d(TAG, "onConnectionSuspended()");
			waitingForReconnect = true;
		}

		@Override
		public void onConnected(Bundle connectionHint) {
			Log.d(TAG, "!!!!!!!!!!!!!!!!!!!!! onConnected()");

			if (waitingForReconnect) {
				waitingForReconnect = false;
				// reconnectChannels();
			} else {
				try {
					Cast.CastApi.launchApplication(apiClient, chromecastAppId,
							false).setResultCallback(resultCallback);

				} catch (Exception e) {
					Log.e(TAG, "Failed to launch application", e);
				}
			}
		}
	};

	private final OnConnectionFailedListener connectionFailedListener = new OnConnectionFailedListener() {

		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Log.d(TAG, "onConnectionFailed");
//			try {
				Toast.makeText(appContext,
						"ConnectionFailed. " + result.getErrorCode(),
						Toast.LENGTH_SHORT).show();
//				result.startResolutionForResult(activity, 23);
//			} catch (SendIntentException e) {
//				e.printStackTrace();
//				Toast.makeText(appContext, e.getMessage(), Toast.LENGTH_SHORT)
//						.show();
//			}
		}
	};

	private void teardown() {
		Log.d(TAG, "teardown");
		if (apiClient != null) {
			if (applicationStarted) {
				if (apiClient.isConnected()) {
					try {
						Cast.CastApi.stopApplication(apiClient, sessionId);
						if (activeChannel != null) {
							Cast.CastApi.removeMessageReceivedCallbacks(
									apiClient, activeChannel.getNamespace());
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
