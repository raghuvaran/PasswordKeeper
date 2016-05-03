package com.rvlabs.passwordkeeper;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**This activity shows the log table from the database*/
public class ViewLog extends AppCompatActivity {

    int entry_id,user_id,viewType;
    String winTitle;
    SQLiteDatabase db;

    /**
     * Creates the Log view and sets the title of the window according to the parent intent
     * @param savedInstanceState
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        Intent i = getIntent();
        entry_id = i.getIntExtra("entryid",2);
        user_id = i.getIntExtra("userid",-1);
        viewType = i.getIntExtra("viewType",0);//0: ItemLog //1: TotalLog
        winTitle = new String("View Logs");



        // Reference to TableLayout
        TableLayout tableLayout=(TableLayout)findViewById(R.id.tablelayout);
        // Add header row
        TableRow rowHeader = new TableRow(getApplication());
        String[] headerText={"Action","Time_Stamp"};
        if(viewType ==1){
             headerText= new String[]{"Entry","Action","Time_Stamp"};
        }
        for(String c:headerText) {
            TextView tv = new TextView(this);
            tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.MATCH_PARENT));
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(18);
            tv.setPadding(5, 5, 5, 5);
            tv.setText(c);
            rowHeader.addView(tv);
        }
        tableLayout.addView(rowHeader);

        try
        {
            db = this.openOrCreateDatabase("PassKeeper_db", MODE_PRIVATE, null);
            Cursor c = db.rawQuery("SELECT title FROM entryTable WHERE entryid ='"+entry_id+"'",null); c.moveToFirst();


           // getActionBar().setTitle(cursor.getString(0) + "'s Log");
            
            String selectQuery = "SELECT actionName, logTime FROM log i, action a WHERE i.actionid = a.actionid AND entryid = '" + entry_id + "'";
            if (viewType == 1) {
                selectQuery = new String("SELECT title, actionName, logTime FROM log i, action a, entryTable e WHERE i.actionid = a.actionid AND e.entryid = i.entryid");
                winTitle = new String ("View log");

            }else{
                winTitle = new String(c.getString(0)+"'s log");
            }
            setTitle(winTitle);

            Cursor cursor = db.rawQuery(selectQuery,null);


            if(cursor.getCount() >0) {
                while (cursor.moveToNext()) {
                    // Read columns data
                String actioName = cursor.getString(cursor.getColumnIndex("actionName"));
                String logTime = cursor.getString(cursor.getColumnIndex("logTime"));
                    String entryTitle;
                    String[] colText={actioName,logTime};

                    if (viewType == 1) {
                        entryTitle = new String(cursor.getString(cursor.getColumnIndex("title")));
                        colText= new String[]{entryTitle,actioName,logTime};

                    }
                    // dara rows
                    TableRow row = new TableRow(this);
                    row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.WRAP_CONTENT));

                    for(String text:colText) {
                        TextView tv = new TextView(this);
                        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.MATCH_PARENT));
                        tv.setGravity(Gravity.CENTER);
                        tv.setTextSize(16);
                        tv.setPadding(5, 5, 5, 5);
                        tv.setText(text);
                        row.addView(tv);
                    }
                    tableLayout.addView(row);

                }
            }

        }catch (SQLiteException e)
        {
            e.printStackTrace();

        }
        db.close();












    }

    /**
     * overrides the navigate up button with on Backpressed
     * @return
     */
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
