package com.rvlabs.passwordkeeper;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


/** This method is responsible for displaying a page for adding entries in to the database*/
public class AddEntry extends AppCompatActivity {

    SQLiteDatabase db;
    Cursor c;
    EditText title, username, password,url,notes;
    Spinner category;
    List<String> catList;
    ArrayAdapter<String> spinAdapter;

    int user_id, curr_index, action_created=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_entry);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent in = getIntent();
        user_id = in.getIntExtra("userid",0);

        title = (EditText) findViewById(R.id.addtitle);
        username = (EditText) findViewById(R.id.addusername);
        password = (EditText) findViewById(R.id.addpassword);
        url = (EditText) findViewById(R.id.addurl);
        notes = (EditText) findViewById(R.id.addnotes);
        category = (Spinner) findViewById(R.id.addCategory);
        catList = new ArrayList<String>();

        db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);

        c = db.rawQuery("SELECT categoryName FROM category", null);
        c.moveToFirst();
        catList.add("All");


        while (!c.isAfterLast()){
            catList.add(c.getString(c.getColumnIndex("categoryName")));

            c.moveToNext();
        }

        spinAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,catList);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        category.setAdapter(spinAdapter);

        c = db.rawQuery("SELECT MAX(entryid)AS Max FROM entryTable",null);
        c.moveToFirst();
        curr_index = c.getInt(c.getColumnIndex("Max"));
        curr_index++;
        db.close();
    }

    /**
     * OnAdd is responsible for commiting changes to the database
     * @param view
     */
    public void onAdd (View view){
        if (!title.getText().toString().isEmpty()) {
            db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
            db.execSQL("INSERT INTO entryTable (title, username, password, url, notes, categoryid, userid, deleted, syncStatus) VALUES ('" + Validation.replaceApostrophe(title.getText().toString()) + "', '" +
                            Validation.replaceApostrophe(username.getText().toString()) + "', '" +
                            Validation.replaceApostrophe(password.getText().toString()) + "', '" +
                            Validation.replaceApostrophe(url.getText().toString()) + "', '" +
                            Validation.replaceApostrophe(notes.getText().toString())+ "', '" +
                            category.getSelectedItemPosition()+"', '" +
                            user_id+ "','0','0')"

            );
            /**log entry*/
            db.execSQL("INSERT INTO log (logTime, entryid, actionid, userid) VALUES ('"+Validation.getCurrentTimeStamp()+"', '"+
                            curr_index+"', '"+action_created+"', '"+user_id+"')"

            );

            db.close();
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("userid",user_id);
            startActivity(i);
            finish();
        }else {
            Toast.makeText(getApplicationContext(),"You might forgot to add \'Title\'",Toast.LENGTH_SHORT).show();
            title.setError("You can't leave this blank");
        }
    }
	//overrides navigateUp with backPress
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        if (id == 111){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("userid",user_id);
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
