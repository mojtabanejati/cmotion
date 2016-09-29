package es.uni_freiburg.de.cmotion;

/**
 * Created by moji on 2/11/2016.
 */

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;

import static java.lang.String.valueOf;

public class Adapter_RecyclerView extends RecyclerView.Adapter<Adapter_RecyclerView.MyViewholder> {

    ArrayList<String> count;
    private int pStatus = 0;



    @Override
    public MyViewholder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cmotion_child, viewGroup, false);
        MyViewholder viewHolder = new MyViewholder(v);

        return viewHolder;

    }


    @Override
    public void onBindViewHolder(final MyViewholder viewHolder, int position) {


        if (position == 0) {

            pStatus = LocalSensorService.getInstance().getProgressBarRate();
            viewHolder.conTextView.setText("Local Device ID");
            viewHolder.packetTextview.setText(valueOf(LocalSensorService.getInstance().getNumberOfSentMessages()));
            viewHolder.progressText.setText(valueOf(LocalSensorService.getInstance().getProgressBarRate()));
            viewHolder.mProgressBar.setProgress(pStatus);

            if (pStatus> viewHolder.mProgressBar.getMax())
                viewHolder.mProgressBar.setMax(pStatus);
                viewHolder.mProgressBar.setProgress(pStatus);



        } else {
            viewHolder.textView.setText("Wearable device ID:");
            pStatus = WearService.getInstance().getDeviceRatio();
            viewHolder.conTextView.setText(WearService.getInstance().DeviceConnected());
            viewHolder.packetTextview.setText(valueOf(WearService.getInstance().getDevicesPackets()));
            viewHolder.progressText.setText(valueOf(WearService.getInstance().getDeviceRatio()));
              viewHolder.mProgressBar.setProgress(pStatus);

            if (pStatus> viewHolder.mProgressBar.getMax())
                viewHolder.mProgressBar.setMax(pStatus);
                viewHolder.mProgressBar.setProgress(pStatus);


        }

    }



    public class MyViewholder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {


        TextView textView;
        TextView conTextView;
        TextView packetTextview;
        ImageButton IButton;

        Button Pause;
        ProgressBar mProgressBar;
        TextView progressText;




        MyViewholder(View v) {
            super(v);
            v.setOnCreateContextMenuListener(this);
            textView = (TextView)v.findViewById(R.id.textView);
            conTextView = (TextView)v.findViewById(R.id.displayConnected2);
            packetTextview = (TextView)v.findViewById(R.id.packet);

            mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);
            progressText = (TextView)v.findViewById(R.id.textProgress);
            mProgressBar.setVisibility(View.VISIBLE);

            mProgressBar.setMax(0);


        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.setHeaderTitle("CMotion");
        }

    }

    @Override
    public int getItemCount() {

        count = WearService.getInstance().getConnectedDevices();
        return count.size() + 1;

    }

}



