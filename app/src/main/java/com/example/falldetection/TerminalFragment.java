package com.example.falldetection;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;


public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected {False, Pending, True}

    private String deviceAddress;
    private SerialService service;

    private String contactNumber;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private int heartRate, spo2;
    private Timestamp fallTime;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy' 'HH:mm:ss");

    FirebaseAuth mAuth;
    FirebaseDatabase fDatabase;
    DatabaseReference dRef;
    FirebaseUser currentUser;

    boolean fall = false;
    boolean abort = false;
    boolean fall_was_true = false;
    TextView BT_MSG;
    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        fDatabase = FirebaseDatabase.getInstance();
        dRef = fDatabase.getReference(currentUser.getDisplayName());

    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if (service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation")
    // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
        } catch (Exception ignored) {
        }
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if (initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        BT_MSG = (TextView) view.findViewById(R.id.BT_MSG);

        return view;
    }

    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            BT_MSG.setText("Connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            BT_MSG.setText("Couldn't Connect");
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
//            String msg = str + "\r\n";
            String msg = str;

            byte[] data;
            data = msg.getBytes();
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receive(byte[] message) {
        dRef.child("fall").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int val = dataSnapshot.getValue(Integer.class);
                if (val == 0){
                    fall = false;
                    if (fall_was_true){
                        abort = true;
                        fall_was_true = false;
                    }

                }
                else {
                    fall = true;
                    abort = false;
                }
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ " + fall + "abort: " + abort);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG,"Error while reading data");
            }
        });


        String msg = new String(message);
        String msg_to_save = msg.replace("\r\n", "");
        if (msg_to_save.length() > 1) {
            if (msg_to_save.startsWith("Fall")) {
                if (!fall) {
                    fall_was_true = true;
                    dRef.child("fall").setValue(1); // added for abort
                    Fragment fragment = new Timer();
                    Bundle args = new Bundle();
                    args.putString("device", deviceAddress);
                    fragment.setArguments(args);
                    getFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "timer").commit();
                }
            }
            else {
                String[] parts = msg_to_save.split(",");
                parts = clean_str(parts);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                String t = sdf.format(now);
                heartRate = Integer.parseInt(parts[0]);
                spo2 = Integer.parseInt(parts[1]);
                dRef.child("heartrate").child(t).setValue(heartRate);
                dRef.child("spo2").child(t).setValue(spo2);

                if (abort){
                    send("abort");
                    abort = false;
                    fall_was_true = false;
                }



            }
        }


    }

    private String[] clean_str(String[] stringsArr){
        for (int i = 0; i < stringsArr.length; i++)  {
            stringsArr[i]=stringsArr[i].replaceAll("H","");
            stringsArr[i]=stringsArr[i].replaceAll("B","");
            stringsArr[i]=stringsArr[i].replaceAll("S","");
            stringsArr[i]=stringsArr[i].replaceAll("P","");
            stringsArr[i]=stringsArr[i].replaceAll("O","");
            stringsArr[i]=stringsArr[i].replaceAll(":","");
            stringsArr[i]=stringsArr[i].replaceAll(" ","");
        }
        return stringsArr;
    }


    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        BT_MSG.setText("Connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        BT_MSG.setText("Connection Failed");
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        try {
            receive(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSerialIoError(Exception e) {
        BT_MSG.setText("Connection Lost");
        disconnect();
    }

}

