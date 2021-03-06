import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sathyam
 */
public class Resolve {

    static Connection con;

    public static void main(String args[]) throws Exception {
        String groupid = args[0];
        long timestamp = Long.parseLong(args[1]);
        getConnection();
        updateRecord(groupid, timestamp);
        closeConnection();

    }
    // This function is used to get a connection to the database
    protected static void getConnection() {
        System.out.println("Beginning connection");
        try {
            Class.forName("org.sqlite.JDBC");
            con = DriverManager.getConnection("jdbc:sqlite:/home/sathyam/Desktop/Interface/frameworkdb");//fill in the path to the framework database here
            try {
                Statement stat = con.createStatement();
                stat.executeUpdate("CREATE TABLE server(groupid TEXT,key text,value text,user text,datatype text,timestamp number,deviceid text, primary key(groupid,key,timestamp));");
                stat.executeUpdate("CREATE TABLE serverbackup(groupid TEXT,key text,value text,user text,datatype text,timestamp number,deviceid text);");
                stat.executeUpdate("CREATE TABLE namespaces(user TEXT,namespace TEXT,permission TEXT,primary key(user,namespace));");
            } catch (Exception e) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Connection got!!!");
    }
    // This function executes a query
    protected static void executeQuery(String query) throws Exception {
        if (con != null) {
            boolean flag = true;
            while (flag) {
                try {
                    Statement st1 = con.createStatement();
                    st1.executeUpdate(query);
                    flag = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    // This function executes a query and returns the result
    protected static ArrayList<String[]> executeQueryForResult(String query) {
        ArrayList<String[]> result = null;
        boolean flag = true;
        while (flag) {
            try {
                if (con != null) {

                    //System.out.println("Inside!!!");
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
                                System.out.print(row[ik - 1] + " ");
                            }
                            result.add(row);
                            System.out.println();
                        } while (rs1.next());
                    } else {
                        result = null;
                    }
                }
                flag = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    protected static void closeConnection() {
        try {
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // This function updates the server records in the main table having groupid 'g' with records having timestamp as 'timestamp' from the backup table and deletes all other conflicting records
    public static void updateRecord(String g, long timestamp) {
        System.out.println("Started function");
        boolean flag = true;
        while (flag) {
            try {
                con.setAutoCommit(false);
                String query = "select * from serverbackup where timestamp=" + timestamp + ";";
                ArrayList<String[]> result = executeQueryForResult(query);
                if (result != null) {
                    for(int i=0;i<result.size();i++) {
		      String groupid = result.get(i)[0];
		      String key = result.get(i)[1];
		      String value = result.get(i)[2];
		      String user = result.get(i)[3];
		      String datatype = result.get(i)[4];
		      long timestamp1 = Long.parseLong(result.get(i)[5]);
		      String deviceId = result.get(i)[6];
		      query = "delete from server  where groupid='" + groupid + "' and key='" + key + "' ";//and datatype='" + r.datatype + "')";
		      System.out.println("Query :" + query);
		      executeQuery(query);
		      query = "insert into server values('" + groupid + "','" + key + "','" + value + "','" + user + "','" + datatype + "'," + timestamp1 + ",'" + deviceId + "')";
		      System.out.println("Query :" + query);
		      executeQuery(query);
		    }
		    query = "delete from serverbackup where groupid='" + g + "'";
		    System.out.println("Query :" + query);
		    executeQuery(query);
		    con.commit();		  
		}
               flag = false;
            } catch (Exception e) {
                if (con != null) {
                    try {
                        System.err.print("Transaction is being rolled back");
                        con.rollback();
                        Thread.sleep(2000);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                e.printStackTrace();
            }
        }
    }
}
