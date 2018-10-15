package com.poc.alitariq.lowchanneldatatransfer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;

public class MyPhoneStateListener extends PhoneStateListener {
	public static Boolean phoneRinging = false;
	public static Boolean offhook = false;
	public static Boolean ideal = false;
    private AudioManager myAudioManager;

	Context c;
	String sdCardRoot = Environment.getExternalStorageDirectory().toString()+ "/testing/";
    int currentVolume = 0;
    long lastStateChanged;
	public MyPhoneStateListener(Context con) {
		// TODO Auto-generated constructor stub
		c = con;

    }

	public void writeToFile(String fileName,String str){
		OutputStream fos;
		try {
			fos = new FileOutputStream(sdCardRoot+fileName);
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

	public void onCallStateChanged(int state, String incomingNumber) {

//        System.out.println("state changed : "+ state);
		myAudioManager = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
		currentVolume = myAudioManager.getStreamVolume(AudioManager.STREAM_RING);



        switch (state) {
		case TelephonyManager.CALL_STATE_IDLE:
			ideal = true;
			phoneRinging = false;
			offhook = false;
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			ideal = false;
			phoneRinging = false;
			offhook = true;
			break;
		case TelephonyManager.CALL_STATE_RINGING:
			ideal = false;
			phoneRinging = true;
			offhook = false;
			break;
		}

		if (offhook){
            long lastOffHook=System.currentTimeMillis();
			String strLastOffHook="\n"+lastOffHook;
			String str=readFromFile("lastState");
            String str2=readFromFile("lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
			if(str!=null && str.equals("offHook")) {}
			else{
			    if (lastOffHook-lastStateChanged >1000) {
                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastOffHook - lastStateChanged));
                    lastStateChanged = lastOffHook;
                    System.out.println(sdCardRoot + "lastOffHook: " + lastOffHook);
                    String prev = readFromFile("lastOffHook");
                    prev = prev + strLastOffHook;
                    writeToFile("lastOffHook", prev);
                    writeToFile("lastState", "offHook");
                    writeToFile("lastStateChanged", lastStateChanged + "");
                }
			}


		}

		if (ideal){
			long lastIdeal=System.currentTimeMillis();
			String strLastIdeal="\n"+lastIdeal;
			String str=readFromFile("lastState");
            String str2=readFromFile("lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
			if(str!=null && str.equals("ideal")){
            }else{
			    if (lastIdeal-lastStateChanged >1000) {
                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastIdeal - lastStateChanged));
                    lastStateChanged = lastIdeal;
                    System.out.println(sdCardRoot + "lastIdeal: " + lastIdeal);
                    String prev = readFromFile("lastIdeal");
                    prev = prev + strLastIdeal;
                    writeToFile("lastIdeal", prev);
                    writeToFile("lastState", "ideal");
                    writeToFile("lastStateChanged", lastStateChanged + "");
                }
			}
//            myAudioManager.setStreamMute(AudioManager.STREAM_RING, false);
            myAudioManager.setStreamVolume(AudioManager.STREAM_RING, currentVolume, 0);
		}

		if(phoneRinging){
//            myAudioManager.setStreamMute(AudioManager.STREAM_RING, true);
            myAudioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
			long lastRinging=System.currentTimeMillis();
			String strLastRinging="\n"+lastRinging;
			String str=readFromFile("lastState");
            String str2=readFromFile("lastStateChanged");
            if (str2.equalsIgnoreCase("")) {
                str2 = "0";
            }
            lastStateChanged = Long.parseLong(str2);
			if(str!=null && str.equals("ringing")){}
			else {
			    if (lastRinging-lastStateChanged >1000) {
                    System.out.println("str: " + str + " lastStateChanged: " + lastStateChanged + " difference: " + (lastRinging - lastStateChanged));
                    lastStateChanged = lastRinging;
                    System.out.println(sdCardRoot + "lastRinging: " + lastRinging);
                    String prev = readFromFile("lastRinging");
                    prev = prev + strLastRinging;
                    writeToFile("lastRinging", prev);
                    writeToFile("lastState", "ringing");
                    writeToFile("lastStateChanged", lastStateChanged + "");
                }
            }
		}
	}
}
