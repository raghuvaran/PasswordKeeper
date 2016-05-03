package com.rvlabs.passwordkeeper;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Launch activity, also performs user authentication to login to app
 */
public class LoginActivity extends AppCompatActivity {


    SQLiteDatabase mydatabase;
    Cursor c;
    EditText username;
    EditText password;
    SharedPreferences sharedPref;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Acts as a launch and login screen

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
		/**creates tables if they weren't created yet*/
        mydatabase = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS users (userid INTEGER PRIMARY KEY AUTOINCREMENT, email varchar, passphrase varchar, fullName varchar, syncStatus int);");

        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS category (categoryid INTEGER PRIMARY KEY AUTOINCREMENT, categoryName varchar);");
        
        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS action (actionid INTEGER PRIMARY KEY AUTOINCREMENT, actionName varchar);");

        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS entryTable (entryid INTEGER PRIMARY KEY AUTOINCREMENT, title varchar, username varchar, password varchar, url varchar, notes varchar, categoryid int, userid int, deleted int, syncStatus int, FOREIGN KEY (categoryid) REFERENCES category(categoryid), FOREIGN KEY (userid) REFERENCES users(userid));");

        mydatabase.execSQL("CREATE TABLE IF NOT EXISTS log (logid INTEGER PRIMARY KEY AUTOINCREMENT, entryid int, actionid int, logTime timestamp , userid int, FOREIGN KEY (entryid) REFERENCES entryTable(entryid), FOREIGN KEY (actionid) REFERENCES action(actionid), FOREIGN KEY (userid) REFERENCES users(userid));");
		/**Check for any user entry if present*/
        c = mydatabase.rawQuery("SELECT COUNT(*) AS C FROM users",null);c.moveToFirst();
        if(Integer.parseInt(c.getString(c.getColumnIndex("C"))) == 0){
            /**if no user entry is present, initialize the database*/
            mydatabase.execSQL("INSERT INTO users (userid, email, passphrase, fullName, syncStatus) VALUES ('1', 'admin', 'admin', 'admin', '0')");

            //mydatabase.execSQL("INSERT INTO entryTable VALUES ('1','Amazon','iamusername','closeYourEyes','amazon.com','You have $5 credit left','1','1','0','0')");
            //mydatabase.execSQL("INSERT INTO entryTable VALUES ('2','Netflix','userHalley','kiddingagain','netflix.com','30 Days subscription','9','1','0','0')");

            mydatabase.execSQL("INSERT INTO action (actionName) VALUES ('created')");
            mydatabase.execSQL("INSERT INTO action (actionName) VALUES ('updated')");
            mydatabase.execSQL("INSERT INTO action (actionName) VALUES ('deleted')");
            mydatabase.execSQL("INSERT INTO action (actionName) VALUES ('copied username')");
            mydatabase.execSQL("INSERT INTO action (actionName) VALUES ('copied password')");

            mydatabase.execSQL("INSERT INTO category VALUES ('1','Internet')");
            mydatabase.execSQL("INSERT INTO category VALUES ('2','Email')");
            mydatabase.execSQL("INSERT INTO category VALUES ('3','Education')");
            mydatabase.execSQL("INSERT INTO category VALUES ('4','Bank')");
            mydatabase.execSQL("INSERT INTO category VALUES ('5','Electricity')");
            mydatabase.execSQL("INSERT INTO category VALUES ('6','Payments')");
            mydatabase.execSQL("INSERT INTO category VALUES ('7','Medical')");
            mydatabase.execSQL("INSERT INTO category VALUES ('8','Work')");
            mydatabase.execSQL("INSERT INTO category VALUES ('9','Others')");

            //mydatabase.execSQL("INSERT INTO log VALUES ('1','1','1', '2016-02-13 22:03:21', '1')");
            //mydatabase.execSQL("INSERT INTO log VALUES ('2','1','2', '2016-02-13 22:26:22', '1')");
            //mydatabase.execSQL("INSERT INTO log VALUES ('3','2','1', '2016-02-13 22:03:21', '1')");
            //mydatabase.execSQL("INSERT INTO log VALUES ('4','2','5', '2016-02-13 23:07:24', '1')");



        }

