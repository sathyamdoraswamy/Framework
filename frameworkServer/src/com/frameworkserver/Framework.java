package com.frameworkserver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Statement;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used to start the framework server
 * @author sathyam
 */
public class Framework extends Thread {

    int port = 8010;
    static String databasePath = "/home/sathyam/Application";
    static String storagePath = "/home/sathyam/Application";
    static boolean callback = false;
    static Events e = null;
    // static ArrayList<ServerThread> threadList = new ArrayList<ServerThread>();
    static NamespaceLock nl;

    /**
     * This is the constructor for the class
     * @param port the port on which to run the server
     * @param databasePath the path to the framework's database
     * @param storagePath the path containing the location where files received by the framework are stored
     */
    public Framework(int port, String databasePath, String storagePath) {
        this.port = port;
        this.databasePath = databasePath;
        this.storagePath = storagePath;
    }

    /**
     * This function runs the server and receives requests from various clients
     */
    public void run() {
        try {
            if (nl != null) {
                nl.start();
            }
            SynchronizedMutex lock = new SynchronizedMutex();
            //System.out.println("FRAMEWORK STARTED!!!");
            ServerSocket server = new ServerSocket(port);
            int index = 0;
            boolean flag = true;
            while (flag) {
                Socket connection = server.accept();
                connection.setSoTimeout(60000);
                //System.out.println("Accepted");
                ServerThread t = new ServerThread(index++, connection, lock);
                //threadList.add(t);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
     * This function is used to set the callback object whose doJob function is invoked when records are received
     * @param obj the callback object
     */
    public void setCallbackObject(Object obj) {
        e = (Events) obj;
        callback = true;
    }

    /**
     * This function is used to set the object of class NamespaceLock
     * @param obj object of class NamespaceLock
     */
    public void setNamespaceLock(Object obj) {
        nl = (NamespaceLock) obj;
    }

    /**
     * This function is used to get the database path set
     * @return path to the framework's database
     */
    public String getDatabasePath() {
        return databasePath;
    }

    /**
     * This function is used to get the storage path set
     * @return storeage location
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * This function is used to get the port set
     * @return port on which the server is run
     */
    public int getPort() {
        return port;
    }
}

// This class handles the requests coming from a client
class ServerThread extends Thread {

    SynchronizedMutex lock;
    DatabaseHelper dh;
    Socket connection;
    ObjectInputStream ois;
    ObjectOutputStream oos;
    int index;

    public ServerThread(int i, Socket con, SynchronizedMutex l) {
        this.dh = new DatabaseHelper(Framework.databasePath);
        index = i;
        connection = con;
        lock = l;
    }
    // This function is used to get the record stored in the given bundle

    public Record getRecordFromBundle(Bundle b) {
        Record r = null;
        try {
            String record = "";
            byte[] recd = b.data;
            record = new String(recd);
            //System.out.println("Record :\n" + record);
            String rec[] = record.split("\n");
            r = new Record();
            r.groupid = rec[0];
            r.key = rec[1];
            r.value = rec[2];
            r.user = rec[3];
            r.datatype = rec[4];
            r.timestamp = Long.parseLong(rec[5]);
            return r;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // This function is used to check the permission of the given user on the namespace present in the key

    boolean checkPermission(String key, String user) {
        String namespace = key.substring(0, key.indexOf("."));
        String query = "select * from namespaces where user='" + user + "' and namespace='" + namespace + "'";
        ArrayList<String[]> result = dh.executeQueryForResult(query);
        if (result != null && result.get(0)[2].indexOf("w") != -1) {
            return true;
        } else {
            return false;
        }
    }
    // This function is used to check the lock status of the namespace present in the key and compare it with the given deviceId

    boolean checkLock(String key, String deviceId) {
        if (Framework.nl != null) {
            String namespace = key.substring(0, key.indexOf("."));
            String id = Framework.nl.getDeviceId(namespace);
            if (id == null) {
                return true;
            }
            if (id.equals(deviceId)) {
                return true;
            }
            return false;
        }
        return true;
    }
    // This function is used to commit the given transaction into the main table

    public synchronized long commit(Transaction t, String user, String deviceId) {
        boolean flag = true, result = true;
        long timestamp = System.currentTimeMillis();
        String groupid = "";
        ArrayList<Record> records = new ArrayList<Record>();
        Record r = new Record();
        r = getRecordFromBundle(t.bundles[0]);
        if (!checkPermission(r.key, user)) {
            return -1;
        }
        if (Framework.nl != null && !checkLock(r.key, deviceId)) {
            return -1;
        }
        try {
            lock.acquire();
        } catch (InterruptedException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
        String namespace = r.key.substring(0, r.key.indexOf("."));
        groupid = r.groupid;
        String query = "select timestamp from server where groupid='" + r.groupid + "'";
        ArrayList<String[]> temp = dh.executeQueryForResult(query);
        if (temp != null && temp.size() != 0) {
            long current_timestamp = Long.parseLong(temp.get(0)[0]);
            if (r.timestamp != current_timestamp) {
                result = false;
                flag = false;
            }
        }
        while (flag) {
            try {
                Statement st1 = dh.con.createStatement();
                dh.con.setAutoCommit(false);
                for (int i = 0; i < t.noOfBundles; i++) {
                    query = "";
                    //Thread.sleep(1000);
                    r = new Record();
                    r = getRecordFromBundle(t.bundles[i]);
                    if (r.datatype.equals("file")) {
                        int l = Integer.parseInt(r.value.substring(0, r.value.indexOf(' ')));
                        String fileName = r.value.substring(r.value.indexOf(' ') + 1);
                        fileName = deviceId + "_" + t.transactionId + "_" + i + "_" + fileName;
                        String path = Framework.storagePath + "/" + fileName;
                        File f = new File(path);
                        FileOutputStream fos = new FileOutputStream(f);
                        for (int j = 1; j <= l; j++) {
                            Bundle b = t.bundles[i + j];
                            fos.write(b.data);
                        }
                        fos.close();
                        i += l;
                        r.value = path;
                    }
                    if (Framework.callback) {
                        records.add(r);
                    }
                    query = "delete from server  where groupid='" + r.groupid + "' and key='" + r.key + "'";
                    st1.executeUpdate(query);
                    query = "insert into server values('" + r.groupid
                            + "','" + r.key + "','" + r.value + "','" + r.user
                            + "','" + r.datatype + "'," + timestamp + ",'"
                            + deviceId + "')";
                    st1.executeUpdate(query);
                }
                query = "update server set timestamp=" + timestamp + " where groupid='" + groupid + "'";
                st1.executeUpdate(query);
                dh.con.commit();

                if (Framework.callback) {
                    String key = records.get(0).key;
                    String className = key.substring(key.indexOf(".") + 1, key.lastIndexOf("."));
                    Object obj = dh.getObject(className, groupid);
                    if (obj != null) {
                        Framework.e.doJob(obj);
                    }
                }
                flag = false;
            } catch (SQLException e) {
                e.printStackTrace();
                if (dh.con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        dh.con.rollback();
                        records = null;
                        records = new ArrayList<Record>();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    flag = false;
                    timestamp = -1;
                }
                if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                    flag = false;
                    timestamp = -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (dh.con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        dh.con.rollback();
                        records = null;
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                result = false;
                flag = false;
            } finally {
                try {
                    dh.con.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        lock.release();
        if (result) {
            return timestamp;
        } else {
            return commitToBackup(t, deviceId);
        }
    }
    // This function is used to commit the given transaction into the backup table

    public synchronized long commitToBackup(Transaction t, String deviceId) {
        boolean flag = true;
        String groupid = "";
        long timestamp = System.currentTimeMillis();
        ArrayList<Record> records = new ArrayList<Record>();
        while (flag) {
            try {
                Statement st1 = dh.con.createStatement();
                dh.con.setAutoCommit(false);
                for (int i = 0; i < t.noOfBundles; i++) {
                    String record = "";
                    String query = "";
                    Bundle b = t.bundles[i];
                    byte[] recd = b.data;
                    record = new String(recd);
                    String rec[] = record.split("\n");
                    Record r = new Record();
                    r.groupid = rec[0];
                    groupid = r.groupid;
                    r.key = rec[1];
                    r.value = rec[2];
                    r.user = rec[3];
                    r.datatype = rec[4];
                    r.timestamp = Long.parseLong(rec[5]);
                    if (r.datatype.equals("file")) {
                        int l = Integer.parseInt(r.value.substring(0, r.value.indexOf(' ')));
                        String fileName = r.value.substring(r.value.indexOf(' ') + 1);
                        fileName = deviceId + "_" + t.transactionId + "_" + i + "_" + fileName;
                        String path = Framework.storagePath + "/" + fileName;
                        File f = new File(path);
                        FileOutputStream fos = new FileOutputStream(f);
                        for (int j = 1; j <= l; j++) {
                            b = t.bundles[i + j];
                            fos.write(b.data);
                        }
                        fos.close();
                        i += l;
                        r.value = path;
                    }
                    if (Framework.callback) {
                        records.add(r);
                    }
                    query = "insert into serverbackup values('" + r.groupid
                            + "','" + r.key + "','" + r.value + "','" + r.user
                            + "','" + r.datatype + "'," + timestamp + ",'"
                            + deviceId + "')";
                    st1.executeUpdate(query);
                }
                dh.con.commit();
                if (Framework.callback) {
                    String key = records.get(0).key;
                    String className = key.substring(key.indexOf(".") + 1, key.lastIndexOf("."));
                    Object obj = dh.getObject(className, groupid);
                    if (obj != null) {
                        Framework.e.doJob(obj);
                    }
                }
                flag = false;
            } catch (SQLException e) {
                if (dh.con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        dh.con.rollback();
                        records = null;
                        records = new ArrayList<Record>();
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                    flag = false;
                    timestamp = -1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (dh.con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        dh.con.rollback();
                        records = null;
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                }
                flag = false;
                timestamp = -1;
            } finally {
                try {
                    dh.con.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return timestamp;
    }
    // This class receieves acknowledgements to the bundles sent to the client

    class ACKThread extends Thread {

        Transaction[] t;
        int seq;
        String deviceId;

        public ACKThread(DatabaseHelper dh, Transaction[] t, ObjectInputStream ois, String deviceId, int seq) {
            this.t = t;
            this.deviceId = deviceId;
            this.seq = seq;
        }
        // This function sends the acknowledgements

        public void run() {
            //System.out.println("ACK STARTED");
            Bundle b;
            int i = 0, j;
            try {
                for (i = 0; i < t.length; i++) {
                    for (j = seq; j < t[i].noOfBundles; j++) {
                        do {
                            //System.out.println("SEQ ACK : " + t[i].noOfBundles);
                            byte[] buffer = (byte[]) ois.readObject();
                            b = new Bundle();
                            b.parse(buffer);
                            //System.out.println("ACK WAITING : " + j);
                            //System.out.println("ACK RECEIVED : " + b.bundleNumber);
                            //System.out.println(t[i].transactionId + " " + t[i].noOfBundles + " " + j);
                        } while (!b.isAcknowledgement(t[i].transactionId, t[i].noOfBundles, j));
                    }
                    seq = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                ObjectOutputStream fout;
                try {
                    //System.out.println("Writing broken transaction");
                    fout = new ObjectOutputStream(new FileOutputStream(new File(deviceId + "_pull")));
                    fout.writeObject(t);
                    fout.close();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            //System.out.println("ACK ENDED");
        }
    }
    // This function runs constantly and serves requests from the client

    public void run() {
        try {
            oos = new ObjectOutputStream(connection.getOutputStream());
            ois = new ObjectInputStream(connection.getInputStream());
            String deviceId = "";
            String user = "";
            ArrayList<String[]> namespaces = null;
            Long[] ids = null;
            while (true) {
                byte[] buffer = (byte[]) ois.readObject();
                String msg = new String(buffer);
                //System.out.println("MESSAGE : " + msg);
                if (msg.equals("UPDATE")) {
                    //System.out.println("UPDATE START");
                    buffer = (byte[]) ois.readObject();
                    msg = new String(buffer);
                    deviceId = msg.substring(0, msg.indexOf(' '));
                    msg = msg.substring(msg.indexOf(' ') + 1);
                    user = msg;
                    namespaces = dh.getNamespaceRecords(user);
                    String temp = "NULL";
                    if (namespaces != null) {
                        int l = namespaces.size();
                        if (l == 1) {
                            temp = namespaces.get(0)[1];
                        } else {
                            temp = namespaces.get(0)[1];
                            int i;
                            for (i = 1; i < l; i++) {
                                temp = temp + "\n" + namespaces.get(i)[1];
                            }
                        }
                    }
                    oos.writeObject(temp.getBytes());
                    oos.flush();
                    if (temp.equals("NULL")) {
                        //System.out.println("UPDATE OVER");
                        continue;
                    }
                    buffer = (byte[]) ois.readObject();
                    String[] id = (new String(buffer)).split("\n");
                    int l = id.length;
                    ids = new Long[l];
                    for (int i = 0; i < l; i++) {
                        ids[i] = Long.parseLong(id[i]);
                    }
                    //System.out.println("UPDATE OVER");
                } else if (msg.equals("PULL")) {
                    //System.out.println("PULL START");
                    ACKThread ack = null;
                    try {
                        buffer = (byte[]) ois.readObject();
                        Bundle b = new Bundle();
                        b.parse(buffer);
                        if (b.bundleType == b.RETRANS) {
                            File f = new File(deviceId + "_pull");
                            if (!f.exists()) {
                                b = new Bundle();
                                b.userId = 1;
                                b.transactionId = -1;
                                b.bundleType = b.STOP;
                                b.noOfBundles = 1;
                                b.bundleNumber = -1;
                                b.bundleSize = 0;
                                b.data = null;
                                oos.writeObject(b.getBytes());
                                oos.flush();
                                //System.out.println("SENT STOP BUNDLE");
                                //System.out.println("PULL OVER");
                                continue;
                            }
                            String[] info = (new String(b.data)).split("\n");
                            int tid = Integer.parseInt(info[0]);
                            int bno = Integer.parseInt(info[1]);
                            ObjectInputStream fin = new ObjectInputStream(new FileInputStream(f));
                            Transaction[] tt = (Transaction[]) fin.readObject();
                            fin.close();
                            Transaction[] t = new Transaction[1];
                            t[0] = tt[tid];
                            if (t[0].bundles == null) {
                                t[0].organizeBundles();
                            }
                            f.delete();
                            ack = new ACKThread(dh, t, ois, deviceId, bno);
                            ack.start();
                            for (int bi = bno; bi < t[0].noOfBundles; bi++) {
                                byte[] bundle = t[0].bundles[bi].getBytes();
                                //Thread.sleep(1000);
                                oos.writeObject(bundle);
                                oos.flush();
                                //System.out.println("Sent bundle -> Transaction id : " + tid + " Bundle No. : " + bi);
                            }
                            ack.join();
                        }
                        if (b.bundleType == b.START) {
                            //System.out.println("Start bundle received");
                            Transaction[] transactions = null;
                            ArrayList<String[]> list = dh.selectNewRecords(namespaces, ids, deviceId);
                            if (list != null) {
                                int l = list.size();
                                Record[] records = new Record[l];
                                int n = 0;
                                String gid = "";
                                for (int i = 0; i < l; i++) {
                                    records[i] = new Record();
                                    records[i].rowid = Long.parseLong(list.get(i)[0]);
                                    records[i].groupid = list.get(i)[1];
                                    records[i].key = list.get(i)[2];
                                    records[i].value = list.get(i)[3];
                                    records[i].user = list.get(i)[4];
                                    records[i].datatype = list.get(i)[5];
                                    records[i].timestamp = Long.parseLong(list.get(i)[6]);
                                    if (!(gid.equals(records[i].groupid))) {
                                        n++;
                                        gid = records[i].groupid;
                                    }
                                }
                                transactions = new Transaction[n];
                                int t = 0, r = 0;
                                transactions[t] = new Transaction();
                                transactions[t].transactionId = t;//records[r].groupid;
                                transactions[t].addRecord(records[r]);
                                r++;
                                while (r < records.length) {
                                    while (r < records.length && records[r].groupid.equals(records[r - 1].groupid)) {
                                        transactions[t].addRecord(records[r]);
                                        r++;
                                    }
                                    if (r < records.length) {
                                        t++;
                                        transactions[t] = new Transaction();
                                        transactions[t].transactionId = t;//records[r].groupid;
                                        transactions[t].addRecord(records[r]);
                                        r++;
                                    }
                                }
                                ack = new ACKThread(dh, transactions, ois, deviceId, 0);
                                ack.start();
                                for (t = 0; t < transactions.length; t++) {
                                    transactions[t].organizeBundles();
                                    for (int bi = 0; bi < transactions[t].noOfBundles; bi++) {
                                        byte[] bundle = transactions[t].bundles[bi].getBytes();
                                        //Thread.sleep(1000);
                                        oos.writeObject(bundle);
                                        oos.flush();
                                        //System.out.println("Sent bundle -> Transaction id : " + t + " Bundle No. : " + bi);
                                    }
                                }
                                ack.join();
                            }
                            b = new Bundle();
                            b.userId = 1;
                            b.transactionId = -1;
                            b.bundleType = b.STOP;
                            b.noOfBundles = 1;
                            b.bundleNumber = -1;
                            b.bundleSize = 0;
                            b.data = null;
                            oos.writeObject(b.getBytes());
                            oos.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (ack != null) {
                            try {
                                ack.join();
                            } catch (InterruptedException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                            }
                        }
                        //System.out.println("Caught Outside");
                    }
                    //System.out.println("PULL OVER");
                } else if (msg.equals("PUSH")) {
                    //System.out.println("PUSH START");
                    Transaction t = null;
                    ArrayList<Long> tlist = null;
                    String fileName = deviceId + "_push";
                    File f = new File(fileName);
                    //System.out.println("ABSOLUTE PATH:"+f.getAbsolutePath());
                    try {
                        boolean flag = true;
                        if (f.exists()) {
                            //System.out.println("File exists");
                            ObjectInputStream fin = new ObjectInputStream(new FileInputStream(f));
                            t = (Transaction) fin.readObject();
                            tlist = (ArrayList<Long>) fin.readObject();
                            fin.close();
                            boolean result = f.delete();
                            Bundle b = new Bundle();
                            b.userId = 1;
                            b.transactionId = -1;
                            b.bundleType = b.RETRANS;
                            b.noOfBundles = 1;
                            b.bundleNumber = -1;
                            String s = Integer.toString(t.transactionId) + "\n" + Integer.toString((t.bundleNo + 1));
                            for (int i = 0; i < tlist.size(); i++) {
                                s = s + "\n" + tlist.get(i);
                            }
                            //System.out.println("String is:" + s + "\n");
                            byte[] bb = s.getBytes();
                            int size = bb.length;
                            b.bundleSize = bb.length;
                            b.data = new byte[size];
                            for (int j = 0; j < size; j++) {
                                b.data[j] = bb[j];
                            }
                            //System.out.println("Sent retrans : " + b.transactionId + " " + b.noOfBundles + " " + b.bundleNumber + " " + b.bundleType + " " + new String(b.data));
                            oos.writeObject(b.getBytes());
                            oos.flush();
                            flag = true;
                            while (flag) {
                                buffer = (byte[]) ois.readObject();
                                b = new Bundle();
                                b.parse(buffer);
                                if (b.bundleType == b.STOP) {
                                    t = null;
                                    break;
                                }
                                t.addBundle(b);
                                if (b.bundleNumber == (b.noOfBundles - 1)) {
                                    long ts = commit(t, user, deviceId);
                                    tlist.add(ts);
                                    Bundle ack = new Bundle();
                                    ack.userId = b.userId;
                                    ack.transactionId = b.transactionId;
                                    ack.bundleType = b.ACK;
                                    ack.noOfBundles = b.noOfBundles;
                                    ack.bundleNumber = b.bundleNumber;
                                    bb = (Long.toString(ts)).getBytes();
                                    size = bb.length;
                                    ack.bundleSize = bb.length;
                                    ack.data = new byte[size];
                                    for (int j = 0; j < size; j++) {
                                        ack.data[j] = bb[j];
                                    }
                                    //System.out.println("Sent ack : " + ack.transactionId + " " + ack.noOfBundles + " " + ack.bundleNumber + " " + ack.bundleType);
                                    //Thread.sleep(1000);
                                    oos.writeObject(ack.getBytes());
                                    oos.flush();
                                    continue;
                                }
                                Bundle ack = new Bundle();
                                ack.createACK(b);
                                //Thread.sleep(1000);
                                //System.out.println("Sent ack : " + ack.transactionId + " " + ack.noOfBundles + " " + ack.bundleNumber + " " + ack.bundleType);
                                oos.writeObject(ack.getBytes());
                                oos.flush();
                            }
                            //System.out.println("PUSH OVER");
                        } else {
                            tlist = new ArrayList<Long>();
                            Bundle b = new Bundle();
                            b.userId = 1;
                            b.transactionId = -1;
                            b.bundleType = b.START;
                            b.noOfBundles = 1;
                            b.bundleNumber = -1;
                            b.bundleSize = 0;
                            b.data = null;
                            oos.writeObject(b.getBytes());
                            flag = true;
                            do {
                                buffer = (byte[]) ois.readObject();
                                b = new Bundle();
                                b.parse(buffer);
                                if (b.bundleType == b.STOP) {
                                    t = null;
                                    break;
                                }
                                if (b.bundleNumber == 0) {
                                    t = new Transaction(b.transactionId, b.noOfBundles);
                                }
                                t.addBundle(b);
                                if (b.bundleNumber == (b.noOfBundles - 1)) {
                                    long ts = commit(t, user, deviceId);//use update if want to delete and add
                                    tlist.add(ts);
                                    Bundle ack = new Bundle();
                                    ack.userId = b.userId;
                                    ack.transactionId = b.transactionId;
                                    ack.bundleType = b.ACK;
                                    ack.noOfBundles = b.noOfBundles;
                                    ack.bundleNumber = b.bundleNumber;
                                    byte[] bb = (Long.toString(ts)).getBytes();
                                    int size = bb.length;
                                    ack.bundleSize = bb.length;
                                    ack.data = new byte[size];
                                    for (int j = 0; j < size; j++) {
                                        ack.data[j] = bb[j];
                                    }
                                    //System.out.println("Sent ack : " + ack.transactionId + " " + ack.noOfBundles + " " + ack.bundleNumber + " " + ack.bundleType + " " + new String(ack.data));
                                    //Thread.sleep(1000);
                                    oos.writeObject(ack.getBytes());
                                    oos.flush();
                                    continue;
                                }
                                Bundle ack = new Bundle();
                                ack.createACK(b);
                                //Thread.sleep(1000);
                                //System.out.println("Sent ack : " + ack.transactionId + " " + ack.noOfBundles + " " + ack.bundleNumber + " " + ack.bundleType);
                                oos.writeObject(ack.getBytes());
                                oos.flush();
                            } while (flag);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (t != null) {
                            //System.out.println("Writing broken transaction!!!");
                            f.createNewFile();
                            ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(f));
                            fout.writeObject(t);
                            fout.writeObject(tlist);
                            fout.close();
                        }
                    }
                    //System.out.println("PUSH OVER");
                } else if (msg.equals("NAMESPACE")) {
                    //System.out.println("NAMESPACE START");
                    boolean flag = true;
                    try {
                        do {
                            //System.out.print("Going to read");
                            buffer = (byte[]) ois.readObject();
                            //System.out.println("Read");
                            Bundle b = new Bundle();
                            b.parse(buffer);
                            //System.out.println("Parsed");
                            if (b.bundleType == b.STOP) {
                                //System.out.println("OVER");
                                break;
                            }
                            String data = new String(b.data);
                            //System.out.println("DATA IS :" + data);
                            String[] fields = data.split("\n");
                            String method = fields[0];
                            user = fields[1];
                            String namespace = fields[2];
                            String permission = fields[3];
                            if (method.equals("ADD")) {
                                dh.addNamespace(user, namespace, permission);
                            } else if (method.equals("REMOVE")) {
                                dh.removeNamespace(user, namespace, permission);
                            }
                            Bundle ack = new Bundle();
                            ack.createACK(b);
                            //System.out.println("Sent ack : " + ack.transactionId + " " + ack.noOfBundles + " " + ack.bundleNumber + " " + ack.bundleType);
                            oos.writeObject(ack.getBytes());
                            oos.flush();
                        } while (flag);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //System.out.println("NAMESPACE OVER");
                } else if (msg.equals("END")) {
                    connection.close();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}