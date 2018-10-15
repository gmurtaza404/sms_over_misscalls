package com.poc.alitariq.lowchanneldatatransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

import static java.lang.Thread.sleep;

/**
 * Created by ali tariq on 13/03/2018.
 */
public class MyBroadcastReceiver extends BroadcastReceiver
{
    TelephonyManager tm;
    String extra_foreground_call_state;
    String sdCardRoot = Environment.getExternalStorageDirectory().toString()+ "/testing/";
//    String sdCardRoot = Environment.getDataDirectory().getAbsolutePath().toString()+ "/testing/";
    String last_State = "none";
    long time_stamp = 0;


    @Override
    public void onReceive(Context context, final Intent intent)
    {
//        final Bundle bundle = intent.getExtras();
//        if(bundle != null && Constants.data) { /*changes done, get boolean from the Constants.data*/
//            System.out.println("hi i have recieve");
//            System.out.println(Constants.str); /*changes done*/
//        }

        time_stamp = System.currentTimeMillis();
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        try {
            Field extra_foreground_call_state_field = tm.getClass().getField("EXTRA_FOREGROUND_CALL_STATE");
            extra_foreground_call_state_field.setAccessible(true);
            Object val = extra_foreground_call_state_field.get(Object.class);
            extra_foreground_call_state = (String)  val;

        } catch (NoSuchFieldException e) {
            System.out.println("No field 'EXTRA_FOREGROUND_CALL_STATE' found!");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }



        switch (intent.getIntExtra(extra_foreground_call_state, -2)) {
            case 0: //  PreciseCallState.PRECISE_CALL_STATE_IDLE:
//                System.out.println("IDLE");
                last_State = readFromFile("lastOutGoingState");
                if (last_State.equalsIgnoreCase("alerting")) {
                    appendToFile("outGoingIdle", ""+time_stamp);
                    writeToFile("lastOutGoingState", "idle");
                } else if (!last_State.equalsIgnoreCase("idle")) {}
//                last_State = "idle";
                break;
            case 3: //  PreciseCallState.PRECISE_CALL_STATE_DIALING:
//                System.out.println("DIALING");
                last_State = readFromFile("lastOutGoingState");
                if (!last_State.equalsIgnoreCase("dialing")) {
                    appendToFile("outGoingDialing", ""+time_stamp);
                    writeToFile("lastOutGoingState", "dialing");
                }
//                last_State = "dialing";
                break;
            case 4: //  PreciseCallState.PRECISE_CALL_STATE_ALERTING:
//                System.out.println("ALERTING");
                last_State = readFromFile("lastOutGoingState");
                if (last_State.equalsIgnoreCase("dialing")) {
                    appendToFile("outGoingAlerting", ""+ time_stamp);
                    writeToFile("lastOutGoingState", "alerting");
                }
//                last_State = "alerting";
                break;
            case 1: //  PreciseCallState.PRECISE_CALL_STATE_ACTIVE:
//                System.out.println("ACTIVE");
                last_State = readFromFile("lastOutGoingState");
                if (!last_State.equalsIgnoreCase("active")) {
                    appendToFile("outGoingActive", "active "+ time_stamp);
                    writeToFile("lastOutGoingState", "active");
                }
//                last_State = "active";
                break;
            default:
//                System.out.println("State_Received: "+ String.valueOf(intent.getIntExtra(extra_foreground_call_state, -2)));
        }

    }

    public void appendToFile(String fileName,String str){
        OutputStream fos;
        try {
//            System.out.println(str);
            String prev = readFromFile(fileName);
            prev = prev + "\n" + str;
            fos= new FileOutputStream(sdCardRoot+fileName);
            byte[] b=prev.getBytes();
            fos.write(b);
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    };

    public String readFromFile(String fileName){
        String temp=null;
        FileInputStream fin;
        try {
            fin = new FileInputStream(sdCardRoot+fileName);
            byte[] b = new byte[fin.available()];
            fin.read(b);
            String s = new String(b);
            temp = s;
            fin.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return temp;
    }

    public void writeToFile(String fileName,String str){
        OutputStream fos;
        try {
//            System.out.println(str);
            fos= new FileOutputStream(sdCardRoot+fileName);
            byte[] b=str.getBytes();
            fos.write(b);
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    };

}
