/**
 * 
 */
package br.unicamp.cogsys.trafficunjammer.adapters;

import it.polito.appeal.traci.LightState;
import it.polito.appeal.traci.TLState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import br.unicamp.cogsys.trafficunjammer.R;
import br.unicamp.cogsys.trafficunjammer.customclasses.PhaseView;

/**
 * @author andre
 *
 */
public class JunctionAdapter extends BaseAdapter 
{
	
	 private Context mContext;	 	 
	 
	 private List<String> junctionIDs;
	 
	 private Map<String, List<TLState>> mapJunctionPhases;
	 
	 private Map<String, List<RectF>> mapJunctionControlledLinksShapes;
	 
	 private int totalNumberOfJunctionPhases = 0;
	 
	 private int maxNumberOfPhasesPerJunction=0;
	 
	 private int maxWidthHeight = 0;	
	 
	 private Map<Integer,View> mapJunctionViews;
	 
	 private float dw;
	 
	 

	/**
	 * @param mContext
	 */
	public JunctionAdapter(Context mContext) 
	{
		super();
		this.mContext = mContext;
		mapJunctionViews = new HashMap<Integer, View>();
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getCount()
	 */
	@Override
	public int getCount() 
	{	
		int numID = 0;
		if(junctionIDs!=null)
			numID = junctionIDs.size();
		return totalNumberOfJunctionPhases+numID;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItem(int)
	 */
	@Override
	public TLState getItem(int position) 
	{	
		TLState item = null;
		
		int line = 0;
		int column = 0;
		
		if(maxNumberOfPhasesPerJunction>0)
		{
			line = position / (maxNumberOfPhasesPerJunction+1);
			column = position % (maxNumberOfPhasesPerJunction+1);
			
			if(column>0)
			{
				if(junctionIDs!=null)
				{
					String junctionID = junctionIDs.get(line);
					
					if(junctionID!=null&&mapJunctionPhases!=null)
					{
						List<TLState> phases = mapJunctionPhases.get(junctionID);
						
						if(phases!=null)
						{
							if((column-1)<phases.size())
								item = phases.get(column-1);
						}
					}
				}
			}		
		}								
		
		return item;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getItemId(int)
	 */
	@Override
	public long getItemId(int position) 
	{		
		//long id = -1;
		int line = -1;
		
		if(maxNumberOfPhasesPerJunction>0)
		{
			line = position / (maxNumberOfPhasesPerJunction+1);				
		}
		return line;
	}

	/* (non-Javadoc)
	 * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		PhaseView phaseView=null;

		RelativeLayout idView = null;

		int line = 0;
		int column = 0;

		if(maxNumberOfPhasesPerJunction>0)
		{				
			line = position / (maxNumberOfPhasesPerJunction+1);
			column = position % (maxNumberOfPhasesPerJunction+1);
		}	
		
		if(column==0)
		{

			idView = (RelativeLayout) mapJunctionViews.get(position);

			if(idView==null)
			{
				idView = (RelativeLayout) LayoutInflater.from(mContext).inflate(R.layout.junction_id_view, null);

				TextView junctionIdTV = (TextView) idView.findViewById(R.id.junction_id);
				String junctionID = junctionIDs.get(line);
				junctionIdTV.setText("Junction id: "+junctionID);
				junctionIdTV.setTextSize(36*dw);

				RelativeLayout.LayoutParams lp = (LayoutParams) junctionIdTV.getLayoutParams();
				if(lp!=null)
				{
					lp.width = (int) (maxWidthHeight*dw);
					lp.height = (int) (maxWidthHeight*dw);
				}	

				mapJunctionViews.put(position, idView);
			}

			return idView;
		}else
		{
			phaseView = (PhaseView) mapJunctionViews.get(position);

			if(phaseView==null)
			{
				phaseView = new PhaseView(mContext);

				phaseView.setMaxWidthHeight(maxWidthHeight);
				phaseView.setDw(dw);

				List<RectF> controlledLinksShapes = getControlledLinksShapes(position);

				if(controlledLinksShapes!=null)
				{
					TLState tlState = getItem(position);
					if(tlState!=null)
					{
						LightState[] lightStates = tlState.lightStates;
						if(lightStates!=null)
						{
							for(int i=0;i<lightStates.length;i++)
							{
								LightState lightState = lightStates[i];
								if(lightState!=null)
								{
									int pathColor = Color.GREEN;
									if(lightState.isYellow())
										pathColor = Color.YELLOW;
									else if(lightState.isRed())
										pathColor = Color.RED;
									for(int j=3*i;j<3*i+3;j++)
									{
										RectF linkShape =  controlledLinksShapes.get(j);
										if(linkShape!=null)
											phaseView.addPhaseViewItem(linkShape,pathColor);
									}		
								}								
							}
						}
					}		
				}
				mapJunctionViews.put(position, phaseView);
			}

			return phaseView;
		}				
	}

	private List<RectF> getControlledLinksShapes(int position)
	{
		List<RectF> controlledLinksShapes = null;
		
		int line = 0;
		
		if(maxNumberOfPhasesPerJunction>0)
		{
			line = position / (maxNumberOfPhasesPerJunction+1);
			
			if(junctionIDs!=null)
			{
				String junctionID = junctionIDs.get(line);
				
				if(junctionID!=null&&mapJunctionControlledLinksShapes!=null)
				{
					controlledLinksShapes = mapJunctionControlledLinksShapes.get(junctionID);				
				}
			}	
		}
						
		return controlledLinksShapes;
	}

	/**
	 * @return the junctionIDs
	 */
	public synchronized List<String> getJunctionIDs() {
		return junctionIDs;
	}

	/**
	 * @param junctionIDs the junctionIDs to set
	 */
	public synchronized void setJunctionIDs(List<String> junctionIDs) {
		this.junctionIDs = junctionIDs;
	}

	/**
	 * @return the mapJunctionPhases
	 */
	public synchronized Map<String, List<TLState>> getMapJunctionPhases() {
		return mapJunctionPhases;
	}

	/**
	 * @param mapJunctionPhases the mapJunctionPhases to set
	 */
	public synchronized void setMapJunctionPhases(
			Map<String, List<TLState>> mapJunctionPhases) {
		this.mapJunctionPhases = mapJunctionPhases;
	}

	/**
	 * @return the mapJunctionControlledLinksShapes
	 */
	public synchronized Map<String, List<RectF>> getMapJunctionControlledLinksShapes() {
		return mapJunctionControlledLinksShapes;
	}

	/**
	 * @param mapJunctionControlledLinksShapes the mapJunctionControlledLinksShapes to set
	 */
	public synchronized void setMapJunctionControlledLinksShapes(Map<String, List<RectF>> mapJunctionControlledLinksShapes) 
	{
		this.mapJunctionControlledLinksShapes = mapJunctionControlledLinksShapes;
		
		calculateMaxColumnWidth();		
	}

	/**
	 * @return the totalNumberOfJunctionPhases
	 */
	public synchronized int getTotalNumberOfJunctionPhases() {
		return totalNumberOfJunctionPhases;
	}

	/**
	 * @param totalNumberOfJunctionPhases the totalNumberOfJunctionPhases to set
	 */
	public synchronized void setTotalNumberOfJunctionPhases(
			int totalNumberOfJunctionPhases) {
		this.totalNumberOfJunctionPhases = totalNumberOfJunctionPhases;
	}

	/**
	 * @return the maxNumberOfPhasesPerJunction
	 */
	public synchronized int getMaxNumberOfPhasesPerJunction() {
		return maxNumberOfPhasesPerJunction;
	}

	/**
	 * @param maxNumberOfPhasesPerJunction the maxNumberOfPhasesPerJunction to set
	 */
	public synchronized void setMaxNumberOfPhasesPerJunction(
			int maxNumberOfPhasesPerJunction) {
		this.maxNumberOfPhasesPerJunction = maxNumberOfPhasesPerJunction;
	}

	private int calculateMaxColumnWidth() 
	{				
		Iterator<Entry<String, List<RectF>>> it = mapJunctionControlledLinksShapes.entrySet().iterator();
		
		while (it.hasNext()) 
		{
	        Entry<String, List<RectF>> pairs = (Entry<String, List<RectF>>)it.next();
	        List<RectF> listControlledLinks = pairs.getValue();
	        for(RectF linkShape: listControlledLinks)
	        {
	        	if((int) (linkShape.right) > maxWidthHeight)
	    			maxWidthHeight = (int) (linkShape.right);
	    		if((int) (linkShape.left) > maxWidthHeight)
	    			maxWidthHeight = (int) (linkShape.left);
	    		
	    		if((int) (linkShape.top ) > maxWidthHeight)
	    			maxWidthHeight = (int) (linkShape.top);
	    		if((int) (linkShape.bottom) > maxWidthHeight)
	    			maxWidthHeight = (int) (linkShape.bottom);
	        }
	    }
		
		return maxWidthHeight;
	}

	/**
	 * @return the maxWidthHeight
	 */
	public synchronized int getMaxWidthHeight() {
		return maxWidthHeight;
	}

	/**
	 * @param maxWidthHeight the maxWidthHeight to set
	 */
	public synchronized void setMaxWidthHeight(int maxWidthHeight) {
		this.maxWidthHeight = maxWidthHeight;
	}

	/**
	 * @return the dw
	 */
	public synchronized float getDw() {
		return dw;
	}

	/**
	 * @param dw the dw to set
	 */
	public synchronized void setDw(float dw) {
		this.dw = dw;
	}	
}
