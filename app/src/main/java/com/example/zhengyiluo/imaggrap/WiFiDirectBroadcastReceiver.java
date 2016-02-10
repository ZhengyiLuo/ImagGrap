package com.example.zhengyiluo.imaggrap;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by ZhengyiLuo on 1/1/16.
 */
public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String DEVICE_NAME = "Zen";
    public boolean isConnected = false;
    public String host = "192.168.49.226";
    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mActivity;
    private WifiP2pManager.PeerListListener myPeerListListener;
    private ArrayList<WifiP2pDevice> deviceList;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        this.mManager = manager;
        this.mChannel = channel;
        this.mActivity = activity;
        this.deviceList = new ArrayList<>();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi P2P is enabled

                Log.d(MainActivity.TAG_IMAG, "WifiP2P Enabled");
            } else {
                Log.d(MainActivity.TAG_IMAG, "WifiP2P not Enabled");
                AlertDialog.Builder dialog = new AlertDialog.Builder(
                        mActivity)
                        .setMessage("Wifi Diret Connection is not Enalbed")
                        .setPositiveButton("Return",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        mActivity.finish();
                                    }
                                })
                        .setNegativeButton("Try again",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                    }
                                });
                AlertDialog alertDialog = dialog.create();

                alertDialog.show();


            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            Log.d(MainActivity.TAG_IMAG, "P2P peers changed");
//
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListListener.onPeersAvailable()

            mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {
                @Override
                public void onPeersAvailable(WifiP2pDeviceList peers) {
                    Log.d(MainActivity.TAG_IMAG, String.format("PeerListListener: %d peers available, updating device list", peers.getDeviceList().size()));
                    deviceList.clear();
                    deviceList.addAll(peers.getDeviceList());
                    if (!isConnected) {
                        connect();
                    }

                }
            });

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (mManager == null) {
                return;
            }

            NetworkInfo networkInfo = intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                Log.d(MainActivity.TAG_IMAG, "is connected");
                isConnected = true;
                //can deal with groups here using ConnectionInfoListener

                mManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                        // InetAddress from WifiP2pInfo struct.
//                        host = info.groupOwnerAddress.getHostAddress();
//                        Log.d(MainActivity.TAG_IMAG, host);
//                        mActivity.sender.setHost(host);

                        // After the group negotiation, we can determine the group owner.
                        if (info.groupFormed && info.isGroupOwner) {
                            // Do whatever tasks are specific to the group owner.
                            // One common case is creating a server thread and accepting
                            // incoming connections.
                        } else if (info.groupFormed) {
                            // The other device acts as the client. In this case,
                            // you'll want to create a client thread that connects to the group
                            // owner.
                        }
                    }

                });


            } else {
                Log.d(MainActivity.TAG_IMAG, "is not connected");
                isConnected = false;
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
        }
    }

    public void connect() {
        // Picking the first device found on the network.

        WifiP2pConfig config = new WifiP2pConfig();

        for (Iterator iterator = deviceList.iterator(); iterator.hasNext(); ) {
            WifiP2pDevice device = (WifiP2pDevice) iterator.next();
            Log.d(MainActivity.TAG_IMAG, device.deviceName);

            if (device.deviceName.equals(DEVICE_NAME)) {
                Log.d(MainActivity.TAG_IMAG, "Trying to connect to desired device");
                //obtain a peer from the WifiP2pDeviceList
                config.deviceAddress = device.deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Toast.makeText(mActivity, "Connected!!",
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(mActivity, "Connect failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }

        }

    }

    public String getIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());
        /*
         * for (NetworkInterface networkInterface : interfaces) { Log.v(TAG,
         * "interface name " + networkInterface.getName() + "mac = " +
         * getMACAddress(networkInterface.getName())); }
         */

            for (NetworkInterface intf : interfaces) {
                if (!getMACAddress(intf.getName()).equalsIgnoreCase(
                        host)) {
                    // Log.v(TAG, "ignore the interface " + intf.getName());
                    // continue;
                }
                if (!intf.getName().contains("p2p"))
                    continue;

                Log.v(MainActivity.TAG_IMAG,
                        intf.getName() + "   " + getMACAddress(intf.getName()));

                List<InetAddress> addrs = Collections.list(intf
                        .getInetAddresses());

                for (InetAddress addr : addrs) {
                    // Log.v(TAG, "inside");

                    if (!addr.isLoopbackAddress()) {
                        // Log.v(TAG, "isnt loopback");
                        String sAddr = addr.getHostAddress().toUpperCase();
                        Log.v(MainActivity.TAG_IMAG, "ip=" + sAddr);


                        if (true) {
                            if (sAddr.contains("192.168.49.")) {
                                Log.v(MainActivity.TAG_IMAG, "ip = " + sAddr);
                                return sAddr;
                            }
                        }

                    }

                }
            }

        } catch (Exception ex) {
            Log.v(MainActivity.TAG_IMAG, "error in parsing");
        } // for now eat exceptions
        Log.v(MainActivity.TAG_IMAG, "returning empty ip address");
        return "";
    }

    public String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections
                    .list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName))
                        continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac == null)
                    return "";
                StringBuilder buf = new StringBuilder();
                for (int idx = 0; idx < mac.length; idx++)
                    buf.append(String.format("%02X:", mac[idx]));
                if (buf.length() > 0)
                    buf.deleteCharAt(buf.length() - 1);
                return buf.toString();
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
        /*
         * try { // this is so Linux hack return
         * loadFileAsString("/sys/class/net/" +interfaceName +
         * "/address").toUpperCase().trim(); } catch (IOException ex) { return
         * null; }
         */
    }


}

