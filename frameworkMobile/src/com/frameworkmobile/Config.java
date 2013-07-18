package com.frameworkmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * This class is used to set certain parameters used by the framework to run
 * 
 * @author sathyam
 * 
 */
public class Config {
	/**
	 * Stores the current context
	 */
	Context c;

	/**
	 * This is the constructor for this class which is passed the current
	 * context
	 * 
	 * @param c
	 *            current context
	 */
	public Config(Context c) {
		this.c = c;
	}

	/**
	 * This function is used to set the user name for the framework
	 * 
	 * @param value
	 *            user name
	 */
	public void setUser(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("USER", value);
		edit.commit();
	}

	/**
	 * This function is used to set the user id for the framework
	 * 
	 * @param value
	 *            user id
	 */
	public void setUserId(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("USERID", value);
		edit.commit();
	}

	/**
	 * This function is used to set the IP of the server to which the framework
	 * connects
	 * 
	 * @param value
	 *            IP
	 */
	public void setIP(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("IP", value);
		edit.commit();
	}

	/**
	 * This function is used to set the port of the server to which the
	 * framework connects
	 * 
	 * @param value
	 *            port
	 */
	public void setPort(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("PORT", value);
		edit.commit();
	}

	/**
	 * This function is used to set the location where files received by the
	 * framework are stored
	 * 
	 * @param value
	 *            storage path
	 */
	public void setStoragePath(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("PATH", value);
		edit.commit();
	}

	/**
	 * This function is used to set the phone number which is used when
	 * namespace lock is used
	 * 
	 * @param value
	 *            phone number
	 */
	public void setPhoneNo(String value) {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		Editor edit = prefs.edit();
		edit.putString("PHONENO", value);
		edit.commit();
	}

	/**
	 * This function is used to get the user name value set
	 * 
	 * @return user name
	 */
	public String getUser() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("USER", null);
		return value;
	}

	/**
	 * This function is used to get the user id value set
	 * 
	 * @return user id
	 */
	public String getUserId() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("USERID", null);
		return value;
	}

	/**
	 * This function is used to get the IP value set
	 * 
	 * @return IP
	 */
	public String getIP() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("IP", null);
		return value;
	}

	/**
	 * This function is used to get the port value set
	 * 
	 * @return port
	 */
	public String getPort() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("PORT", null);
		return value;
	}

	/**
	 * This function is used to get the storage location value set
	 * 
	 * @return storage path
	 */
	public String getStoragePath() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("PATH", null);
		return value;
	}

	/**
	 * This function is used to get the phone number value set
	 * 
	 * @return phone number
	 */
	public String getPhoneNo() {
		SharedPreferences prefs = c.getSharedPreferences("DATA", 0);
		String value = prefs.getString("PHONENO", null);
		return value;
	}

}
