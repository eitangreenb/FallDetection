package com.example.falldetection;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Timer extends Fragment {
    public int counter = 15;
    Button abort;
    TextView counterText;
    TextView msgText;
    TextView msgText2;
    String contactNumber;
    String deviceAddress;
    FirebaseAuth mAuth;
    FirebaseDatabase fDatabase;
    DatabaseReference dRef;
    FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

    }
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        deviceAddress = getArguments().getString("device");

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        String name = currentUser.getDisplayName();
        fDatabase = FirebaseDatabase.getInstance();
        dRef = fDatabase.getReference(name);

        dRef.child("selected").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                contactNumber = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG,"Error while reading data");
            }
        });


        View view = inflater.inflate(R.layout.timer, container, false);

        abort= (Button) view.findViewById(R.id.button);
        counterText= (TextView) view.findViewById(R.id.textView2);
        msgText = (TextView) view.findViewById(R.id.textView3);
        msgText2 = (TextView) view.findViewById(R.id.textView4);
        CountDownTimer myTimer = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                counterText.setText(String.valueOf(counter));
                counter--;
            }

            @Override
            public void onFinish() {
                counterText.setText("");
                msgText2.setText("");
                msgText.setText("Calling For Help!");
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + contactNumber));
                startActivity(callIntent);

                Bundle args = new Bundle();
                args.putString("device", deviceAddress);
                Fragment fragment = new Graph();
                fragment.setArguments(args);
                getFragmentManager().beginTransaction().replace(R.id.fragmentTimer, fragment, "graph").commit();

            }
        };
        myTimer.start();
        abort.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                myTimer.cancel();

                dRef.child("fall").setValue(0); // added for abort

                Bundle args = new Bundle();
                args.putString("device", deviceAddress);
                Fragment fragment = new TerminalFragment();
                fragment.setArguments(args);
                getFragmentManager().beginTransaction().remove(Timer.this).commit();
//                Fragment fragmentToKill = getFragmentManager().findFragmentByTag("terminal");
//                if(fragmentToKill != null)
//                    getFragmentManager().beginTransaction().remove(fragment).commit();
//
//                getFragmentManager().beginTransaction().add(R.id.fragment, fragment, "terminal").commit();

                Fragment frg = getFragmentManager().findFragmentByTag("terminal");
                if(frg != null) {
                    getFragmentManager().beginTransaction().detach(frg);
                    getFragmentManager().beginTransaction().attach(frg).commit();
                }

            }
        });

        return view;
    }

}
