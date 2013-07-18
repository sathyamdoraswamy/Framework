package com.frameworkserver;


/**
 * This class stores the various fields of a database record at the server
 * @author sathyam
 */
public class ServerRecord implements java.io.Serializable
{        
	public String groupid;
	public String key, value, user, datatype;
	public long timestamp;
        public String deviceId;
}