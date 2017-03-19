package com.shivagoud.pakkalocal;

import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.iid.FirebaseInstanceId;


import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int TIMEOUT_IN_SECONDS = 60;
    DatabaseReference mFirebaseDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final TextView statusView = (TextView) findViewById(R.id.statusText);
        final EditText nickView = (EditText) findViewById(R.id.nickNameView);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.myProgressBar);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        // Hide after some seconds
        final Handler handler  = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                statusView.setText("Timeout occurred!");
                progressBar.setVisibility(View.INVISIBLE);
                nickView.setVisibility(View.VISIBLE);
                fab.show();
            }
        };

        //FirebaseApp.initializeApp(getApplicationContext());
        FirebaseApp.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                statusView.setText("Connecting");
                fab.hide();
                progressBar.setVisibility(View.VISIBLE);
                nickView.setVisibility(View.INVISIBLE);
                handler.postDelayed(runnable,TIMEOUT_IN_SECONDS*1000);

                final String iitoken = FirebaseInstanceId.getInstance().getToken();

                if(iitoken == null) {
                    Log.d(TAG,"Failed to fetch instance id token");
                    return;
                }

                final Map<String, Object> user = new HashMap<>();
                user.put("id", FirebaseInstanceId.getInstance().getId());
                user.put("nick", nickView.getText().toString());
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
                        long time = System.currentTimeMillis()/1000;
                        if(!dataSnapshot.hasChild("time") || time > TIMEOUT_IN_SECONDS + (Long)dataSnapshot.child("time").getValue()) {
                            dataSnapshot.getRef().removeValue();
                        }else if(!dataSnapshot.getKey().equals(iitoken) && !dataSnapshot.hasChild("paired")) {
                            dataSnapshot.getRef().child("paired").setValue(user);

                            String opponent_nick = (String) dataSnapshot.child("nick").getValue();
                            statusView.setText("Paired with user: " + opponent_nick);

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
                                String opponent_iitoken = (String) dataSnapshot.child("paired").child("token").getValue();
                                String opponent_nick = (String) dataSnapshot.child("paired").child("nick").getValue();
                                Log.d(TAG, "connected to "+opponent_iitoken);
                                statusView.setText("Paired with user: " + opponent_nick);
                            }
                            removeProgressBar();
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        statusView.setText("Failed to connect.");
                        removeProgressBar();
                    }

                    private void removeProgressBar(){
                        handler.removeCallbacks(runnable);
                        progressBar.setVisibility(View.INVISIBLE);
                        nickView.setVisibility(View.VISIBLE);
                        fab.show();
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
