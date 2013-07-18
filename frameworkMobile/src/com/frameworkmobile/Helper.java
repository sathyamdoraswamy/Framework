package com.frameworkmobile;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * This class is used to perform database operations and lock and unlock
 * namespaces
 * 
 * @author sathyam
 * 
 */
public class Helper {

	private static final String DATABASE_NAME = "frameworkdb_client";
	private static final int DATABASE_VERSION = 1;
	private static final String CLIENT_TABLE = "phone";
	private static final String NAMESPACES_TABLE = "namespaces";
	private static final String LOCKS_TABLE = "locks";
	private String user = "";
	private String phoneNo = "";
	private String number = "";
	private String deviceId = "";
	private Context context;
	private SQLiteDatabase db;

	private SQLiteStatement insertStmt, insertStmt_namespaces,
			insertStmt_locks;

	private static final String INSERT = "insert into " + CLIENT_TABLE
			+ " values (?,?,?,?,?,?,?,?)";
	private static final String NAMESPACES_INSERT = "insert into "
			+ NAMESPACES_TABLE + " values (?,?,?,?,?)";
	private static final String LOCKS_INSERT = "insert into " + LOCKS_TABLE
			+ " values (?,?)";
	public OpenHelper openHelper;

	/**
	 * This is the constructor for the class which is passed the current context
	 * 
	 * @param context
	 *            current context
	 */
	public Helper(Context context) {

		this.context = context;
		openHelper = new OpenHelper(this.context);
		deviceId = Secure.getString(context.getContentResolver(),
				Secure.ANDROID_ID);
		Config c = new Config(context);
		user = c.getUser();
		phoneNo = c.getPhoneNo();
		TelephonyManager tm = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		number = tm.getLine1Number();
		// System.out.println("NUMBER IS : " + number);
	}

