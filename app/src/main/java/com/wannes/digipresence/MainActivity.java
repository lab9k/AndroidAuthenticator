package com.wannes.digipresence;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;


import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.wannes.digipresence.api.APIClient;
import com.wannes.digipresence.api.APIInterface;
import com.wannes.digipresence.models.Campus;
import com.wannes.digipresence.models.CheckinPost;
import com.wannes.digipresence.models.Location;
import com.wannes.digipresence.models.Message;
import com.wannes.digipresence.models.Segment;
import com.wannes.digipresence.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity  {

    private String stickerId = "";
    private static String uniqueID;
    private static String site = "https://wiw.lab9k.gent";
    private ProgressBar spinner;
    private APIInterface apiInterface;
    private User user;
    private List<Campus> campuses = null;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(site);
        } catch (URISyntaxException e) {
            Log.i("Socket", e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        spinner = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.VISIBLE);
        uniqueID = readFile();
        if(uniqueID.isEmpty()) {
            String filename = "UUID";
            String fileContents = UUID.randomUUID().toString();
            FileOutputStream outputStream;

            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(fileContents.getBytes());
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            uniqueID = readFile();
        }
        Log.i("ID", uniqueID);
        NfcAdapter mAdapter = NfcAdapter.getDefaultAdapter(this);

        if( mAdapter == null) {
            //nfc not supported on device
            Toast.makeText(getApplicationContext(), R.string.nfc_not_supported_message, Toast.LENGTH_LONG).show();
            scanQR();
        }

        if(mAdapter != null && !mAdapter.isEnabled()) {
            Log.i("NFC", "NFC is disabled");
            Toast.makeText(getApplicationContext(), R.string.nfc_disabled_message, Toast.LENGTH_LONG).show();
            scanQR();
        }
        else {
            Log.i("NFC", "NFC is enabled");
            if(getIntent().getAction() == null) {
                CloseApplication("NFC ENABLED, BAD TAG?");
            }
            else if(getIntent().getAction().equals(Intent.ACTION_MAIN) && mAdapter != null) {
                showDialog();
            }
        }

        mSocket.connect();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(intentResult != null) {
            if(intentResult.getContents() == null) {
                Log.d("MainActivity", "Cancelled");
                CloseApplication("CANCELED QR");
            }
            else {
                Log.d("MainActivity", "Scanned");
                Log.d("MainActivity", intentResult.getContents());
                stickerId = intentResult.getContents().replaceAll(".*\\/", "").toLowerCase();
                GetUser(false);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent i = getIntent();
        if(i != null ) {
            byte[] tagId = i.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            StringBuilder hexDump = new StringBuilder();
            if(tagId != null) {
                for (byte aTagId : tagId) {
                    String x = Integer.toHexString(((int) aTagId & 0xff));
                    if (x.length() == 1) {
                        x = '0' + x;
                    }
                    hexDump.append(x);
                }
                stickerId = hexDump.toString().toLowerCase();
                GetUser(false);
                getTagInfo(i);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
    }

    private void showDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.empty_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(R.string.app_opened);
        dialogBuilder.setMessage(R.string.app_opened_message);
        dialogBuilder.setPositiveButton(R.string.app_opened_positive_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                scanQR();
            }
        });
        dialogBuilder.setNegativeButton(R.string.app_opened_neutral_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(site));
                //startActivity(browserIntent);
                //CloseApplication("CHECKOUT");
                GetUser(true);
            }
        });
        dialogBuilder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                CloseApplication("APP CLOSED");
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                CloseApplication("Dialog closed");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void scanQR() {
        Log.i("open", "scan qr");
        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt(getString(R.string.scan_code));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    private String readFile() {
        try {
            FileInputStream fis = openFileInput("UUID");
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            return "";
        } catch (UnsupportedEncodingException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    private void getTagInfo(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.i("NFC", tag.toString());
        Log.i("NFC ID", Arrays.toString(tag.getId()));
        Log.i("NFC contents", String.valueOf(tag.describeContents()));
    }

    private void GetUser(final boolean checkout) {
        Call<User> getUser = apiInterface.getUserByPhoneid(uniqueID.trim());
        Log.i("API", "GET USER");
        getUser.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                user = response.body();

                if(response.code() == 400) {
                    RegisterPhoneDialog();
                }
                else {
                    if(user.getMessages().length > 0) {
                        for(Message m: user.getMessages()) {
                            if(!m.isRead()) {
                                NewMessage(m);
                            }
                        }
                    }

                    if(!checkout) {
                        // Look if user is checked in
                        if(user.getCheckin() == null) {
                            CreateCheckin();
                        }
                        else {
                            GetLocationById(user.getCheckin().getLocation().getId());
                        }
                    } else {
                        DeleteCheckin();
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                CloseApplication("GET USER FAIL");
            }
        });
    }

    private void RegisterPhoneDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.empty_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(R.string.register_phone_title);
        dialogBuilder.setMessage(R.string.register_phone_message);
        dialogBuilder.setPositiveButton(R.string.open_browser, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(site + "/account/register/" + uniqueID));
                startActivity(browserIntent);
                CloseApplicationLate("GET USER 400");
            }
        });
        dialogBuilder.setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                CloseApplication("APP CLOSED");
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                CloseApplication("Dialog closed");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void NewMessage(Message message) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), "notify_001");

        mBuilder.setSmallIcon(R.drawable.ic_message_black_24dp);
        mBuilder.setContentTitle(message.getSender().getName() + ": " + message.getSubject());
        mBuilder.setContentText(message.getContent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mBuilder.setPriority(Notification.PRIORITY_MAX);
        }

        NotificationManager mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("notify_001",
                    "Wie Is Waar Message Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(0, mBuilder.build());
        }
        for(Message m: user.getMessages()) {
            if(m.getId().equals(message.getId())) {
                m.setRead(true);
                //UpdateUser(false);
                UpdateMessage(m);
            }
        }
    }

    private void UpdateMessage(Message message) {
        Call<Message> updateMessage = apiInterface.updateMessage(message);
        updateMessage.enqueue(new Callback<Message>() {
            @Override
            public void onResponse(Call<Message> call, Response<Message> response) {

            }

            @Override
            public void onFailure(Call<Message> call, Throwable t) {

            }
        });
    }

    private void DeleteCheckin() {
        user.setCheckin(null);
        Call<ResponseBody> deleteCheckin = apiInterface.deleteCheckin(user.getId());
        deleteCheckin.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    EmitCheckout(response.body().string());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    }else{
                        //deprecated in API 26
                        v.vibrate(500);
                    }
                }
                Toast.makeText(getApplicationContext(), R.string.checkout_success, Toast.LENGTH_SHORT).show();
                CloseApplication("DELETE CHECKIN");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.checkout_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("DELETE CHECKIN");
            }
        });
    }

    private void CreateCheckin() {
        CheckinPost checkin = new CheckinPost(user.getId(), stickerId);

        Call<ResponseBody> createCheckin = apiInterface.createCheckin(checkin);
        Log.i("API", "CREATE CHECKIN");
        createCheckin.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String message = response.body().string();

                    switch(message) {
                        default:
                            EmitCheckin(message);
                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            // Vibrate for 500 milliseconds
                            if (v != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                                }else{
                                    //deprecated in API 26
                                    v.vibrate(500);
                                }
                            }
                            Toast.makeText(getApplicationContext(), R.string.checkin_success, Toast.LENGTH_SHORT).show();
                            CloseApplication("CREATE CHECKIN SUCCESS");
                            break;
                        case "{\"message\":\"New sticker\"}":
                            GetCampuses();
                            break;
                        case "{\"message\":\"Location has no name.\"}":
                            GetCampuses();
                            break;
                        case "{\"message\":\"User does not exist\"}":

                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("error", t.getMessage());
                Toast.makeText(getApplicationContext(), R.string.checkin_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("CREATE CHECKIN FAIL");
            }
        });
    }

    private void CreateCampus(final Campus campus) {
        Call<Campus> createCampus = apiInterface.createCampus(campus);
        Log.i("API", "CREATE CAMPUS");
        createCampus.enqueue(new Callback<Campus>() {
            @Override
            public void onResponse(Call<Campus> call, Response<Campus> response) {
                campuses.add(response.body());
                Toast.makeText(getApplicationContext(), getString(R.string.create_campus_success) + campus.getName(), Toast.LENGTH_SHORT).show();
                createSegmentDialog(response.body());
            }

            @Override
            public void onFailure(Call<Campus> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.create_campus_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("CREATE CAMPUS");
            }
        });
    }

    private void UpdateCampus(final Campus campus, final Segment segment) {
        Call<Campus> updateCampus = apiInterface.updateCampus(campus);
        Log.i("API", "UPDATE CAMPUS");
        updateCampus.enqueue(new Callback<Campus>() {
            @Override
            public void onResponse(Call<Campus> call, Response<Campus> response) {
                Toast.makeText(getApplicationContext(), R.string.update_campus_success, Toast.LENGTH_SHORT).show();
                createLocationDialog(campus.getName(), segment);
            }

            @Override
            public void onFailure(Call<Campus> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.update_campus_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("UPDATE CAMPUS");
            }
        });
    }

    private void CreateSegment(final Campus campus, final Segment segment) {
        Call<Segment> createSegment = apiInterface.createSegment(segment);
        Log.i("API", "CREATE SEGMENT");
        createSegment.enqueue(new Callback<Segment>() {
            @Override
            public void onResponse(Call<Segment> call, Response<Segment> response) {
                campus.addSegment(response.body());
                Toast.makeText(getApplicationContext(), getString(R.string.create_segment_success) + segment.getName(), Toast.LENGTH_SHORT).show();
                UpdateCampus(campus, response.body());
            }

            @Override
            public void onFailure(Call<Segment> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.create_segment_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("CREATE SEGMENT");
            }
        });
    }

    private void UpdateSegment(Segment segment) {
        Call<Segment> updateSegment = apiInterface.updateSegment(segment);
        Log.i("API", "UPDATE SEGMENT");
        updateSegment.enqueue(new Callback<Segment>() {
            @Override
            public void onResponse(Call<Segment> call, Response<Segment> response) {
                Toast.makeText(getApplicationContext(), R.string.update_segment_success, Toast.LENGTH_SHORT).show();
                CreateCheckin();
            }

            @Override
            public void onFailure(Call<Segment> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.update_segment_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("UPDATE SEGMENT");
            }
        });
    }

    private void CreateLocation(final Segment segment, Location location) {
        Call<Location> createLocation = apiInterface.createLocation(location);
        Log.i("API", "CREATE LOCATION");
        createLocation.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Toast.makeText(getApplicationContext(), R.string.create_location_success, Toast.LENGTH_SHORT).show();
                segment.addLocation(response.body());
                UpdateSegment(segment);
            }

            @Override
            public void onFailure(Call<Location> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.create_location_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("CREATE LOCATION");
            }
        });
    }

    private void UpdateLocation(Location location) {
        Call<Location> updateLocation = apiInterface.updateLocation(location);
        Log.i("API", "UPDATE LOCATION");
        updateLocation.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Toast.makeText(getApplicationContext(), R.string.update_location_success, Toast.LENGTH_SHORT).show();
                CreateCheckin();
            }

            @Override
            public void onFailure(Call<Location> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.update_location_failure, Toast.LENGTH_SHORT).show();
                CloseApplication("UPDATE LOCATION");
            }
        });
    }

    private void GetCampuses() {
        Call<List<Campus>> getCampuses = apiInterface.doGetCampuses();
        Log.i("API", "GET CAMPUSES");
        getCampuses.enqueue(new Callback<List<Campus>>() {
            @Override
            public void onResponse(Call<List<Campus>> call, Response<List<Campus>> response) {
                campuses = response.body();
                newStickerDialog();
            }

            @Override
            public void onFailure(Call<List<Campus>> call, Throwable t) {
                CloseApplication("GET CAMPUSES");
            }
        });
    }

    private void GetLocationById(String id) {
        Call<Location> getLocationById = apiInterface.getLocationById(id);
        Log.i("API", "GET LOCATION BY ID");
        getLocationById.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Location location = response.body();
                if(location.getStickers().indexOf(stickerId) == -1) {
                    //not same location
                    CreateCheckin();
                }
                else {
                    //same location
                    showRemoveCheckinDialog();
                }
            }

            @Override
            public void onFailure(Call<Location> call, Throwable t) {
                CloseApplication("GET LOCATION BY ID");
            }
        });
    }

    private void showRemoveCheckinDialog() {
        Log.i("DIALOG", "REMOVE CHECKIN");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.empty_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle(R.string.remove_checkin);
        dialogBuilder.setMessage(R.string.remove_checkin_message);
        dialogBuilder.setPositiveButton(R.string.remove_checkin_positive_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                user.setCheckin(null);
                //UpdateUser(true);
                DeleteCheckin();
            }
        });
        dialogBuilder.setNegativeButton(R.string.remove_checkin_negative_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                CreateCheckin();
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                CloseApplication("Dialog closed");
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void newStickerDialog() {
        Log.i("DIALOG", "NEW STICKER");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.select_campus);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        for(Campus campus: campuses) {
            if(campus.isThuiswerk()) {
                arrayAdapter.insert(campus.getName(), 0);
            }
            else if (campus.isLunch()) {
                arrayAdapter.insert(campus.getName(), arrayAdapter.getCount());
            }
            else {
                if(arrayAdapter.getCount() > 0)
                    arrayAdapter.insert(campus.getName(), arrayAdapter.getCount()-1);
                else
                    arrayAdapter.add(campus.getName());
            }
        }
        arrayAdapter.add(getString(R.string.andere));
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication("CANCEL  NEW STICKER DIALOG");
                dialog.dismiss();
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String campus = arrayAdapter.getItem(which);
                if((campus != null && campus.equals(getString(R.string.andere))) && arrayAdapter.getCount() == which + 1) {
                    createCampusDialog();
                }
                else {
                    selectSegmentDialog(campus);
                }
            }
        });
        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                CloseApplication("Dialog closed");
            }
        });
        dialogBuilder.show();
    }

    private void createCampusDialog() {
        Log.i("DIALOG", "CREATE CAMPUS");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.create_campus_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(R.string.create_campus);

        final EditText edt = dialogView.findViewById(R.id.editCampus);
        final CheckBox isLunchCheck = dialogView.findViewById(R.id.checkIsLunch);
        final CheckBox isThuiswerkCheck = dialogView.findViewById(R.id.checkIsThuiswerk);

        dialogBuilder.setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edt.getText().toString();
                boolean isLunch = isLunchCheck.isChecked();
                boolean isThuiswerk = isThuiswerkCheck.isChecked();
                if (!name.trim().isEmpty()) {
                    Campus campus = new Campus("", name, isLunch, isThuiswerk, null);
                    CreateCampus(campus);
                } else {
                    createCampusDialog();
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newStickerDialog();
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                newStickerDialog();
            }
        });

        dialogBuilder.show();
    }

    private void selectSegmentDialog(final String campusName) {
        Log.i("DIALOG", "SELECT SEGMENT");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.select_segment);

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        for(Campus campus: campuses) {
            if(campus.getName().equals(campusName)) {
                for(Segment segment: campus.getSegments()) {
                    if(segment.isVergadering()) {
                        arrayAdapter.insert(segment.getName(), 0);
                    }
                    else {
                        arrayAdapter.add(segment.getName());
                    }
                }
            }
        }
        arrayAdapter.add(getString(R.string.andere));
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication("CANCEL SELECT SEGMENT DIALOG");
                dialog.dismiss();
            }
        });

        dialogBuilder.setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newStickerDialog();
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                newStickerDialog();
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String segment = arrayAdapter.getItem(which);
                if((segment != null && segment.equals(getString(R.string.andere))) && arrayAdapter.getCount() == which + 1) {
                    for(Campus campus: campuses) {
                        if(campus.getName().equals(campusName)) {
                            createSegmentDialog(campus);
                        }
                    }
                }
                else {
                    selectLocationDialog(campusName, segment);
                }

            }
        });

        dialogBuilder.show();
    }

    private  void createSegmentDialog(final Campus campus) {
        Log.i("DIALOG", "CREATE SEGMENT");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.create_segment_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(R.string.create_segment);

        final EditText edt = dialogView.findViewById(R.id.editSegment);
        final CheckBox isVergaderingCheck = dialogView.findViewById(R.id.checkIsVergadering);

        dialogBuilder.setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edt.getText().toString();
                boolean isVergadering = isVergaderingCheck.isChecked();
                if (!name.trim().isEmpty()) {
                    Segment segment = new Segment(null, name, isVergadering, null);
                    CreateSegment(campus, segment);
                } else {
                    createSegmentDialog(campus);
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectSegmentDialog(campus.getName());
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                selectSegmentDialog(campus.getName());
            }
        });

        dialogBuilder.show();
    }

    private void selectLocationDialog(final String campusName, final String segmentName) {
        Log.i("DIALOG", "SELECT LOCATION");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(R.string.select_location);
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        for(Campus campus: campuses) {
            if(campus.getName().equals(campusName)) {
                for(Segment segment: campus.getSegments()) {
                    if(segment.getName().equals(segmentName)) {
                        for(Location location: segment.getLocations()) {
                            arrayAdapter.add(location.getName());
                        }
                    }
                }
            }
        }
        arrayAdapter.add(getString(R.string.andere));
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication("CANCEL SELECT LOCATION DIALOG");
                dialog.dismiss();
            }
        });

        dialogBuilder.setPositiveButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectSegmentDialog(campusName);
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                selectSegmentDialog(campusName);
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> stickers = new ArrayList<>();
                stickers.add(stickerId);
                String locationName = arrayAdapter.getItem(which);

                if((locationName != null && locationName.equals(getString(R.string.andere))) && arrayAdapter.getCount() == which + 1) {
                    for(Campus campus: campuses) {
                        if(campus.getName().equals(campusName)) {
                            for(Segment segment: campus.getSegments()) {
                                if(segment.getName().equals(segmentName)) {
                                    createLocationDialog(campusName, segment);
                                }
                            }
                        }
                    }
                }
                else {
                    for(Campus campus: campuses) {
                        if (campus.getName().equals(campusName)) {
                            for (Segment segment : campus.getSegments()) {
                                if (segment.getName().equals(segmentName)) {
                                    for(Location location: segment.getLocations()) {
                                        if(location.getName().equals(locationName)) {
                                            if(location.getStickers() != null && location.getStickers().size() > 0) {
                                                location.addSticker(stickerId);
                                            }
                                            else {
                                                location.setStickers(stickers);
                                            }
                                            UpdateLocation(location);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
        dialogBuilder.show();
    }

    private void createLocationDialog(final String campusName, final Segment segment) {
        Log.i("DIALOG", "CREATE LOCATION");
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.create_location_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle(R.string.create_location);

        final EditText edt = dialogView.findViewById(R.id.editLocation);
        final CheckBox isDoNotDisturbCheck = dialogView.findViewById(R.id.checkDoNotDisturb);

        dialogBuilder.setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edt.getText().toString();
                boolean isDoNotDisturb = isDoNotDisturbCheck.isChecked();
                if (!name.trim().isEmpty()) {
                    List<String> stickers = new ArrayList<>();
                    stickers.add(stickerId);
                    Location location = new Location("", name, stickers, isDoNotDisturb);
                    CreateLocation(segment, location);
                } else {
                    createLocationDialog(campusName, segment);
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.back, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectLocationDialog(campusName, segment.getName());
            }
        });

        dialogBuilder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {
            @Override
            public void onCancel(DialogInterface dialog)
            {
                selectLocationDialog(campusName, segment.getName());
            }
        });

        dialogBuilder.show();
    }

    public void CloseApplication(String source) {
        Log.i("CLOSING", "Called from " + source);
        spinner.setVisibility(View.INVISIBLE);
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

    public void CloseApplicationLate(String source) {
        Log.i("CLOSING", "Called from " + source);
        spinner.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 21)
                    finishAndRemoveTask();
                else
                    finish();
                System.exit(0);
            }
        }, 15000);

    }

    private void EmitCheckin(String user) {
        JSONObject json = null;
        try {
            json = new JSONObject(user);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("checkin", json);
    }

    private void EmitCheckout(String user) {
        JSONObject json = null;
        try {
            json = new JSONObject(user);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("checkout", json);
    }
}