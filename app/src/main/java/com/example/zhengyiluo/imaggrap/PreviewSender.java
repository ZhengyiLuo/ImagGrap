package com.example.zhengyiluo.imaggrap;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by ZhengyiLuo on 1/1/16.
 */
public class PreviewSender implements Runnable {

    MainActivity mActivity;
    public boolean sending = true;
    public String host = "192.168.49.226";
    int port;
    int len;
    DatagramSocket socket;
    OutputStream outputStream;
    InetAddress address;

    public PreviewSender(MainActivity mActivity, String host, int port) {
        this.mActivity = mActivity;
//        this.host = host;
        this.port = port;


    }


    public int send(byte[] data) {
        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */

            outputStream.write(data);
//            while ((len = inputStream.read(data)) != -1) {
//                outputStream.write(data, 0, len);
//            }
            outputStream.flush();
            return 1;
        } catch (FileNotFoundException e) {
            return -1;
        } catch (IOException e) {
            //catch logic
            return -2;
        }


    }


    public void startsend() {
        try {

            socket = new DatagramSocket();
            address = InetAddress.getByName(host);
        } catch (Exception e) {

            Log.d(mActivity.TAG_IMAG, e.toString());
        }
        this.sending = true;
    }

    public void stop() {
        this.sending = false;
        if (socket != null) {

            socket.close();

        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * Starts executing the active part of the class' code. This method is
     * called when a thread is started that has been created with a class which
     * implements {@code Runnable}.
     */
    @Override
    public void run() {
        Log.d(mActivity.TAG_IMAG, "Running");
        while (sending) {

            startsend();
            int msg_lenght = mActivity.sent.length;
            DatagramPacket p = new DatagramPacket(mActivity.sent, msg_lenght, address, port);
            try {
                //  Log.d(mActivity.TAG_IMAG, "We are sending it");
                socket.send(p);
            } catch (Exception e) {
                Log.d(mActivity.TAG_IMAG, e.toString());

            }

//            if (host != null) {
//                Log.d(mActivity.TAG_IMAG, "This is data" + mActivity.sent[99]);
//                Log.d(mActivity.TAG_IMAG, "This is data's length" + mActivity.sent.length);
//                send(mActivity.sent);
//            }
            try {
                Thread.sleep(60);
            } catch (Exception e) {

            }

        }


    }
}