	/**
	 * This function is used to get connection to the database<br>
	 * The function should be called whenever an activity is begun or resumed
	 */
	public void open() {
		boolean flag = true;
		while (flag) {
			try {
				this.db = openHelper.getWritableDatabase();
				this.insertStmt = this.db.compileStatement(INSERT);
				this.insertStmt_namespaces = this.db
						.compileStatement(NAMESPACES_INSERT);
				this.insertStmt_locks = this.db.compileStatement(LOCKS_INSERT);
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function is used to subscribe to a namespace
	 * 
	 * @param user
	 *            user name
	 * @param namespace
	 *            namespace to subscribe to
	 * @param permission
	 *            user's permission on the namespace
	 * @return rowid of record inserted or error status
	 */
	public long subscribeToNamespace(String user, String namespace,
			String permission) {
		this.insertStmt_namespaces.bindString(1, "ADD");
		this.insertStmt_namespaces.bindString(2, user);
		this.insertStmt_namespaces.bindString(3, namespace);
		this.insertStmt_namespaces.bindString(4, permission);
		this.insertStmt_namespaces.bindLong(5, System.currentTimeMillis());
		return this.insertStmt_namespaces.executeInsert();
	}

	/**
	 * This function is used to unsubscribe from a namespace
	 * 
	 * @param user
	 *            user name
	 * @param namespace
	 *            namespace to unsubscribe from
	 * @param permission
	 *            user's permission on the namespace
	 * @return rowid of record inserted or error status
	 */
	public long unsubscribeFromNamespace(String user, String namespace,
			String permission) {
		this.insertStmt_namespaces.bindString(1, "REMOVE");
		this.insertStmt_namespaces.bindString(2, user);
		this.insertStmt_namespaces.bindString(3, namespace);
		this.insertStmt_namespaces.bindString(4, permission);
		this.insertStmt_namespaces.bindLong(5, System.currentTimeMillis());
		return this.insertStmt_namespaces.executeInsert();
	}

	/**
	 * This function is used to put a single record into the database
	 * 
	 * @param key
	 *            the key of the record
	 * @param value
	 *            the value of the record
	 * @param datatype
	 *            the datatype of the record
	 * @return boolean indicating success or failure of the function
	 */
	public boolean putToRepository(String key, String value, String datatype) {
		boolean result = false;
		boolean flag = true;
		while (flag) {
			try {
				String groupid = UUID.randomUUID().toString();// getID();
				long timestamp = 0;// System.currentTimeMillis();
				this.insertStmt.bindLong(1, 0);
				this.insertStmt.bindString(2, groupid);
				this.insertStmt.bindString(3, key);
				this.insertStmt.bindString(4, value);
				this.insertStmt.bindString(5, user);
				this.insertStmt.bindString(6, datatype);
				this.insertStmt.bindLong(7, timestamp);
				this.insertStmt.bindString(8, "N");
				Log.d("CHECK INSERT", Long.toString(System.currentTimeMillis()));
				this.insertStmt.executeInsert();
				flag = false;
				result = true;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * This function is used to update a record identified by (groupid, key)
	 * 
	 * @param groupid
	 *            groupid used to identify record
	 * @param key
	 *            key used to identify record
	 * @param value
	 *            new value with which to update the value field of the record
	 * @param datatype
	 *            new value with which to update the datatype field of the
	 *            record
	 */
	public void updateRepository(String groupid, String key, String value,
			String datatype) {
		boolean flag = true;
		do {
			try {
				// System.out.println("UPDATE " + CLIENT_TABLE + " SET value='"
				// + value + "', datatype='" + datatype
				// + "', synced='N' WHERE groupid = '" + groupid
				// + "' AND key = '" + key + "' AND VALUE <> '" + value
				// + "'");
				db.execSQL("UPDATE " + CLIENT_TABLE + " SET value='" + value
						+ "', datatype='" + datatype
						+ "', synced='N' WHERE groupid = '" + groupid
						+ "' AND key = '" + key + "' AND VALUE <> '" + value
						+ "'");
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} while (flag);
	}

	/**
	 * This function is used to put a set of records into the database
	 * 
	 * @param records
	 *            an array of type 'Record' to be inserted into the database
	 */
	public void putToRepository(Record[] records) {
		String groupid = UUID.randomUUID().toString();
		long timestamp = 0;
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					for (int i = 0; i < records.length; i++) {
						putToRepository(0, groupid, records[i].key,
								records[i].value, user, records[i].datatype,
								timestamp, " ");
					}
					db.execSQL("UPDATE " + CLIENT_TABLE
							+ " SET synced='N' WHERE groupid = '" + groupid
							+ "'");
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
		Log.d("CHECK PUT", Long.toString(System.currentTimeMillis()));
	}

	/**
	 * This function is used to update a set of records using (groupid, key)
	 * fields of the records
	 * 
	 * @param records
	 *            an array of type 'Record' that is used to update records
	 * */
	public void updateRepository(Record[] records) {
		// System.out.println("INSIDE UPDATE OBJECT");
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					for (int i = 0; i < records.length; i++) {
						// System.out.println("UPDATE " + CLIENT_TABLE
						// + " SET value='" + records[i].value
						// + "', datatype='" + records[i].datatype
						// + "', synced='N' WHERE groupid = '"
						// + records[i].groupid + "' AND key = '"
						// + records[i].key + "' AND VALUE <> '"
						// + records[i].value + "'");
						db.execSQL("UPDATE " + CLIENT_TABLE + " SET value='"
								+ records[i].value + "', datatype='"
								+ records[i].datatype
								+ "', synced='N' WHERE groupid = '"
								+ records[i].groupid + "' AND key = '"
								+ records[i].key + "' AND VALUE <> '"
								+ records[i].value + "'");
					}
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * This function is used to get an arraylist of records whose key matches
	 * the given key
	 * 
	 * @param key
	 *            the key pattern to be matched with
	 * @return an arraylist of records matching the given key
	 */
	public ArrayList<Record> getRecords(String key) {
		ArrayList<Record> records = null;
		Cursor cursor = db.query(CLIENT_TABLE, new String[] { "groupid", "key",
				"value", "user", "datatype", "timestamp", "synced" },
				"key like '%" + key + "%'", null, null, null, "groupid");
		if (cursor.moveToFirst()) {
			records = new ArrayList<Record>();
			// System.out.println("ASSIGNED NEW RECORDS");
			do {
				Record r = new Record();
				r.groupid = cursor.getString(0);
				r.key = cursor.getString(1);
				r.value = cursor.getString(2);
				r.user = cursor.getString(3);
				r.datatype = cursor.getString(4);
				r.timestamp = Long.parseLong(cursor.getString(5));
				r.synced = cursor.getString(6);
				records.add(r);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return records;
	}

	/**
	 * This function is used to put an object of a class into the database under
	 * a given namespace
	 * 
	 * @param obj
	 *            the object to be inserted into the database
	 * @param namespace
	 *            the namespace under which to insert the object
	 */
	public void putObject(Object obj, String namespace) {
		try {
			Class c = obj.getClass();
			String className = c.getName();
			Field[] fields = c.getDeclaredFields();
			Record[] r = new Record[fields.length];
			int i = 0;
			// System.out.println("ENTERING LOOP NOW");
			for (Field f : fields) {
				r[i] = new Record();
				String fieldName = f.getName();
				r[i].key = namespace + "." + className + "." + fieldName;
				r[i].value = "";
				if (f.get(obj) != null) {
					r[i].value = f.get(obj).toString();
					if (fieldName.contains("Path"))
						r[i].datatype = "file";
					else
						r[i].datatype = "data";
				} else {
					r[i].datatype = "";
					r[i].value = "";
				}
				i++;
			}
			// System.out.println("EXXITING LOOP NOW");
			putToRepository(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function is used to update an object present in the database
	 * identified by groupid and namespace
	 * 
	 * @param groupid
	 *            groupid to identify object's records
	 * @param obj
	 *            object containing new values to update the records of the
	 *            object
	 * @param namespace
	 *            namespace to identify object's records
	 */
	public void updateObject(String groupid, Object obj, String namespace) {
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
					if (fieldName.contains("Path"))
						r[i].datatype = "file";
					else
						r[i].datatype = "data";
				} else {
					r[i].datatype = "";
					r[i].value = "";
				}
				i++;
			}
			updateRepository(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function is used to get an arraylist of objects present in the
	 * database belonging to a particular class
	 * 
	 * @param className
	 *            used to specify the class whose objects need to be fetched
	 * @return an arraylist of objects of type 'Set' containing required object
	 *         and metadata
	 */
	public ArrayList<Set> getObjects(String className) {
		ArrayList<Record> records = getRecords(className);
		if (records != null) {
			String key = records.get(0).key;
			String namespace = key.substring(0, key.indexOf("."));
			// System.out.println("NAMESPACE IS : " + namespace);
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
					if (i == l)
						break;
					c = records.get(i);
				}
				for (int f = 0; f < r.size(); f++)
					printRecord(r.get(f));
				o.groupid = p.groupid;
				o.namespace = namespace;
				o.obj = getObject(className, r);
				objects.add(o);
				p = c;
			}
			return objects;
		}
		// System.out.println("RETURNING NULL AS RECORDS IN NULL");
		return null;
	}

	/**
	 * This function is used to add a lock belonging to a particular namespace
	 * 
	 * @param namespace
	 *            namespace whose lock needs to be added
	 */
	public void addLock(String namespace) {
		boolean flag = true;
		while (flag) {
			try {
				this.insertStmt_locks.bindString(1, namespace);
				this.insertStmt_locks.bindString(2, "UNLOCKED");
				Log.d("CHECK INSERT", Long.toString(System.currentTimeMillis()));
				this.insertStmt_locks.executeInsert();
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This function is used to remove a lock of a particular namespace
	 * 
	 * @param namespace
	 *            namespace whose lock needs to be removed
	 */
	public void removeLock(String namespace) {
		boolean flag = true;
		do {
			try {
				db.execSQL("DELETE FROM " + LOCKS_TABLE + " WHERE namespace='"
						+ namespace + "';");
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} while (flag);
	}

	/**
	 * This function is used to get the status of the lock of a particular
	 * namespace
	 * 
	 * @param namespace
	 *            namespace whose lock's status needs to be got
	 * @return
	 */
	public String getLockStatus(String namespace) {
		String status = null;
		Cursor cursor = db.query(LOCKS_TABLE, new String[] { "status" },
				"namespace='" + namespace + "'", null, null, null, null);
		if (cursor.moveToFirst()) {
			status = cursor.getString(0);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return status;
	}

	/**
	 * This function is used to check the sync status of records of a namespace
	 * 
	 * @param namespace
	 *            namespace whose records' status needs to be checked
	 * 
	 */
	public boolean isSynced(String namespace) {
		boolean result = true;
		Cursor cursor = db.query(CLIENT_TABLE, new String[] { "groupid" },
				"key like '%" + namespace + "%' AND synced='N'", null, null,
				null, null);

		if (cursor.moveToFirst()) {
			result = false;
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return result;
	}

	/**
	 * This function is used to lock a particular namespace
	 * 
	 * @param namespace
	 *            namespace that needs to be locked
	 */
	public void lockNamespace(String namespace) {
		String msg = "LOCK" + "\n" + user + "\n" + namespace + "\n" + deviceId
				+ "\n" + "09582784102";
		SmsManager sms = SmsManager.getDefault();
		// System.out.println("MESSAGE IS : " + msg);
		sms.sendTextMessage(phoneNo, null, msg, null, null);
	}

	/**
	 * This function is used to unlock a particular namespace
	 * 
	 * @param namespace
	 *            namespace that needs to be unlocked
	 */
	public void unlockNamespace(String namespace) {
		String msg = "UNLOCK" + "\n" + user + "\n" + namespace + "\n"
				+ deviceId + "\n" + number;
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNo, null, msg, null, null);
		changeLockStatus("LOCKED", "UNLOCKED", namespace);
	}

	/**
	 * This function is used to set the lock status of a particular namespace
	 * 
	 * @param namespace
	 *            namespace whose lock status needs to be set
	 * @param status
	 *            status to be set
	 */
	public void setLockStatus(String namespace, String status) {
		boolean flag = true;
		do {
			try {
				db.execSQL("UPDATE " + LOCKS_TABLE + " SET status='" + status
						+ "' WHERE namespace='" + namespace + "';");
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} while (flag);
	}

	/**
	 * This function is used to change status of the locks
	 * 
	 * @param from
	 *            current status of locks
	 * @param to
	 *            final status of locks
	 */
	public void changeLockStatus(String from, String to) {
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					db.execSQL("UPDATE " + LOCKS_TABLE + " SET status='" + to
							+ "' WHERE status='" + from + "';");
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * This function is used to change status of the lock of a particular
	 * namespace
	 * 
	 * @param from
	 *            current status of lock
	 * @param to
	 *            final status of lock
	 * @param namespace
	 *            namespace whose lock status needs to be changed
	 */
	public void changeLockStatus(String from, String to, String namespace) {
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					db.execSQL("UPDATE " + LOCKS_TABLE + " SET status='" + to
							+ "' WHERE status='" + from + "' AND namespace='"
							+ namespace + "';");
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * This function is used for closing connection to the database<br>
	 * The function should be called whenever an activity is destroyed or exited
	 * from
	 */
	public void close() {
		openHelper.close();
	}

	// This function is used to set the given records as synced
	protected void setSynced(long timestamp, ArrayList<Record> r) {
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					String groupid = r.get(0).groupid;
					for (int i = 0; i < r.size(); i++) {
						db.execSQL("UPDATE " + CLIENT_TABLE
								+ " SET synced='Y' WHERE groupid = '"
								+ r.get(i).groupid + "' AND key = '"
								+ r.get(i).key + "' AND value = '"
								+ r.get(i).value + "' AND synced ='N' ");
					}
					db.execSQL("UPDATE " + CLIENT_TABLE + " SET timestamp="
							+ timestamp + " WHERE groupid = '" + groupid + "'");
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
	}

	// This function is used to get an arraylist of namespace records
	protected ArrayList<String[]> getNamespaceRecords() {
		ArrayList<String[]> records = null;
		Cursor cursor = db.query(NAMESPACES_TABLE, new String[] { "method",
				"userid", "namespace", "permission" }, null, null, null, null,
				"timestamp");
		if (cursor.moveToFirst()) {
			records = new ArrayList<String[]>();
			do {
				records.add(new String[] { cursor.getString(0),
						cursor.getString(1), cursor.getString(2),
						cursor.getString(3) });
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return records;
	}

	// This function is used to get an arraylist of namespaces
	protected ArrayList<String[]> getNamespaces() {
		ArrayList<String[]> records = null;
		Cursor cursor = db.query(NAMESPACES_TABLE, new String[] { "method",
				"userid", "namespace", "permission" }, null, null, null, null,
				"timestamp");
		if (cursor.moveToFirst()) {
			records = new ArrayList<String[]>();
			do {
				records.add(new String[] { cursor.getString(0),
						cursor.getString(1), cursor.getString(2),
						cursor.getString(3) });
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return records;
	}

	// This function is used to remove a namespace
	protected void removeNamespace(String userid, String namespace,
			String permission) {
		boolean flag = true;
		do {
			try {
				// System.out.println("DELETE FROM " + NAMESPACES_TABLE
				// + " WHERE userid='" + userid + "' AND namespace='"
				// + namespace + "' AND permission='" + permission + "';");
				db.execSQL("DELETE FROM " + NAMESPACES_TABLE
						+ " WHERE userid='" + userid + "' AND namespace='"
						+ namespace + "' AND permission='" + permission + "';");
				flag = false;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} while (flag);
	}

	// This function is used to get the value of a record storing a variable
	// specified by the given variable name from the given set of records
	protected String getValue(ArrayList<Record> ans, String name) {
		// System.out.println("Name is:" + name);
		for (int i = 0; i < ans.size(); i++) {
			String key = ans.get(i).key;
			// System.out.println("CHECK : "
			// + key.substring(key.lastIndexOf(".") + 1) + " "
			// + ans.get(i).value);
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
				int i = 0;
				for (Field f : fields) {
					String name = f.getName();
					String type = f.getType().toString();
					String value = getValue(records, name);// records.get(i).value;
					// System.out.println(name + " " + type + " " + value);
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
					i++;
				}
				return obj;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	/**
	 * This function is used to print the given Record
	 * 
	 * @param r
	 *            the record to be printed
	 */
	public void printRecord(Record r) {
		System.out.println("ROWID : " + r.rowid);
		System.out.println("GROUPID : " + r.groupid);
		System.out.println("KEY : " + r.key);
		System.out.println("VALUE : " + r.value);
		System.out.println("USER : " + r.user);
		System.out.println("DATATYPE : " + r.datatype);
		System.out.println("TIMESTAMP : " + r.timestamp);
		System.out.println("SYNCED : " + r.synced);
	}

	// This function is used to put a record into the database
	protected long putToRepository(long rowid, String gid, String key,
			String value, String user, String datatype, long timestamp,
			String synced) {
		this.insertStmt.bindLong(1, rowid);
		this.insertStmt.bindString(2, gid);
		this.insertStmt.bindString(3, key);
		this.insertStmt.bindString(4, value);
		this.insertStmt.bindString(5, user);
		this.insertStmt.bindString(6, datatype);
		this.insertStmt.bindLong(7, timestamp);
		this.insertStmt.bindString(8, synced);
		return this.insertStmt.executeInsert();
	}

	// This function is used to put an arraylist of records into the database
	protected void putToRepository(ArrayList<Record> records) {
		boolean flag = true;
		try {
			do {
				try {
					db.beginTransaction();
					for (int i = 0; i < records.size(); i++) {
						insert(records.get(i).rowid, records.get(i).groupid,
								records.get(i).key, records.get(i).value,
								records.get(i).user, records.get(i).datatype,
								records.get(i).timestamp, records.get(i).synced);
					}
					db.execSQL("UPDATE " + CLIENT_TABLE + " SET timestamp="
							+ records.get(0).timestamp + " WHERE groupid='"
							+ records.get(0).groupid + "'");
					db.setTransactionSuccessful();
					flag = false;
				} catch (SQLException e) {
					// System.out.println("HELPER");
					e.printStackTrace();
				}
			} while (flag);
		} catch (Exception e) {
		} finally {
			db.endTransaction();
		}
		Log.d("CHECK INSERT", Long.toString(System.currentTimeMillis()));
	}

	// This function is used to insert a record into the database
	protected long insert(long rowid, String groupid, String key, String value,
			String user, String datatype, long timestamp, String synced) {
		long result = -1;
		db.execSQL("DELETE FROM " + CLIENT_TABLE + " WHERE groupid='" + groupid
				+ "' AND key ='" + key + "'");
		this.insertStmt.bindLong(1, rowid);
		this.insertStmt.bindString(2, groupid);
		this.insertStmt.bindString(3, key);
		this.insertStmt.bindString(4, value);
		this.insertStmt.bindString(5, user);
		this.insertStmt.bindString(6, datatype);
		this.insertStmt.bindLong(7, timestamp);
		this.insertStmt.bindString(8, synced);

		result = this.insertStmt.executeInsert();
		return result;
	}

	// This function is used to get the maximum rowid from records belonging to
	// the given namespace
	protected long getMaxRowIdGivenNamespace(String namespace) {
		int r = 0;
		String query = "SELECT MAX(RECORDID) FROM " + CLIENT_TABLE
				+ " WHERE KEY LIKE %'" + namespace + "'%";
		// System.out.println(query);
		Cursor cursor = db.query(CLIENT_TABLE,
				new String[] { "MAX(recordid)" }, "key like '%" + namespace
						+ "%'", null, null, null, null);
		if (cursor.moveToFirst())
			r = cursor.getInt(0);
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		// System.out.println("ROWID : " + r);
		return r;
	}

	// This function is used to get the set of maximum rowids from records
	// belonging to the given set of namespaces
	protected Long[] getRowIds(String[] namespaces) {
		int l = namespaces.length;
		Long[] rowids = new Long[l];
		for (int i = 0; i < l; i++) {
			// System.out.println("NAMESPACE IS : " + namespaces[i]);
			rowids[i] = getMaxRowIdGivenNamespace(namespaces[i]);
		}
		return rowids;
	}

	// This function is used to delete the client side table
	protected void clearRepositoryContents() {
		this.db.delete(CLIENT_TABLE, null, null);
	}

	// This function is used to select new unsynced records from the database
	protected List<String[]> selectNewRecords() {
		List<String[]> list = null;
		Cursor cursor = db.query(CLIENT_TABLE, new String[] { "groupid", "key",
				"value", "user", "datatype", "timestamp" }, "synced='N'", null,
				null, null, "timestamp, datatype desc, groupid");

		if (cursor.moveToFirst()) {
			list = new ArrayList<String[]>();
			do {
				list.add(new String[] { cursor.getString(0),
						cursor.getString(1), cursor.getString(2),
						cursor.getString(3), cursor.getString(4),
						cursor.getString(5) });
				// list.add(cursor.getString(1));
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return list;
	}

	// This class is used to manage the database
	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE "
					+ CLIENT_TABLE
					+ "(RECORDID NUMBER,GROUPID TEXT,KEY TEXT,VALUE TEXT,USER TEXT,DATATYPE TEXT,TIMESTAMP NUMBER,SYNCED TEXT, PRIMARY KEY(RECORDID,KEY,TIMESTAMP))");
			db.execSQL("CREATE TABLE "
					+ NAMESPACES_TABLE
					+ "(method TEXT,userid TEXT,namespace TEXT,permission TEXT,timestamp NUMBER)");
			db.execSQL("CREATE TABLE " + LOCKS_TABLE
					+ "(namespace TEXT,status TEXT)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w("Example",
					"Upgrading database, this will drop tables and recreate.");
			db.execSQL("DROP TABLE IF EXISTS " + CLIENT_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + NAMESPACES_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + LOCKS_TABLE);
			onCreate(db);
		}
	}
}