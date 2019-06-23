package info.androidcam;

/**
 * Created by Shreyas on 10-03-2017.
 */

import android.support.annotation.NonNull;

import java.io.File;

/**
 * Created by Keval on 14-Oct-16.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */
interface CameraCallbacks {

    void onImageCapture(@NonNull File imageFile);

    void onCameraError(@CameraError.CameraErrorCodes int errorCode);
}
