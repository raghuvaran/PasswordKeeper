package com.rvlabs.passwordkeeper;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Responsible for data validations
 * helps app in being stable when it encounters '(apostrophe) and helps in managing database transaction stuff
 *
 * @author Raghuvaran
 */
 
public class Validation {
    /**
     * Replaces if any apostrophe's found in the input with escape character
     * @param input
     * @return input with escape character (if any apostrophe found)
     */
    public static String replaceApostrophe (String input){
        int a=0;
        if (input.contains("'")){

            return(input.replaceAll("[']","''"));

        }
        return input;
        //return input.contains("'")? input.replace("'","\'") : input;
    }
    public static String replaceApostrophe (Context context,String input){
        int a=0;
        if (input.contains("'")){
            Toast.makeText(context,"You can't crash me ;)",Toast.LENGTH_SHORT);
            return(input.replaceAll("[']","''"));

        }
        return input;
        //return input.contains("'")? input.replace("'","\'") : input;
    }

    /**
     * Returns current timestamp
     * @return yyyy-MM-dd HH:mm:ss formate date as string
     */
    public static String getCurrentTimeStamp(){
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentTimeStamp = dateFormat.format(new Date()); // Find todays date

            return currentTimeStamp;
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

}
