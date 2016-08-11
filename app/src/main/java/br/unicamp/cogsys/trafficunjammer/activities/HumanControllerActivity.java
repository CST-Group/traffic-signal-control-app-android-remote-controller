/**
 * 
 */
package br.unicamp.cogsys.trafficunjammer.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import java.util.List;

import br.unicamp.cogsys.trafficunjammer.R;
import br.unicamp.cogsys.trafficunjammer.adapters.JunctionAdapter;
import br.unicamp.cogsys.trafficunjammer.services.HumanControllerService;
import br.unicamp.cogsys.trafficunjammer.services.HumanControllerService.LocalBinder;
import br.unicamp.cogsys.trafficunjammer.services.HumanControllerService.OnScenarioBuilt;

/**
 * @author andre
 *
 */
public class HumanControllerActivity extends Activity implements OnScenarioBuilt
{
	private HumanControllerService humanControllerService;
	
	private ServiceConnection mConnection;
	
	private boolean mBound = false;
	
	private GridView gridview;
	
	private JunctionAdapter junctionAdapter;
	
	private WakeLock wl;
	
	private Handler handler;
	
	private int width;
	
	private int height;
	 
	 private float dw;
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_humancontroller);
		
		/*
		 * Customizing for different devices
		 */

		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);		

		height = display.getHeight();
		width = display.getWidth();

		if(width<height)
		{
			int aux = width;
			width = height;
			height = aux;
		}

		handler = new Handler();

		gridview = (GridView) findViewById(R.id.gridview);

		junctionAdapter = new JunctionAdapter(this);
		
		gridview.setAdapter(junctionAdapter);

		gridview.setOnItemClickListener(new OnItemClickListener() 
		{
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) 
			{
				if(mBound)
				{
					int maxNumberOfPhasesPerJunction = junctionAdapter.getMaxNumberOfPhasesPerJunction();
					int line = 0;
					int column = 0;

					if(maxNumberOfPhasesPerJunction>0)
					{
						line = position / (maxNumberOfPhasesPerJunction+1);
						column = position % (maxNumberOfPhasesPerJunction+1);

						if(column>0)
						{
							List<String> junctionIDs = junctionAdapter.getJunctionIDs();

							if(junctionIDs!=null)
							{
								String junctionID = junctionIDs.get(line);			        							        				

								humanControllerService.changeToPhase(junctionID, column);
							}		
						}		        					        			
					}	
				}		        		
			}
		});

		mConnection = new ServiceConnection() 
		{
			@Override
			public void onServiceConnected(ComponentName className, IBinder service) 
			{
				// We've bound to  HumanControllerService, cast the IBinder and get  humanControllerService instance
				LocalBinder binder = (LocalBinder) service;
				humanControllerService = binder.getService();
				mBound = true;

				humanControllerService.setOnScenarioBuiltListener(HumanControllerActivity.this);				

			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) 
			{
				mBound = false;
			}
		};	 

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "WakeLock");
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onStart()
	 */
	@Override
	protected void onStart() 
	{
		super.onStart();
		
		// Bind to LocalService
        Intent intent = new Intent(this, HumanControllerService.class);       
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		if(wl!=null && !wl.isHeld())
		{
			wl.acquire();			
		}
		
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() 
	{		
		super.onPause();
		
		if(wl!=null && wl.isHeld())
		{
			wl.release();
		}
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onStop()
	 */
	@Override
	protected void onStop() 
	{
		super.onStop();
		// Unbind from the service
        if (mBound) 
        {
            unbindService(mConnection);
            mBound = false;
        }

	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() 
	{		
		super.onDestroy();				
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch (item.getItemId()) 
		{
		case R.id.connect:
			if(mBound)
			{
				AlertDialog.Builder alert = new AlertDialog.Builder(this);

				alert.setTitle(getString(R.string.server_ip));

				// Set an EditText view to get user input
				final EditText serverIPinput = new EditText(this);
				serverIPinput.setHint(R.string.type_server_ip);
				serverIPinput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
				final EditText portInput = new EditText(this);
				portInput.setHint(R.string.type_server_port);
				portInput.setInputType(InputType.TYPE_CLASS_NUMBER);

				LinearLayout ll=new LinearLayout(this);
				ll.setOrientation(LinearLayout.VERTICAL);
				ll.addView(serverIPinput);
				ll.addView(portInput);

				alert.setView(ll);
				
				String serverIP = null;
				String serverPort = null;
				
				final SharedPreferences prefs = getSharedPreferences("humanController", 0);		
				serverIP = prefs.getString("server_ip", null);
				serverPort =  prefs.getString("serverPort", null);
				
				if(serverIP!=null)
					serverIPinput.setText(serverIP);
				if(serverPort!=null)
					portInput.setText(serverPort);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int whichButton) 
					{
						String serverIP = serverIPinput.getText().toString();
						Integer serverPort = Integer.valueOf(portInput.getText().toString());
						if(serverIP!=null && serverPort!=null)
						{
							humanControllerService.startSimulation(junctionAdapter,gridview,serverIP,serverPort);
							
							SharedPreferences.Editor editor = prefs.edit();	
							editor.putString("server_ip", serverIP);
							editor.putString("serverPort", serverPort.toString());
							editor.commit();
						}
						
					}
				});

				alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() 
				{
					public void onClick(DialogInterface dialog, int whichButton) 
					{
						// Canceled.
					}
				});

				alert.show();
			}

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void finishedBuildingScenario() 
	{
		handler.post(new Runnable() 
        {
			
			@Override
			public void run()
			{
				int maxColumnWidth = junctionAdapter.getMaxWidthHeight();
				//gridview.setColumnWidth(maxColumnWidth);
				
				float displayWidth  = (float) ((maxColumnWidth+ (maxColumnWidth*0.01f)+20)*(junctionAdapter.getMaxNumberOfPhasesPerJunction()+1));
				
				dw = width / displayWidth ;
				
				junctionAdapter.setDw(dw);
				
				gridview.setColumnWidth((int) (maxColumnWidth*dw));
				gridview.setHorizontalSpacing((int)(20*dw));
				LayoutParams gridviewLp = (LayoutParams) gridview.getLayoutParams();
				//gridviewLp.width = (maxColumnWidth+ (int) (maxColumnWidth*0.01)+20)*(junctionAdapter.getMaxNumberOfPhasesPerJunction()+1);
				gridviewLp.width = width;
				
				junctionAdapter.notifyDataSetChanged();							
			}
		});		
	}
}