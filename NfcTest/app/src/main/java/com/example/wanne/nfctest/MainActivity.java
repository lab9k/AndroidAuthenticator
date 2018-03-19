package com.example.wanne.nfctest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "GoogleActivity";
    NfcAdapter mAdapter;
    private static final int RC_SIGN_IN = 9001;
    private String stickerid = "";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://agile-everglades-38755.herokuapp.com/");
        } catch (URISyntaxException e) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();

        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if( mAdapter == null) {
            //nfc not supported on device
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(!mAdapter.isEnabled()) {
            Log.i("NFC", "NFC is disabled");
        }
        else {
            Log.i("NFC", "NFC is enabled");
        }

        mSocket.connect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                startActivity(intent);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            Log.i("USER: onStart", currentUser.getEmail());
            Intent i = getIntent();
            if(i != null ) {
                byte[] tagId = i.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                String hexdump = new String();
                for (int j = 0; j < tagId.length; j++) {
                    String x = Integer.toHexString(((int) tagId[j] & 0xff));
                    if (x.length() == 1) {
                        x = '0' + x;
                    }
                    hexdump += x + ' ';
                }
                Tag tag = i.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                Log.i("USER: onStart", tag.getId().toString());
                new CheckIn().execute(new String[]{currentUser.getEmail(), hexdump.replace(" ", "")});
                getTagInfo(i);
            }
        }
        else {
            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.i("USER:firebaseWithGoogle", user.getEmail());
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                            startActivity(intent);
                        }
                    }
                });
    }

    private void getTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.i("NFC", tag.toString());
        Log.i("NFC ID", tag.getId().toString());
        Log.i("NFC contents", String.valueOf(tag.describeContents()));
    }

    private HttpURLConnection createConnection(String api, String method, String[] params, String[] paramNames) {
        HttpURLConnection urlConnection;
        URL url;
        JSONObject object = new JSONObject();

        try {
            url = new URL(api);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);

            urlConnection.setDoInput(true);
            if(!method.equals("GET")) {
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(true);

                for(int i = 0; i < params.length; i++) {
                    object.put(paramNames[i], params[i]);
                }

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(object.toString());
                wr.flush();
            }

            return urlConnection;
        }
        catch (Exception e)
        {
            Log.i(api, e.toString());
            return null;
        }
    }

    private HttpURLConnection createConnection(String api, String method, JSONObject object) {
        HttpURLConnection urlConnection;
        URL url;

        try {
            url = new URL(api);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);

            urlConnection.setDoInput(true);
            if(!method.equals("GET")) {
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                urlConnection.setDoOutput(true);

                OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
                wr.write(object.toString());
                wr.flush();
            }

            return urlConnection;
        }
        catch (Exception e)
        {
            Log.i(api, e.toString());
            return null;
        }
    }

    private class CheckIn extends AsyncTask<String, Void, String> {
        String api = "http://agile-everglades-38755.herokuapp.com/API/checkin/";
        String[] paramNames = new String[]{"userid", "locationid"};
        Exception mException = null;

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection urlConnection;
            stickerid = strings[1];

            try {
                urlConnection = createConnection(api, "POST", strings, paramNames);

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection != null ? urlConnection.getResponseCode() : 0;
                Log.i("RESULT", String.valueOf(urlConnection.getResponseMessage()));
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    Log.i("SUCCES", sb.toString());
                    JSONObject response = new JSONObject(sb.toString());
                    if(response.has("message") && (response.get("message").equals("New sticker") || response.get("message").equals("Location has no name."))) {
                        Log.i("CHECKIN", "Location not in db");
                        return "EnterName";
                    }
                    EmitCheckin(response);
                    return "Succes";
                }
                return "Fail";
            }
            catch (Exception e)
            {
                Log.i("CHECKIN", e.toString());
                this.mException = e;
                return "Exception";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            switch (result) {
                case "Succes":
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500,VibrationEffect.DEFAULT_AMPLITUDE));
                    }else{
                        //deprecated in API 26
                        v.vibrate(500);
                    }
                    v.vibrate(500);
                    Toast.makeText(getApplicationContext(), "Succesfully checked in", Toast.LENGTH_SHORT).show();
                    CloseApplication();
                    break;
                case "EnterName":
                    //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://agile-everglades-38755.herokuapp.com/checkin/" + locationid));
                    //startActivity(browserIntent);
                    //CloseApplication();
                    new GetLocations().execute();
                    break;
                case "Fail":
                    Toast.makeText(getApplicationContext(), "Something went wrong while checking in", Toast.LENGTH_SHORT).show();
                    CloseApplication();
                    break;
                default:
                    break;
            }
        }
    }

    private class GetLocations extends AsyncTask<String, Void, String> {

        String api = "http://agile-everglades-38755.herokuapp.com/API/locations/";
        String[] paramNames = new String[]{};
        Exception mException = null;
        JSONArray o;

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection urlConnection;

            try {
                urlConnection = createConnection(api, "GET", strings, paramNames);

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection != null ? urlConnection.getResponseCode() : 0;
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    o = new JSONArray(sb.toString());
                    for(int i = 0; i<o.length();i++) {
                        Log.i("SUCCES", new JSONObject(o.get(i).toString()).getString("name"));
                    }
                    return "Succes";
                }
                Log.i("FAIL", String.valueOf(urlConnection.getResponseCode()));
                return "Fail";
            }
            catch (Exception e)
            {
                Log.i("GET LOCATIONS", e.toString());
                this.mException = e;
                return "Exception";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            showDialog();
        }

        private void showDialog() {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.new_location_dialog, null);
            dialogBuilder.setView(dialogView);

            final EditText edt = dialogView.findViewById(R.id.editLocation);
            final Spinner spin = dialogView.findViewById((R.id.spinnerLocations));
            String[] items = new String[o.length()+1];
            items[0] = "";
            for(int i = 1; i<o.length()+1;i++) {
                try {
                    items[i] = new JSONObject(o.get(i-1).toString()).getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, items);
            spin.setAdapter(adapter);
            dialogBuilder.setTitle("New Sticker Scanned");
            dialogBuilder.setMessage("Add to existing location OR Create new location:");
            dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String location = edt.getText().toString();
                    String t = spin.getSelectedItem().toString();
                    ArrayList<String> stickers = new ArrayList<String>();
                    stickers.add(stickerid);
                    if(!location.equals("")) {
                        //Create Location
                        Log.i("CREATE LOCATION", location);
                        JSONObject object = new JSONObject();
                        try {
                            object.put("name", location);
                            object.put("stickers", new JSONArray(stickers));
                            new CreateLocation().execute(object);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else if(!t.equals("")) {
                        //Add to existing location
                        Log.i("ADDTO EXISTING LOCATION", t);
                        for(int i = 1; i<o.length()+1;i++) {
                            try {
                                JSONObject loc = new JSONObject(o.get(i).toString());
                                if(loc.get("name").equals(t)) {
                                    if(loc.has("stickers")) {
                                        loc.put("stickers", loc.getJSONArray("stickers").put(stickerid));
                                    }
                                    else {
                                        loc.put("stickers", new JSONArray(stickers));
                                    }
                                    new AddToLocation().execute(loc);
                                    break;
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        //User doesn't want to select anything
                        CloseApplication();
                    }
                }
            });
            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //pass
                    CloseApplication();
                }
            });
            AlertDialog b = dialogBuilder.create();
            b.show();
        }
    }

    private class CreateLocation extends AsyncTask<JSONObject, Void, String> {

        String api = "http://agile-everglades-38755.herokuapp.com/API/location/";
        Exception mException = null;

        @Override
        protected String doInBackground(JSONObject... location) {
            HttpURLConnection urlConnection;

            try {
                urlConnection = createConnection(api, "POST", location[0]);

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection != null ? urlConnection.getResponseCode() : 0;
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    Log.i("SUCCES", sb.toString());
                    return "Succes";
                }
                Log.i("FAIL", String.valueOf(urlConnection.getResponseCode()));
                return "Fail";
            }
            catch (Exception e)
            {
                Log.i("CREATE LOCATION", e.toString());
                this.mException = e;
                return "Exception";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            switch(result) {
                case "Succes":
                    Toast.makeText(getApplicationContext(), "Succesfully created new location", Toast.LENGTH_LONG).show();
                    break;
                case "Fail":
                    Toast.makeText(getApplicationContext(), "Something went wrong while creating new location", Toast.LENGTH_LONG).show();
                    break;
            }
            CloseApplication();
        }
    }

    private class AddToLocation extends AsyncTask<JSONObject, Void, String> {

        String api = "http://agile-everglades-38755.herokuapp.com/API/location/";
        Exception mException = null;

        @Override
        protected String doInBackground(JSONObject... location) {
            HttpURLConnection urlConnection;

            try {
                urlConnection = createConnection(api, "PUT", location[0]);

                StringBuilder sb = new StringBuilder();
                int HttpResult = urlConnection != null ? urlConnection.getResponseCode() : 0;
                if (HttpResult == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    br.close();
                    Log.i("SUCCES", sb.toString());
                    return "Succes";
                }
                Log.i("FAIL", String.valueOf(urlConnection.getResponseCode()));
                return "Fail";
            }
            catch (Exception e)
            {
                Log.i("ADD TO LOCATION", e.toString());
                this.mException = e;
                return "Exception";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            switch(result) {
                case "Succes":
                    Toast.makeText(getApplicationContext(), "Succesfully added sticker to location", Toast.LENGTH_LONG).show();
                    break;
                case "Fail":
                    Toast.makeText(getApplicationContext(), "Something went wrong while add sticker to location", Toast.LENGTH_LONG).show();
                    break;
            }
            CloseApplication();
        }
    }

    private void CloseApplication() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 21)
                    finishAndRemoveTask();
                else
                    finish();
                System.exit(0);
            }
        }, 2000);

    }

    private void EmitCheckin(JSONObject user) {
        mSocket.emit("checkin", user);
    }
}