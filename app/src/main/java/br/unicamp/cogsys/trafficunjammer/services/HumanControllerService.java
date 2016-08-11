/**
 * 
 */
package br.unicamp.cogsys.trafficunjammer.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.widget.GridView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import br.unicamp.cogsys.trafficunjammer.adapters.JunctionAdapter;
import it.polito.appeal.traci.ChangeLightsStateQuery;
import it.polito.appeal.traci.ControlledLink;
import it.polito.appeal.traci.Logic;
import it.polito.appeal.traci.Phase;
import it.polito.appeal.traci.SumoTraciConnection;
import it.polito.appeal.traci.TLState;
import it.polito.appeal.traci.TrafficLight;
import it.polito.appeal.traci.Vehicle;

/**
 * @author andre
 *
 */
public class HumanControllerService extends Service
{
	
	 // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

	private Looper mServiceLooper;
	 
	private ServiceHandler mServiceHandler;
	
	private Runnable trafficSimulation;
	
	private SumoTraciConnection sumo;
	
	private TLState nextTLState = null;
	
	private TrafficLight nextTrafficLight = null;
	
	private Map<String, TrafficLight> mapTrafficLights;
	
	private List<String> junctionIDs;
	
    private Map<String, List<TLState>> mapJunctionPhases;
    
	private Map<String, List<RectF>> mapJunctionControlledLinksShapes;
	
	private JunctionAdapter junctionAdapter;
	
	private GridView gridview;
	
	private OnScenarioBuilt onScenarioBuiltListener;
	
	private String serverIP;

	private Integer serverPort;
	 
		
	/* (non-Javadoc)
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() 
	{
		super.onCreate();
		
		 HandlerThread thread = new HandlerThread("HumanControllerService",Process.THREAD_PRIORITY_BACKGROUND);
		 thread.start();

		 // Get the HandlerThread's Looper and use it for our Handler 
		 mServiceLooper = thread.getLooper();
		 mServiceHandler = new ServiceHandler(mServiceLooper);		 
		 		 
		 trafficSimulation = new TrafficSimulation();
	}

	@Override
	public IBinder onBind(Intent intent) 
	{
		return mBinder;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Service#onDestroy()
	 */
	@Override
	public void onDestroy() 
	{	
		super.onDestroy();
	}
	
	public void startSimulation(JunctionAdapter junctionAdapter, GridView gridview, String serverIP, Integer serverPort)
	{
		this.serverPort = serverPort;
		this.serverIP = serverIP;
		this.gridview = gridview;
		this.junctionAdapter = junctionAdapter;
		mServiceHandler.post(trafficSimulation);		
	}
	
	public void changeToPhase(String junctionID,int column) 
	{

		if(mapTrafficLights!=null)
			nextTrafficLight = mapTrafficLights.get(junctionID);

		if(junctionID!=null&&mapJunctionPhases!=null)
		{
			List<TLState> phases = mapJunctionPhases.get(junctionID);

			if(phases!=null)
			{
				if((column-1)<phases.size())
					nextTLState = phases.get(column-1);
			}
		}
	}	
		
	// Handler that receives messages from the thread
	private static final class ServiceHandler extends Handler 
	{
		public ServiceHandler(Looper looper) 
		{
			super(looper);
		}
	}
	
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder 
    {
    	public HumanControllerService getService() 
        {
            // Return this instance of LocalService so clients can call public methods
            return HumanControllerService.this;
        }
    }
	
	private class TrafficSimulation implements Runnable
	{

