package com.rvlabs.passwordkeeper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Registers new users to the database, thereby providing access to the app
 */

public class Signup extends AppCompatActivity {

    EditText fullName, email, passphrase;
    SQLiteDatabase db;
    SharedPreferences sharedPref;

    SyncUser syncUser;

    /**
     * Launches the form
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fullName = (EditText) findViewById(R.id.fullName);
        email = (EditText) findViewById(R.id.reg_email);
        passphrase = (EditText) findViewById(R.id.reg_password);


    }

    /**
     * Writes into the database upon successful validation of data
     * @param view
     */

    public int onSignUp (View view){

        //Ensure email is not empty
        if (!email.getText().toString().isEmpty()){
            //Device is online
            if(isOnline()) {

                    db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
                //search for any conflicting user
                    Cursor c = db.rawQuery("SELECT Count(*) AS COUNT FROM users WHERE email = '" + email.getText().toString() + "'", null);
                    c.moveToFirst();
                    if (Integer.parseInt(c.getString(c.getColumnIndex("COUNT"))) > 0) {
                        email.setError("Username already exists");
                    } else if (passphrase.getText().toString().isEmpty()) {
                        passphrase.setError("Password is required");
                    } else if (fullName.getText().toString().isEmpty()) {
                        fullName.setError("Name is also required");
                    } else {
                        //try to add user to EXT database
                        syncUser = new SyncUser();
                        String result = new String();
                try {
                    //if successful result contains userid of new user
                    result = syncUser.execute(getResources().getString(R.string.php_syncUser)+"?email="+email.getText().toString()+"&passphrase="+passphrase.getText().toString()+"&fullName="+fullName.getText().toString()).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }//check if php communication was successful
                if(result.isEmpty() || result.equalsIgnoreCase("Things didn't go expected")){
                    Toast.makeText(Signup.this, "Unable to establish connection. Try again!", Toast.LENGTH_SHORT).show();return 0;
                    //check if there is a conflict with EXT database users
                }else if(result.equalsIgnoreCase("failed")){
                    email.setError("Username already exists");
                    return 0;
                }else { //insert into internal database
                        db.execSQL("INSERT INTO users (userid, email, passphrase, fullName, syncStatus) VALUES ('"+result+"', '" + email.getText().toString() + "', '" + passphrase.getText().toString() + "', '" + fullName.getText().toString() + "', '0');");
                    /**
                     * Changes the FIRST_RUN variable to false
                     */
                        sharedPref = getSharedPreferences("myPref", 0);
                        if (sharedPref.getBoolean("my_first_time", true)) {
                            sharedPref.edit().putBoolean("my_first_time", false).commit();
                            sharedPref.edit().putString("username", email.getText().toString()).commit();
                        }

                        Toast.makeText(Signup.this, "You are now authorized to login", Toast.LENGTH_SHORT).show();
                        finish();
                        db.close();
                    }
                }

            }else{//if device has not network
                Toast.makeText(Signup.this, "Network unavailable! Try agaian", Toast.LENGTH_SHORT).show();
            }
        }else{//if email field is left blank
            email.setError("Username is required");
        }
        return 1;

    }

    /**
     * Syncs user details or more appropriately registers user on external database
     */
    public class SyncUser extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                String result = new String("");

                InputStream in = urlConnection.getInputStream();

                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1){

                    char current = (char) data;
                    result += current;

                    data = reader.read();

                }
                Log.i("result",result);
                return(result);

                //returns php output


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Things didn't go expected"; //if any error occurs in above execution


        }
    }
    /**
     * Determines if the device is internet able.
     * From: Sean's code. Thanks :)
     */
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
