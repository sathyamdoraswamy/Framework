package com.frameworkmobile;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.net.*;
import android.content.Intent; //import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings.Secure;
import android.util.Log;
import android.content.Context; //import android.view.View;
//import android.widget.EditText;
//import android.widget.Button;
//import android.widget.TextView;

import java.util.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.io.*;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * 
 * @author sathyam
 * 
 */
public class FrameworkService extends Service {
	private static final String TAG = "FRAMEWORK SERVICE";
	private FrameworkThread thread;
	PowerManager pm;
	PowerManager.WakeLock wl;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		startThread();
		Log.d(TAG, "Service Created");
	}

	// This function starts the thread that carries out the various events of
	// the framework
	public void startThread() {
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		wl.acquire();
		thread = new FrameworkThread((Context) this);
		thread.start();
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, "Service Started");
	}

	@Override
	public void onDestroy() {
		if (thread != null) {
			try {
				thread.flag = false;
				thread.connection.close();
				Log.d(TAG, "THREAD WAS NOT NULL");
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else
			Log.d(TAG, "THREAD WAS NULL");
		wl.release();
	}
}

// The given thread performs the various events of the framework
class FrameworkThread extends Thread {
	String extStorageDirectory = Environment.getExternalStorageDirectory()
			.toString();
	boolean flag = true;

	Socket connection;
	ObjectOutputStream oos;
	ObjectInputStream ois;
	boolean newdata = false;

	Helper dh;
	ConnectivityManager cm;
	String deviceId;
	String user;
	int userId;
	InetAddress server;
	int port;
	String storagePath;

	public FrameworkThread(Context c) {
		dh = new Helper(c);
		dh.open();
		ConnectivityManager connMgr = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		deviceId = Secure.getString(c.getContentResolver(), Secure.ANDROID_ID);
		Config con = new Config(c);
		user = con.getUser();
		userId = Integer.parseInt(con.getUserId());
		String IP = con.getIP();
		try {
			server = InetAddress.getByName(IP);
		} catch (Exception e) {
			e.printStackTrace();
		}
		port = Integer.parseInt(con.getPort());
		storagePath = con.getStoragePath();
	}

	// This function is used to commit the given transaction into the client
	// table
	public void commit(Transaction t) {
		try {
			ArrayList<Record> records = new ArrayList<Record>();
			for (int i = 0; i < t.noOfBundles; i++) {
				String record = "";
				Bundle b = t.bundles[i];
				byte[] recd = b.data;
				record = new String(recd);
				String rec[] = record.split("\n");
				Record r = new Record();
				r.rowid = Long.parseLong(rec[0]);
				r.groupid = rec[1];
				r.key = rec[2];
				r.value = rec[3];
				r.user = rec[4];
				r.datatype = rec[5];
				r.timestamp = Long.parseLong(rec[6]);
				r.synced = "Y";
				if (r.datatype.equals("file")) {
					int l = Integer.parseInt(r.value.substring(0,
							r.value.indexOf(' ')));
					String fileName = r.value
							.substring(r.value.indexOf(' ') + 1);
					fileName = deviceId + "_" + t.transactionId + "_" + i + "_"
							+ fileName;
					String path = storagePath + "/" + fileName;
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
				records.add(r);
			}
			dh.putToRepository(records);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// This function performs the UPDATE event
	public boolean update() {
		try {
			byte[] buffer = "UPDATE".getBytes();
			oos.writeObject(buffer);
			buffer = (deviceId + " " + user).getBytes();
			oos.writeObject(buffer);
			buffer = (byte[]) ois.readObject();
			String reply = (new String(buffer));
			if (reply.equals("NULL"))
				return true;
			// System.out.println("REPLY IS : " + reply);
			String[] namespaces = reply.split("\n");
			Long[] rowids = dh.getRowIds(namespaces);
			int l = rowids.length;
			String ids;
			if (l == 1)
				ids = Long.toString(rowids[0]);
			else {
				ids = Long.toString(rowids[0]);
				int i;
				for (i = 1; i < l; i++) {
					ids = ids + "\n" + Long.toString(rowids[i]);
				}
			}
			// System.out.println("IDs : " + ids);
			buffer = ids.getBytes();
			oos.writeObject(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// This function performs the PULL event
	public boolean pull() {
		try {
			Transaction t = null;
			String fileName = storagePath + "/pull";
			File f = new File(fileName);
			try {
				boolean flag = true;
				if (f.exists()) {
					byte[] buffer = "PULL".getBytes();
					oos.writeObject(buffer);
					// System.out.println("PULL packet sent!!!");
					ObjectInputStream fin = new ObjectInputStream(
							new FileInputStream(f));
					t = (Transaction) fin.readObject();
					fin.close();
					boolean result = f.delete();
					// System.out.println("Delete event : " + result);
					Bundle b = new Bundle();
					b.userId = userId;
					b.transactionId = -1;
					b.bundleType = b.RETRANS;
					b.noOfBundles = 1;
					b.bundleNumber = -1;
					String s = Integer.toString(t.transactionId) + "\n"
							+ Integer.toString((t.bundleNo + 1));
					byte[] bb = s.getBytes();
					int size = bb.length;
					b.bundleSize = bb.length;
					b.data = new byte[size];
					for (int j = 0; j < size; j++) {
						b.data[j] = bb[j];
					}
					// System.out.println("Sent retrans : " + b.transactionId
					// + " " + b.noOfBundles + " " + b.bundleNumber + " "
					// + b.bundleType + " " + new String(b.data));
					oos.writeObject(b.getBytes());
					oos.flush();
					// System.out.println("RETRANS bundle sent");
					flag = true;
					while (flag) {
						// System.out.print("Going to read");
						buffer = (byte[]) ois.readObject();
						// System.out.println("Read");
						b = new Bundle();
						b.parse(buffer);
						// System.out.println("Parsed");
						if (b.bundleType == b.STOP)
							break;
						if (!newdata)
							newdata = true;
						t.addBundle(b);
						if (b.bundleNumber == (b.noOfBundles - 1)) {
							commit(t);
							flag = false;
						}
						Bundle ackb = new Bundle();
						ackb.createACK(b);
						// System.out.println(ackb.transactionId + " "
						// + ackb.noOfBundles + " " + ackb.bundleNumber
						// + " " + ackb.bundleType);
						oos.writeObject(ackb.getBytes());
						oos.flush();
						// System.out.println("Written!!!");
					}
					update();
				}
				byte[] buffer = "PULL".getBytes();
				oos.writeObject(buffer);
				// System.out.println("PULL packet sent!!!");
				Bundle b = new Bundle();
				b.userId = userId;
				b.transactionId = -1;
				b.bundleType = b.START;
				b.noOfBundles = 1;
				b.bundleNumber = -1;
				b.bundleSize = 0;
				b.data = null;
				oos.writeObject(b.getBytes());
				oos.flush();
				// System.out.println("START bundle sent");
				flag = true;
				// System.out.println("Waiting to receive");
				do {
					// System.out.print("Going to read");
					buffer = (byte[]) ois.readObject();
					// System.out.println("Read");
					b = new Bundle();
					b.parse(buffer);
					// System.out.println("Parsed");
					if (b.bundleType == b.STOP)
						break;
					if (!newdata)
						newdata = true;
					if (b.bundleNumber == 0) {
						t = new Transaction(b.transactionId, b.noOfBundles);
					}
					t.addBundle(b);
					if (b.bundleNumber == (b.noOfBundles - 1)) {
						commit(t);
					}
					Bundle ackb = new Bundle();
					ackb.createACK(b);
					// System.out.println("Sent ack : " + ackb.transactionId +
					// " "
					// + ackb.noOfBundles + " " + ackb.bundleNumber + " "
					// + ackb.bundleType);
					oos.writeObject(ackb.getBytes());
					oos.flush();
				} while (flag);
			} catch (Exception e) {
				e.printStackTrace();
				if (t != null && t.bundleNo != (t.noOfBundles - 1)) {
					// System.out.println("Writing broken transaction!!!");
					f.createNewFile();
					ObjectOutputStream fout = new ObjectOutputStream(
							new FileOutputStream(f));
					fout.writeObject(t);
					fout.close();
				}
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				sleep(100);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			return false;
		}
		return true;
	}

	// This function performs the NAMESPACE event
	public boolean namespace() {
		try {
			ArrayList<String[]> records = dh.getNamespaces();
			if (records != null) {
				// System.out.println("NAMESPACE RECORDS FOUND");
				int l = records.size();
				byte[] startPacket = ("NAMESPACE").getBytes();
				oos.writeObject(startPacket);
				// System.out.println("Start packet sent!!!");
				AckThread ack = new AckThread(dh, ois, records);
				ack.start();
				for (int i = 0; i < l; i++) {
					String method = records.get(i)[0];
					String user = records.get(i)[1];
					String namespace = records.get(i)[2];
					String permission = records.get(i)[3];
					String data = method + "\n" + user + "\n" + namespace
							+ "\n" + permission;
					Bundle b = new Bundle();
					b.data = data.getBytes();
					b.userId = userId;
					b.transactionId = i;
					b.bundleType = b.DATA;
					b.noOfBundles = 1;
					b.bundleNumber = 0;
					b.bundleSize = b.data.length;
					byte[] bundle = b.getBytes();
					oos.writeObject(bundle);
					oos.flush();
					// System.out.println("Sent bundle -> Transaction id : " + i
					// + " Bundle No. : " + 0);
				}
				ack.join();
				Bundle b = new Bundle();
				b.userId = userId;
				b.transactionId = -1;
				b.bundleType = b.STOP;
				b.noOfBundles = 1;
				b.bundleNumber = -1;
				b.bundleSize = 0;
				b.data = null;
				oos.writeObject(b.getBytes());
				oos.flush();
				// System.out.println("Sent STOP bundle");
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// This function performs the PUSH event
	public boolean push() {
		Transaction[] transactions = null;
		ACKThread ack = null;
		try {
			byte[] buffer = "PUSH".getBytes();
			oos.writeObject(buffer);
			// System.out.println("PUSH packet sent!!!");
			buffer = (byte[]) ois.readObject();
			Bundle b = new Bundle();
			b.parse(buffer);
			if (b.bundleType == b.RETRANS) {
				File f = new File(storagePath + "/push");
				if (!f.exists()) {
					b = new Bundle();
					b.userId = userId;
					b.transactionId = -1;
					b.bundleType = b.STOP;
					b.noOfBundles = 1;
					b.bundleNumber = -1;
					b.bundleSize = 0;
					b.data = null;
					oos.writeObject(b.getBytes());
					oos.flush();
				} else {
					String[] info = (new String(b.data)).split("\n");
					int tid = Integer.parseInt(info[0]);
					int bno = Integer.parseInt(info[1]);
					ObjectInputStream fin = new ObjectInputStream(
							new FileInputStream(f));
					Transaction[] tt = (Transaction[]) fin.readObject();
					fin.close();
					for (int i = 0; i < (info.length - 2); i++) {
						dh.setSynced(Long.parseLong(info[i + 2]), tt[i].records);
					}
					Transaction[] t = new Transaction[1];
					t[0] = tt[tid];
					if (t[0].bundles == null)
						t[0].organizeBundles();
					boolean result = f.delete();
					// System.out.println("Delete event : " + result);
					if (!(t[0].noOfBundles == bno)) {
						ack = new ACKThread(dh, t, ois, bno);
						ack.start();
						for (int bi = bno; bi < t[0].noOfBundles; bi++) {
							byte[] bundle = t[0].bundles[bi].getBytes();
							oos.writeObject(bundle);
							oos.flush();
							// System.out
							// .println("Sent bundle -> Transaction id : "
							// + tid + " Bundle No. : " + bi);
						}
						ack.join();
					}
					b = new Bundle();
					b.userId = userId;
					b.transactionId = -1;
					b.bundleType = b.STOP;
					b.noOfBundles = 1;
					b.bundleNumber = -1;
					b.bundleSize = 0;
					b.data = null;
					oos.writeObject(b.getBytes());
				}
				return push();
			}
			if (b.bundleType == b.START) {
				List<String[]> list = dh.selectNewRecords();
				if (list != null) {
					int l = list.size();
					Record[] records = new Record[l];
					int n = 0;
					String gid = "";
					for (int i = 0; i < l; i++) {
						records[i] = new Record();
						records[i].groupid = list.get(i)[0];
						records[i].key = list.get(i)[1];
						records[i].value = list.get(i)[2];
						records[i].user = list.get(i)[3];
						records[i].datatype = list.get(i)[4];
						records[i].timestamp = Long.parseLong(list.get(i)[5]);
						records[i].synced = "N";
						if (!(gid.equals(records[i].groupid))) {
							n++;
							gid = records[i].groupid;
						}
					}
					// System.out.println("NO OF TRANSACTIONS:" + n);
					transactions = new Transaction[n];
					int t = 0, r = 0;
					transactions[t] = new Transaction();
					transactions[t].transactionId = t;
					transactions[t].addRecord(records[r]);
					r++;
					while (r < records.length) {
						while (r < records.length
								&& records[r].groupid
										.equals(records[r - 1].groupid)) {
							transactions[t].addRecord(records[r]);
							// System.out.println(records[r].key);
							r++;
						}
						// System.out.println("NO OF RECORDS :" +
						// records.length);
						// System.out.println("R VALUE :" + r);
						if (r < records.length) {
							t++;
							transactions[t] = new Transaction();
							transactions[t].transactionId = t;
							transactions[t].addRecord(records[r]);
							r++;
						}
					}
					ack = new ACKThread(dh, transactions, ois, 0);
					ack.start();
					for (t = 0; t < transactions.length; t++) {
						transactions[t].organizeBundles();
						for (int bi = 0; bi < transactions[t].noOfBundles; bi++) {
							byte[] bundle = transactions[t].bundles[bi]
									.getBytes();
							oos.writeObject(bundle);
							oos.flush();
							// System.out
							// .println("Sent bundle -> Transaction id : "
							// + t + " Bundle No. : " + bi);
						}
					}
					ack.join();
				}
				b = new Bundle();
				b.userId = userId;
				b.transactionId = -1;
				b.bundleType = b.STOP;
				b.noOfBundles = 1;
				b.bundleNumber = -1;
				b.bundleSize = 0;
				b.data = null;
				oos.writeObject(b.getBytes());
				transactions = null;
				ack = null;
				sleep(100);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// System.out.println("Caught Outside");
			if (ack != null) {
				try {
					ack.join();
					sleep(100);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return false;
			}
		}
		return true;
	}

	// This function sends an END message to indicate the end of events to the
	// server
	public boolean end() {
		try {
			byte[] buffer = "END".getBytes();
			oos.writeObject(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	// This function performs the various events of the framework at regular
	// intervals
	public void run() {
		while (flag) {
			try {
				newdata = false;
				File folder = new File(storagePath);
				if (!folder.exists())
					folder.mkdirs();
				// System.out.println("SERVER IS:" + server);
				// System.out.println("PORT IS:" + port);
				connection = new Socket(server, port);
				connection.setSoTimeout(180000);
				ois = new ObjectInputStream(connection.getInputStream());
				oos = new ObjectOutputStream(connection.getOutputStream());
				boolean result = namespace();
				// System.out.println("NAMESPACE RESULT : " + result);
				if (result)
					result = update();
				// System.out.println("UPDATE RESULT : " + result);
				if (result)
					result = pull();
				// System.out.println("PULL RESULT : " + result);
				if (result) {
					if (!newdata)
						dh.changeLockStatus("PENDING", "LOCKED");
					result = push();
				}
				// System.out.println("PUSH RESULT : " + result);
				if (result) {
					end();
				}
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				int x = 0;
				while (x < 5) {
					// System.out.println("THREAD IS SLEEPING NOW!!! "
					// + System.currentTimeMillis() + " " + x);
					sleep(60000);
					x++;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// System.out.println("EXITING THREAD NOW!!!");
	}

	// This class receives acknowledgments to the bundles containing namespace
	// records that are sent to the server
	class AckThread extends Thread {

		Helper h;
		ObjectInputStream ois;
		ArrayList<String[]> records;

		public AckThread(Helper h, ObjectInputStream ois,
				ArrayList<String[]> records) {
			this.h = h;
			this.ois = ois;
			this.records = records;
		}

		public void run() {
			int l = records.size();
			try {
				for (int i = 0; i < l; i++) {
					byte[] buffer = (byte[]) ois.readObject();
					Bundle b = new Bundle();
					b.parse(buffer);
					if (b.isAcknowledgement(i, 1, 0)) {
						String user = records.get(i)[1];
						String namespace = records.get(i)[2];
						String permission = records.get(i)[3];
						h.removeNamespace(user, namespace, permission);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// This class receives acknowledgments to the bundles containing update
	// records that are sent to the server
	class ACKThread extends Thread {
		Transaction[] t;
		int tid;
		int noOfBundles;
		ObjectInputStream ois;
		int seq;
		Helper dh;
		String extStorageDirectory = Environment.getExternalStorageDirectory()
				.toString();

		public ACKThread(Helper dh, Transaction[] t, ObjectInputStream ois,
				int seq) {
			this.dh = dh;
			this.t = t;
			this.ois = ois;
			this.seq = seq;
		}

		public void run() {
			// System.out.println("ACK STARTED");
			// System.out.println("No of transactions : " + t.length);
			// System.out.println("Seq : " + seq);
			Bundle b = null;
			int i = 0, j;
			try {
				for (i = 0; i < t.length; i++) {
					String timestamp = "";
					while (t[i].noOfBundles == 0)
						;
					for (j = seq; j < t[i].noOfBundles; j++) {
						do {
							// System.out.println("SEQ ACK : " +
							// t[i].noOfBundles);
							byte[] buffer = (byte[]) ois.readObject();
							b = new Bundle();
							b.parse(buffer);
							if (b.bundleNumber == b.noOfBundles - 1)
								timestamp = new String(b.data);
							// System.out.println("ACK WAITING : " + j);
							// System.out.println("ACK RECEIVED : "
							// + b.bundleNumber);
							// System.out.println(t[i].transactionId + " "
							// + t[i].noOfBundles + " " + j);
						} while (!b.isAcknowledgement(t[i].transactionId,
								t[i].noOfBundles, j));
					}
					// System.out.println("TIMESTAMP RECEIVED IS :" +
					// timestamp);
					long ts = Long.parseLong(timestamp);
					dh.setSynced(ts, t[i].records);
					seq = 0;
				}
			} catch (Exception e) {
				e.printStackTrace();
				ObjectOutputStream fout;
				try {
					fout = new ObjectOutputStream(new FileOutputStream(
							new File(storagePath + "/push")));
					fout.writeObject(t);
					// System.out
					// .println("-----------------------------------------------------");
					// System.out.println("Written broken transaction!!!");
					// System.out
					// .println("-----------------------------------------------------");
					fout.close();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
			// System.out.println("ACK ENDED");
		}
	}
}