package com.example.wanne.qrcodetest;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private EditText editName;
    private EditText editId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnScanner = this.findViewById(R.id.btnScanner);
        Button btnUpdate = this.findViewById(R.id.btnUpdate);
        editName = this.findViewById(R.id.txtName);
        editId = this.findViewById(R.id.txtId);

        btnScanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt("Scan Code");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(true);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = editId.getText().toString();
                String name = editName.getText().toString();
                if(!id.isEmpty() && !name.isEmpty())
                    new CreateUser().execute(id, name);
                else
                    Toast.makeText(getApplicationContext(), "Enter name and id", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(intentResult != null) {
            if(intentResult.getContents() == null) {
                Log.d("MainActivity", "Cancelled");
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            }
            else {
                Log.d("MainActivity", "Scanned");
                Log.d("MainActivity", intentResult.getContents().substring(intentResult.getContents().length()-1));
                //new CheckIn().execute(editId.getText().toString(), intentResult.getContents().substring(intentResult.getContents().length()-1));
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intentResult.getContents().toLowerCase().replaceAll("localhost:4200", "virtualhost:4200")));
                startActivity(browserIntent);
                Toast.makeText(this, "Scanned: " + intentResult.getContents(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private HttpURLConnection createConnection(String api, String method, String[] params, String[] paramNames) {
        HttpURLConnection urlConnection;
        URL url;
        JSONObject object = new JSONObject();

        try {
            url = new URL(api);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);

            for(int i = 0; i < params.length; i++) {
                object.put(paramNames[i], params[i]);
            }

            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(object.toString());
            wr.flush();

            return urlConnection;
        }
        catch (Exception e)
        {
            Log.i(api, e.toString());
            return null;
        }
    }

    private class CreateUser extends AsyncTask<String, Void, String> {

        String api = "http://10.0.2.2:3000/API/user/";
        String[] paramNames = new String[]{"_id", "name"};
        Exception mException = null;

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection urlConnection;

            try {
                urlConnection = createConnection(api, "POST", strings, paramNames);

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
                return "Fail";
            }
            catch (Exception e)
            {
                Log.i("CREATE USER", e.toString());
                this.mException = e;
                return "Exception";
            }
        }
    }

    private class CheckIn extends AsyncTask<String, Void, String> {

        String api = "http://10.0.2.2:3000/API/checkin/";
        String[] paramNames = new String[]{"userid", "locationid"};
        Exception mException = null;
        String locationid = "";

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection urlConnection;
            locationid = strings[1];

            try {
                urlConnection = createConnection(api, "POST", strings, paramNames);

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
                    JSONObject response = new JSONObject(sb.toString());
                    if(response.has("message") && (response.get("message").equals("Location not in db.") || response.get("message").equals("Location has no name."))) {
                        return "EnterName";
                    }
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
                    break;
                case "EnterName":
                    showDialog();
                    break;
                default:
                    break;
            }
        }

        private void showDialog() {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
            LayoutInflater inflater = MainActivity.this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.new_location_dialog, null);
            dialogBuilder.setView(dialogView);

            final EditText edt = dialogView.findViewById(R.id.editLocation);

            dialogBuilder.setTitle("Enter name for location");
            dialogBuilder.setMessage("Name:");
            dialogBuilder.setPositiveButton("Done", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String location = edt.getText().toString();
                    new CreateLocation().execute(locationid, location);
                }
            });
            dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //pass
                }
            });
            AlertDialog b = dialogBuilder.create();
            b.show();
        }
    }

    private class CreateLocation extends AsyncTask<String, Void, String> {

        String api = "http://10.0.2.2:3000/API/location/";
        String[] paramNames = new String[]{"_id", "name"};
        Exception mException = null;

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection urlConnection;

            try {
                urlConnection = createConnection(api, "PUT", strings, paramNames);

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
    }
}