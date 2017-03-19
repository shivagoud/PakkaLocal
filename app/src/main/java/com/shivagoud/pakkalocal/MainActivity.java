package com.shivagoud.pakkalocal;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    DatabaseReference mFirebaseDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final TextView statusView = (TextView) findViewById(R.id.statusText);

        //FirebaseApp.initializeApp(getApplicationContext());
        FirebaseApp.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                statusView.setText("Connecting");


                final String iitoken = FirebaseInstanceId.getInstance().getToken();

                if(iitoken == null) {
                    Log.d(TAG,"Failed to fetch instance id token");
                    return;
                }

                Map<String, Object> user = new HashMap<>();
                user.put("id", FirebaseInstanceId.getInstance().getId());
                user.put("token", iitoken);
                user.put("time", System.currentTimeMillis()/1000);
                final DatabaseReference selfRef = mFirebaseDatabaseReference.child("queued_users")
                        .child(iitoken);
                selfRef.setValue(user);

                final Query qUsers = mFirebaseDatabaseReference.child("queued_users");
                qUsers.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                        //match with the user if not self, is single, and is not expired
                        long time = System.currentTimeMillis()/1000 - 5*60;
                        if(!dataSnapshot.hasChild("time") || time > 300 + (Long)dataSnapshot.child("time").getValue()) {
                            dataSnapshot.getRef().removeValue();
                        }else if(!dataSnapshot.getKey().equals(iitoken) && !dataSnapshot.hasChild("paired")) {
                            dataSnapshot.getRef().child("paired").setValue(iitoken);

                            Snackbar.make(view, dataSnapshot.toString(), Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();

                            selfRef.removeValue();
                        }


                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                        //Someone posted a pairing request, let me accept it
                        if(dataSnapshot.getKey().equals(iitoken) && dataSnapshot.hasChild("paired")) {
                            selfRef.removeValue();
                        }
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        if(dataSnapshot.getKey().equals(iitoken)){
                            qUsers.removeEventListener(this);
                            if(dataSnapshot.hasChild("paired")) {
                                String opponent = (String) dataSnapshot.child("paired").getValue();
                                statusView.setText("Connected with user: " + opponent);
                            }else{
                                statusView.setText("Failed to connect.");
                            }
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        statusView.setText("Failed to connect.");
                    }
                });


            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
