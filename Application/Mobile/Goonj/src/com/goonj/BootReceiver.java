package com.goonj;

import com.frameworkmobile.FrameworkService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BootReceiver extends BroadcastReceiver 
{	
	@Override
    public void onReceive(Context context, Intent intent)
    {        
		Intent framework = new Intent(context, FrameworkService.class);			
		context.startService(framework);        
    }    
}
    