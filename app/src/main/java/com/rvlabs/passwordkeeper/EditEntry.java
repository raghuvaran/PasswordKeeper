package com.rvlabs.passwordkeeper;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * This activity helps in editing the entries in database
 */

public class EditEntry extends AppCompatActivity {


    SQLiteDatabase db;
    EditText title, username, password,url,notes;
    Spinner category;
    List<String> catList;
    ArrayAdapter<String> spinAdapter;
    int listPosition, user_id,action_modified=2;

    /**
     * Fetches the called item from database and pre-populates into the input fields
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_entry);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent i = getIntent();
        listPosition = i.getIntExtra("listPosition",-1);
        user_id = i.getIntExtra("userid",0);
        Log.i("Editfunc",String.valueOf(listPosition));
        title = (EditText) findViewById(R.id.title);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        url = (EditText) findViewById(R.id.url);
        notes = (EditText) findViewById(R.id.notes);
        category = (Spinner) findViewById(R.id.category);
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

        Cursor cursor = db.rawQuery("SELECT * FROM entryTable WHERE entryid = '" + listPosition + "'",null);
        cursor.moveToFirst();

        Log.i("EditEntry","title column is "+cursor.getColumnIndex("titleid"));
        int entryid_id = cursor.getColumnIndex("entryid");
        int title_id = cursor.getColumnIndex("title");
        int username_id = cursor.getColumnIndex("username");
        final int password_id = cursor.getColumnIndex("password");
        int url_id = cursor.getColumnIndex("url");
        int notes_id = cursor.getColumnIndex("notes");
        int cat_id = cursor.getColumnIndex("categoryid");

        title.setText(cursor.getString(title_id));
        username.setText(cursor.getString(username_id));
        password.setText(cursor.getString(password_id));
        url.setText(cursor.getString(url_id));
        notes.setText(cursor.getString(notes_id));
        category.setSelection(Integer.parseInt(cursor.getString(cat_id)));

        final ImageButton passButton = (ImageButton) findViewById(R.id.passButton);


        /**
         * on press the password is visible
         * on release it again shows dots
         */
        passButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    password.setInputType(InputType.TYPE_CLASS_TEXT);
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    password.setInputType(129);

                }
                return true;
            }
        });



    }
	//override upNavigation to backPress
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    /**
     * This method is called to commit changes to database on modifying the record values
     * @param view
     */
    public void  onSave (View view){
        new AlertDialog.Builder(EditEntry.this)
                .setTitle("Updating entry ...")
                .setMessage("Is that OK?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db = getApplicationContext().openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
                        db.execSQL("UPDATE entryTable SET title = '" + Validation.replaceApostrophe(title.getText().toString()) + "', username = '" +
                                        Validation.replaceApostrophe(username.getText().toString()) + "', password = '" +
                                        Validation.replaceApostrophe(password.getText().toString()) + "', url = '" +
                                        Validation.replaceApostrophe(url.getText().toString()) + "', notes = '" +
                                        Validation.replaceApostrophe(notes.getText().toString()) + "', categoryid = '" +
                                        String.valueOf(category.getSelectedItemPosition())
                                        + "', syncStatus = '0' WHERE entryid = '" +
                                        listPosition + "'"

                        );

                        /**log entry*/
                        db.execSQL("INSERT INTO log (logTime, entryid, actionid, userid) VALUES ('" + Validation.getCurrentTimeStamp() + "', '" +
                                        listPosition + "', '" + action_modified + "', '" + user_id + "')"

                        );
                        db.close();
                        Intent i = new Intent(getApplicationContext(), MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.putExtra("userid", user_id);
                        startActivity(i);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //stay calm
                    }
                })
                .show();


    }



}
