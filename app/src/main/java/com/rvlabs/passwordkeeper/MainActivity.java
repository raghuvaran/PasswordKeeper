package com.rvlabs.passwordkeeper;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


/**
 *  Main activity is the display activity
 */

public class MainActivity extends AppCompatActivity {



    SQLiteDatabase db;
    List<String> entries = new ArrayList<>();
    ListView listview;
    ArrayAdapter arrayAdapter;
    String updateQuery;
    Spinner category;
    List<String> catList;
    ArrayAdapter<String> spinAdapter;

    Map<Integer,Integer> listIndex;
    Cursor cursor;

    int itemPos =0, user_id,action_deleted=3, action_copyUser=4, action_copyPass=5;
    TextView title;
    SharedPreferences sharedPref, settingsPref;

    SwipeRefreshLayout swipeRefreshLayout;

        /**
         *This method is called when this activity is called
         *
         * It loads the listview & arrayadapter from database
         *
         */
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent i = getIntent();
        user_id = i.getIntExtra("userid",0);
        Log.i("user_id", String.valueOf(user_id));

        swipeRefreshLayout =(SwipeRefreshLayout) findViewById(R.id.swipeRefresh);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                if(onSync()){Toast.makeText(MainActivity.this, "Sync successful", Toast.LENGTH_SHORT).show();}else Toast.makeText(MainActivity.this, "Network unavailable! Try agaian", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);

            }
        });


        category = (Spinner) findViewById(R.id.viewCategory);
        catList = new ArrayList<String>();
        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

        /**Populating spinner starts*/
        Cursor c = db.rawQuery("SELECT categoryName FROM category", null);
        c.moveToFirst();
        catList.add("All");

        while (!c.isAfterLast()){
            catList.add(c.getString(c.getColumnIndex("categoryName")));
            c.moveToNext();
        }

        spinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,catList);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        category.setAdapter(spinAdapter);
        /**populating spinner ends*/
        db.close();


        title = (TextView) findViewById(R.id.ActTitle);
       listview = (ListView) findViewById(R.id.listView);
       arrayAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1,entries);
        listview.setAdapter(arrayAdapter);
        arrayAdapter.notifyDataSetChanged();
        updateQuery = new String("SELECT * FROM entryTable WHERE deleted = '0'");


		//update listview onCreate of activity
        updateList(updateQuery);

        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(position !=0) {
                    updateQuery = new String("SELECT * FROM entryTable WHERE deleted = '0' AND categoryid = '" + position + "'");
                }else {updateQuery = new String("SELECT * FROM entryTable WHERE deleted = '0'");}
                updateList(updateQuery);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
		/**notify to long press on list for options on FIRST TIME RUN ONLY*/
        sharedPref = getSharedPreferences("myPref", 0);
        if(sharedPref.getBoolean("my_first_time",true)){

            Toast.makeText(MainActivity.this, "Long press to edit entry!", Toast.LENGTH_LONG).show();
            sharedPref.edit().putBoolean("my_first_time", false).commit();
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(MainActivity.this, AddEntry.class);
                in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                in.putExtra("userid", user_id);
                startActivity(in);

            }
        });

		/**on click on list view prompt popup menu to copy to clipboard*/
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemPos = position;

                PopupMenu popupMenu = new PopupMenu(getApplicationContext(), view);
                popupMenu.getMenuInflater()
                        .inflate(R.menu.pop_menu2, popupMenu.getMenu());

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        switch (item.getItemId()) {
                            /**copy username to clipboard*/
                            case R.id.pop_username:
                                if (copyToClip(itemPos, 0)) {
                                    Toast.makeText(MainActivity.this, "Username copied to clipboard", Toast.LENGTH_SHORT).show();
                                }


                                return true;
                            /**copy password to clipboard	*/
                            case R.id.pop_password:
                                if (copyToClip(itemPos, 1)) {
                                    Toast.makeText(MainActivity.this, "Password copied to clipboard", Toast.LENGTH_SHORT).show();
                                }
                                return true;

                        }

                        return true;
                    }
                });
                popupMenu.show();
            }
        });
		/**on long click display edit/delete options*/
        listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                itemPos = position;
                PopupMenu pop = new PopupMenu(getApplicationContext(), view);

                pop.getMenuInflater()
                        .inflate(R.menu.pop_menu, pop.getMenu());

                pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
							/**opens EditEntry activity*/
                            case R.id.pop_edit:

                                Intent i = new Intent(getApplicationContext(), EditEntry.class);

                                i.putExtra("listPosition", listIndex.get(itemPos));
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                i.putExtra("userid",user_id);
                                startActivity(i);
                                return true;
							/** Deletes selected entry on confirmation*/
                            case R.id.pop_delete:
                                new AlertDialog.Builder (MainActivity.this)
                                        .setTitle("Delete entry")
                                        .setMessage("Are you sure?")
                                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                //Stay cool
                                            }
                                        })
                                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                deleteEntry(listIndex.get(itemPos));
                                            }
                                        })
                                        .show();
                                //Toast.makeText(getApplicationContext(),"Code under construction",Toast.LENGTH_SHORT).show();

                                return true;
							/**opens logs of the selected entry*/
                            case R.id.pop_viewLogs:
                                startActivity( new Intent(MainActivity.this, ViewLog.class).putExtra("entryid",listIndex.get(itemPos)).putExtra("userid",user_id));
                        }

                        return true;
                    }
                });
                pop.show();


                return true;
            }
        });

        settingsPref = PreferenceManager.getDefaultSharedPreferences(this);

       
        try {
            if(settingsPref.getBoolean("syncOnLogin",false)){
                if(onSync()){Toast.makeText(MainActivity.this, "Sync successful", Toast.LENGTH_SHORT).show();}else Toast.makeText(MainActivity.this, "Network unavailable! Try agaian", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {}



    }


    @Override
    public void onResume(){
        super.onResume();
        updateList(updateQuery);
        //Toast.makeText(MainActivity.this, "OnResuming :P", Toast.LENGTH_SHORT).show();
    }

/**
 * Updates the listview
 */
    public void updateList (String query){

        try {
            if(settingsPref.getString("sortListView","0").equalsIgnoreCase("1")){
                query += " ORDER BY entryid DESC";

            }else if(settingsPref.getString("sortListView","0").equalsIgnoreCase("2")){
                query += " ORDER BY title";

            }else if(settingsPref.getString("sortListView","0").equalsIgnoreCase("3")){
                query += " ORDER BY title DESC";

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

//        Log.i("setting",settingsPref.getString("sortListView","0"));

        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
        cursor = db.rawQuery(query, null);


        cursor.moveToFirst();


        int entryid_id = cursor.getColumnIndex("entryid");
        int title_id = cursor.getColumnIndex("title");

        int counter = 0;
        listIndex = new HashMap<Integer,Integer>();
        entries.clear();
        if (cursor.getCount() == 0){
            entries.clear();
            arrayAdapter.notifyDataSetChanged();
        } else {
            while (!cursor.isAfterLast()) {

                entries.add(cursor.getString(title_id));

                listIndex.put(counter, Integer.parseInt(cursor.getString(entryid_id)));
                arrayAdapter.notifyDataSetChanged();

                cursor.moveToNext();
                counter++;
            }
        }

        db.close();

    }

    /**
     * Deletes the entry at position from database
     * @param position
     */
    public void deleteEntry (int position){
        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
        db.execSQL("UPDATE entryTable SET deleted = '1', syncStatus = '0' WHERE entryid = '" + position + "'");
        /**log entry*/
        db.execSQL("INSERT INTO log (logTime, entryid, actionid, userid) VALUES ('" + Validation.getCurrentTimeStamp() + "', '" +
                        position + "', '" + action_deleted + "', '" + user_id + "')"

        );
        db.close();
        updateList(updateQuery);
        try {
            if(settingsPref.getBoolean("syncOnAction",false)){
                onSync();
                //Toast.makeText(MainActivity.this, "SyncOnAction", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * Copies content to clipboard
     */

    public boolean copyToClip (int position,int casenum){

        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
        Cursor c;

        if (casenum == 0) {
            Log.i("Userquery","SELECT username AS VAL FROM entryTable WHERE entryid = '" + listIndex.get(position) + "'");
            c = db.rawQuery("SELECT username AS VAL FROM entryTable WHERE entryid = '" + listIndex.get(position) + "'", null);
            //log entry
            db.execSQL("INSERT INTO log (logTime, entryid, actionid, userid) VALUES ('"+Validation.getCurrentTimeStamp()+"', '"+
                            listIndex.get(position)+"', '"+action_copyUser+"', '"+user_id+"')"

            );
        }else{

                 c = db.rawQuery("SELECT password AS VAL FROM entryTable WHERE entryid = '"+listIndex.get(position)+"'",null);
            db.execSQL("INSERT INTO log (logTime, entryid, actionid, userid) VALUES ('"+Validation.getCurrentTimeStamp()+"', '"+
                            listIndex.get(position)+"', '"+action_copyPass+"', '"+user_id+"')"

            );
                        }
        c.moveToFirst();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (!c.getString(c.getColumnIndex("VAL")).isEmpty()) {

            ClipData clip = ClipData.newPlainText("Safe", c.getString(c.getColumnIndex("VAL")));
            clipboard.setPrimaryClip(clip);
            db.close();
            return true;
        }else {
            Toast.makeText(MainActivity.this, "You haven't stored any value", Toast.LENGTH_SHORT).show();
            db.close();
            return false;
        }



    }
	/**
     * Creates menu items programatically*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);

        menu.add(0,12,3,"View Logs");
        /*menu.add(0, 11, 2, "Change Password");*/
        menu.add(1,1,1,"Sync").setIcon(android.R.drawable.ic_popup_sync).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0,10,1,"Settings");

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == 1) {
            if(onSync()){Toast.makeText(MainActivity.this, "Sync successful", Toast.LENGTH_SHORT).show();}else Toast.makeText(MainActivity.this, "Network unavailable! Try agaian", Toast.LENGTH_SHORT).show();
            return true;
        }

        //noinspection SimplifiableIfStatement
        if (id == R.id.info) {
            Intent i = new Intent(getApplicationContext(),AboutMe.class);
            startActivity(i);
            return true;
        }
		//viewing entire log
        if (id == 12) {
            Intent i = new Intent(this, ViewLog.class);
            i.putExtra("userid", user_id);
            i.putExtra("viewType", 1);
            startActivity(i);
        }

        if (id == 10) {
            Intent i = new Intent(this, SettingsActivity.class);
            i.putExtra("userid", user_id);

            startActivity(i);
        }
		/**Changing password*/
        if(id == 11){

            AlertDialog.Builder alertBox = new AlertDialog.Builder(MainActivity.this);

            alertBox.setTitle("Change Password");
            final TextInputLayout newPass = new TextInputLayout(MainActivity.this);
            final EditText editText = new EditText(MainActivity.this);
            newPass.setHint("Password");

            editText.setInputType(129);

            LinearLayout.LayoutParams ll =  new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            editText.setLayoutParams(ll);
            newPass.addView(editText);
            alertBox.setView(newPass)
            .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    String password = Validation.replaceApostrophe(editText.getText().toString());



                    db = getApplicationContext().openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

                    db.execSQL("UPDATE users SET passphrase = '"+password+"', syncStatus = '0' WHERE userid = '"+user_id+"'");

                    db.close();
                    Toast.makeText(MainActivity.this, "I have changed it for you ;)", Toast.LENGTH_SHORT).show();
                }
            })
            .show();


        }



        return super.onOptionsItemSelected(item);
    }

    public void randomTip (String word){

        String[] letters = word.split("");
        String temp;

        for(int i =word.length()-1, j=0 ; i >0 ; i++){
            j =  (int) Math.random()*(i+1);
            temp = letters[i];
            letters[i] = letters[j];
            letters[j]= temp;
                    }
        title.setText(TextUtils.join("", letters));





        }

    /**
     * Takes the JSON string and replaces the records in internal database
     * @param response
     */
    protected String updateDB (String response){

        JSONArray syncedJsonArr = new JSONArray();

        try {

            JSONArray jsonArray = new JSONArray(response);

            JSONObject jsonObject;

            //Log.i("jsonObj",jsonObject.getString("email"));

            db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

            for (int i=0; i < jsonArray.length();i++){
                jsonObject = jsonArray.getJSONObject(i);
                ContentValues contentValues = new ContentValues();
                JSONObject syncedJsonObj = new JSONObject();
                if(!jsonObject.getString("entryid_int").equalsIgnoreCase("null")){
                    contentValues.put("entryid", jsonObject.getInt("entryid_int"));
                }
                contentValues.put("title", jsonObject.getString("title"));
                contentValues.put("username", jsonObject.getString("username"));
                contentValues.put("password", jsonObject.getString("password"));
                contentValues.put("url", jsonObject.getString("url"));
                contentValues.put("notes", jsonObject.getString("notes"));
                if(!jsonObject.getString("categoryid").equalsIgnoreCase("null")){
                contentValues.put("categoryid", jsonObject.getInt("categoryid"));
                }else{
                contentValues.put("categoryid", 0);
                }
                if(!jsonObject.getString("userid").equalsIgnoreCase("null")){
                contentValues.put("userid", jsonObject.getInt("userid"));}
                contentValues.put("deleted", jsonObject.getInt("deleted"));
                contentValues.put("syncStatus",1);

                try {
                    syncedJsonObj.put("entryid",jsonObject.getInt("entryid"));
                    syncedJsonObj.put("entryid_int", db.replace("entryTable", null, contentValues));

                    //  Log.i("rowID", String.valueOf(mydatabase.replace("users", null, contentValues)));
                    syncedJsonArr.put(syncedJsonObj);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            db.close();

            Log.i("Json_frmEXTdb_updtINTdb", syncedJsonArr.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return syncedJsonArr.toString();
        //UploadToPhp(syncedJsonArr.toString(), getResources().getString(R.string.php_updateDownSyncStat));
    }

    /**
     *
     * Gets the un-synchronized data from internal database
     */

    public String getDB (){

        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject;


        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery("SELECT entryid, title, username, password, url, notes, categoryid, userid, deleted FROM entryTable WHERE syncStatus = '0'",null);

        while(cursor.moveToNext()){
            jsonObject = new JSONObject();

            try {

                jsonObject.put("entryid",cursor.getInt(0));
                jsonObject.put("title",cursor.getString(1));
                jsonObject.put("username",cursor.getString(2));
                jsonObject.put("password",cursor.getString(3));
                jsonObject.put("url",cursor.getString(4));
                jsonObject.put("notes",cursor.getString(5));
                jsonObject.put("categoryid",cursor.getInt(6));
                jsonObject.put("userid",cursor.getInt(7));
                jsonObject.put("deleted",cursor.getInt(8));
                jsonArray.put(jsonObject);


            } catch (Exception e) {
                e.printStackTrace();
            }


        }
        db.close();
        Log.i("Json_data2sendFromINTdb", jsonArray.toString());
        return jsonArray.toString();
    }

    /**
     * Instance when called is executed on different thread other than main thread
     */

    public class SyncDown extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... params) {

            UploadToPhp(updateDB(syncIntDB(params[1])), params[2]);
            //Toast.makeText(getApplicationContext(), "Downloaded content", Toast.LENGTH_SHORT).show();
            setSyncStatus(UploadToPhp(getDB(), params[3]));
            //Toast.makeText(MainActivity.this, "Uploaded content", Toast.LENGTH_SHORT).show();

            /*switch (params[0]){
                case "0": return syncIntDB(params[1]);

                case "1": UploadToPhp(params[1],params[2]);return null;

                default: return null;
            }*/
            return  null;


        }

        /**
         * Updates the listview post synchronization
         * @param s
         */
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateQuery = new String("SELECT * FROM entryTable WHERE deleted = '0'");
            updateList(updateQuery);
        }

        /**
         * Copies all the data into Int DB with syncStatus = '0'
         *
         * This method gets the JSON data from the input URL @param
         *
         * @param link
         * @return SyncStatus
         */
        protected  String syncIntDB (String link){
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(link);
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
                return(result);
                //return "Synced successfully";


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "Things didn't go expected";
        }

        /**
         * Sends data to the specified URL
         * @param acknowledge is the data to be sent to php script
         * @param urlString is the php script URL
         * @return the reply from the php post execution
         */
        protected String UploadToPhp (String acknowledge, String urlString){
            URL url;
            HttpURLConnection urlConnection = null;



            try {
                //Get URL from strings.xml
                url = new URL(urlString);
                //Open HTTP connection
                urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setReadTimeout( 10000 /*milliseconds*/ );
                urlConnection.setConnectTimeout( 15000 /* milliseconds */ );
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);
                urlConnection.setFixedLengthStreamingMode(acknowledge.getBytes().length);
                urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
                urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");



                urlConnection.connect();

                OutputStream os = new BufferedOutputStream(urlConnection.getOutputStream());

                os.write(acknowledge.getBytes());
                //clean up
                os.flush();
                StringBuffer response = null;
                //InputStream is = urlConnection.getInputStream();
                int responseCode = urlConnection.getResponseCode();
                System.out.println("responseCode" + responseCode);
                switch (responseCode) {
                    case 200:
                        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        String inputLine;
                        response = new StringBuffer();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        Log.i("Json_replyFromEXTdb", response.toString());
                        return response.toString();

                }


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if (urlConnection != null){
                    urlConnection.disconnect();
                }


            }
            return null;

        }



    }

    /**
     * Updates synchronization status in internal database ONLY after successful sync.
     * @param phpResponse is the response from php script
     */

    public void setSyncStatus (String phpResponse){

        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(phpResponse);


        JSONObject jsonObject;

        //Log.i("jsonObj",jsonObject.getString("email"));

        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

        for (int i=0; i < jsonArray.length();i++) {
            jsonObject = jsonArray.getJSONObject(i);
            ContentValues contentValues = new ContentValues();

            if(jsonObject.getString("status").equalsIgnoreCase("yes")){
                contentValues.put("syncStatus", 1);

               System.out.print("UpdatedSyncStatus_int" + db.update("entryTable", contentValues, "entryid=?", new String[]{jsonObject.getString("entryid")}));

            }


        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        db.close();

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

    /**
     * Performs sync when called
     * @return the success status of sync
     */

    public boolean onSync (){
        if (isOnline()) {
            SyncDown sd = new SyncDown();

            try {
                sd.execute("", getResources().getString(R.string.php_getAllEntries) + "?userid=" + user_id, getResources().getString(R.string.php_updateDownSyncStat), getResources().getString(R.string.php_updateExtDB)).get();


            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return true;
        } else {

            return false;
        }
    }

}


