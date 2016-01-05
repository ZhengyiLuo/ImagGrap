package com.example.zhengyiluo.imaggrap;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class MainActivity extends Activity implements SensorEventListener, View.OnClickListener {

    public static final String TAG = "java_side";
//    SQuadformationJNINative squadformationJNINative;

//    CameraManager cameraManager;

    public static final String TAG_IMAG = "ImagGrap";
    /**
     * Default dimensions of camera resolution
     */
    private static final int PREFERRED_WIDTH = 640;
    private static final int PREFERRED_HEIGHT = 480;
    public int mCameraOrientation;
    public PreviewSender sender;
    public byte[] data = new byte[1024];
    public byte[] sent = new byte[2048];
    /**
     * Camera object
     */
    Camera mCamera;
    int CameraId = 0;
    /**
     * Android Sensor manager
     */
    SensorManager sensorManager;
    Sensor accelerometer;
    Sensor gyroscope;
    boolean initSensorsJava = true;
    /**
     * Top-level layout object
     */
    ViewGroup mMainViewGroup;
    /**
     * Surface for camera preview
     */
    CameraSurface mCameraSurface;
    /**
     * Image capture dimensions
     */
    int mCaptureWidth = 0, mCaptureHeight = 0;
    /**
     * Buffer for preview frames
     */
    byte[] mPreviewBuffer1 = null;
    byte[] mPreviewBuffer2 = null;
    int pixels = PREFERRED_WIDTH * PREFERRED_HEIGHT;
    IntentFilter mIntentFilter;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    WiFiDirectBroadcastReceiver mReceiver;
    Thread ImageTransferThread;
    /**
     * Callback fires when a new frame is available from the camera
     */
    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {

            //    incoming data is NV21
            //    pass to native state
            long image_timestamp = System.nanoTime();
//            SQuadformationJNINative.update(data, mCaptureWidth, mCaptureHeight, data.length, image_timestamp);
            //convert to the right format

            if (camera != null) {
                camera.addCallbackBuffer(data);
//                Log.d(TAG_IMAG, "This is data" + data[0]);
//                Log.d(TAG_IMAG, "This is data" + data[1]);
//                Log.d(TAG_IMAG, "This is data" + data[2]);
            }
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            int[] rgb = decodeYUV420SP(data, previewSize.width, previewSize.height);
            Bitmap bmp = Bitmap.createBitmap(rgb, previewSize.width, previewSize.height, Bitmap.Config.ARGB_8888);
            int smallWidth, smallHeight;
            int dimension = 200;
            // stream is lagging, cut resolution and catch up

            Matrix matrix = new Matrix();
            matrix.postRotate(mCameraOrientation);

            Bitmap bmpSmall = Bitmap.createScaledBitmap(bmp, dimension, dimension, false);
            Bitmap bmpSmallRotated = Bitmap.createBitmap(bmpSmall, 0, 0, dimension, dimension, matrix, false);
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            Log.d(TAG_IMAG, "bmp" + bmpSmallRotated.toString());
//            bmpSmallRotated.compress(Bitmap.CompressFormat.WEBP, 30, baos);


            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmpSmallRotated.compress(Bitmap.CompressFormat.WEBP, 100, stream);
            byte[] byteArray = stream.toByteArray();

            if (mReceiver.isConnected) {
                sent = byteArray;

//                Log.d(TAG_IMAG, "SENT" + sent[51]);
//                Log.d(TAG_IMAG, "SENT" + sent[35]);
//                Log.d(TAG_IMAG, "SENT" + sent[90]);

//                sent = data;
//                for (int i = 0; i < data.length; i++) {
//                    Log.d(TAG, Byte.toString(data[i]));
//                }

            }
            bmp.recycle();
            bmpSmall.recycle();
            bmpSmallRotated.recycle();


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //    configure user interface
        mMainViewGroup = (ViewGroup) this.findViewById(R.id.contentLayout);    //    actually a relative layout...
        //    install preview surface

        try {

            mCamera = Camera.open(CameraId);

        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG_IMAG, "Error: No camera available for capture!");
        }

        mCameraSurface = new CameraSurface(this, mCamera);
        this.mMainViewGroup.addView(mCameraSurface);

        //    start capture
        startCameraPreview();


        if (initSensorsJava) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

//        SQuadformationJNINative.onCreate();

        //       Create BroadCastReceiver and WifiManager Instance
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        //Create an intent filter and add the same intents that your broadcast receiver checks for:
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        //Initialize buttons
        Button Connect = (Button) findViewById(R.id.button_connect);
        Button Send = (Button) findViewById(R.id.button_send);
        Button Stop = (Button) findViewById(R.id.button_stop);

        //Add button listeners
        Connect.setOnClickListener(this);
        Send.setOnClickListener(this);
        Stop.setOnClickListener(this);

        sender = new PreviewSender(this, null, 8888);
        ImageTransferThread = new Thread(sender);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera == null) {
            try {

                mCamera = Camera.open(CameraId);


            } catch (Exception e) {
                // Camera is not available (in use or does not exist)
                Log.e(TAG_IMAG, "Error: No camera available for capture!");
            }

        }

        setCameraDisplayOrientation(this, CameraId, mCamera);
        registerReceiver(mReceiver, mIntentFilter);

        if (initSensorsJava) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        }


