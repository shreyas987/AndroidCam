package gq.androidcam.androidcam.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

import gq.androidcam.androidcam.R;
import gq.androidcam.androidcam.app.AppConfig;
import gq.androidcam.androidcam.app.AppController;

/**
 * Created by Shreyas on 09-05-2017.
 */

public class LogActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        // Get token
        final String token = FirebaseInstanceId.getInstance().getToken();

        final TextView Lgview=(TextView)findViewById(R.id.Lgview);

        String tag_string_req = "req log";
        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_LOG, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    Log.i("tagconvertstr", "["+response+"]");
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        int i = 1;
                        String[] str = new String[200];
                        String ImageURL;//= jObj.getString("image"+i);
                        while(i<jObj.length()){
                            ImageURL = jObj.getString("image"+ i);
                            str[i]= ImageURL;
                            i++;
                        }
                        Lgview.append("Intrusions detected till now(Latest First)\n");
                        i=1;
                        //Lgview.setMovementMethod(new ScrollingMovementMethod());
                        while(str[i]!=null){

                            Lgview.append("\nImage"+i+":"+"\n"+str[i]+"\n");
                            Lgview.setMovementMethod(new ScrollingMovementMethod());
                            i++;
                            Linkify.addLinks(Lgview, Linkify.WEB_URLS);
                        }
                    } else{
                        // Error in log. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }) {
            @Override
            protected Map<String, String> getParams(){
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("fcm",token);
                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }
}
