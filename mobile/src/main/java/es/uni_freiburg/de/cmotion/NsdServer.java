package es.uni_freiburg.de.cmotion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;


/**
 * Created by moji on 7/1/2016.
 *
 * this class broadcast the information form the already registered application on the network to any device with correct port number and IP address
 *
 */
public class NsdServer extends Service {

    public static final String TAG = "NsdServer";

    int serverPort = 5050;



    @Override
    public void onCreate() {
        super.onCreate();
        Log.v("service", "listeningService started");
        new serviceSocketThread().start();


    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v("service", "listeningService binded");
        return null;
    }

    private class serviceSocketThread extends Thread {

        private BlockingDeque<byte[]> q;
        private boolean mIsSending = true;
        private InetAddress mAdress;


        public serviceSocketThread() {
            try {
                q = new LinkedBlockingDeque<>(1024);
                mAdress = InetAddress.getByName("255.255.255.255");         /// broadcasting
                serverPort = 5050;
                new Thread(mUDP).start();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }

        private Runnable mUDP = new Runnable() {
            public DatagramPacket packet;
            public DatagramSocket socket;
        @Override
        public void run() {

                        while (mIsSending) {
                            try {
                                socket = new DatagramSocket(serverPort);
                                byte[] receiveData = new byte[1024];
                                // waiting for the incoming client's message packet
                                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                                socket.receive(receivePacket);


                                packet = new DatagramPacket(new byte[]{}, 0, mAdress, serverPort);
                                byte[] buf = q.takeLast();
                                packet.setData(buf);
                                packet.setLength(buf.length);
                                socket.send(packet);

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (SocketException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
            }


        }


