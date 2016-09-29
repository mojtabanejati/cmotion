package es.uni_freiburg.de.cmotion;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This service picks up messages from the Wear network, augments them with an ID and timestamp
 * and hands them over to the UDPTransport.
 *
 * Created by phil on 1/5/16.
 */
public class WearService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final String TAG = WearService.class.getName();
    private GoogleApiClient mApiClient;
    private static final String MESSAGE_API_PATH = "ROTATION_VECTOR_MESSAGE";
    private ArrayList<String> mWearableIdentifcations = new ArrayList<String>();
    private long mStarttime;
    static  WearService swInstance;
    private String mTargetNode ;
    int c;
    LinkedList<Long> events = new LinkedList<Long>();
    public int ratio;


    private static final String START_ACTIVITY = "/start_activity";

    public static WearService getInstance(){
        return swInstance;
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        /*
         * initialize a wearable connection
         */




        getInstance();
        swInstance = this;

        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();

        mStarttime = System.currentTimeMillis();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mApiClient.disconnect();
        super.onDestroy();
    }



    public boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("es.uni_freiburg.de.cmotion.WearService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



    @Override
    public void onConnected(@Nullable Bundle bundle) {


        Log.d(TAG, "connected to GoogleApi");
        Wearable.MessageApi.addListener(mApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                String p = messageEvent.getPath();

                if (!p.equalsIgnoreCase(MESSAGE_API_PATH))
                    return;

                String id = messageEvent.getSourceNodeId();
                if (!mWearableIdentifcations.contains(id))
                    mWearableIdentifcations.add(id);
                c++;

                /*
                 * wrap the quaternion rx'ed from the wearable in an id and a timestamp
                 */
                UDPTransport.getInstance().send(
                        ByteBuffer.allocate(6 * 8).order(ByteOrder.LITTLE_ENDIAN)
                                .putInt(mWearableIdentifcations.indexOf(id) + 1)
                                .putInt((int) (System.currentTimeMillis() - mStarttime))
                                .put(messageEvent.getData())
                                .array());

                long current = System.currentTimeMillis();
                events.add(current);

                while (current - events.getFirst() > 1000)
                    events.removeFirst();

                ratio = events.size();


            }
        });

        sendMessage(START_ACTIVITY, "");

    }


    public int getDevicesPackets(){
        return c;
    }
    public int getDeviceRatio(){ return ratio;}



   public ArrayList<String> getConnectedDevices(){
        return mWearableIdentifcations;
    }



    ///all the wearable connected devices////
    public String DeviceConnected(){
        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback(

                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult result) {
                        if (result.getNodes().size() > 0)
                            mTargetNode = result.getNodes().get(0).getId();
                        if (result.getNodes().size() == 0) {

                            Toast.makeText(getApplicationContext(), "No devices are connected ", Toast.LENGTH_LONG).show();
                        }
                    }
                });
        return mTargetNode;
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended");
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "connection GoogleApi failed: " + connectionResult);
    }


    public void sendMessage( final String path, final String text ) {

        if (mApiClient.isConnected()) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                                mApiClient, node.getId(), path, text.getBytes()).await();
                        if(!result.getStatus().isSuccess()){
                            Log.e("test", "error");
                        } else {
                            Log.e("test", "success!! sent to: " + node.getDisplayName());
                        }
                    }
                }
            }).start();
        } else {
            Log.d("test", "not connected");

        }
    }



}