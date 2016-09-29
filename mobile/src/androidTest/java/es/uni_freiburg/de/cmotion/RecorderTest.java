package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

/**
 * Created by moji 3/9/2016
 */


@RunWith(AndroidJUnit4.class)
@MediumTest
public class RecorderTest {


    private Context c;
    private Intent i;
    private int RealNumberOfPackets ;
    private int NumberOfPackets = 200 ;


                                                            ///in 5 seconds 220 packets are transferred to the wear service

    @Before
    public  void setup(){

        c = InstrumentationRegistry.getTargetContext();
        i = new Intent(c,WearService.class);
        c.startService(i);


        while (WearService.getInstance() == null){
            try {
                Thread.sleep(5000);            //five  second.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (WearService.getInstance()!= null)
                break;
        }
    }

    @After public void teardown() {
        c.stopService(i);
    }

   @Test
    public void start() throws TimeoutException {

       while (WearService.getInstance() == null) ;


       RealNumberOfPackets = WearService.getInstance().getDevicesPackets();

       i.putExtra("packets", RealNumberOfPackets);

   }


    @Test
    public void recording() {  ////or throws InterruptedException??Your method waits for a value from the network to finish the computation and return a result.
     // If the blocking network call throws an InterruptedException your method can not finish computation in a normal way. You let the InterruptedException propagate

            RealNumberOfPackets = WearService.getInstance().getDevicesPackets();

            Assert.assertTrue("number of packets are increasing" + RealNumberOfPackets, WearService.getInstance() != null);

            Assert.assertTrue("The service is started", RealNumberOfPackets > NumberOfPackets + (1/10)*NumberOfPackets);

            Log.d("RealNumber Of Packets", String.valueOf(RealNumberOfPackets));



    }


}
