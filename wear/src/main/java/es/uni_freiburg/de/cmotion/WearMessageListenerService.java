package es.uni_freiburg.de.cmotion;

import android.content.*;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by moji on 4/15/2016.
 */
public class WearMessageListenerService extends WearableListenerService {
    private static final String START_ACTIVITY = "/start_activity";
    private static final String PAUSE_ACTIVITY = "/pause_activity";
    private static final String RESUME_ACTIVITY = "/resume_activity";
    public static final String BROADCAST = "CMOTION.android.action.broadcast";



    @Override
    public void onMessageReceived(MessageEvent messageEvent) {



            if (messageEvent.getPath().equalsIgnoreCase(START_ACTIVITY)) {
                Intent intent = new Intent(this, CMotionWearActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
             if (messageEvent.getPath().equalsIgnoreCase(PAUSE_ACTIVITY)) {
                Intent intent = new Intent();
                intent.setAction(BROADCAST);
                intent.putExtra("on pause", 100);
                sendBroadcast(intent);
            }
             if (messageEvent.getPath().equalsIgnoreCase(RESUME_ACTIVITY)) {
                Intent intent = new Intent();
                intent.setAction(BROADCAST);
                intent.putExtra("on play", 100);
                sendBroadcast(intent);
            }
            else{
                super.onMessageReceived(messageEvent);
            }

        }

}

