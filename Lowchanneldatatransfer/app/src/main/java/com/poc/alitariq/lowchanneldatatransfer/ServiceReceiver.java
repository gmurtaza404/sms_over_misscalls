package com.poc.alitariq.lowchanneldatatransfer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class ServiceReceiver extends BroadcastReceiver {
	TelephonyManager telephony;

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub

		MyPhoneStateListener phoneListener = new MyPhoneStateListener(context);
		telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		telephony.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		
	}

	public void onDestroy() {
		telephony.listen(null, PhoneStateListener.LISTEN_NONE);
	}

}
