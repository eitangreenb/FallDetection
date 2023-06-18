package com.example.falldetection;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
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

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Graph extends Fragment {
    String deviceAddress;
    FirebaseAuth mAuth;
    FirebaseDatabase fDatabase;
    DatabaseReference dRef;
    FirebaseUser currentUser;

    LineChart mpLineChart;
    LineDataSet lineDataSetHR = new LineDataSet(null,"Heart Rate");
    LineDataSet lineDataSetSPO2= new LineDataSet(null,"SPO2");
    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
    LineData data;

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

        View view = inflater.inflate(R.layout.graph, container, false);

        mpLineChart = view.findViewById(R.id.heartChart);

        dRef.child("heartrate").limitToLast(24).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Entry> dataVals = new ArrayList<>();
                int t = 0;
                if (snapshot.hasChildren()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        int val = ds.getValue(Integer.class);
                        if (val == -999) {
                            t++;
                            continue;
                        }
                        t++;
                        dataVals.add(new Entry(t, val));
                    }
                    lineDataSetHR.setValues(dataVals);
                    if (dataSets.size() > 0) {
                        if (dataSets.get(0).getLabel().equals("Heart Rate"))
                            dataSets.remove(0);
                        else if (dataSets.size() > 1 && dataSets.get(1).getLabel().equals("Heart Rate"))
                            dataSets.remove(1);
                    }
                    lineDataSetHR.setColor(Color.rgb(0, 211, 21));
                    lineDataSetHR.setCircleColor(Color.rgb(44, 44, 44));
                    lineDataSetHR.setLineWidth(2);
                    dataSets.add(lineDataSetHR);
                    data = new LineData(dataSets);
                    mpLineChart.clear();
                    mpLineChart.setData(data);
                    mpLineChart.invalidate();
                }
                else{
                    mpLineChart.clear();
                    mpLineChart.invalidate();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Error while reading data");

            }
        });


        dRef.child("spo2").limitToLast(24).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Entry> dataVals = new ArrayList<>();
                int t = 0;
                if (snapshot.hasChildren()){
                    for (DataSnapshot ds : snapshot.getChildren()){
                        int val = ds.getValue(Integer.class);
                        if (val == -999) {
                            t++;
                            continue;
                        }
                        t++;
                        dataVals.add(new Entry(t, val));
                    }
                    lineDataSetSPO2.setValues(dataVals);
                    if (dataSets.size() > 0) {
                        if (dataSets.get(0).getLabel().equals("SPO2"))
                            dataSets.remove(0);
                        else if (dataSets.size() > 1 && dataSets.get(1).getLabel().equals("SPO2"))
                            dataSets.remove(1);
                    }
                    lineDataSetSPO2.setColor(Color.rgb(0, 94, 255));
                    lineDataSetSPO2.setCircleColor(Color.rgb(117, 117, 117));
                    lineDataSetSPO2.setLineWidth(2);
                    dataSets.add(lineDataSetSPO2);
                    data = new LineData(dataSets);
                    mpLineChart.clear();
                    mpLineChart.setData(data);
                    mpLineChart.invalidate();
                }
                else{
                    mpLineChart.clear();
                    mpLineChart.invalidate();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG,"Error while reading data");

            }
        });

        return view;
    }

}
