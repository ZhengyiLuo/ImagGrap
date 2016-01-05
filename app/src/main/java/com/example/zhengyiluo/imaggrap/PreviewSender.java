package com.example.zhengyiluo.imaggrap;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by ZhengyiLuo on 1/1/16.
 */
public class PreviewSender implements Runnable {

    MainActivity mActivity;
    public boolean sending = true;
    public String host = "192.168.49.226";
    int port;
    int len;
    Socket socket = new Socket();
    OutputStream outputStream;

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
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, port)), 500);

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data will be retrieved by the server device.
             */
            outputStream = socket.getOutputStream();

            if (outputStream == null) {

                Log.d(mActivity.TAG_IMAG, "This thing is null...");
            }

        } catch (Exception e) {

            Log.d(mActivity.TAG_IMAG, "Exception connect to host");
        }
        this.sending = true;
    }

    public void stop() {
        this.sending = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                //catch logic
            }
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
            if (outputStream == null) {
                startsend();
                continue;
            }
            Log.d(mActivity.TAG_IMAG, "We are sending it");
            if (host != null) {
                Log.d(mActivity.TAG_IMAG, "This is data" + mActivity.sent[99]);
                Log.d(mActivity.TAG_IMAG, "This is data's length" + mActivity.sent.length);
                send(mActivity.sent);
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {

            }

        }


    }
}
