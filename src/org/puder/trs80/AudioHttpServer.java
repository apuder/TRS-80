package org.puder.trs80;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import android.util.Log;

/**
 * Used to serve the audio under an HTTP URL so it can be e.g. used by the
 * Google Cast Receiver app.
 */
public class AudioHttpServer implements Container {
    private static final String    TAG        = "AudioHttpServer";
    private static final int       PORT       = 4242;
    private static final String    AUDIO_PATH = "/audio";

    private static AudioHttpServer instance;

    private Connection             connection;
    private OutputStream           outputStream;


    public static AudioHttpServer get() {
        if (instance == null) {
            instance = new AudioHttpServer();
        }
        return instance;
    }

    public void start() {
        try {
            Server server = new ContainerServer(this);
            connection = new SocketConnection(server);
            SocketAddress address = new InetSocketAddress(PORT);
            connection.connect(address);

        } catch (IOException ex) {
            Log.e(TAG, "Could not start audio server: " + ex.getMessage());
        }

    }

    public void stop() {
        try {
            outputStream = null;
            connection.close();
        } catch (IOException ex) {
            Log.e(TAG, "Could not stop audio server: " + ex.getMessage());
        }
    }

    @Override
    public void handle(Request request, Response response) {
        String path = request.getPath().toString();
        Log.d(TAG, "Incoming request for path: " + path);
        try {

            if (!AUDIO_PATH.equals(path)) {
                Log.d(TAG, "Not serving " + path);
                response.setCode(404);
                response.close();
                return;
            }

            Log.d(TAG, "Serving audio!");
            response.setCode(200);
            response.setContentType("audio/wav");

            response.commit();
            outputStream = response.getOutputStream();
        } catch (IOException ex) {
            Log.e(TAG, "Error handling request", ex);
        }
    }

    public void write(byte[] audioBuffer, int offset, int bufferSize) {
        if (outputStream != null) {
            try {
                outputStream.write(audioBuffer, offset, bufferSize);
            } catch (IOException ex) {
                Log.d(TAG, "Could not serve audio data. " + ex.getMessage());
            }
        }
    }
}
