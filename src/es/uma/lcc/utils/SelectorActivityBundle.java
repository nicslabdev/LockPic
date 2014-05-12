package es.uma.lcc.utils;

import java.util.ArrayList;

import es.uma.lcc.lockpic.SelectorActivity.ViewMode;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Auxiliary class for conveniently saving state information in SelectorActivity
 * 
 * Copyright (C) 2014  Carlos Parés: carlosparespulido (at) gmail (dot) com
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
