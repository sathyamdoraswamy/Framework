package com.frameworkmobile;

/**
 * This class stores the various fields of a database record
 * 
 * @author sathyam
 * 
 */
public class Record implements java.io.Serializable {
	public long rowid;
	public String groupid;
	public String key, value, user, datatype, synced;
	public long timestamp;
}