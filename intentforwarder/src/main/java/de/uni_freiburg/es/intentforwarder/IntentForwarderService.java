package de.uni_freiburg.es.intentforwarder;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

/** Activated through the GlassForwarder Receiver, which either receives recordingstarts commands
 * and forwards them to all connected Glass devices. Or it is activated when a new Glass Device
 * is bonded with.
 *
 * XXX forwarded action is hard-coded
 * 
 * Created by phil on 4/29/16.
 */
public class IntentForwarderService extends Service {

    protected static final String TAG = IntentForwarderService.class.getName();
    protected static final UUID uuid = UUID.fromString("5a28e1e2-5e00-49eb-9854-2a2f9d8c5dec");
    protected static final String NAME = TAG;
    protected ServerThread mServerThread = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override /** we can called either if there is a Bluetooth devices that has been bound, in
     which case we start the serversocket. And we get called when a startrecord intent has been
     received, which we then forward to all bound devices which have our uuid. */
    public int onStartCommand(Intent intent, int flags, int startId) {
        /** make sure that we can receive forwarded messages on the Bluetooth connection */
        if (mServerThread == null) {
            mServerThread = new ServerThread();
        } else {
            mServerThread.interrupt();
        }

        if (intent == null || (intent.getAction() != null &&
            BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())))
            return super.onStartCommand(intent, flags, startId);

        if (intent.getAction() != null) {
            /** got a broadcast action, let's forward to all bound nodes */
            BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
            JSONObject extras = ForwardedUtils.toJson(intent);

            if (extras != null && a.isEnabled())
                for (BluetoothDevice d : a.getBondedDevices())
                    new SenderThread(d, extras);
        }

        Log.d(TAG, "service running with " + mServerThread);
        return super.onStartCommand(intent, flags, startId);
    }

    protected boolean hasUUID(BluetoothDevice d, UUID uuid) {
        boolean yay = false;
        for (ParcelUuid u : d.getUuids())
            yay = yay || u.getUuid().equals(uuid);
        return yay;
    }

    @Override
    public void onDestroy() {
        if (mServerThread != null)
            mServerThread.running = false;
        super.onDestroy();
    }

    protected class ServerThread extends Thread {
        protected BluetoothServerSocket mServerSocket;
        protected boolean running = true;

        public ServerThread()  {
            start();
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Log.d(TAG, "serverthread started");

                    BluetoothAdapter a = BluetoothAdapter.getDefaultAdapter();
                    if (!a.isEnabled()) break;

                    Log.d(TAG, "starting to listen for Bluetooth connections");

                    mServerSocket = a.listenUsingRfcommWithServiceRecord(NAME, uuid);
                    BluetoothSocket s = mServerSocket.accept();
                    mServerSocket.close();
                    mServerSocket = null;

                    Log.d(TAG, "accepted connection");

                    BufferedInputStream is = new BufferedInputStream(s.getInputStream());
                    byte[] fuckingjava = new byte[4];
                    is.read(fuckingjava);
                    byte[] msg = new byte[ByteBuffer.wrap(fuckingjava).asIntBuffer().get()];
                    is.read(msg);
                    s.close();

                    Intent jmsg = ForwardedUtils.fromJson(msg);
                    jmsg.putExtra(IntentForwarder.EXTRA_DOBLUETOOTHFORWARD, false);
                    sendBroadcast(jmsg);

                    Log.d(TAG, "forwarded intent " + jmsg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "serverthread killed");
            /** this is part of handling the case when bluetooth is not enabled */
            mServerThread = null;
        }
    }

    private class SenderThread extends Thread {
        protected final JSONObject mExtras;
        protected final BluetoothDevice mDevice;

        public SenderThread(BluetoothDevice d, JSONObject extras) {
            mDevice = d;
            mExtras = extras;
            start();
        }

        @Override
        public void run() {
            BluetoothSocket s = null;
            try {
                ByteBuffer buf = ByteBuffer.allocate(mExtras.toString().getBytes().length + 4);
                s = mDevice.createRfcommSocketToServiceRecord(uuid);
                s.connect();
                buf.putInt(mExtras.toString().length());
                buf.put(mExtras.toString().getBytes());
                s.getOutputStream().write(buf.array());
                s.getOutputStream().flush();
                s.getInputStream().read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try { // cleanup
                if (s!=null)
                    s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
