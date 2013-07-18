package com.frameworkserver;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used when the namespace locking mechanism is used and a dongle is attached to the machine on which the server is run<br>
 * An object of this class is to be passed to the setNamespaceLock function in  Framework.java
 * @author sathyam
 */
public class NamespaceLock extends Thread {

    class Lock {

        String namespace;
        String deviceId;
        long timestamp;
    }
    static ArrayList<Lock> locks;
    static DatabaseHelper dh;
    static long timeout = 0;

    /**
     * This is the constructor for this class
     * @param databasePath the path to the framework database
     * @param timeout the timeout to be used for the lock(in milliseconds)
     */
    public NamespaceLock(String databasePath, long timeout) {
        this.dh = new DatabaseHelper(databasePath);
        this.timeout = timeout;
        init();
    }

    // This function initializes the locks from the namespaces present in the namespaces table
    protected void init() {
        locks = new ArrayList<Lock>();
        ArrayList<String[]> namespaces = dh.getNamespaces();
        if (namespaces != null) {
            for (String s[] : namespaces) {
                Lock l = new Lock();
                l.namespace = s[0];
                l.deviceId = null;
                l.timestamp = 0;
                locks.add(l);
            }
        }
    }

    // This function is used to unlock a given namespace
    protected void unlock(String user, String namespace, String deviceId) {
        for (int i = 0; i < locks.size(); i++) {
            if (locks.get(i).namespace.equals(namespace)) {
                if (locks.get(i).deviceId.equals(deviceId)) {
                    locks.get(i).deviceId = null;
                }
                break;
            }
        }
    }

    // This function is used to lock a given namespace 
    protected boolean lock(String user, String namespace, String deviceId) {
        long ts = System.currentTimeMillis();
        boolean result = false;
        for (int i = 0; i < locks.size(); i++) {
            if (locks.get(i).namespace.equals(namespace)) {
                if (locks.get(i).deviceId == null || (ts - locks.get(i).timestamp) > timeout) {
                    locks.get(i).deviceId = deviceId;
                    locks.get(i).timestamp = ts;
                    result = true;
                }
                break;
            }
        }
        return result;
    }

    // This function is used to get the deviceId of the mobile holding a lock to the given namespace
    protected static synchronized String getDeviceId(String namespace) {
        for (int i = 0; i < locks.size(); i++) {
            if (locks.get(i).namespace.equals(namespace)) {
                return locks.get(i).deviceId;
            }
        }
        return null;
    }

    // This function runs constantly and receives lock and unlock requests from clients
    public void run() {
        boolean flag = true;
        while (flag) {
            String sms = getSMS();
            if (sms != null) {
                deleteSMS();
                //System.out.println(sms);
                String lines[] = sms.split("\n");
                String method = lines[0];
                String user = lines[1];
                String namespace = lines[2];
                String deviceId = lines[3];
                String mobileNo = lines[4];
                if (method.equals("LOCK") && dh.checkPermission(user, namespace)) {
                    boolean result = lock(user, namespace, deviceId);
                    String message = "";
                    if (result) {
                        message = "FRAMEWORK" + "\n" + namespace + "\nPENDING";
                    } else {
                        message = "FRAMEWORK" + "\n" + namespace + "\nUNLOCKED";
                    }
                    //System.out.println(message);
                    sendSMS(message, mobileNo);
                }
                if (method.equals("UNLOCK") && dh.checkPermission(user, namespace)) {
                    unlock(user, namespace, deviceId);
                }
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(NamespaceLock.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // This function is used to send an SMS to the client
    protected void sendSMS(String message, String mobileNo) {
        String command = "bash sendsms.sh " + mobileNo + " " + message;
        try {
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String temp = in.readLine();
            while (temp != null) {
                //System.out.println(temp);
                temp = in.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // This function is used to read SMS messages from the attached dongle
    protected static String getSMS() {
        //System.out.println("Reading SMS");
        try {
            String command = "sudo gammu --getsms 1 1";
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String temp = in.readLine();
            while (temp != null) {
                //System.out.println(temp);
                if (temp.indexOf("Empty") != -1) {
                    return null;
                }
                if (temp.indexOf("Status") != -1) {
                    temp = in.readLine();
                    temp = in.readLine();
                    String sms = temp;
                    temp = in.readLine();
                    while (temp != null) {
                        sms = sms + "\n" + temp;
                        temp = in.readLine();
                    }
                    return sms;
                }
                temp = in.readLine();
            }
            in.close();
            return temp;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // This function is used to delete messages from the attached dongle
    protected static boolean deleteSMS() {
        try {
            String command = "sudo gammu --deletesms 1 1";
            Process p = Runtime.getRuntime().exec(command);
            //System.out.println("After");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String temp = in.readLine();
            while (temp != null) {
                //System.out.println(temp);
                if (temp.indexOf("empty") != -1) {
                    return false;
                }
                //System.out.println("-------------------------");
                temp = in.readLine();
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
