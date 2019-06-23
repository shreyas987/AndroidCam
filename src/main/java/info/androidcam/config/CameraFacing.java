package info.androidcam.config;

/**
 * Created by Shreyas on 10-03-2017.
 */

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Keval on 10-Nov-16.
 * Supported camera facings.
 *
 * @author {@link 'https://github.com/kevalpatel2106'}
 */

public final class CameraFacing {

    /**
     * Rear facing camera id.
     *
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_BACK
     */
    public static final int REAR_FACING_CAMERA = 0;
    /**
     * Front facing camera id.
     *
     * @see android.hardware.Camera.CameraInfo#CAMERA_FACING_FRONT
     */
    public static final int FRONT_FACING_CAMERA = 1;

    private CameraFacing() {
        throw new RuntimeException("Cannot initialize this class.");
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REAR_FACING_CAMERA, FRONT_FACING_CAMERA})
    public @interface SupportedCameraFacing {
    }
}
