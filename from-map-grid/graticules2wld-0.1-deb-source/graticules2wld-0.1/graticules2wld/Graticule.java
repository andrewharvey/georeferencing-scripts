/*
 * Copyright (C) 2011 by Andrew Harvey <andrew.harvey4@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package graticules2wld;

import java.awt.geom.*;

/**
 * A graticule is a line of latitude or longitude.
 * This class stores information about a vector graticule on a raster image. 
 */
public class Graticule implements Comparable<Graticule> {
	public enum LATLON {LAT, LON};
	public enum DIR {N,S,E,W};
	
	public LATLON latlon; // is this graticule a north/south pointing line (LON), or east/west pointing line (LAT)
	public DIR dir; // is the value in the north, south, east or west direction?
	public int value; // in projected units, dist from axis in DIR direction, commonly called either the Easting or Northing
	public Line2D l; // the line segment representing this graticule in image/pixel space
	
	public Graticule(LATLON latlon, DIR dir, int value, Line2D l) {
		super();
		
		this.latlon = latlon;
		this.dir = dir;
		this.value = value;
		this.l = l;
	}
	
	public double getMeanX() {
		return (l.getX1() + l.getX2()) / 2;
	}
	
	public double getMeanY() {
		return (l.getY1() + l.getY2()) / 2;
	}
	
	/**
	 * @return The angle the graticule makes with the horizontal (if it is a LAT graticule), from the positive x axis
	 * with counter clockwise the positive direction, or the angle the vector makes with the vertical (if it is a LON
	 * graticule), from the positive y axis (upwards) with clockwise in the positive direction. 
	 */
	public double angle() {
		// convert the line segment to a vector
		Line2D vec = new Line2D.Double(0,0,l.getX2() - l.getX1(), l.getY2() - l.getY1());
		
		double angle;
		
		if (latlon.equals(LATLON.LAT)) {
			// find the angle with the horizontal, as an angle between pi/2 and -pi/2
			
			// push the vector into quadrant 1 and 4
			if (vec.getX2() < 0)
				vec.setLine(0, 0, -vec.getX2(), -vec.getY2());
				
			
			/*
			 *           |
			 *           |  /|
			 *           | / |
			 *           |/ a|           +
			 * ----------+---------- +x  A
			 *           |               -
			 *           |
			 *           |
			 *           |
			 *           +y
			 */
			angle = Math.atan2(-vec.getY2(), vec.getX2());
		}else {
			// find the angle with the vertical, as an angle between pi/2 and -pi/2
			
			// push the vector into quadrant 1 and 2
			if (vec.getY2() > 0)
				vec.setLine(0, 0, -vec.getX2(), -vec.getY2());
				
			
			/*         - A +
			 *           |__
			 *           |  /
			 *           |a/
			 *           |/
			 * ----------+---------- +x
			 *           |
			 *           |
			 *           |
			 *           |
			 *           +y
			 */
			angle = Math.atan2(vec.getX2(), -vec.getY2());
		}

		return angle;
	}
	
	/**
	 * @return Returns the real value of the graticule (ie. a negative value if south or west)
	 */
	public int realValue() {
		switch (dir) {
		case S:
		case W:
			return -value;
		}
		return value;
	}

	@Override
	public String toString() {
		return latlon + " " + dir + value + " " + l.getX1() + "," + l.getY1() + "," + l.getX2() + "," + l.getY2() + "\n";
	}

	@Override
	public int compareTo(Graticule o) {
		Graticule g = (Graticule) o;
		if (!latlon.equals(g.latlon))
			return 0;
			//throw new Exception("Cannot compare LAT with LON.");
		
		return Integer.valueOf(realValue()).compareTo(Integer.valueOf(g.realValue()));
	}
}
