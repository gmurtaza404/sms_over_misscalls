package com.poc.alitariq.lowchanneldatatransfer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Settings extends Activity{

	Button save;
	EditText noOfBytes;
	EditText byteSize;
	EditText callSetupTime;
	EditText callDisconnectTime;
	EditText stateTime;
	EditText phoneNo;

	String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/testing/";
//	String sdCardRoot = Environment.getDataDirectory().getAbsolutePath().toString()+ "/testing/";
	public void writeToFile(String fileName,String str){
        OutputStream fos;
        try {
            fos = new FileOutputStream(sdCardRoot+fileName);
			byte[] b=str.getBytes();
			fos.write(b);
			fos.close();
			System.out.println(sdCardRoot+fileName+": " + str);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		save=(Button) findViewById(R.id.bSave);
//		noOfBytes=(EditText)findViewById(R.id.etNumberOfBytes);
//		byteSize=(EditText)findViewById(R.id.etByteSize);
//		callSetupTime=(EditText)findViewById(R.id.etCallSetupTime);
		callDisconnectTime=(EditText)findViewById(R.id.etCallDisconnectTime);
		stateTime=(EditText)findViewById(R.id.etStateTime);
		phoneNo=(EditText)findViewById(R.id.etPhoneNumber);

//		noOfBytes.setText(readFromFile("noOfBytes"));
//		byteSize.setText(readFromFile("byteSize"));
//		callSetupTime.setText(readFromFile("callSetupTime"));
		callDisconnectTime.setText(readFromFile("callDisconnectTime"));
		stateTime.setText(readFromFile("stateTime"));
		phoneNo.setText(readFromFile("phoneNo"));

		save.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				writeToFile("callDisconnectTime", callDisconnectTime.getText().toString());
				writeToFile("stateTime", stateTime.getText().toString());
				writeToFile("phoneNo", phoneNo.getText().toString());
                writeToFile("lastIdeal", "generated");
                writeToFile("lastState", "generated");
                writeToFile("lastRinging", "generated");

            }
		});

	}

}