//        SQuadformationJNINative.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (initSensorsJava)
            sensorManager.unregisterListener(this);
//        SQuadformationJNINative.onStop();
        stopCameraPreview();
        mCamera.release();
        ImageTransferThread.destroy();
    }

    private void startCameraPreview() {
        if (mCamera != null) {
            // configure buffer for camera preview
            int pformat = mCamera.getParameters().getPreviewFormat();    //    should default to NV21
            int bufferSize = 0;

            PixelFormat info = new PixelFormat();
            PixelFormat.getPixelFormatInfo(pformat, info);

            mCaptureWidth = PREFERRED_WIDTH;
            mCaptureHeight = PREFERRED_HEIGHT;

            float bytesPerPixel = info.bitsPerPixel / 8.0f;                //    NV21 uses 12 bits per pixel
            bufferSize = (int) Math.ceil(mCaptureWidth * mCaptureHeight * bytesPerPixel);

            // buffer for preview frames
            mPreviewBuffer1 = new byte[bufferSize];
            mPreviewBuffer2 = new byte[bufferSize];


            mCamera.addCallbackBuffer(mPreviewBuffer1);
            mCamera.addCallbackBuffer(mPreviewBuffer2);

            mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);

            // select best camera settings
            selectCameraMode();

            // buffer is configured, start preview
            mCamera.startPreview();

            setFocusInfinity();

            // Initiate garbage collection to hint cleaning up previous buffers:
            System.gc();
        }
    }
        /*
     *    Camera management
     */

    /**
     * @throws if the camera is null
     *            <p/>
     *            This method attempts to select:
     *            - Auto-focus: infinity
     *            - White-balance: automatic
     *            - Dimensions: PREFERRED, or the first available size if PREFFERED is absent
     * @brief Select optimal settings for the device camera.
     */
    private void selectCameraMode() {
        assert mCamera != null;    //    always check for valid camera _first_

        Camera.Parameters params = mCamera.getParameters();

        // get supported frame rates
        List<int[]> frameRates = params.getSupportedPreviewFpsRange();

        // take the first available frame rate
        int[] rate = frameRates.get(0);

        Log.d(TAG, "Selected image size: " + mCaptureWidth + ", " + mCaptureHeight);
        Log.d(TAG, "Selected max frame rate: " + rate[1] / 1000.0f);

        params.setPreviewSize(mCaptureWidth, mCaptureHeight);
        params.setPreviewFpsRange(rate[1], rate[1]);

        //    enable automatic white balance if possible
        List<String> whiteBalances = params.getSupportedWhiteBalance();
        if (whiteBalances.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            Log.d(TAG, "Automatic white balance enabled");
        }

        //    try setting camera parameters
        //    since everything has been checked, this should not fail
        try {
            params.set("iso", "800");
            mCamera.setParameters(params);
        } catch (RuntimeException exception) {
            Log.e(TAG, "Error: Failed to set camera parameters!", exception);

            //    we will fall back to default width and height
            Camera.Size size = mCamera.getParameters().getPreviewSize();

            mCaptureWidth = size.width;
            mCaptureHeight = size.height;
        }
    }


    private void setFocusInfinity() {

        Camera.Parameters params = mCamera.getParameters();

        //    try to select a good focus mode
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes != null) {
            mCamera.cancelAutoFocus();

            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                Log.d(TAG, "Setting FOCUS_MODE_INFINITY");
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
            }
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                Log.d(TAG, "Setting FOCUS_MODE_FIXED");
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                Log.d(TAG, "Setting FOCUS_MODE_AUTO");
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }
        }

        mCamera.setParameters(params);
    }


    /**
     * @brief Disables the camera preview and corresponding callback.
     */
    private void stopCameraPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);    //    stop receiving frames

        }
    }


    public void onSensorChanged(SensorEvent event) {
        //Log.d(TAG, event.sensor.getName() + " " + event.timestamp);
        //SQuadJNINative.sensorUpdate(event.sensor.getType(), event.values, event.timestamp);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void setCameraDisplayOrientation(Activity activity,
                                            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCameraOrientation = result;
        camera.setDisplayOrientation(result);
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

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_connect:
                Toast toast = Toast.makeText(this, "We trying to connect it", Toast.LENGTH_SHORT);
                toast.show();


                if (mManager != null) {
                    mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(MainActivity.TAG_IMAG, "Success");


                        }

                        @Override
                        public void onFailure(int reasonCode) {
                            Log.d(MainActivity.TAG_IMAG, "Fail" + "Reason Code:" + Integer.toString(reasonCode));


                        }
                    });
                }
                if (!ImageTransferThread.isAlive()) {
                    ImageTransferThread.start();
                }

                break;
            case R.id.button_send:
                Log.d(MainActivity.TAG_IMAG, "We can't send it");
                break;
            case R.id.button_stop:
                Log.d(MainActivity.TAG_IMAG, "Stop");
                sender.stop();
                break;

            default:
                break;
        }
    }


}

