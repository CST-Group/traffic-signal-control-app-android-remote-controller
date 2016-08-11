/**
 * 
 */
package br.unicamp.cogsys.trafficunjammer.customclasses;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * @author andre
 *
 */
public class PhaseView extends View 
{
	private List<PhaseViewItem> phaseViewItems = new ArrayList<PhaseViewItem>();
	
	private Paint phasePaint;
	
	private int maxWidthHeight = 0;		
	
	private float dw;

	public PhaseView(Context context) 	
	{
		super(context);
		init();
	}

	public PhaseView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		init();
	}
	
	private void init() 
	{
		phasePaint = new Paint(Paint.ANTI_ALIAS_FLAG);	
		phasePaint.setStrokeWidth(5.0f*dw);
	}


	public void addPhaseViewItem(RectF linkShape, int pathColor) 
	{
		PhaseViewItem phaseViewItem = new PhaseViewItem();
		
		phaseViewItem.setLinkShape(linkShape);
		phaseViewItem.setPathColor(pathColor);
		
		phaseViewItems.add(phaseViewItem);
	
		onDataChanged();
		
	}

	private void onDataChanged() 
	{
		postInvalidate();
		
	}

	private class PhaseViewItem
	{
		private RectF linkShape;
		
		private int pathColor;

		/**
		 * @return the linkShape
		 */
		public synchronized RectF getLinkShape() 
		{
			return linkShape;
		}

		/**
		 * @param linkShape the linkShape to set
		 */
		public synchronized void setLinkShape(RectF linkShape) 
		{
			this.linkShape = linkShape;
		}

		/**
		 * @return the pathColor
		 */
		public synchronized int getPathColor() 
		{
			return pathColor;
		}

		/**
		 * @param pathColor the pathColor to set
		 */
		public synchronized void setPathColor(int pathColor) 
		{
			this.pathColor = pathColor;
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onDraw(android.graphics.Canvas)
	 */
	@Override
	protected void onDraw(Canvas canvas) 
	{		
		super.onDraw(canvas);
				
		int widthheight = (int)(maxWidthHeight*dw) + (int) (maxWidthHeight*0.01f*dw);
		
		for(PhaseViewItem phaseViewItem : phaseViewItems)
		{
			phasePaint.setColor(phaseViewItem.getPathColor());
			canvas.drawLine(phaseViewItem.getLinkShape().left*dw, widthheight-phaseViewItem.getLinkShape().bottom*dw, phaseViewItem.getLinkShape().right*dw, widthheight-phaseViewItem.getLinkShape().top*dw, phasePaint);
		}
	}

	/* (non-Javadoc)
	 * @see android.view.View#onMeasure(int, int)
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{
		int widthheight = (int)(maxWidthHeight*dw) + (int) (maxWidthHeight*0.01f*dw);
		
        setMeasuredDimension(widthheight, widthheight);
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
