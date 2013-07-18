package com.goonj;


import java.io.File;
import java.lang.reflect.Method;

import com.frameworkmobile.Config;
import com.frameworkmobile.FrameworkService;
import com.frameworkmobile.Helper;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class Goonj extends Activity implements OnClickListener 
{	 
    private static final int AUDIO_REQUEST = 1001;
	private static final int CAMERA_VIDEO_REQUEST = 1002;
	private static final int CAMERA_PIC_REQUEST = 1003;	
	double gps[] = new double[2]; 
	String appFolder = "Goonj";
	String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
	public static String currentFile = "";
	MyFileObserver fo;
	LocationManager locationManager;
	LocationListener locationListener;
	private Helper dh;	
	   
	@Override
	public void onCreate(Bundle savedInstanceState)
	{		 
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    Config c = new Config(this);			
		c.setUser("SATHYAM");
		c.setUserId("11");
		c.setIP("10.193.14.210");
		c.setPort("8010");
		c.setPhoneNo("+919818962835");
		c.setStoragePath(extStorageDirectory + "/Goonj");
	    dh = new Helper(this);
	    dh.open();	    
	    if(!isMyServiceRunning("com.frameworkmobile.FrameworkService"))
		{	    
			Log.d("FRAMEWORK","STARTING SERVICES NOW!!!");
			dh.subscribeToNamespace("SATHYAM", "first", "rwx");
			Intent framework = new Intent(this, FrameworkService.class);			
			startService(framework);
		}
	    File file = new File(extStorageDirectory + "/" + appFolder);
	    if(!file.exists()) 
	    	file.mkdirs();  
	    // Set up click listeners for all the buttons
	    View audioButton = findViewById(R.id.record_audio);
	    audioButton.setOnClickListener(this);
	    View videoButton = findViewById(R.id.capture_video);
	    videoButton.setOnClickListener(this);
	    View photoButton = findViewById(R.id.capture_photo);
	    photoButton.setOnClickListener(this);
	    View closeButton = findViewById(R.id.close);
	    closeButton.setOnClickListener(this);
	    enableGPRS();
	    enableGPS();    	        	 	      
    }
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		dh.close();
	}
	  
	@Override
	public void onResume()
	{
		super.onResume();
		dh.open();
	}
	@Override
	public void onPause()
	{
		super.onPause();
		dh.close();
	}
	private boolean isMyServiceRunning(String serv)
	{
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
	    {
	        if (serv.equals(service.service.getClassName()))
	        {
	        	System.out.println("Hello:  "+service.service.getClassName());
	            return true;
	        }
	    }
	    return false;
	}
	public void enableGPS()
	{
		// Check GPS status and prompt an alert dialog
	    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
	    {  
	    	createGpsDisabledAlert();  
	    }   
	    // Initialize GPS parameters
	    Criteria crit = new Criteria();
	    crit.setAccuracy(Criteria.ACCURACY_FINE);
	    String provider = locationManager.getBestProvider(crit, true);
	    if(provider!=null)
	    {
		    Location loc = locationManager.getLastKnownLocation(provider);
		    if(loc != null)
		    {
			    gps[0] = loc.getLatitude();
			    gps[1] = loc.getLongitude();
		    }
	    }
    	// Define a listener that responds to location updates
    	locationListener = new LocationListener() 
    	{
    		// Called when a new location is found by the network location provider.
    		public void onLocationChanged(Location location) 
    		{					
    			gps[0] = location.getLatitude();
    			gps[1] = location.getLongitude();   	    	
    		}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
	        public void onProviderEnabled(String provider) {}
	        public void onProviderDisabled(String provider) {}
    	};
    	// Register the listener with the Location Manager to receive location updates
    	locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	}
	
	public void enableGPRS()
	{
		 // Enable packet data
	    try 
	    {
			setMobileDataEnabled(getApplicationContext(),true);
		} 
	    catch (Exception e) 
	    {
			e.printStackTrace();
		}
	}
		
	// Function shows alert dialog giving user an option to switch on GPS
	private void createGpsDisabledAlert()
	{  
		AlertDialog.Builder builder = new AlertDialog.Builder(this);  
		builder.setMessage("Your GPS is disabled! Would you like to enable it?")  
		     .setCancelable(false)  
		     .setPositiveButton("Enable GPS",  
	          new DialogInterface.OnClickListener()
		     {  
		          public void onClick(DialogInterface dialog, int id)
		          {  
		               showGpsOptions();  
		          }  
		     });  
		     builder.setNegativeButton("Do nothing",  
	          new DialogInterface.OnClickListener()
		     {  
		          public void onClick(DialogInterface dialog, int id)
		          {  
		               dialog.cancel();  
		          }  
		     });  
		AlertDialog alert = builder.create();  
		alert.show();  
	}  
	// Function shows options for the GPS enable dialog
	private void showGpsOptions()
	{  
	        Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);  
	        startActivity(gpsOptionsIntent);  
	}  	
	// Function enables/disables packet data according to value of parameter enabled
	private void setMobileDataEnabled(Context context, boolean enabled)
	{
		try 
		{
		    final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    final Class conmanClass = Class.forName(conman.getClass().getName());
		    final java.lang.reflect.Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
		    iConnectivityManagerField.setAccessible(true);
		    final Object iConnectivityManager = iConnectivityManagerField.get(conman);
		    final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
		    final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
		    setMobileDataEnabledMethod.setAccessible(true);
		    setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);
		}
		catch(Exception e){}
	}			
	// Function checks for button clicked and takes suitable actions
	public void onClick(View v) 
	{
		
		switch (v.getId()) 
    	{
    		case R.id.record_audio:
    			fo = new MyFileObserver(extStorageDirectory+"/Sounds/");
	    		fo.startWatching();
	    		Intent audioIntent =new Intent(android.provider.MediaStore.Audio.Media.RECORD_SOUND_ACTION);
	    		audioIntent.putExtra(android.provider.MediaStore.EXTRA_FINISH_ON_COMPLETION, "true");
	    		startActivityForResult(audioIntent, AUDIO_REQUEST);
	    		break;
	    	case R.id.capture_video:
	    		Intent videoIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);    	    			    		
	    		videoIntent.putExtra(android.provider.MediaStore.EXTRA_FINISH_ON_COMPLETION, "true");
	    		startActivityForResult(videoIntent, CAMERA_VIDEO_REQUEST);
	    		break;
	    	case R.id.capture_photo:
	    		Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
	    		currentFile = extStorageDirectory + "/" + appFolder +  "/" + "photo" + System.currentTimeMillis() + "_" + gps[0] + "_" + gps[1] + ".png";
	    		Uri photoUri = Uri.fromFile(new File(currentFile));	    		
	    		cameraIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
	    		cameraIntent.putExtra(android.provider.MediaStore.EXTRA_FINISH_ON_COMPLETION, "true");
	    		startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
	    		break;
	    	case R.id.close:	    		
	    		dh.close();
	    		this.finish();
	    		break;
    	}
    }
	    
    public String getVideoPath(Uri uri) 
    {
        String[] projection = { MediaStore.Video.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }
    // Function gets invoked when the home screen returns after media recording
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {  
    	super.onActivityResult(requestCode, resultCode, data); 
    	dh.open();
    	if (requestCode == AUDIO_REQUEST)
        {          	   
    		fo.stopWatching(); 
    		if(currentFile != "")
    		{
				String sourcePath = extStorageDirectory + "/Sounds/" + currentFile;						
				String destPath = extStorageDirectory + "/" + appFolder + "/" + "audio" + System.currentTimeMillis() + "_" + gps[0] + "_" + gps[1] + ".3ga";
				File sourceF = new File(sourcePath);
				try 
				{
				    sourceF.renameTo(new File(destPath));
				    dh.putToRepository("first.Sathyam.Audio", destPath,"file");
					Toast.makeText(this, "Recorded Audio", Toast.LENGTH_SHORT).show();
				}
				catch (Exception e) 
				{				
				    Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}						
    		}
        } 
    	if(resultCode == RESULT_OK)    
    	{	    	
	    	if (requestCode == CAMERA_PIC_REQUEST)
	        {  
	    		dh.putToRepository("first.Sathyam.Photo", currentFile,"file");
				Toast.makeText(this, "Captured photo", Toast.LENGTH_SHORT).show();				
	        }
    		if (requestCode == CAMERA_VIDEO_REQUEST)
	        {  
	        	String sourcePath = getVideoPath(data.getData());			
				String destPath = extStorageDirectory + "/" + appFolder + "/" + "video" + System.currentTimeMillis() + "_" + gps[0] + "_" + gps[1] + ".mp4";
				File sourceF = new File(sourcePath);
				try 
				{ 
				    sourceF.renameTo(new File(destPath));
					dh.putToRepository("first.Sathyam.Video", destPath, "file");
					Toast.makeText(this, "Captured Video", Toast.LENGTH_SHORT).show(); 
				}  
				catch (Exception e)   
				{				 
				    Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}	  	 	   
	        }
    	} 
    	currentFile = "";
    }  
}
// Class listens for a new audio file getting created
class MyFileObserver extends FileObserver
{
	public MyFileObserver(String path,int mask)
	{
		super(path,mask);
	}
	public MyFileObserver(String path)
	{
		super(path);
	}
	@Override
	public void onEvent(int event,String path)
	{		
		if(path!=null)
			Goonj.currentFile = path;		
	}
}