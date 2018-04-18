package com.wannes.digipresence;

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
import com.google.gson.Gson;
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

import org.json.JSONArray;
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

    private static final String TAG = "GoogleActivity";
    NfcAdapter mAdapter;
    private String stickerid = "";
    private static String uniqueID;
    //private static String site = "http://10.0.2.2:5000";
    private static String site = "https://agile-everglades-38755.herokuapp.com";
    private ProgressBar spinner;
    APIInterface apiInterface;
    private User user;
    private List<Campus> campuses = null;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(site);
        } catch (URISyntaxException e) {
            Log.i(TAG, e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiInterface = APIClient.getClient().create(APIInterface.class);

        spinner = findViewById(R.id.progressBar1);
        spinner.setVisibility(View.VISIBLE);
        uniqueID = readFile("UUID");
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
        }
        Log.i("ID", uniqueID);
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        if( mAdapter == null) {
            //nfc not supported on device
            scanQR();
        }

        if(mAdapter != null && !mAdapter.isEnabled()) {
            Log.i("NFC", "NFC is disabled");
            scanQR();
        }
        else {
            Log.i("NFC", "NFC is enabled");
            if(getIntent().getAction() == null) {
                CloseApplication();
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
                CloseApplication();
            }
            else {
                Log.d("MainActivity", "Scanned");
                Log.d("MainActivity", intentResult.getContents());
                stickerid = intentResult.getContents();
                GetUser();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent i = getIntent();
        if(i != null ) {
            byte[] tagId = i.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            StringBuilder hexdump = new StringBuilder();
            if(tagId != null) {
                for (byte aTagId : tagId) {
                    String x = Integer.toHexString(((int) aTagId & 0xff));
                    if (x.length() == 1) {
                        x = '0' + x;
                    }
                    hexdump.append(x);
                }
                stickerid = hexdump.toString();
                GetUser();
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
        final View dialogView = inflater.inflate(R.layout.show_phoneid_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("App opened");
        dialogBuilder.setMessage("Scan a nfc tag to checkin OR checkin in with QR.:");
        dialogBuilder.setPositiveButton("Use QR", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                scanQR();
            }
        });
        dialogBuilder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                CloseApplication();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void scanQR() {
        Log.i("open", "scan qr");
        IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    private String readFile(String filename) {
        try {
            FileInputStream fis = openFileInput(filename);
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

    private void GetUser() {
        Call<User> getUser = apiInterface.getUserByPhoneid(uniqueID.trim());
        getUser.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                user = response.body();
                // Check if user has a message
                if(user.getMessages().length > 0) {
                    for(Message m: user.getMessages()) {
                        if(!m.isRead()) {
                            NewMessage(m);
                        }
                    }
                }

                // Look if user is checked in
                if(user.getCheckin() == null) {
                    CreateCheckin();
                }
                else {
                    GetLocationById(user.getCheckin().getLocation().getId());
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(site + "/account/register/" + uniqueID));
                startActivity(browserIntent);
                CloseApplication();
            }
        });
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
                    "DigiPresence Message Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }

        if (mNotificationManager != null) {
            mNotificationManager.notify(0, mBuilder.build());
        }
        for(Message m: user.getMessages()) {
            if(m.getId().equals(message.getId())) {
                m.setRead(true);
                UpdateUser(false);
            }
        }
    }

    private void UpdateUser(final boolean close) {
        Call<User> updateUser = apiInterface.updateUser(user);
        updateUser.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if(close) {
                    CloseApplication();
                    EmitCheckout(response.body());
                }

            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if(close) {
                    CloseApplication();
                }
            }
        });
    }

    private void CreateCheckin() {
        CheckinPost checkin = new CheckinPost(user.getId(), stickerid);

        Call<ResponseBody> createCheckin = apiInterface.createCheckin(checkin);
        createCheckin.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String message = null;
                try {
                    message = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                switch(message) {
                    default:
                        EmitCheckin(response.body());
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
                        Toast.makeText(getApplicationContext(), "Succesfully checked in", Toast.LENGTH_SHORT).show();
                        CloseApplication();
                        break;
                    case "{\"message\":\"New sticker\"}":
                        GetCampuses();
                        break;
                    case "{\"message\":\"Location has no name.\"}":
                        GetCampuses();
                        break;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.i("error", t.getMessage());
                Toast.makeText(getApplicationContext(), "Something went wrong while checking in", Toast.LENGTH_SHORT).show();
                CloseApplication();
            }
        });
    }

    private void CreateCampus(final Campus campus) {
        Call<Campus> createCampus = apiInterface.createCampus(campus);
        createCampus.enqueue(new Callback<Campus>() {
            @Override
            public void onResponse(Call<Campus> call, Response<Campus> response) {
                campuses.add(response.body());
                createSegmentDialog(campus);
            }

            @Override
            public void onFailure(Call<Campus> call, Throwable t) {

            }
        });
    }

    private void UpdateCampus(Campus campus) {
        Call<Campus> updateCampus = apiInterface.updateCampus(campus);
        updateCampus.enqueue(new Callback<Campus>() {
            @Override
            public void onResponse(Call<Campus> call, Response<Campus> response) {
                Toast.makeText(getApplicationContext(), "Succesfully updated campus", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Campus> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong while updating campus", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void CreateSegment(final Campus campus, final Segment segment) {
        Call<Segment> createSegment = apiInterface.createSegment(segment);
        createSegment.enqueue(new Callback<Segment>() {
            @Override
            public void onResponse(Call<Segment> call, Response<Segment> response) {
                campus.addSegment(segment);
                UpdateCampus(campus);
                createLocationDialog(segment);
            }

            @Override
            public void onFailure(Call<Segment> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong while creating new segment", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void UpdateSegment(Segment segment) {
        Call<Segment> updateSegment = apiInterface.updateSegment(segment);
        updateSegment.enqueue(new Callback<Segment>() {
            @Override
            public void onResponse(Call<Segment> call, Response<Segment> response) {
                Toast.makeText(getApplicationContext(), "Succesfully updated segment", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFailure(Call<Segment> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong while updating segment", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void CreateLocation(final Segment segment, Location location) {
        Call<Location> createLocation = apiInterface.createlocation(location);
        createLocation.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Toast.makeText(getApplicationContext(), "Succesfully created new location", Toast.LENGTH_LONG).show();
                segment.addLocation(response.body());
                UpdateSegment(segment);
            }

            @Override
            public void onFailure(Call<Location> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong while creating new location", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void UpdateLocation(Location location) {
        Call<Location> updateLocation = apiInterface.updateLocation(location);
        updateLocation.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Toast.makeText(getApplicationContext(), "Succesfully added sticker to location", Toast.LENGTH_LONG).show();
                CloseApplication();
            }

            @Override
            public void onFailure(Call<Location> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Something went wrong while add sticker to location", Toast.LENGTH_LONG).show();
                CloseApplication();
            }
        });
    }

    private void GetCampuses() {
        Call<List<Campus>> getCampuses = apiInterface.doGetCampuses();
        getCampuses.enqueue(new Callback<List<Campus>>() {
            @Override
            public void onResponse(Call<List<Campus>> call, Response<List<Campus>> response) {
                campuses = response.body();
                newStickerDialog();
            }

            @Override
            public void onFailure(Call<List<Campus>> call, Throwable t) {
                CloseApplication();
            }
        });
    }

    private void GetLocationById(String id) {
        Call<Location> getLocationById = apiInterface.getLocationById(id);
        getLocationById.enqueue(new Callback<Location>() {
            @Override
            public void onResponse(Call<Location> call, Response<Location> response) {
                Location location = response.body();
                if(location.getStickers().indexOf(stickerid) == -1) {
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
                CloseApplication();
            }
        });
    }

    private void showRemoveCheckinDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.show_phoneid_dialog, null);
        dialogBuilder.setView(dialogView);

        dialogBuilder.setTitle("Checkin");
        dialogBuilder.setMessage("Checked in to the same location twice.");
        dialogBuilder.setPositiveButton("Checkout", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                user.setCheckin(null);
                UpdateUser(true);
            }
        });
        dialogBuilder.setNegativeButton("Checkin again", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
                CreateCheckin();
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    private void newStickerDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("New sticker scanned:\nSelect campus");

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
        arrayAdapter.add("Andere?");
        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication();
                dialog.dismiss();
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String campus = arrayAdapter.getItem(which);
                if((campus != null && campus.equals("Andere?")) && arrayAdapter.getCount() == which + 1) {
                    createCampusDialog();
                }
                else {
                    selectSegmentDialog(campus);
                }
            }
        });
        dialogBuilder.show();
    }

    private void createCampusDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.create_campus_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Create campus");

        final EditText edt = dialogView.findViewById(R.id.editCampus);
        final CheckBox isLunchCheck = dialogView.findViewById(R.id.checkIsLunch);
        final CheckBox isThuiswerkCheck = dialogView.findViewById(R.id.checkIsThuiswerk);

        dialogBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
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

        dialogBuilder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newStickerDialog();
            }
        });

        dialogBuilder.show();
    }

    private void selectSegmentDialog(final String campusName) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("New sticker scanned:\nSelect segment");

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
        arrayAdapter.add("Andere?");
        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication();
                dialog.dismiss();
            }
        });

        dialogBuilder.setPositiveButton("back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newStickerDialog();
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String segment = arrayAdapter.getItem(which);
                if((segment != null && segment.equals("Andere?")) && arrayAdapter.getCount() == which + 1) {
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
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.create_segment_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Create segment");

        final EditText edt = dialogView.findViewById(R.id.editSegment);
        final CheckBox isVergaderingCheck = dialogView.findViewById(R.id.checkIsVergadering);

        dialogBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
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

        dialogBuilder.setNegativeButton("Back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newStickerDialog();
            }
        });

        dialogBuilder.show();
    }

    private void selectLocationDialog(final String campusName, final String segmentName) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("New sticker scanned:\nSelect location");
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
        arrayAdapter.add("Andere?");
        dialogBuilder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication();
                dialog.dismiss();
            }
        });

        dialogBuilder.setPositiveButton("back", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectSegmentDialog(campusName);
            }
        });

        dialogBuilder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> stickers = new ArrayList<>();
                stickers.add(stickerid);
                String locationName = arrayAdapter.getItem(which);

                if((locationName != null && locationName.equals("Andere?")) && arrayAdapter.getCount() == which + 1) {
                    for(Campus campus: campuses) {
                        if(campus.getName().equals(campusName)) {
                            for(Segment segment: campus.getSegments()) {
                                if(segment.getName().equals(segmentName)) {
                                    createLocationDialog(segment);
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
                                                location.addSticker(stickerid);
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

    private void createLocationDialog(final Segment segment) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.create_location_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setTitle("Create location");

        final EditText edt = dialogView.findViewById(R.id.editLocation);
        final CheckBox isDoNotDisturbCheck = dialogView.findViewById(R.id.checkDoNotDisturb);

        dialogBuilder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = edt.getText().toString();
                boolean isDoNotDisturb = isDoNotDisturbCheck.isChecked();
                if (!name.trim().isEmpty()) {
                    Location location = new Location("", name, null, isDoNotDisturb);
                    CreateLocation(segment, location);
                } else {
                    createLocationDialog(segment);
                }
            }
        });

        dialogBuilder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CloseApplication();
            }
        });

        dialogBuilder.show();
    }

    public void CloseApplication() {
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

    private void EmitCheckin(ResponseBody user) {
        mSocket.emit("checkin", user);
    }

    private void EmitCheckout(User user) {
        Gson gson = new Gson();
        mSocket.emit("checkout", gson.toJson(user));
    }
}