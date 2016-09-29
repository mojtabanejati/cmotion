package es.uni_freiburg.de.cmotion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import static es.uni_freiburg.de.cmotion.R.id.recyclerview;


public class CMotionActivity extends Activity {

    private Handler mHandler;
    NsdHelper mNsdHelper;
    private final int CMotion_PORT = 5050;




///   initiating the adapter and recycler view

    private RecyclerView mRecyclerView;
    private Adapter_RecyclerView mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    public static final String TAG = "CMotion";

    FloatingActionButton fButton;
    boolean isPlay = true;



    private static final String PAUSE_ACTIVITY = "/pause_activity";
    private static final String RESUME_ACTIVITY = "/resume_activity";




    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */



    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


        mNsdHelper = new NsdHelper(this);
        mNsdHelper.initializeNsd();
        mNsdHelper.registerService(CMotion_PORT);


        Intent serviceIntent = new Intent(this, NsdServer.class);
        startService(serviceIntent);


        setContentView(R.layout.activity_cmotion);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mRecyclerView = (RecyclerView) findViewById(recyclerview);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter

        mAdapter = new Adapter_RecyclerView();
        mRecyclerView.setAdapter(mAdapter);


        fButton = (FloatingActionButton) findViewById(R.id.fab) ;



        fButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {



                if (isPlay) {
                    fButton.setImageResource(R.drawable.play);
                    onPause();

                    WearService.getInstance(). sendMessage(PAUSE_ACTIVITY, "");

                    Toast.makeText(getApplicationContext(), "Devices are Paused ", Toast.LENGTH_LONG).show();
                    isPlay = false;
                }


                   else  {
                    fButton.setImageResource(R.drawable.stop);
                    onResume();

                    WearService.getInstance().sendMessage(RESUME_ACTIVITY,"");

                    Toast.makeText(getApplicationContext(), "Devices are Restarted ", Toast.LENGTH_LONG).show();
                    isPlay = true;


                    }
                }

        });
        fButton.setOnLongClickListener(new View.OnLongClickListener() {

                public boolean onLongClick(View v) {
                    fButton.setImageResource(R.drawable.setting);
                    Toast.makeText(getApplicationContext(), "Setting is pushed ", Toast.LENGTH_LONG).show();
                    return true;
                                          }
                                      });



        mHandler = new Handler();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {

            mAdapter.notifyDataSetChanged();

            mHandler.postDelayed(mStatusChecker, 50);   // 2 milliseconds
        }
    };


    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_cmotion, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        startService(new Intent(this, LocalSensorService.class));
        startService(new Intent(this, WearService.class));

        LocalSensorService.getInstance();
        startRepeatingTask();


        if (mNsdHelper != null) {
            mNsdHelper.discoverServices();
            mNsdHelper.initializeNsd();
            mNsdHelper.registerService(CMotion_PORT);

        }
        super.onResume();
    }


    @Override
    protected void onPause() {
        stopService(new Intent(this, LocalSensorService.class));
        stopService(new Intent(this, WearService.class));


        if (mNsdHelper != null) {
            mNsdHelper.tearDown();
        }

        LocalSensorService.getInstance();
        stopRepeatingTask();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        mNsdHelper.stopDiscovery();
        super.onDestroy();
    }



}



