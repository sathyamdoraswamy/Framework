package com.frameworkserver;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 
 * This class is used to perform database operations
 * @author sathyam
 */
public class DatabaseHelper {

    Connection con;
    String user = "SATHYAM";
    String databasePath = "/home/sathyam/Application";

    /**
     * This is the default constructor for this class which is passed the database path
     * @param value the path to the database
     */
    public DatabaseHelper(String value) {
        databasePath = value;
        getConnection();
    }
    // This function is used to connect to the database

    protected void getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            try {
                con = DriverManager.getConnection("jdbc:sqlite:" + databasePath + "/frameworkdb");
                Statement stat = con.createStatement();
                stat.executeUpdate("CREATE TABLE server(groupid TEXT,key text,value text,user text,datatype text,timestamp number,deviceid text, primary key(groupid,key,timestamp));");
                stat.executeUpdate("CREATE TABLE serverbackup(groupid TEXT,key text,value text,user text,datatype text,timestamp number,deviceid text);");
                stat.executeUpdate("CREATE TABLE namespaces(user TEXT,namespace TEXT,permission TEXT,primary key(user,namespace));");
            } catch (SQLException e) {
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    // This function is used to execute the given query

    protected boolean executeQuery(String query) {
        boolean result = true;
        if (con != null) {
            boolean flag = true;
            while (flag) {
                try {
                    Statement st1 = con.createStatement();
                    st1.executeUpdate(query);
                    flag = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                        flag = false;
                        result = false;
                    }
                }
            }
        } else {
            result = false;
        }
        return result;
    }
    // This function is used to execute the given query and get the results

