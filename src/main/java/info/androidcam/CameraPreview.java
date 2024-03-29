package info.androidcam;

/**
 * Created by Shreyas on 10-03-2017.
 */


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import info.androidcam.config.CameraResolution;
import info.androidcam.config.CameraRotation;

import java.io.IOException;
import java.util.List;

/**
 * Created by Keval on 10-Nov-16.
 * This surface view works as the fake preview for the camera.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

@SuppressLint("ViewConstructor")
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private CameraCallbacks mCameraCallbacks;

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private CameraConfig mCameraConfig;

    private boolean safeToTakePicture = false;

    CameraPreview(@NonNull Context context, CameraCallbacks cameraCallbacks) {
        super(context);

        mCameraCallbacks = cameraCallbacks;

        //Set surface holder
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException | NullPointerException e) {
            // left blank for now
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        safeToTakePicture = false;
        //if(mCamera!=null) {
            // Now that the size is known, set up the camera parameters and begin the preview.
            Camera.Parameters parameters = mCamera.getParameters();

            List<Camera.Size> pictureSizes = mCamera.getParameters().getSupportedPictureSizes();
//        List<Camera.Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
//
//        //set the preview sizes that are the lowest, as we don't have to display the preview.
//        parameters.setPreviewSize(previewSizes.get(previewSizes.size() - 1).width,
//                previewSizes.get(previewSizes.size() - 1).height);

            //set the camera image size based on config provided
            Camera.Size cameraSize;
            switch (mCameraConfig.getResolution()) {
                case CameraResolution.HIGH_RESOLUTION:
                    cameraSize = pictureSizes.get(0);   //Highest res
                    break;
                case CameraResolution.MEDIUM_RESOLUTION:
                    cameraSize = pictureSizes.get(pictureSizes.size() / 2);     //Resolution at the middle
                    break;
                case CameraResolution.LOW_RESOLUTION:
                    cameraSize = pictureSizes.get(pictureSizes.size() - 1);       //Lowest res
                    break;
                default:
                    throw new RuntimeException("Invalid camera resolution.");
            }
            parameters.setPictureSize(cameraSize.width, cameraSize.height);

            requestLayout();
            mCamera.setParameters(parameters);

            mCamera.startPreview();
            safeToTakePicture = true;
        //}
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return, so stop the preview.
        // Call stopPreview() to stop updating the preview surface.
        if (mCamera != null) {
            safeToTakePicture = false;
            mCamera.stopPreview();
        }
    }

    /**
     * Initialize the camera and start the preview of the camera.
     *
     * @param cameraConfig camera config builder.
     */
    void startCameraInternal(@NonNull CameraConfig cameraConfig) {
        mCameraConfig = cameraConfig;

        if (safeCameraOpen(mCameraConfig.getFacing())) {
            if (mCamera != null) {
                requestLayout();

                try {
                    mCamera.setPreviewDisplay(mHolder);
                    mCamera.startPreview();

                    safeToTakePicture = true;
                } catch (IOException e) {
                    e.printStackTrace();

                    mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
                }
            }
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;

        try {
            stopPreviewAndFreeCamera();

            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e("CameraPreview", "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    boolean isSafeToTakePictureInternal() {
        return safeToTakePicture;
    }

    void takePictureInternal() {
        if (mCamera != null) {
            mCamera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(final byte[] bytes, Camera camera) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            //Convert byte array to bitmap
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                            //Rotate the bitmap
                            Bitmap rotatedBitmap;
                            if (mCameraConfig.getmImageRotation() != CameraRotation.ROTATION_0) {
                                rotatedBitmap = HiddenCameraUtils.rotateBitmap(bitmap, mCameraConfig.getmImageRotation());

                                //noinspection UnusedAssignment
                                bitmap = null;
                            } else {
                                rotatedBitmap = bitmap;
                            }

                            //Save image to the file.
                            if (HiddenCameraUtils.saveImageFromFile(rotatedBitmap,
                                    mCameraConfig.getImageFile(),
                                    mCameraConfig.getImageFormat())) {
                                //Post image file to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onImageCapture(mCameraConfig.getImageFile());
                                    }
                                });
                            } else {
                                //Post error to the main thread
                                new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mCameraCallbacks.onCameraError(CameraError.ERROR_IMAGE_WRITE_FAILED);
                                    }
                                });
                            }
                        }
                    }).start();
                }
            });
        } else {
            mCameraCallbacks.onCameraError(CameraError.ERROR_CAMERA_OPEN_FAILED);
        }
    }

    /**
     * When this function returns, mCamera will be null.
     */
    void stopPreviewAndFreeCamera() {
        safeToTakePicture = false;

        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}