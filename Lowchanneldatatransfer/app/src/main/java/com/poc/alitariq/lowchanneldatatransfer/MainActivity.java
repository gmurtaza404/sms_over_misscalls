package com.poc.alitariq.lowchanneldatatransfer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static android.provider.AlarmClock.EXTRA_MESSAGE;
import static java.lang.Thread.sleep;

//import android.telecom.*;

public class MainActivity extends Activity {
    //UI handlers
    TextView userNotification;
    TextView receiverPhoneNumber;
    Button call;
    Button reset;
    Button export;
    Button refresh;
    Button settings;
    Button updateParameters;
    Button receiverMode;
    ProgressBar progress;
    EditText text;
    Button send;
    Button sendquiz;
    Button sendanswers;
    EditText answers;

    //parameters
    int callDisconnectTime = 4000;
    int stateTime = 1500;
    int max_wait_for_ringing = 200;
    String phoneNo = "03202471439"; //"03474406284";

    //variables
    int closed = 0;
    int callsLeft = 0;
    //int data_time[] = {1000, 2000, 3000, 4000, 5000, 6000, 7000, 8000, 9000, 10000, 11000, 12000, 13000, 14000, 15000, 16000};
    int data_time[] = {1500, 3000, 4500, 6000, 7500, 9000, 10500, 12000, 13500, 15000, 16500, 18000, 19500, 21000, 22500, 24000};
    int err_cor_sam_code[] = {0, 8, 3, 4, 3, 2, 0, 9, 7, 0, 5, 2, 4, 10, 0, 2, 2, 11, 12, 11, 4, 6, 1, 3, 8, 14, 4, 3, 6, 6, 14, 7, 8, 6, 13, 5, 10, 3, 13, 12, 9, 12, 12, 1, 13, 15, 14, 13, 1, 4, 13, 5, 6, 6, 15, 1, 9, 4, 6, 2, 11, 7, 12, 2, 1, 13, 9, 0, 8, 0, 3, 13, 2, 7, 9, 12, 12, 15, 3, 5, 2, 9, 1, 15, 14, 8, 10, 9, 12, 8, 10, 9, 3, 0, 4, 10, 11, 10, 15, 15, 12, 5, 5, 13, 0, 10, 8, 8, 6, 7, 2, 10, 13, 9, 4, 13, 13, 3, 11, 7, 6, 9, 13, 4, 5, 10, 10, 4, 14, 5, 5, 15, 0, 12, 0, 1, 4, 8, 13, 5, 5, 0, 8, 5};
    String freq_char_set = "! etoainshrdlcumwfgypbvkjxqz0123456789****";
    String status = "null";
    int call_locked = 0;
    int waiting_for_call_connect;
    Method telephonyEndCall;
    Object telephonyObject;
    int receiverState = 0;
    long constant_disconnect_time = 1000;

    //Encoding
    Map<Object, String> charToBinary = null;
    Map<String, String> binaryToString = null;
    Map<Integer, String> callLengthToBinary = null;
    String suggestions;

    String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath()+"/testing/";
//    String sdCardRoot = Environment.getDataDirectory().getAbsolutePath().toString()+ "/testing/";


