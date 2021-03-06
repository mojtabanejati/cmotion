package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CMotionWearActivity extends Activity  implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, SensorEventListener{



    private static final String MESSAGE_API_PATH = "ROTATION_VECTOR_MESSAGE";
    private static final String TAG = CMotionWearActivity.class.getName();
    private ProgressBar mProgressBar;
    private TextView mLargeText;

    private GoogleApiClient mApiClient;
    private SensorManager mSensorManager;
    private String mTargetNode = null;
    private Handler mHandler;
    private TextView mMediumText;
    private long mCounter = 0;
    private long mStarttime;


    public static final String BROADCAST = "CMOTION.android.action.broadcast";


    private static final String WEAR_MESSAGE_PATH = "/message";
    private ArrayAdapter<String> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cmotion_wear);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mLargeText = (TextView) findViewById(R.id.largetext);
        mMediumText = (TextView) findViewById(R.id.medtext);
        mStarttime = System.currentTimeMillis();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
 /*
         * initialize a wearable connection
         */
        mApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mApiClient.connect();

          /*
         * register a sensor listener for the local sensors
         */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME, 0);


        IntentFilter filter = new IntentFilter(BROADCAST);
        registerReceiver(myReceiver,filter);

    }

        private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BROADCAST)) {

                    if (intent.hasExtra("on pause")) {
                        mApiClient.disconnect();
                        finish();
                    }


                    }  if (intent.hasExtra("on play")) {
                            if (!mApiClient.isConnected()) {
                            mApiClient.connect();
                                finish();
                }
                }
            }
        };

    @Override
    protected void onResume() {

        /*
         * register a sensor listener for the local sensors
         */
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_GAME, 0);
       /*
        *
        * register BroadcastReceiver
        */

        IntentFilter filter = new IntentFilter(BROADCAST);
        registerReceiver(myReceiver,filter);



        mHandler = new Handler();
        super.onResume();
    }


    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(this);
        mApiClient.disconnect();


        /*
        unregister BroadcastReceiver
         */
        unregisterReceiver(myReceiver);

        mMediumText.setText("no target");
        mLargeText.setText("");
        mProgressBar.setVisibility(View.GONE);

        super.onPause();
    }

    @Override
    public void onConnected(Bundle bundle) {


        Wearable.NodeApi.getConnectedNodes(mApiClient).setResultCallback(
                new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult result) {
                        for (Node n : result.getNodes()) {
                            if (n.getDisplayName().contains("Nexus"))
                                mTargetNode = n.getId();  // XXX omg, wtf, use the capabilities API
                            System.out.println("message node " + mTargetNode + " name: " + n.getDisplayName());
                        }
                    }
                });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "connection suspended " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "connection to GoogleApi failed " + connectionResult);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!mApiClient.isConnected()) {
            mMediumText.setText("not connected");
            mLargeText.setText("");
            mProgressBar.setVisibility(View.GONE);
            return;
        }

        if (mTargetNode == null) {
            mMediumText.setText("no target");
            mLargeText.setText("");
            mProgressBar.setVisibility(View.GONE);
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        mMediumText.setText("");

        if (mCounter > 1000)
            mLargeText.setText(mCounter / 1000 + "k");
        else if (mCounter > 1000 * 1000)
            mLargeText.setText(mCounter / (1000 * 1000) + "m");
        else if (mCounter > 1000 * 1000 * 1000)
            mLargeText.setText(mCounter / (1000 * 1000 * 1000) + "t");
        else
            mLargeText.setText("" + mCounter);

        float[] rot = new float[4];
        SensorManager.getQuaternionFromVector(rot, sensorEvent.values);
        Wearable.MessageApi.sendMessage(mApiClient, mTargetNode, MESSAGE_API_PATH,
                ByteBuffer.allocate(4 * 5).order(ByteOrder.LITTLE_ENDIAN)
                        .putInt((int) (System.currentTimeMillis() - mStarttime))
                        .putFloat(rot[0]) // q
                        .putFloat(rot[1]) // x
                        .putFloat(rot[2]) // y
                        .putFloat(rot[3]) // z
                        .array());

        mCounter++;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}