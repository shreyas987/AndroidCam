package gq.androidcam.androidcam.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import android.os.Handler;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gq.androidcam.androidcam.R;
import gq.androidcam.androidcam.app.AppConfig;
import gq.androidcam.androidcam.app.AppController;
import gq.androidcam.androidcam.helper.SessionManager;

import static android.content.ContentValues.TAG;
import static gq.androidcam.androidcam.activity.MyService.FirstImage;

public class MainActivity extends Activity {

	private Button btnLogout;
	private Button btnstrt;
	private  Button btnlg;
	private  Button btnabt;
	private TextView txtrtbt;
	private Button tglbtn;
	private TextView latsttxt;

	private SessionManager session;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnLogout = (Button) findViewById(R.id.btnLogout);
		btnstrt = (Button) findViewById(R.id.btnstrt);
		btnlg = (Button) findViewById(R.id.btnlg);
		btnabt = (Button) findViewById(R.id.btnabt);
		tglbtn = (Button) findViewById(R.id.tglbtn);
		latsttxt = (TextView) findViewById(R.id.latsttxt);

		// session manager
		session = new SessionManager(getApplicationContext());

		if (!session.isLoggedIn()) {
			logoutUser();
		}else{

			// Get token
			final String token = FirebaseInstanceId.getInstance().getToken();

			String tag_string_req = "req log";
			StringRequest strReq = new StringRequest(Request.Method.POST,
					AppConfig.URL_MAINLOG, new Response.Listener<String>() {

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
							String ImageURL;
							while(i<jObj.length()){
								ImageURL = jObj.getString("image"+ i);
								str[i]= ImageURL;
								i++;
							}
							i=1;
							if(str[i]!=null){

								latsttxt.append("\nImage Link"+":"+"\n"+str[i]+"\n");
								latsttxt.setMovementMethod(new ScrollingMovementMethod());
								Linkify.addLinks(latsttxt, Linkify.WEB_URLS);
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


		// Logout button_style click event
		btnLogout.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				logoutUser();
			}
		});

		btnlg.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v){

				// Launch log activity
				Intent intent = new Intent(MainActivity.this, LogActivity.class);
				startActivity(intent);
			}
		});

		btnabt.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v){

				// Launch about activity
				Intent intent = new Intent(MainActivity.this, MainActivity2.class);
				startActivity(intent);
			}
		});

		tglbtn.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v){

				latsttxt.setVisibility((latsttxt.getVisibility() == View.VISIBLE)? View.GONE : View.VISIBLE);

			}
		});

		btnstrt.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {

				Intent i = new Intent(MainActivity.this,MyService.class);
				//if ((PendingIntent.getService(MainActivity.this, 0, i,0) != null))
				if(btnstrt.getText().equals("Start Camera"))
				{   Toast.makeText(getApplicationContext(),
						"Monitoring begins in 15 seconds", Toast.LENGTH_LONG).show();
					startService(i);
					btnstrt.setText("Stop Camera");
				}else{

                    Toast.makeText(getApplicationContext(),
                            "Stoping the service and closing the app", Toast.LENGTH_LONG).show();
					stopService(i);
					if(FirstImage != null) {
						new Handler().postDelayed(new Runnable() {
							@Override
							public void run() {
								FirstImage.delete();
								finish();
								System.exit(0);
							}
						}, 2000);
					}
					else{
						finish();
						System.exit(0);
					}

				}

			}
		});

	}
	/*
	  Logging out the user. Will set isLoggedIn flag to false in shared
	  preferences Clears the user data from sqlite users table
	 */
	private void logoutUser() {

		Intent i = new Intent(MainActivity.this,MyService.class);
		stopService(i);
		session.setLogin(false);

		final String token = FirebaseInstanceId.getInstance().getToken();

		String tag_string_req = "req_logout";

		StringRequest strReq = new StringRequest(Request.Method.POST,
				AppConfig.URL_LOGOUT, new Response.Listener<String>() {

			@Override
			public void onResponse(String response) {
				Log.d(TAG, "Logout Response: " + response.toString());

				try {
					JSONObject jObj = new JSONObject(response);
					boolean error = jObj.getBoolean("error");

					// Check for error node in json
					if (!error) {
						// Launch main activity
						Intent intent = new Intent(MainActivity.this, LoginActivity.class);
						startActivity(intent);
						finish();
					} else {
						// Error in login. Get the error message
						String errorMsg = jObj.getString("error_msg");
						Toast.makeText(getApplicationContext(),
								"Logout not successful", Toast.LENGTH_LONG).show();
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
				Log.e(TAG, "Logout Error: " + error.getMessage());
				Toast.makeText(getApplicationContext(),
						error.getMessage(), Toast.LENGTH_LONG).show();
			}
		}) {

			@Override
			protected Map<String, String> getParams() {
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