        mydatabase.close();
		/**check if it is the first time run of app*/
        sharedPref = getSharedPreferences("myPref", 0);
        if(sharedPref.getBoolean("my_first_time",true)){
			/**if first run then throw an alert window with tip*/
            new  AlertDialog.Builder(LoginActivity.this)
                .setTitle("New User?")
                .setMessage("If you haven't set up any password yet, do tap on top right menu")
                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();

        }else{
			/**if NOT first run then populate last user used username (if any)*/
            try{
                username = (EditText) findViewById(R.id.email);
                password = (EditText) findViewById(R.id.passphrase);

                username.setText(sharedPref.getString("username", ""));
                if(!username.getText().toString().isEmpty()){
                    password.requestFocus();
                }

            }catch (Exception e){

            }
        }




    }

    /** The method is called to validate the login credentials
     *
     * On successful authentication starts MainActivity intent
     * @param view
     */
    public void validate (View view){

        username = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.passphrase);

		/**save attempted username to sharedPref for next time login username pre-populate*/
        sharedPref.edit().putString("username",username.getText().toString()).commit();



        mydatabase = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
		/**query for attempted username*/
         c = mydatabase.rawQuery("SELECT * FROM users WHERE email = '" + Validation.replaceApostrophe(username.getText().toString()) + "'",null);

            int userid_id = c.getColumnIndex("userid");
            int email_id = c.getColumnIndex("email");
            int passphrase_id = c.getColumnIndex("passphrase");
			/**if atleast one entry found proceed to match password*/
            if (c.getCount() == 1) {
                c.moveToFirst();
                while (!c.isAfterLast()) {


                    System.out.println("Userid: " + c.getString(userid_id) + " and email: " + c.getString(email_id) + " and pass: " + c.getString(passphrase_id));

                    Log.i("pass_db", c.getString(passphrase_id));
                    Log.i("pass_input",Validation.replaceApostrophe(password.getText().toString()));
					/**if authenticated successfully launch MainActivity*/
                    if (c.getString(email_id).equals(username.getText().toString()) && c.getString(passphrase_id).equals(password.getText().toString())) {
                        Toast.makeText(getApplicationContext(), "Login successful", Toast.LENGTH_SHORT).show();
                        mydatabase.close();
                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("userid", Integer.parseInt(c.getString(userid_id)));
                        startActivity(i);
                        finish();
                        break;

                    } else {
                        Toast.makeText(getApplicationContext(), "Login attempt failed!", Toast.LENGTH_SHORT).show();
                        password.setError("Incorrect password");
                    }
                    c.moveToNext();

                }

            } else if (c.getCount() > 1) {
                Toast.makeText(getApplicationContext(), "More than one match found!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "No matches found!", Toast.LENGTH_SHORT).show();
                username.setError("You might have to correct this");
            }
        }

	/**populate username on resuming login activity*/
    @Override
    public void onResume() {
        super.onResume();

        username = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.passphrase);

        username.setText(sharedPref.getString("username", ""));
        if(!username.getText().toString().isEmpty()){password.requestFocus();}


    }
	/**if first time run then add new user login button to menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /** Inflate the menu; this adds items to the action bar if it is present.*/
        getMenuInflater().inflate(R.menu.menu_login, menu);
        sharedPref = getSharedPreferences("myPref", 0);
        if(sharedPref.getBoolean("my_first_time",true)){

        menu.add("New user?").setIntent(new Intent(this, Signup.class));

        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.info) {
            Intent i = new Intent(getApplicationContext(),AboutMe.class);
            startActivity(i);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }
}
