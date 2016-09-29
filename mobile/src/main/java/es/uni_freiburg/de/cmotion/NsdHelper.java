package es.uni_freiburg.de.cmotion;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.net.InetAddress;

/**
 * Created by moji on 6/23/2016.
 */



/*
        *                          Application start
        *                                 |
        *                                 |
        *                                 |                  onServiceRegistered()
        *                     Register any local services  /
        *                      to be advertised with       \
        *                       registerService()            onRegistrationFailed()
        *                                 |
        *                                 |
        *                          discoverServices()
        *                                 |
        *                      Maintain a list to track
        *                        discovered services
        *                                 |
        *                                 |--------->
        *                                 |          |
        *                                 |      onServiceFound()
        *                                 |          |
        *                                 |     add service to list
        *                                 |          |
        *                                 |<----------
        *                                 |
        *                                 |--------->
        *                                 |          |
        *                                 |      onServiceLost()
        *                                 |          |
        *                                 |   remove service from list
        *                                 |          |
        *                                 |<----------
        *                                 |
        *                                 |
        *                                 | Connect to a service
        *                                 | from list ?
        *                                 |
        *                          resolveService()
        *                                 |
        *                         onServiceResolved()
        *                                 |
        *                     Establish connection to service
        *                     with the host and port information
        *
        * </pre>
        * An application that needs to advertise itself over a network for other applications to
        * discover it can do so with a call to {@link #registerService}. If Example is a http based
        * application that can provide HTML data to peer services, it can register a name "Example"
        * with service type "_http._tcp". A successful registration is notified with a callback to
        * {@link RegistrationListener#onServiceRegistered} and a failure to register is notified
        * over {@link RegistrationListener#onRegistrationFailed}
        *
     */

/**
 *
 * this class advertises the application on the network and register the application
 *
 */

public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;
    static NsdHelper NsdHInstance;

    public static final String SERVICE_TYPE = "_cmotion._udp.";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "CMotion";

    NsdServiceInfo mService;

    public static NsdHelper getInstance(){
        return NsdHInstance;
    }

    public NsdHelper(Context context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeResolveListener();
        initializeDiscoveryListener();
        initializeRegistrationListener();

        getInstance();
        NsdHInstance= this;

            }

    public void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success" + service);
                Log.d(TAG, "Host: " + service.getHost());
                Log.d(TAG, "port = " + String.valueOf(service.getPort()));
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    // Service type is the string containing the protocol and
                    // transport layer for this service.
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same machine: " + mServiceName);
                } else if (service.getServiceName().contains("CMotion")) {
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                //mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    public void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }
                mService = serviceInfo;
                int port = mService.getPort();
                InetAddress host = mService.getHost();
            }
        };
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                mServiceName = NsdServiceInfo.getServiceName();
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo arg0, int arg1) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }

        };
    }

    public void registerService(int port) {


        initializeRegistrationListener();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);

        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);

    }

    public void discoverServices() {
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            } finally {
            }
            mDiscoveryListener = null;
        }
    }


    public void tearDown() {

        if (mRegistrationListener != null) {
            try {
                mNsdManager.unregisterService(mRegistrationListener);
            } finally {
            }
            mRegistrationListener = null;
        }
    }
}

