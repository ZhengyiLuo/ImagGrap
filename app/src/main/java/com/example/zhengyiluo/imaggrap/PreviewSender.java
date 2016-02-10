package com.example.zhengyiluo.imaggrap;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by ZhengyiLuo on 1/1/16.
 */
public class PreviewSender implements Runnable {

    public String host = "192.168.129.212";
    public boolean sending = false;
    public boolean isUpdated = false;
    public boolean isPacking = false;
    MainActivity mActivity;
    int port;
    int len;
    DatagramSocket socket;
    InetAddress address;
    byte[] data;
    byte[] buffer;
    Thread thread;
    Queue<byte[]> framebuffer;
    Camera.Size previewSize;
    int mCameraOrientation;
    private byte[] byteArray;

    public PreviewSender(MainActivity mActivity, String host, int port) {
        this.mActivity = mActivity;
//        this.host = host;
        this.port = port;
        framebuffer = new ArrayDeque<>();


    }

    public void connect() {
        try {

            socket = new DatagramSocket();
            address = InetAddress.getByName(host);
        } catch (Exception e) {

            Log.d(MainActivity.TAG_IMAG, e.toString());
        }
    }

    public void stop() {
        this.sending = true;
    }

    public void send() {
        this.sending = true;
    }

    public int[] decodeYUV420SP(byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;
        int rgb[] = new int[width * height];
        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);
                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;
                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000)
                        | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    public void updatedata(Camera mCamera, byte[] data, int mCameraOrientation) {
        this.previewSize = mCamera.getParameters().getPreviewSize();
        this.data = data;
        this.mCameraOrientation = mCameraOrientation;
        isUpdated = true;
    }

    public void imageprocess() {
        int[] rgb = decodeYUV420SP(data, previewSize.width, previewSize.height);
        Bitmap bmp = Bitmap.createBitmap(rgb, previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888);
        int smallWidth, smallHeight;
        int dimension = 200;
        // stream is lagging, cut resolution and catch up

        Matrix matrix = new Matrix();
        matrix.postRotate(mCameraOrientation);

        //     Bitmap bmpSmall = Bitmap.createScaledBitmap(bmp, dimension, dimension, false);

        Bitmap bmpSmallRotated = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            Log.d(TAG_IMAG, "bmp" + bmpSmallRotated.toString());
//            bmpSmallRotated.compress(Bitmap.CompressFormat.WEBP, 30, baos);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmpSmallRotated.compress(Bitmap.CompressFormat.WEBP, 5, stream);
        byte[] streamarray = stream.toByteArray();
        byteArray = new byte[streamarray.length];
        if (streamarray != null) {
            System.arraycopy(streamarray, 0, byteArray, 0, streamarray.length);
            framebuffer.add(byteArray);
            Log.d(MainActivity.TAG_IMAG, "added" + streamarray.length + "  :" + byteArray.length + " : " + framebuffer.size());
        }

//        byteArray = stream.toByteArray();

        bmp.recycle();
        //   bmpSmall.recycle();
//            bmpSmallRotated.recycle();


    }

    /**
     * Starts executing the active part of the class' code. This method is
     * called when a thread is started that has been created with a class which
     * implements {@code Runnable}.
     */
    @Override
    public void run() {
        connect();

        while (sending) {
            //Log.d(MainActivity.TAG_IMAG, "sending1");
            if (isUpdated) {
                imageprocess();

                //Log.d(MainActivity.TAG_IMAG, "sending2");
                buffer = framebuffer.poll();
                if (buffer != null && buffer.length > 100) {
                    //  Log.d(MainActivity.TAG_IMAG, "sending3");
                    int msg_lenght = buffer.length;
                    DatagramPacket p = new DatagramPacket(buffer, msg_lenght, address, port);
                    try {
                        socket.send(p);
                        Log.d(MainActivity.TAG_IMAG, "sent");
                        // Log.d(MainActivity.TAG_IMAG, "sending5");
                    } catch (Exception e) {
                        Log.d(MainActivity.TAG_IMAG, e.toString());

                    }
//            if (host != null) {
//                Log.d(mActivity.TAG_IMAG, "This is data" + mActivity.sent[99]);
//                Log.d(mActivity.TAG_IMAG, "This is data's length" + mActivity.sent.length);
//                send(mActivity.sent);
//            }
                    try {
                       // Thread.sleep(100);
                    } catch (Exception e) {

                    }

                }
                isUpdated = false;
            }
        }
    }
}
