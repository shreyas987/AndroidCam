package info.androidcam;

/**
 * Created by Shreyas on 10-03-2017.
 */


import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import info.androidcam.config.CameraFacing;

/**
 * Created by Keval on 27-Oct-16.
 * This abstract class provides ability to handle background camera to the fragment in which it is
 * extended.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

public abstract class HiddenCameraFragment extends Fragment implements CameraCallbacks {

    private CameraPreview mCameraPreview;

    @Override
    public void onDestroy() {
        super.onDestroy();

        //stop preview and release the camera.
        stopCamera();
    }

    /**
     * Start the hidden camera. Make sure that you check for the runtime permissions before you start
     * the camera.
     *
     * @param cameraConfig camera configuration {@link CameraConfig}
     */
    @RequiresPermission(Manifest.permission.CAMERA)
    public void startCamera(CameraConfig cameraConfig) {

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) { //check if the camera permission is available

            onCameraError(CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE);
        } else if (cameraConfig.getFacing() == CameraFacing.FRONT_FACING_CAMERA
                && !HiddenCameraUtils.isFrontCameraAvailable(getActivity())) {   //Check if for the front camera

            onCameraError(CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA);
        } else {
            //Add the camera preview surface to the root of the activity view.
            if (mCameraPreview == null) mCameraPreview = addPreView();
            mCameraPreview.startCameraInternal(cameraConfig);
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
        CameraPreview cameraSourceCameraPreview = new CameraPreview(getActivity(), this);
        cameraSourceCameraPreview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View view = ((ViewGroup) getActivity().getWindow().getDecorView().getRootView()).getChildAt(0);

        if (view instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) view;

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(1, 1);
            linearLayout.addView(cameraSourceCameraPreview, params);
        } else if (view instanceof RelativeLayout) {
            RelativeLayout relativeLayout = (RelativeLayout) view;

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(1, 1);
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            relativeLayout.addView(cameraSourceCameraPreview, params);
        } else if (view instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) view;

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(1, 1);
            frameLayout.addView(cameraSourceCameraPreview, params);
        } else {
            throw new RuntimeException("Root view of the activity/fragment cannot be frame layout");
        }

        return cameraSourceCameraPreview;
    }


}