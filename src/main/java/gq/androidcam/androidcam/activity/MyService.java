package gq.androidcam.androidcam.activity;

        import android.Manifest;
        //import android.app.Service;
        import android.app.ProgressDialog;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.graphics.Camera;
        import android.graphics.Color;
        import android.media.ThumbnailUtils;
        import android.net.Uri;
        import android.os.AsyncTask;
        import android.os.Environment;
        import android.os.Handler;
        import android.os.IBinder;
        import android.os.Looper;
        import android.provider.Settings;
        import android.support.annotation.NonNull;
        import android.support.annotation.Nullable;
        import android.support.v4.app.ActivityCompat;
        import android.util.Base64;
        import android.util.Log;
        import android.widget.Toast;
        import android.provider.MediaStore;

        import com.android.volley.AuthFailureError;
        import com.android.volley.RequestQueue;
        import com.android.volley.VolleyError;
        import com.android.volley.Request.Method;
        import com.android.volley.Response;
        import com.android.volley.toolbox.StringRequest;
        import com.android.volley.toolbox.ImageLoader;
        import com.android.volley.toolbox.StringRequest;
        import com.android.volley.toolbox.Volley;
        import com.google.firebase.iid.FirebaseInstanceId;

        import java.io.BufferedReader;
        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.IOException;
        import java.io.InputStreamReader;
        import java.io.OutputStreamWriter;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.net.URLConnection;
        import java.util.HashMap;
        import java.util.Hashtable;
        import java.util.Map;


        import android.content.Intent;

        import org.json.JSONObject;

        import gq.androidcam.androidcam.R;
        import gq.androidcam.androidcam.app.AppConfig;
        import gq.androidcam.androidcam.app.AppController;
        import in.nashapp.apicontroller.ApiController;
        import info.androidcam.CameraConfig;
        import info.androidcam.CameraError;
        import info.androidcam.HiddenCameraService;
        import info.androidcam.HiddenCameraUtils;
        import info.androidcam.config.CameraFacing;
        import info.androidcam.config.CameraImageFormat;
        import info.androidcam.config.CameraResolution;
        import info.androidcam.config.CameraRotation;

 public class MyService extends HiddenCameraService {
            static File FirstImage = null;
            boolean var = true;

            @Nullable
            @Override
            public IBinder onBind(Intent intent) {
                return null;
            }

     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         String dirstr = Environment.getExternalStorageDirectory().getPath() + "/androidcam/";
         final File dir = new File(dirstr);
         if (!dir.exists())
             dir.mkdirs();
         new Thread( new Runnable() {
             @Override
             public void run() {
                 while(var){
                     try{Thread.sleep(15000);}catch(Exception e){}
                     new Handler(Looper.getMainLooper()).post(new Runnable() {
                         @Override
                         public void run() {
                             if (ActivityCompat.checkSelfPermission(MyService.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                 if (HiddenCameraUtils.canOverDrawOtherApps(MyService.this)) {
                                     stopCamera();
                                     String fname = "Image"+ System.currentTimeMillis() + ".jpg";
                                     final File img = new File(dir, fname);
                                     CameraConfig cameraConfig = new CameraConfig()
                                             .getBuilder(MyService.this)
                                             .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                                             .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                                             .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                                             .setImageRotation(CameraRotation.ROTATION_90)
                                             .setImageFile(img)
                                             .build();
                                     startCamera(cameraConfig);

                                     new Handler().postDelayed(new Runnable() {
                                              @Override
                                              public void run() {
                                                  try{takePicture();}catch (Exception e){e.printStackTrace();}

                                              }
                                          }, 2000);
                                 } else {
                                     HiddenCameraUtils.openDrawOverPermissionSetting(MyService.this);
                                 }
                             } else {
                                 Toast.makeText(MyService.this, "Camera permission not available", Toast.LENGTH_SHORT).show();

                             }
                         }
                     });
                 }
             }
         }).start();
         return START_NOT_STICKY;
     }

     @Override
     public void onImageCapture(@NonNull final File imageFile) {
         BitmapFactory.Options options = new BitmapFactory.Options();
         options.inPreferredConfig = Bitmap.Config.RGB_565;
         final Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
         Log.d("ImageCapture", 1 + "");

         //thread
         final Thread thread = new Thread() {
             @Override
             public void run() {
                 //getsystem time
                 final long time1= System.currentTimeMillis();
                 if (FirstImage == null)
                 {   FirstImage = imageFile;}
                 else {
                     //reduce image size
                     Bitmap thumbBitmap1 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(FirstImage.getAbsolutePath()), 320, 240);
                     Bitmap thumbBitmap2 = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imageFile.getAbsolutePath()), 320, 240);

                     final double change = compareImage(thumbBitmap1, thumbBitmap2);
                     Log.d("CameraCompare", change + "");

                     if (change > 10) {
                     //alert();
                      uploadImage(imageFile);
                         imageFile.delete();
                     }
                     imageFile.delete();


                     if(var==false){
                         FirstImage.delete();
                     }
                 }
                 long time2= System.currentTimeMillis();
                 long time=(time2-time1);
                 Log.d("time", String.valueOf(time));
             }
         };
         thread.start();
         //stopSelf();
     }

     @Override
     public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
         switch (errorCode) {
             case CameraError.ERROR_CAMERA_OPEN_FAILED:
                 //Camera open failed. Probably because another application
                 //is using the camera
                 Toast.makeText(this, "Cannot open camera.", Toast.LENGTH_LONG).show();
                 break;
             case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                 //camera permission is not available
                 //Ask for the camra permission before initializing it.
                 Toast.makeText(this, "Camera permission not available.", Toast.LENGTH_LONG).show();
                 break;
             case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                 //Display information dialog to the user with steps to grant "Draw over other app"
                 //permission for the app.
                 HiddenCameraUtils.openDrawOverPermissionSetting(this);
                 break;
             case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                 Toast.makeText(this, "Your device does not have front camera.", Toast.LENGTH_LONG).show();
                 break;
         }

         stopSelf();
     }

     private double compareImage(Bitmap bitmap_f, Bitmap bitmap_c) {
         int f_w=bitmap_f.getWidth();
         int f_h=bitmap_f.getHeight();
         long diff=0;
         for(int i=0;i<f_w;i++)
         {
             for(int j=0;j<f_h;j++)
             {
                 int pixel_f=bitmap_f.getPixel(i,j);
                 int red_f = Color.red(pixel_f);
                 int blue_f = Color.blue(pixel_f);
                 int green_f = Color.green(pixel_f);

                 int pixel_c=bitmap_c.getPixel(i,j);
                 int red_c = Color.red(pixel_c);
                 int blue_c = Color.blue(pixel_c);
                 int green_c = Color.green(pixel_c);

                 diff=diff+Math.abs(red_f-red_c)+Math.abs(blue_f-blue_c)+Math.abs(green_f-green_c);
             }
         }
         double n=f_w*f_h*3;
         double p=(diff*100)/(n*255);
         return p;
     }

     @Override
     public void onDestroy() {

         super.onDestroy();
         var = false;

     }

    private void uploadImage(File imageFile){
        HashMap<String,String> params = new HashMap<String,String>();
        params.put("image","file://"+imageFile.getAbsolutePath());
        String name = "image"+System.currentTimeMillis();
        final String token = FirebaseInstanceId.getInstance().getToken();
        params.put("name", name);
        params.put("fcm", token);
        ApiController apic = new ApiController(this);
        String response = apic.PostRequest("http://androidcam.gq.cp-in-16.hostgatorwebservers.com/uploadimage.php",params);

    }

 }