    protected ArrayList<String[]> executeQueryForResult(String query) {
        ArrayList<String[]> result = null;
        boolean flag = true;
        if (con != null) {
            while (flag) {
                try {
                    PreparedStatement st1 = con.prepareStatement(query);
                    ResultSet rs1 = st1.executeQuery();
                    if (rs1.next()) {
                        result = new ArrayList<String[]>();
                        ResultSetMetaData rsmd = rs1.getMetaData();
                        int col_cnt = rsmd.getColumnCount();
                        do {
                            String[] row = new String[col_cnt];
                            for (int ik = 1; ik <= col_cnt; ik++) {
                                row[ik - 1] = rs1.getString(ik);
                            }
                            result.add(row);
                        } while (rs1.next());
                    } else {
                        result = null;
                    }
                    flag = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                        flag = false;
                        result = null;
                    }
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * This function is used to add a namespace
     * @param user user name
     * @param namespace namespace to add
     * @param permission user's permission on the namespace
     * @return boolean indicating success or failure of the function
     */
    public boolean addNamespace(String user, String namespace, String permission) {
        boolean result = true;
        boolean flag = true;
        if (con != null) {
            while (flag) {

                try {
                    Statement st1 = con.createStatement();
                    con.setAutoCommit(false);
                    String query = "delete from namespaces where user='" + user + "' and namespace='" + namespace + "';";
                    st1.executeUpdate(query);
                    query = "insert into namespaces values('" + user + "','" + namespace + "','" + permission + "');";
                    st1.executeUpdate(query);
                    con.commit();
                    flag = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (con != null) {
                        try {
                            System.err.print("Transaction is being rolled back");
                            con.rollback();
                            flag = true;
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                        flag = false;
                        result = false;
                    }
                } finally {
                    try {
                        con.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            result = false;
            //System.out.println("Connection broken!!!");
        }
        return result;
    }

    /**
     * This function is used to remove a namespace
     * @param user user name
     * @param namespace namespace to remove
     * @param permission user's permission on the namespace
     * @return boolean indicating success or failure of the function
     */
    public boolean removeNamespace(String user, String namespace, String permission) {
        String query = "delete from namespaces where user='" + user + "' and namespace='" + namespace + "' and permission='" + permission + "';";
        return executeQuery(query);
    }

    /**
     * This function is used to put a single record into the database
     * @param key the key of the record
     * @param value the value of the record
     * @param datatype the datatype of the record
     * @return boolean indicating success or failure of the function
     */
    public boolean putToRepository(String key, String value, String datatype) {
        String groupid = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis();
        String query = "insert into server values('" + groupid + "','" + key + "','" + value + "','" + user + "','" + datatype + "'," + timestamp + "," + "'SERVER');";
        boolean result = executeQuery(query);
        return result;
    }

    /**
     * This function is used to update a record identified by (groupid, key)
     * @param groupid groupid used to identify record
     * @param key key used to identify record
     * @param value new value with which to update the value field of the record
     * @param datatype new value with which to update the datatype field of the record
     * @return boolean indicating success or failure of the function
     */
    public boolean updateRepository(String groupid, String key, String value, String datatype) {
        String query = "update server set value='" + value + "', datatype='" + datatype + "' where groupid='" + groupid + "' and key='" + key + "';";
        boolean result = executeQuery(query);
        return result;
    }

    /**
     * This function is used to put a set of records into the database
     * @param records an array of type 'Record' to be inserted into the database
     * @return boolean indicating success or failure of the function
     */
    public boolean putToRepository(Record[] records) {
        boolean result = true;
        String groupid = UUID.randomUUID().toString();
        boolean flag = true;
        long timestamp = System.currentTimeMillis();
        if (con != null) {
            while (flag) {

                try {
                    Statement st1 = con.createStatement();
                    con.setAutoCommit(false);
                    for (int i = 0; i < records.length; i++) {
                        String query = "insert into server values('" + groupid + "','" + records[i].key + "','" + records[i].value + "','" + user + "','" + records[i].datatype + "'," + timestamp + ",'SERVER');";
                        //System.out.println(query);
                        st1.executeUpdate(query);
                    }
                    con.commit();
                    flag = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (con != null) {
                        try {
                            System.err.print("Transaction is being rolled back");
                            con.rollback();
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                        flag = false;
                        result = false;
                    }
                } finally {
                    try {
                        con.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            result = false;
            //System.out.println("Connection broken!!!");
        }
        return result;
    }

    /**
     * This function is used to update a set of records using (groupid, key) fields of the records
     * @param records an array of type 'Record' that is used to update records
     * @return boolean indicating success or failure of the function
     * 	 */
    public boolean updateRepository(Record[] records) {
        boolean result = true;
        boolean flag = true;
        if (con != null) {
            while (flag) {

                try {
                    Statement st1 = con.createStatement();
                    con.setAutoCommit(false);
                    String query;
                    for (int i = 0; i < records.length; i++) {
                        query = "update server set value='" + records[i].value + "', datatype='" + records[i].datatype + "' where groupid='" + records[i].groupid + "' and key='" + records[i].key + "';";
                        st1.executeUpdate(query);
                    }

                    con.commit();
                    flag = false;
                } catch (SQLException e) {
                    e.printStackTrace();
                    if (con != null) {
                        try {
                            System.err.print("Transaction is being rolled back");
                            con.rollback();
                            flag = true;
                        } catch (SQLException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                        flag = false;
                        result = false;
                    }
                } finally {
                    try {
                        con.setAutoCommit(true);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } else {
            result = false;
            //System.out.println("Connection broken!!!");
        }
        return result;
    }

    /**
     * This function is used to get an arraylist of records whose key matches the given key
     * @param key the key pattern to be matched with
     * @return an arraylist of records matching the given key
     */
    public ArrayList<Record> getRecords(String key) {
        ArrayList<Record> records = null;
        String query = "select * from server where key like '%" + key + "%' order by groupid;";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            records = new ArrayList<Record>();
            int l = result.size();
            int i = 0;
            do {
                Record r = new Record();
                r.groupid = result.get(i)[0];
                r.key = result.get(i)[1];
                r.value = result.get(i)[2];
                r.user = result.get(i)[3];
                r.datatype = result.get(i)[4];
                r.timestamp = Long.parseLong(result.get(i)[5]);
                records.add(r);
                i++;
            } while (i < l);
        }
        return records;
    }

    /** This function is used to put an object of a class into the database under a given namespace
     * 
     * @param obj the object to be inserted into the database
     * @param namespace the namespace under which to insert the object
     * @return boolean indicating success or failure of the function
     */
    public boolean putObject(Object obj, String namespace) {
        try {
            Class c = obj.getClass();
            String className = c.getName();
            Field[] fields = c.getDeclaredFields();
            Record[] r = new Record[fields.length];
            int i = 0;
            for (Field f : fields) {
                r[i] = new Record();
                String fieldName = f.getName();
                r[i].key = namespace + "." + className + "." + fieldName;
                r[i].value = "";
                if (f.get(obj) != null) {
                    r[i].value = f.get(obj).toString();
                    if (fieldName.contains("Path")) {
                        r[i].datatype = "file";
                    } else {
                        r[i].datatype = "data";
                    }
                } else {
                    r[i].datatype = "";
                    r[i].value = "";
                }
                i++;
            }
            return putToRepository(r);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This function is used to update an object present in the database identified by groupid and namespace
     * 
     * @param groupid groupid to identify object's records
     * @param obj object containing new values to update the records of the object
     * @param namespace namespace to identify object's records
     * @return boolean indicating success or failure of the function
     */
    public boolean updateObject(String groupid, Object obj, String namespace) {
        try {
            Class c = obj.getClass();
            String className = c.getName();
            Field[] fields = c.getDeclaredFields();
            Record[] r = new Record[fields.length];
            int i = 0;
            for (Field f : fields) {
                r[i] = new Record();
                r[i].groupid = groupid;
                String fieldName = f.getName();
                r[i].key = namespace + "." + className + "." + fieldName;
                r[i].value = "";
                if (f.get(obj) != null) {
                    r[i].value = f.get(obj).toString();
                    if (fieldName.contains("Path")) {
                        r[i].datatype = "file";
                    } else {
                        r[i].datatype = "data";
                    }
                } else {
                    r[i].datatype = "";
                    r[i].value = "";
                }
                i++;
            }
            return updateRepository(r);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * This function is used to get an arraylist of objects present in the database belonging to a particular class
     * @param className used to specify the class whose objects need to be fetched
     * @return an arraylist of objects of type 'Set' containing required object and metadata 
     */
    public ArrayList<Set> getObjects(String className) {
        ArrayList<Record> records = getRecords(className);
        if (records != null) {
            String key = records.get(0).key;
            String namespace = key.substring(0, key.indexOf("."));
            //System.out.println("NAMESPACE IS:" + namespace);
            ArrayList<Set> objects = new ArrayList<Set>();
            int l = records.size();
            Record p = records.get(0);
            Record c = records.get(0);
            int i = 0;
            while (i < l) {
                ArrayList<Record> r = new ArrayList<Record>();
                Set o = new Set();
                while (p.groupid.equals(c.groupid)) {
                    r.add(c);
                    p = c;
                    i++;
                    if (i == l) {
                        break;
                    }
                    c = records.get(i);
                }
                for (int f = 0; f < r.size(); f++) {
                    printRecord(r.get(f));
                }
                o.groupid = p.groupid;
                o.namespace = namespace;
                o.obj = getObject(className, r);
                objects.add(o);
                p = c;
            }
            return objects;
        }
        //System.out.println("RETURNING NULL AS RECORDS IN NULL");
        return null;
    }

    /**
     * This function is used to get the set of conflicting records
     * @return an arraylist of type 'ConflictingRecordSet' containing the conflicting record sets
     */
    public ArrayList<ConflictingRecordSet> getConflictingRecordsSet() {
        ArrayList<ConflictingRecordSet> cr = null;
        String query = "select distinct groupid from serverbackup;";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            cr = new ArrayList<ConflictingRecordSet>();
            int l = result.size();
            int i = 0;
            do {
                ConflictingRecordSet r = new ConflictingRecordSet();
                String groupid = result.get(i)[0];
                r.currentRecords = getCurrentRecords(groupid);
                r.conflictingRecordsSet = getConflictingRecordsSet(groupid);
                cr.add(r);
                i++;
            } while (i < l);
        }
        return cr;
    }

    /**
     * This function is used to update the database main table with a set of records and delete other conflicting records
     * @param r an array of records to update the main table with
     * @return boolean indicating success or failure of the function
     */
    public boolean updateRecords(ServerRecord[] r) {
        boolean result = true;
        boolean flag = true;
        while (flag) {
            try {
                Statement st1 = con.createStatement();
                con.setAutoCommit(false);
                String query = "delete from server  where groupid='" + r[0].groupid + "';";
                //System.out.println("Query :" + query);
                st1.executeUpdate(query);
                for (int i = 0; i < r.length; i++) {
                    query = "insert into server values('" + r[i].groupid + "','" + r[i].key + "','" + r[i].value + "','" + r[i].user + "','" + r[i].datatype + "'," + r[i].timestamp + ",'" + r[i].deviceId + "')";
                    //System.out.println("Query :" + query);
                    st1.executeUpdate(query);
                }
                query = "delete from serverbackup where groupid='" + r[0].groupid + "'";
                //System.out.println("Query :" + query);
                st1.executeUpdate(query);
                con.commit();
                flag = false;
            } catch (SQLException e) {
                if (con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        con.rollback();
                        Thread.sleep(2000);
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
                if (e.toString().indexOf("java.sql.SQLException: database is locked") == -1) {
                    flag = false;
                    result = false;
                }
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return result;
    }
    // This function is used to select records whose groupid equals the given groupid

    protected ArrayList<String[]> selectRecords(String gid) {
        String query = "select rowid,* from server where groupid='" + gid + "';";
        ArrayList<String[]> result = executeQueryForResult(query);

        return result;
    }
    // This function is used to check the write permission of the given user on the given namespace

    protected boolean checkPermission(String user, String namespace) {
        String query = "select * from namespaces where user='" + user + "' and namespace='" + namespace + "'";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null && result.get(0)[2].indexOf("w") != -1) {
            return true;
        } else {
            return false;
        }
    }
    // This function is used to get an arraylist of namespaces

    protected ArrayList<String[]> getNamespaces() {
        String query = "select distinct namespace from namespaces;";
        ArrayList<String[]> namespaces = new ArrayList<String[]>();
        namespaces = executeQueryForResult(query);
        return namespaces;
    }
    // This function is used to get an arraylist of namespace records

    protected ArrayList<String[]> getNamespaceRecords(String user) {
        String query = "select * from namespaces where user='" + user + "';";
        ArrayList<String[]> result = executeQueryForResult(query);
        return result;
    }

    // This function is used to get new records belonging to given list of namespaces and not from the given deviceid
    protected ArrayList<String[]> selectNewRecords(ArrayList<String[]> namespaces, Long[] ids, String deviceId) {
        if (namespaces == null || ids == null) {
            return null;
        }
        ArrayList<String[]> result = null;
        String query = "";
        for (int i = 0; i < namespaces.size(); i++) {
            if (namespaces.get(i)[2].indexOf("r") != -1) {
                query = "select rowid,* from server where rowid>" + ids[i] + " and key like '%" + namespaces.get(i)[1] + "%' and deviceid<>'" + deviceId + "' order by timestamp, datatype desc, groupid;";
                ArrayList<String[]> records = executeQueryForResult(query);
                if (records != null) {
                    if (result == null) {
                        result = new ArrayList<String[]>();
                    }
                    result.addAll(records);
                }
            }
        }
        return result;
    }
    // This function closes the connection to the database

    protected void closeConnection() {
        try {
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // This function is used to get records whose key matches the given classname and groupid is equal to the given groupid

    protected ArrayList<Record> getRecords(String className, String groupid) {
        ArrayList<Record> records = null;
        String query = "select * from server where groupid='" + groupid + "' and key like '%" + className + "%';";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            records = new ArrayList<Record>();
            int l = result.size();
            int i = 0;
            do {
                Record r = new Record();
                r.groupid = result.get(i)[0];
                r.key = result.get(i)[1];
                r.value = result.get(i)[2];
                r.user = result.get(i)[3];
                r.datatype = result.get(i)[4];
                r.timestamp = Long.parseLong(result.get(i)[5]);
                records.add(r);
                i++;
            } while (i < l);
        }
        return records;
    }
    // This function is used to get the value of a record storing a variable specified by the given variable name from the given set of records

    protected String getValue(ArrayList<Record> ans, String name) {
        for (int i = 0; i < ans.size(); i++) {
            //System.out.println(ans.get(i).key + " " + ans.get(i).value);
            String key = ans.get(i).key;
            if (name.equals(key.substring(key.lastIndexOf(".") + 1))) {
                return ans.get(i).value;
            }
        }
        return null;
    }
    // This function is used to get an object from the given set of records

    protected Object getObject(String className, ArrayList<Record> records) {
        if (records != null) {
            try {
                Class c = Class.forName(className);
                Field[] fields = c.getDeclaredFields();
                Object obj = c.newInstance();
                for (Field f : fields) {
                    String name = f.getName();
                    String type = f.getType().toString();
                    String value = getValue(records, name);//records.get(i).value;
                    //System.out.println(name + " " + type + " " + value);
                    if (type.equals("int")) {
                        f.set(obj, (Integer.parseInt(value)));
                    }
                    if (type.equals("long")) {
                        f.set(obj, Long.parseLong(value));
                    }
                    if (type.equals("float")) {
                        f.set(obj, (Float.parseFloat(value)));
                    }
                    if (type.equals("double")) {
                        f.set(obj, (Double.parseDouble(value)));
                    }
                    if (type.equals("class java.lang.String")) {
                        f.set(obj, (value));
                    }
                    if (type.equals("char")) {
                        f.set(obj, value.charAt(0));
                    }
                }
                return obj;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    // This function is used to get an object of the given className and groupid

    protected Object getObject(String className, String groupid) {
        ArrayList<Record> records = getRecords(className, groupid);
        if (records != null) {
            try {
                Class c = Class.forName(className);
                Field[] fields = c.getDeclaredFields();
                Object obj = c.newInstance();
                for (Field f : fields) {
                    String name = f.getName();
                    String type = f.getType().toString();
                    String value = getValue(records, name);//records.get(i).value;
                    //System.out.println(name + " " + type + " " + value);
                    if (type.equals("int")) {
                        f.set(obj, (Integer.parseInt(value)));
                    }
                    if (type.equals("long")) {
                        f.set(obj, Long.parseLong(value));
                    }
                    if (type.equals("float")) {
                        f.set(obj, (Float.parseFloat(value)));
                    }
                    if (type.equals("double")) {
                        f.set(obj, (Double.parseDouble(value)));
                    }
                    if (type.equals("class java.lang.String")) {
                        f.set(obj, (value));
                    }
                    if (type.equals("char")) {
                        f.set(obj, value.charAt(0));
                    }
                }
                return obj;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * This function prints the given Record
     * @param r the record to be printed
     */
    public void printRecord(Record r) {
        System.out.print(r.rowid);
        System.out.print("\t");
        System.out.print(r.groupid);
        System.out.print("\t");
        System.out.print(r.key);
        System.out.print("\t");
        System.out.print(r.value);
        System.out.print("\t");
        System.out.print(r.user);
        System.out.print("\t");
        System.out.print(r.datatype);
        System.out.print("\t");
        System.out.print(r.timestamp);
        System.out.print("\n");
    }

    /**
     * This function prints the given ServerRecord
     * @param r the server record to be printed
     */
    public void printRecord(ServerRecord r) {
        System.out.print(r.groupid);
        System.out.print("\t");
        System.out.print(r.key);
        System.out.print("\t");
        System.out.print(r.value);
        System.out.print("\t");
        System.out.print(r.user);
        System.out.print("\t");
        System.out.print(r.datatype);
        System.out.print("\t");
        System.out.print(r.timestamp);
        System.out.print("\t");
        System.out.print(r.deviceId);
        System.out.print("\n");
    }

    /**
     * This function is used to set the user name
     * @param value user name to be set
     */
    public void setUser(String value) {
        user = value;
    }
    // This function is used to put a record into the database

    protected boolean putToRepository(String gid, String key, String value, String user, String datatype, long timestamp, String deviceid) {
        String query = "insert into server values('" + gid + "','" + key + "','" + value + "','" + user + "','" + datatype + "," + timestamp + ",'" + deviceid + "')";
        return executeQuery(query);
    }
    // This function is used to get conflicting records based on the given filter

    protected ServerRecord[] getRecordsGivenFilter(String filter) {
        ServerRecord[] r = null;
        String query = "select * from server where " + filter + ";";
        //System.out.println(query);
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            r = new ServerRecord[result.size()];
            for (int i = 0; i < result.size(); i++) {
                r[i] = new ServerRecord();
                r[i].groupid = result.get(i)[0];
                r[i].key = result.get(i)[1];
                r[i].value = result.get(i)[2];
                r[i].user = result.get(i)[3];
                r[i].datatype = result.get(i)[4];
                r[i].timestamp = Long.parseLong(result.get(i)[5]);
                r[i].deviceId = result.get(i)[6];
            }
        }
        return r;
    }
    /**
     * This function is used to delete records from the database having the given groupid
     * @param groupid groupid whose records need to be deleted
     * @return boolean indicating success or failure of the function
     */

    public boolean deleteRecords(String groupid) {
        String query = "delete from server where groupid='" + groupid + "';";
        return executeQuery(query);
    }
    // This function is used to get records whose groupid equals the given groupid

    protected ServerRecord[] getCurrentRecords(String groupid) {
        ServerRecord[] r = null;
        String query = "select * from server where groupid = '" + groupid + "';";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            r = new ServerRecord[result.size()];
            for (int i = 0; i < result.size(); i++) {
                r[i] = new ServerRecord();
                r[i].groupid = result.get(i)[0];
                r[i].key = result.get(i)[1];
                r[i].value = result.get(i)[2];
                r[i].user = result.get(i)[3];
                r[i].datatype = result.get(i)[4];
                r[i].timestamp = Long.parseLong(result.get(i)[5]);
                r[i].deviceId = result.get(i)[6];
            }
        }
        return r;
    }
    // This function is used to get a set of conflicting record sets for the record set identified by the given groupid

    protected ArrayList<ServerRecord[]> getConflictingRecordsSet(String groupid) {
        ArrayList<ServerRecord[]> records = null;
        String query = "select distinct timestamp from serverbackup where groupid='" + groupid + "';";
        ArrayList<String[]> result = executeQueryForResult(query);
        if (result != null) {
            records = new ArrayList<ServerRecord[]>();
            for (int i = 0; i < result.size(); i++) {
                query = "select * from serverbackup where timestamp=" + result.get(i)[0] + ";";
                ArrayList<String[]> results = executeQueryForResult(query);
                if (results != null) {
                    ServerRecord[] r = new ServerRecord[results.size()];
                    for (int j = 0; j < results.size(); j++) {
                        r[j] = new ServerRecord();
                        r[j].groupid = results.get(j)[0];
                        r[j].key = results.get(j)[1];
                        r[j].value = results.get(j)[2];
                        r[j].user = results.get(j)[3];
                        r[j].datatype = results.get(j)[4];
                        r[j].timestamp = Long.parseLong(results.get(j)[5]);
                        r[j].deviceId = results.get(j)[6];
                    }
                    records.add(r);
                }
            }
        }
        if (records.size() == 0) {
            records = null;
        }
        return records;
    }
}
