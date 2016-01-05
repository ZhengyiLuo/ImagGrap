package com.example.zhengyiluo.imaggrap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by ZhengyiLuo on 12/28/15.
 */
@SuppressLint("ViewConstructor")
public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {
    /**
     * Camera this surface uses
     */
    Camera mCamera;
    SurfaceHolder holder;

    /**
     * Class logging tag
     */
    private static final String TAG = "CameraSurface";

    /**
     * Create new camera surface
     *
     * @param context
     * @param camera
     */
    public CameraSurface(Context context, Camera camera) {
        super(context);

        mCamera = camera;

        holder = getHolder();
        holder.addCallback(this);
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here


        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();

        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
            }
        } catch (Exception exception) {
            Log.e(TAG, "Caught exception while trying to set preview display", exception);
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }


}