		@Override
		public void run() 
		{
			try 
			{
				sumo = new SumoTraciConnection(InetAddress.getByName(serverIP), serverPort);
				
				/*
				 * Separating network scenario info
				 */
				junctionIDs = new ArrayList<String>();			 
				mapJunctionPhases = new HashMap<String, List<TLState>>();		 
				mapJunctionControlledLinksShapes = new HashMap<String, List<RectF>>();
								
				mapTrafficLights = sumo.getTrafficLightRepository().getAll();	
				
				int totalNumberOfJunctionPhases=0;
				int maxNumberOfPhasesPerJunction=0;
				
				for(Entry<String, TrafficLight> trafficLightPairs : mapTrafficLights.entrySet())
				{
					String junctionID = trafficLightPairs.getKey();
					TrafficLight trafficLight = trafficLightPairs.getValue();
					
					/*
					 * Junction ID
					 */
					junctionIDs.add(junctionID);
					
					/*
					 * Junction Phases
					 */
					List<TLState> TLStates = new ArrayList<TLState>();
					Logic[] logics = trafficLight.queryReadCompleteDefinition().get().getLogics();
					int totalNumberOfPhasesInThisJunction=0;
					for(Logic logic: logics)
					{
						Phase[] phases = logic.getPhases();
						for(Phase phase :phases)
						{
							TLStates.add(phase.getState());
							totalNumberOfPhasesInThisJunction++;
						}
					}
					if(totalNumberOfPhasesInThisJunction>maxNumberOfPhasesPerJunction)
						maxNumberOfPhasesPerJunction = totalNumberOfPhasesInThisJunction;
					totalNumberOfJunctionPhases+=totalNumberOfPhasesInThisJunction;
					mapJunctionPhases.put(junctionID, TLStates);
					
					/*
					 * ControlledLinks Shapes
					 */
					List<RectF> controlledLinksShapes = new ArrayList<RectF>();
					ControlledLink[][] controlledLinks = trafficLight.queryReadControlledLinks().get().getLinks();
					for(int i=0;i<controlledLinks.length;i++)
					{		
						for(int j=0;j<controlledLinks[i].length;j++)
						{
							ControlledLink controlledLink = controlledLinks[i][j];
							
							Path incomingPath = controlledLink.getIncomingLane().queryReadShape().get(); 
							RectF incomingPathBounds = new RectF();
							incomingPath.computeBounds(incomingPathBounds, true);
							controlledLinksShapes.add(incomingPathBounds);
							
							Path acrossPath = controlledLink.getAcrossLane().queryReadShape().get();
							RectF acrossPathBounds = new RectF();
							acrossPath.computeBounds(acrossPathBounds, true);
							controlledLinksShapes.add(acrossPathBounds);
							
							Path outgoingPath = controlledLink.getOutgoingLane().queryReadShape().get();
							RectF outgoingPathBounds = new RectF();
							outgoingPath.computeBounds(outgoingPathBounds, true);
							controlledLinksShapes.add(outgoingPathBounds);
						
						}						
					}
					mapJunctionControlledLinksShapes.put(junctionID, controlledLinksShapes);
				}
				
				/*
				 * Building scenario
				 */
				gridview.setNumColumns(maxNumberOfPhasesPerJunction+1);
				junctionAdapter.setJunctionIDs(junctionIDs);
				junctionAdapter.setMapJunctionControlledLinksShapes(mapJunctionControlledLinksShapes);
				junctionAdapter.setMapJunctionPhases(mapJunctionPhases);
				junctionAdapter.setTotalNumberOfJunctionPhases(totalNumberOfJunctionPhases);
				junctionAdapter.setMaxNumberOfPhasesPerJunction(maxNumberOfPhasesPerJunction);
				
				onScenarioBuiltListener.finishedBuildingScenario();
				
				int timeStep = sumo.getSimulationData().queryCurrentSimTime().get();
				
				int vehicleNUmber = 0;

				Collection<Vehicle> vehicles = sumo.getVehicleRepository().getAll().values();
				vehicleNUmber = vehicles.size();
						
				while(timeStep ==0 || vehicleNUmber >0)
				{		
					
					if(nextTrafficLight!=null&&nextTLState!=null)
					{
						ChangeLightsStateQuery lstQ  = nextTrafficLight.queryChangeLightsState();

						lstQ.setValue(nextTLState);
						lstQ.run();
						
						nextTLState = null;
						nextTrafficLight = null;
					}
					
					/*
					 * Next step
					 */
					sumo.nextSimStep();
					timeStep++;	
					vehicles = sumo.getVehicleRepository().getAll().values();
					vehicleNUmber = vehicles.size();
				}	
				
				sumo.close();
								
			} catch (UnknownHostException e) 
			{
				e.printStackTrace();
			} catch (IOException e) 
			{
				e.printStackTrace();
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}				
		}		
	}
	
	public interface OnScenarioBuilt
	{
		public void finishedBuildingScenario();
	}

	/**
	 * @return the gridview
	 */
	public synchronized GridView getGridview() 	
	{
		return gridview;
	}

	/**
	 * @param gridview the gridview to set
	 */
	public synchronized void setGridview(GridView gridview) 
	{
		this.gridview = gridview;
	}

	/**
	 * @return the onScenarioBuiltListener
	 */
	public synchronized OnScenarioBuilt getOnScenarioBuiltListener() {
		return onScenarioBuiltListener;
	}

	/**
	 * @param onScenarioBuiltListener the onScenarioBuiltListener to set
	 */
	public synchronized void setOnScenarioBuiltListener(
			OnScenarioBuilt onScenarioBuiltListener) {
		this.onScenarioBuiltListener = onScenarioBuiltListener;
	}
}