package info.androidcam;

/**
 * Created by Shreyas on 10-03-2017.
 */

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.view.ViewGroup;
import android.view.WindowManager;

import info.androidcam.config.CameraFacing;

/**
 * Created by Keval on 27-Oct-16.
 * This abstract class provides ability to handle background camera to the service in which it is
 * extended.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

public abstract class HiddenCameraService extends Service implements CameraCallbacks {

    private CameraPreview mCameraPreview;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCamera();
    }

    /**
     * Start the hidden camera. Make sure that you check for the runtime permissions before you start
     * the camera.
     * <p>
     * <B>Note: </B>Developer has to check if the "Draw over other apps" permission is available by
     * calling {@link HiddenCameraUtils#canOverDrawOtherApps(Context)} before staring the camera.
     *
     * @param cameraConfig camera configuration {@link CameraConfig}
     */
    @RequiresPermission(allOf = {Manifest.permission.CAMERA, Manifest.permission.SYSTEM_ALERT_WINDOW})
    public void startCamera(CameraConfig cameraConfig) {

        if (!HiddenCameraUtils.canOverDrawOtherApps(this)) {    //Check if the draw over other app permission is available.

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION);
        } else if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) { //check if the camera permission is available

            //Throw error if the camera permission not available
            onCameraError(CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE);
        } else if (cameraConfig.getFacing() == CameraFacing.FRONT_FACING_CAMERA
                && !HiddenCameraUtils.isFrontCameraAvailable(this)) {   //Check if for the front camera

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA);
        } else {

            //Add the camera preview surface to the root of the activity view.
            if (mCameraPreview == null) mCameraPreview = addPreView();
            try{mCameraPreview.startCameraInternal(cameraConfig);}catch(Exception e){e.printStackTrace();}
        }
    }

    /**
     * Call this method to capture the image using the camera you initialized. Don't forget to
     * initialize the camera using {@link #startCamera(CameraConfig)} before using this function.
     */
    public void takePicture() {
        if (mCameraPreview != null && mCameraPreview.isSafeToTakePictureInternal()) {
            mCameraPreview.takePictureInternal();
        } else {
            throw new RuntimeException("Background camera not initialized. Call startCamera() to initialize the camera.");
        }
    }

    /**
     * Stop and release the camera forcefully.
     */
    public void stopCamera() {
        if (mCameraPreview != null) mCameraPreview.stopPreviewAndFreeCamera();
    }

    /**
     * Add camera preview to the root of the activity layout.
     *
     * @return {@link CameraPreview} that was added to the view.
     */
    private CameraPreview addPreView() {
        //create fake camera view
        CameraPreview cameraSourceCameraPreview = new CameraPreview(this, this);
        cameraSourceCameraPreview.setLayoutParams(new ViewGroup
                .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);

        wm.addView(cameraSourceCameraPreview, params);
        return cameraSourceCameraPreview;
    }
}