    //utility functions
    public void askPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.MODIFY_AUDIO_SETTINGS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
                //showExplanation("Permission Needed", "Rationale", Manifest.permission.READ_PHONE_STATE, 1);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                        1);
            }
        } else {
            Toast.makeText(MainActivity.this, "Permission (already) Granted!", Toast.LENGTH_SHORT).show();
        }

    }

    public void waitForCall() {
        String state = "none";
        while (state.compareTo("alerting") != 0) {
            try {
                sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            state = readFromFile("lastOutGoingState");
        } // ringing started!
    }

    public int waitForCallUsingVirtualizer() {
        waiting_for_call_connect = 0;
        Visualizer mVisualizer = new Visualizer(0);
        mVisualizer.setEnabled(false);
        int capRate = Visualizer.getMaxCaptureRate();
        int capSize = Visualizer.getCaptureSizeRange()[1];
        mVisualizer.setCaptureSize(capSize);
        Visualizer.OnDataCaptureListener captureListener = new Visualizer.OnDataCaptureListener() {
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
                                              int samplingRate) {
                for (int i = 0; i < bytes.length; i++) {
                    if (bytes[i] != -128) {
                        //yes detected
                        waiting_for_call_connect = 1;
                        break;
                    }
                }
            }

            public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int samplingRate) {
            }
        };

        int status2 = mVisualizer.setDataCaptureListener(captureListener, capRate, true/*wave*/, false/*no fft needed*/);
        mVisualizer.setEnabled(true);
        while (true) {
            if (waiting_for_call_connect == 1) {
                break;
            }
        }
        mVisualizer.setEnabled(false);
        mVisualizer.release();
        return 1;

    }

    public void clearFile(String fileName) {
        OutputStream fos;
        try {
            fos = new FileOutputStream(sdCardRoot + fileName);//"lastOffHook"
            String str = "";
            byte[] b = str.getBytes();
            fos.write(b);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void makeFile(String fileName, File myDir) {
        //make dir
        String temp = null;
        FileInputStream fin;
        try {
            fin = new FileInputStream(sdCardRoot + fileName);
            byte[] b = new byte[fin.available()];
            fin.read(b);
            String s = new String(b);
            temp = s;
            fin.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        File file = new File(myDir, fileName );
        try {
            FileOutputStream out = new FileOutputStream(file);
            out.write(temp.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File makeDir() {
        long name = System.currentTimeMillis();
        File myDir = new File(sdCardRoot );
        myDir.mkdirs();

        return myDir;
    }

    public Method disconnectInitializer() {

        try {
            Class<?> telephonyClass = Class.forName("com.android.internal.telephony.ITelephony");
            Class<?> telephonyStubClass = telephonyClass.getClasses()[0];
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Class<?> serviceManagerNativeClass = Class.forName("android.os.ServiceManagerNative");

            Object serviceManagerObject;
            Method getService = // getDefaults[29];
                    serviceManagerClass.getMethod("getService", String.class);
            Method tempInterfaceMethod = serviceManagerNativeClass.getMethod(
                    "asInterface", IBinder.class);
            Binder tmpBinder = new Binder();
            tmpBinder.attachInterface(null, "fake");
            serviceManagerObject = tempInterfaceMethod.invoke(null, tmpBinder);
            IBinder retbinder = (IBinder) getService.invoke(
                    serviceManagerObject, "phone");
            Method serviceMethod = telephonyStubClass.getMethod("asInterface",
                    IBinder.class);
            telephonyObject = serviceMethod.invoke(null, retbinder);
            telephonyEndCall = telephonyClass.getMethod("endCall");


        } catch (Exception e) {
            e.printStackTrace();

        }
        return telephonyEndCall;
    }

    public void disconnectCall() {
        try {
            telephonyEndCall.invoke(telephonyObject);
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeToFile(String fileName,String str){
        OutputStream fos;
        try {
            fos = new FileOutputStream(sdCardRoot+fileName);
            byte[] b=str.getBytes();
            fos.write(b);
            fos.close();
//            System.out.println(sdCardRoot+fileName+": " + str);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    };

    public void appendToFile(String fileName,String str){
        OutputStream fos;
        try {
//            System.out.println(sdCardRoot+fileName+" : "+str);
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

    public String readFromFile(String fileName) {
        String temp = null;
        FileInputStream fin;
        try {
            fin = new FileInputStream(sdCardRoot + fileName);
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

    public void saveParameters() {
        writeToFile("parameters", "\ncallDisconnectTime= " +
                callDisconnectTime);

    }

    public void writeParametersInSeparateFiles() {
        File myDir = makeDir();
//        writeToFile("callDisconnectTime", "" + callDisconnectTime);
//        writeToFile("stateTime", "" + stateTime);
//        writeToFile("phoneNo", "" + phoneNo);
    }

    public void myToast(String str) {
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_LONG).show();
    }

    public void export() {
        File myDir = makeDir();
        makeFile("lastOffHook", myDir);
        makeFile("lastIdeal", myDir);
        makeFile("lastRinging", myDir);
        makeFile("lastState", myDir);
        makeFile("parameters", myDir);
//        makeFile(status, myDir);
        makeFile("callDisconnectTime", myDir);
        makeFile("stateTime", myDir);
        makeFile("phoneNo", myDir);
        makeFile("disconnect", myDir);
        makeFile("connect", myDir);
        makeFile("output", myDir);
        makeFile("outGoingIdle", myDir);
        makeFile("outGoingDialing", myDir);
        makeFile("outGoingAlerting", myDir);
        makeFile("outGoingActive", myDir);
        makeFile("lastStateChanged", myDir);


        try {
            File file = new File(myDir, "_log.txt");
            Runtime.getRuntime().exec("logcat -d -v time -f " + file.getAbsolutePath());
        } catch (IOException e) {
        }

    }

    public void send_func(final int[] symbols) {
        writeToFile("lastState", "r");
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

//                while(true) {
//                    try {
//                        sleep(10000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                    float batteryPercentage = getBatteryStats();
//                    System.out.println("Current Battery Remaining : "+ batteryPercentage);
//                    String str0 = readFromFile("batteryEndCallLogs");
//                    writeToFile("batteryEndCallLogs", str0 + batteryPercentage + "\n");
//                    str0 = readFromFile("connect");
//                    writeToFile("connect", str0 + System.currentTimeMillis() + "\n");
//                    System.out.println(System.currentTimeMillis()+" "+batteryPercentage);
//
//                }


                Intent callIntent = new Intent(Intent.ACTION_CALL);
//                Constants.data = true; /*see change, set data in Constants.data*/
//                Constants.str = "some data to pass..."; /*see change*/
//                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                callIntent.setData(Uri.parse("tel:" + phoneNo));
                status = "not done";

                //UI
                callsLeft = symbols.length * 2;
                progress.setProgress(0);
                progress.setMax(symbols.length);
                //int last_export=0;
                for (int test = 0; test < 1; test++) {
                    for (int count = 0; count < symbols.length; count++) {
                        if (closed == 1) {
                            break;
                        }
                        System.out.println("before: " + System.currentTimeMillis());
                        float batteryPercentage = getBatteryStats();
//                        double batteryCapacity = getBatteryCapacity();
                        System.out.println("Current Battery Remaining : "+ batteryPercentage);
                        String str0 = readFromFile("batteryStartCallLogs");
                        writeToFile("batteryStartCallLogs", str0 + batteryPercentage  + "\n");


                        long l = System.currentTimeMillis();
                        try {
                            startActivity(callIntent);
                        } catch (SecurityException e) {
                            e.printStackTrace();
                        }
//                        waitForCall();
                        waitForCallUsingVirtualizer();
                        String state = "none";
                        int counter = 0;
//                        while (state.compareTo("alerting") != 0) {
//                            counter++;
//                            if (counter > max_wait_for_ringing) {
//                                try {
//                                    startActivity(callIntent);
//                                } catch (SecurityException e) {
//                                    e.printStackTrace();
//                                }
//                                System.out.println("redialing!!!");
//                                counter = 0;
//                            }
//                            try {
//                                //Thread.sleep(data_time[count]);
//                                sleep(100);
//                            } catch (InterruptedException e) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace();
//                                System.out.println(e.toString());
//                            }
//                            state = readFromFile("lastOutGoingState");
////                           System.out.println("waiting for ringing to start with state: "+ state);
//                        } // ringing started!

                        System.out.println("connect: " + System.currentTimeMillis());
                        str0 = readFromFile("connect");
                        writeToFile("connect", str0 + System.currentTimeMillis() + "\n");
                        try {
                            //Thread.sleep(data_time[count]);
                            sleep(data_time[symbols[count]]);
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        String str = readFromFile("disconnect");
                        writeToFile("disconnect", str + System.currentTimeMillis() + "\n");
                        disconnectCall();
                        System.out.println("disconnect: " + System.currentTimeMillis());
//                        while ((state.compareTo("idle") != 0) && (state.compareTo("active") != 0)) {
//                            try {
//                                //Thread.sleep(data_time[count]);
//                                sleep(100);
//                            } catch (InterruptedException e) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace();
//                            }
//                            state = readFromFile("lastOutGoingState");
////                            System.out.println("waiting for ringing to stop");
////                            System.out.println(state);
//                        } // ringing started!
                        try {
                            sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        batteryPercentage = getBatteryStats();
                        System.out.println("Current Battery Remaining : "+ batteryPercentage);
                        str0 = readFromFile("batteryEndCallLogs");
                        writeToFile("batteryEndCallLogs", str0 + batteryPercentage + "\n");

                        progress.setProgress(progress.getProgress() + 1);
                        callsLeft--;
                    }
                }
                status = "done";
                call_locked = 0;
            }
        });
        t.start();
    }

    public double getBatteryCapacity() {
        Object mPowerProfile_ = null;
        double batteryCapacity = 0;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
        try {
            mPowerProfile_ = Class.forName(POWER_PROFILE_CLASS)
                    .getConstructor(Context.class).newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            batteryCapacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile_, "battery.capacity");
//            Toast.makeText(MainActivity.this, batteryCapacity + " mah", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return batteryCapacity;
    }

    private float getBatteryStats() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        System.out.println("level: "+ (float)level + " scale: "+ (float)scale + " health: "+health);
        float batteryPct = (float)level / (float)scale;

        return (float)(batteryPct*100);
    }


    public int decode(long num, long corr) {
        num = num - ((corr+constant_disconnect_time*3)/4) + (stateTime / 2) - data_time[0];
        int num2 = (int) (num / stateTime);
        //num2=(num2-1);
        if (num2 < 0) {
            num2 = 0;
        }
        if (num2 > 15) {
            num2 = 15;
        }//recheck this
        return num2;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if(requestCode==777)
        {
            if(resultCode==RESULT_OK) {
                suggestions=data.getStringExtra("RESULTS");
                sendTrimmed_function(suggestions);
            }
        }
    }

    public String getBinaryString(String str) {
        String returnStr = "";
        for (int i = 0; i < str.length(); i++) {
            returnStr = returnStr + charToBinary.get(str.charAt(i));
        }
        return returnStr;
    }

    private void generateBinaryMap() {
        charToBinary = new HashMap<>();
        charToBinary.put("e".charAt(0), "100");
        charToBinary.put("t".charAt(0), "000");
        charToBinary.put("a".charAt(0), "1110");
        charToBinary.put("o".charAt(0), "1101");
        charToBinary.put("i".charAt(0), "1011");
        charToBinary.put("n".charAt(0), "1010");
        charToBinary.put("s".charAt(0), "0111");
        charToBinary.put("h".charAt(0), "0110");
        charToBinary.put("r".charAt(0), "0101");
        charToBinary.put("d".charAt(0), "11111");
        charToBinary.put("l".charAt(0), "11110");
        charToBinary.put("c".charAt(0), "01001");
        charToBinary.put("u".charAt(0), "01000");
        charToBinary.put("m".charAt(0), "00111");
        charToBinary.put("w".charAt(0), "00110");
        charToBinary.put("f".charAt(0), "00101");
        charToBinary.put("g".charAt(0), "110011");
        charToBinary.put("y".charAt(0), "110010");
        charToBinary.put("p".charAt(0), "110001");
        charToBinary.put("b".charAt(0), "110000");
        charToBinary.put("v".charAt(0), "001000");
        charToBinary.put("k".charAt(0), "0010011");
        charToBinary.put("j".charAt(0), "001001011");
        charToBinary.put("x".charAt(0), "001001010");
        charToBinary.put("q".charAt(0), "001001001");
        charToBinary.put("z".charAt(0), "001001000");
    }

    private void generateEncodingMap() {
        binaryToString = new HashMap<>();
        binaryToString.put("100", "e");
        binaryToString.put("000", "t");
        binaryToString.put("1110", "a");
        binaryToString.put("1101", "o");
        binaryToString.put("1011", "i");
        binaryToString.put("1010", "n");
        binaryToString.put("0111", "s");
        binaryToString.put("0110", "h");
        binaryToString.put("0101", "r");
        binaryToString.put("11111", "d");
        binaryToString.put("11110", "l");
        binaryToString.put("01001", "c");
        binaryToString.put("01000", "u");
        binaryToString.put("00111", "m");
        binaryToString.put("00110", "w");
        binaryToString.put("00101", "f");
        binaryToString.put("110011", "g");
        binaryToString.put("110010", "y");
        binaryToString.put("110001", "p");
        binaryToString.put("110000", "b");
        binaryToString.put("001000", "v");
        binaryToString.put("0010011", "k");
        binaryToString.put("001001011", "j");
        binaryToString.put("001001010", "x");
        binaryToString.put("001001001", "q");
        binaryToString.put("001001000", "z");
    }

    private String decodeEncoded(String input) {
        int index = 0;
        int offset = 0;
        String encoded = "";
        System.out.println("Enter String : ");
        while (index < input.length()) {
            offset = 0;
            while (true) {
                if (binaryToString.get(input.substring(index, index + offset)) != null) {
                    encoded = encoded + binaryToString.get(input.substring(index, index + offset));
                    break;
                } else {
                    offset++;
                }
            }
            index = index + offset;
        }
        //System.out.println("Encoded String: " + encoded);
        return encoded;
    }

    private void callLengthToBinary() {
        callLengthToBinary = new HashMap<>();
        callLengthToBinary.put(0, "0000");
        callLengthToBinary.put(1, "0001");
        callLengthToBinary.put(2, "0010");
        callLengthToBinary.put(3, "0011");
        callLengthToBinary.put(4, "0100");
        callLengthToBinary.put(5, "0101");
        callLengthToBinary.put(6, "0110");
        callLengthToBinary.put(7, "0111");
        callLengthToBinary.put(8, "1000");
        callLengthToBinary.put(9, "1001");
        callLengthToBinary.put(10, "1010");
        callLengthToBinary.put(11, "1011");
        callLengthToBinary.put(12, "1100");
        callLengthToBinary.put(13, "1101");
        callLengthToBinary.put(14, "1110");
        callLengthToBinary.put(15, "1111");
    }

    private void appendEmptySpace(){
        String str0 = readFromFile("connect");
        writeToFile("connect",str0+"\n\n");
        str0 = readFromFile("disconnect");
        writeToFile("disconnect",str0+"\n\n");
        str0 = readFromFile("lastIdeal");
        writeToFile("lastIdeal",str0+"\n\n");
        str0 = readFromFile("lastOffHook");
        writeToFile("lastOffHook",str0+"\n\n");
        str0 = readFromFile("lastRinging");
        writeToFile("lastRinging",str0+"\n\n");
        str0 = readFromFile("outGoingAlerting");
        writeToFile("outGoingAlerting",str0+"\n\n");
        str0 = readFromFile("outGoingDialing");
        writeToFile("outGoingDialing",str0+"\n\n");
        str0 = readFromFile("outGoingIdle");
        writeToFile("outGoingIdle",str0+"\n\n");

    }

    private void sendTrimmed_function (String str) {

        System.out.println("real function called");

        String rec = "";
        str = "!!!!!!!" ;
        int index = 0;
        for (int i = 0; i < str.length(); i++) {
            index = 0;
            for (int j = 0; j < freq_char_set.length(); j++) {
                if (str.charAt(i) == freq_char_set.charAt(j)) {
                    index = j;
                    break;
                }
            }
            String temp = Integer.toString(index, 2);
            if (temp.length() == 7) {
                temp = "0" + temp;
            }
            if (temp.length() == 6) {
                temp = "00" + temp;
            }
            if (temp.length() == 5) {
                temp = "000" + temp;
            }
            if (temp.length() == 4) {
                temp = "0000" + temp;
            }
            if (temp.length() == 3) {
                temp = "00000" + temp;
            }
            if (temp.length() == 2) {
                temp = "000000" + temp;
            }
            if (temp.length() == 1) {
                temp = "0000000" + temp;
            }
            rec = rec + temp;

        }
        rec = rec + "110010100011100111011011111111111101111110000011001111100111110111100111101011011011111110111111100111100101011010110100000101110011111000001011100111111100111111100111110001111010100011110010110111001001110010100111101111101111111100111010100110001101011000111011011101001100110001000000";

        int symbols[] = new int[rec.length() / 4];
        int jk = 0;
        String writer = "";
        for (int k = 0; k < symbols.length; k++) {
            int characters = 8 * Integer.valueOf("" + rec.charAt(jk)) + 4 * Integer.valueOf("" + rec.charAt(jk + 1)) + 2 * Integer.valueOf("" + rec.charAt(jk + 2)) + Integer.valueOf("" + rec.charAt(jk + 3));
            symbols[k] = characters;
            jk = jk + 4;
            writer = writer + "~" + symbols[k] + "~";
        }
        writeToFile("notifications", writer);
        send_func(symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        writeParametersInSeparateFiles();
        disconnectInitializer();
        askPermissions();

        //for main activity
        call = (Button) findViewById(R.id.bCall);
        reset = (Button) findViewById(R.id.bReset);
        export = (Button) findViewById(R.id.bExport);
        userNotification = (TextView) findViewById(R.id.tvUserNotification);
        progress = (ProgressBar) findViewById(R.id.pbCallsleft);
        refresh = (Button) findViewById(R.id.bRefresh);
        settings = (Button) findViewById(R.id.bSettings);
        receiverMode = (Button) findViewById(R.id.bReceiverMode);
        updateParameters = (Button) findViewById(R.id.bUpdateSettings);
        text = (EditText) findViewById(R.id.et_text1);
        send = (Button) findViewById(R.id.b_send);

        generateBinaryMap();
        generateEncodingMap();
        callLengthToBinary();

        reset.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                clearFile("lastOffHook");
                clearFile("lastIdeal");
                clearFile("lastRinging");
                clearFile("parameters");
                clearFile("connect");
                clearFile("disconnect");
                clearFile("notifications");
                clearFile("output");
                clearFile("outGoingState");
                clearFile("lastOutGoingState");
                clearFile("ringingTime");
                clearFile("outGoingIdle");
                clearFile("outGoingDialing");
                clearFile("outGoingAlerting");
                clearFile("outGoingActive");
                clearFile("batteryStartCallLogs");
                clearFile("batteryEndCallLogs");
                clearFile("lastStateChanged");
            }
        });

        export.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                export();
            }
        });

        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v != null) {
                    String output = "";
                    String str1 = readFromFile("lastRinging");
                    String[] lines1 = str1.split("\\r?\\n");
                    long ring[] = new long[lines1.length];
                    for (int i = 0; i < lines1.length-1; i++) {
                        ring[i] = Long.valueOf(lines1[i+1]).longValue();
                    }
                    String str2 = readFromFile("lastIdeal");
                    String[] lines2 = str2.split("\\r?\\n");
                    long ideal[] = new long[lines2.length];
                    for (int i = 0; i < lines2.length-1; i++) {
                        ideal[i] = Long.valueOf(lines2[i+1]).longValue();
                    }

                    //mean correction
                    long corr = 0;
                    try {
                        corr = ideal[0] - ring[0];
                        corr = corr + (ideal[1] - ring[1]);
                        corr = corr + (ideal[2] - ring[2]);
                        corr = corr + (ideal[3] - ring[3]);
                        corr = (corr) / 4 - data_time[0];
                    } catch (Exception e) {
                    }

                    if (ideal.length == ring.length) {
                        long ringtime[] = new long[ideal.length];
                        int symbols[] = new int[ideal.length];
                        for (int i = 0; i < ideal.length; i++) {
                            ringtime[i] = ideal[i] - ring[i];
                            symbols[i] = decode(ringtime[i], corr);
                        }
                        String rec = "";
                        for (int i = 0; i < symbols.length; i++) {
                            rec = rec + callLengthToBinary.get(Integer.valueOf(symbols[i]));
                        }

                        for (int i = 0; i < (rec.length()); i = i + 8) {
                            try {
                                String t1 = "" + rec.charAt(i);
                                String t2 = "" + rec.charAt(i + 1);
                                String t3 = "" + rec.charAt(i + 2);
                                String t4 = "" + rec.charAt(i + 3);
                                String t5 = "" + rec.charAt(i + 4);
                                String t6 = "" + rec.charAt(i + 5);
                                String t7 = "" + rec.charAt(i + 6);
                                String t8 = "" + rec.charAt(i + 7);
                                String t9 = t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8;
                                int characters = 128 * Integer.valueOf("" + rec.charAt(i)) + 64 * Integer.valueOf("" + rec.charAt(i + 1)) + 32 * Integer.valueOf("" + rec.charAt(i + 2)) + 16 * Integer.valueOf("" + rec.charAt(i + 3)) + 8 * Integer.valueOf("" + rec.charAt(i + 4))
                                        + 4 * Integer.valueOf("" + rec.charAt(i + 5)) + 2 * Integer.valueOf("" + rec.charAt(i + 6)) + 1 * Integer.valueOf("" + rec.charAt(i + 7));
                                if (freq_char_set.length() > characters) {
                                    output = output + freq_char_set.charAt(characters);
                                }
                            } catch (Exception e) {
                            }
                        }

                        output = output.substring(2);

                        userNotification.setText("" + output);
                        writeToFile("output", output);

                    } else {
                        userNotification.setText("transmission error");
                        output = "err no size match";
                    }
                }


            }
        });

        receiverMode.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                switchReceiverState();
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (call_locked == 0) {
                    Runnable showDialogRun = new Runnable() {
                        public void run() {
                            Intent showDialogIntent = new Intent(getBaseContext(), Settings.class);
                            startActivity(showDialogIntent);
                        }
                    };
                    Handler h = new Handler(Looper.getMainLooper());
                    h.postDelayed(showDialogRun, 100);
                } else {
                    myToast("call in progress");

                }

            }
        });

        updateParameters.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                stateTime = Integer.parseInt(readFromFile("stateTime"));
                phoneNo = readFromFile("phoneNo");
//                for (int i = 0; i < byteSize; i = i + 2) {
//                    data_time[i] = encodeTime0;
//                    data_time[i + 1] = stateTime;
//                }
//                ;
                saveParameters();
            }
        });


        send.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String str = text.getText().toString();
                    //getTrimmedString(str);
                sendTrimmed_function(str);
            }
        });

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        closed = 1;
    }

    public void switchReceiverState() {
        if (receiverState == 0) {
            receiverState = 1;
            receiverMode.setText("receiver Mode on");
            float batteryPercentage = getBatteryStats();
            double batteryCapacity = getBatteryCapacity();
            text.setText("level: " + batteryPercentage+ ", capacity: "+batteryCapacity);
        } else {
            receiverState = 0;
            receiverMode.setText("receiver Mode off");
            text.setText("");
        }
    }

}
