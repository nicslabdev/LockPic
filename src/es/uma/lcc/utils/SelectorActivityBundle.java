package es.uma.lcc.utils;

import java.util.ArrayList;

import es.uma.lcc.lockpic.SelectorActivity.ViewMode;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Auxiliary class for conveniently saving state information in SelectorActivity
 */

public class SelectorActivityBundle {
	ArrayList<Rectangle> rects;
	ViewMode mode;
	float aspectRate;
	
	public SelectorActivityBundle(ArrayList<Rectangle> rects, ViewMode mode, float aspectRate)  {
		this.rects = rects;
		this.mode = mode;
		this.aspectRate = aspectRate;
	}
	
	public ArrayList<Rectangle> getRectangles()  { return rects; }
	public ViewMode getViewMode() { return mode; }
	public float getAspectRate() { return aspectRate; }
}
