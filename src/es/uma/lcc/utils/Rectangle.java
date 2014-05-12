package es.uma.lcc.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author - Carlos Parés Pulido, Oct 2013
 * 
 * Auxiliary class to represent a rectangle, its coordinates, and if necessary,
 * the list of emails to whom permissions will be granted.
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

public class Rectangle {
	    public int x0, y0, xEnd, yEnd;
	    public ArrayList<String> permissions;
	    
	    public Rectangle() {
	    	x0 = 0;
	    	y0 = 0;
	    	xEnd = 0;
	    	yEnd = 0;
	    	permissions = new ArrayList<String>();
	    }
	    
	    public Rectangle(int xStart, int yStart, int xEnd, int yEnd)
	    {
	    	x0 = xStart;
	    	y0 = yStart;
	    	this.xEnd = xEnd;
	    	this.yEnd = yEnd;
	    	permissions = new ArrayList<String>();
	    }

	    
	    public Rectangle(String s)
	    {
	    	StringTokenizer st = new StringTokenizer(s, ";");
	    	if(st.countTokens() == 5)  {
	    		x0 = Integer.parseInt(st.nextToken());
	    		xEnd = Integer.parseInt(st.nextToken());
	    		y0 = Integer.parseInt(st.nextToken());
	    		yEnd = Integer.parseInt(st.nextToken());
	    		permissions = buildPermissionsFromString(st.nextToken());
	    	}
	    }
	    
	    public Rectangle(String s, int sampleSize)
	    {
	    	StringTokenizer st = new StringTokenizer(s, ";");
	    	if(st.countTokens() == 5)  {
	    		x0 = Integer.parseInt(st.nextToken())/sampleSize;
	    		xEnd = Integer.parseInt(st.nextToken())/sampleSize;
	    		y0 = Integer.parseInt(st.nextToken())/sampleSize;
	    		yEnd = Integer.parseInt(st.nextToken())/sampleSize;
	    		permissions = buildPermissionsFromString(st.nextToken());
	    	}
	    }
	    
	    public Rectangle(String s, float aspectRatio)
	    {
	    	StringTokenizer st = new StringTokenizer(s, ";");
	    	if(st.countTokens() == 5)  {
	    		x0 = (int)(Integer.parseInt(st.nextToken())/aspectRatio);
	    		xEnd = (int)(Integer.parseInt(st.nextToken())/aspectRatio);
	    		y0 = (int)(Integer.parseInt(st.nextToken())/aspectRatio);
	    		yEnd = (int)(Integer.parseInt(st.nextToken())/aspectRatio);
	    		permissions = buildPermissionsFromString(st.nextToken());
	    	}
	    }
	    
	    
	    /** 
	     * Fills the permissions ArrayList in this class from a list of permissions,
	     * held in a String in the format "[user_id_1, user_id_2, ..., user_id_n]"
	     */
	    
	    public static ArrayList<String> buildPermissionsFromString(String strPermissions) {
			ArrayList<String> result = new ArrayList<String>();
			StringTokenizer st = new StringTokenizer(strPermissions, "[] ,");
			while(st.hasMoreTokens())
				result.add(st.nextToken());
			return result;
		}

		@Override
	    public String toString() {
	    	return x0 + ";" + xEnd + ";" 
	    			+ y0 + ";" + yEnd + ";" + getPermissions(); }
	    
	    public String toString(int sampleSize) {
	    	return sampleSize*x0 + ";" + sampleSize*xEnd + ";" 
	    			+ sampleSize*y0 + ";" + sampleSize*yEnd + ";" + getPermissions(); }
	    
	    public String toString(float aspectRatio)
	    {
	    	return (int)(aspectRatio*x0) + ";" + (int)(aspectRatio*xEnd) + ";"
	    		+ (int)(aspectRatio*y0) + ";" + (int)(aspectRatio*yEnd) + ";" + getPermissions();
	    }
	    
	    public boolean isInRectangle(int x, int y)
	    {
	    	return (x >= x0) && (x <= xEnd) && (y >= y0) && (y <= yEnd);
	    }
	    
	    public void setCoord(int xStart, int yStart, int xEnd, int yEnd)
	    {
	    	x0 = xStart;
	    	y0 = yStart;
	    	this.xEnd = xEnd;
	    	this.yEnd = yEnd;
	    }
	    
	    public int getWidth()
	    {
	    	return xEnd - x0;
	    }
	    
	    public int getHeight()
	    {
	    	return yEnd - y0;
	    }

		public void resizeBy(float f) {
			x0 = (int)(x0*f);
			xEnd = (int)(xEnd*f);
			y0 = (int)(y0*f);
			yEnd = (int)(yEnd*f);
		}
		
		public void addPermission(String username)  {
			permissions.add(username);
		}
		
		public void setPermissions(ArrayList<String> permissions)  {
			this.permissions = permissions;
		}
		
		public String getPermissions()  {
			return permissions.toString();
		}
		
		public ArrayList<String> getPermissionsArrayList()  {
			return permissions;
		}
		
		public boolean hasPermissions() { return !permissions.isEmpty(); }
		
		public boolean hasIntersection(Rectangle r)  {
			return (x0 < r.xEnd &&
					r.x0 < xEnd &&
					y0 < r.yEnd &&
					r.y0 < yEnd  );
		}
		
		/** 
		 * Checks if this rectangle has an intersection with any of the
		 * rectangles in the list. Returns -1 if it does not, and the index
		 * of the first intersecting rectangle if it does.
		 */
		public int hasIntersectionList(List<Rectangle> rectangles)  {
			int ret = -1;
			int n = 0;
			while(ret == -1 && n < rectangles.size())  {
				ret = (hasIntersection(rectangles.get(n)) ? n : -1);
				n++;
			}
			return ret;
		}
		
		/**
		 * Same as hasIntersectionList, but skips the given index.
		 * This can be used to check if a rectangle already in a list
		 * intersects with any *other* (hasIntersectionList would always 
		 * return true)
		 */
		public int hasIntersectionListExcept(List<Rectangle> rectangles, int notCheckedIndex)  {
			int ret = -1;
			int n = 0;
			while(ret == -1 && n < rectangles.size())  {
				if(n != notCheckedIndex)
					ret = (hasIntersection(rectangles.get(n)) ? n : -1);
				n++;
			}
			return ret;
		}
		
		public int area()  {
			return (xEnd-x0)*(yEnd-y0);
		}
	
}
