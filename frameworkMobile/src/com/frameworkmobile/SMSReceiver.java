package com.frameworkmobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {
	// This function receives SMS messages from the server and sets lock status
	@Override
	public void onReceive(Context context, Intent intent) {
		// ---get the SMS message passed in---
		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;
		String str = "";
		if (bundle != null) {
			// ---retrieve the SMS message received---
			Object[] pdus = (Object[]) bundle.get("pdus");
			msgs = new SmsMessage[pdus.length];
			for (int i = 0; i < msgs.length; i++) {
				msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				// str += "SMS from " + msgs[i].getOriginatingAddress();
				// str += " :";
				str += msgs[i].getMessageBody().toString();
				// str += "\n";
			}
			// System.out.println("SMS message from receiver is : "+str);
			if (str != null) {
				String[] msg = str.split(" ");
				String method = msg[0];
				if (method.equals("FRAMEWORK")) {
					String namespace = msg[1];
					String status = msg[2];
					Toast.makeText(context, status, Toast.LENGTH_LONG).show();
					// System.out.println(str);
					Helper dh = new Helper(context);
					dh.open();
					dh.setLockStatus(namespace, status);
					dh.close();
					try {
						Thread.sleep(10000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Config c = new Config(context);
					String phoneNo = c.getPhoneNo();
					deleteSMS(context, str, phoneNo);
				}
			}
		}
	}

	// This function deletes SMS messages that are received from the server from
	// the phone
	public void deleteSMS(Context context, String message, String number) {
		try {
			// System.out.println("MESSAGE TO BE DELETED : "+message);
			// System.out.println("MESSAGE NUMBER : "+number);
			// System.out.println("Deleting SMS from inbox");
			Uri uriSms = Uri.parse("content://sms/inbox");
			Cursor c = context.getContentResolver().query(
					uriSms,
					new String[] { "_id", "thread_id", "address", "person",
							"date", "body" }, null, null, null);

			if (c != null && c.moveToFirst()) {
				do {
					long id = c.getLong(0);
					long threadId = c.getLong(1);
					String address = c.getString(2);
					String body = c.getString(5);
					// System.out.println("ADDRESS IS : "+address);
					if (message.equals(body) && address.equals(number)) {
						// System.out.println("Deleting SMS with id: " +
						// threadId);
						context.getContentResolver().delete(
								Uri.parse("content://sms/" + id), null, null);
					}
				} while (c.moveToNext());
			}
		} catch (Exception e) {
			// System.out.println("Could not delete SMS from inbox: " +
			// e.getMessage());
		}
	}
}