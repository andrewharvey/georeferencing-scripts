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

import java.awt.geom.Line2D;

import graticules2wld.Graticule.*;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GraticuleTest {
	
	/* Remember that image pixel space coordinates are,
	 * 
	 * 0,0 ---> +x
	 *  |
	 *  |
	 *  \/
	 *  +y
	 */
	
	/*
	 *  B
	 *  |
	 *  A
	 */
	Graticule vert_origin = new Graticule(LATLON.LON, DIR.E, 0, new Line2D.Float(0,1,0,0));
	
	/*
	 *  A
	 *  |
	 *  B
	 */
	Graticule vert_origin_rev = new Graticule(LATLON.LON, DIR.E, 0, new Line2D.Float(0,0,0,1));
	
	/*
	 *  A-B
	 */
	Graticule horiz_origin = new Graticule(LATLON.LAT, DIR.N, 0, new Line2D.Float(0,0,1,0));
	
	/*
	 *  B-A
	 */
	Graticule horiz_origin_rev = new Graticule(LATLON.LAT, DIR.N, 0, new Line2D.Float(1,0,0,0));
	
	/*
	 *   B
	 *  /
	 * A
	 */
	Graticule angle_45deg = new Graticule(LATLON.LON, DIR.E, 100, new Line2D.Float(0,1,1,0));
	
	/*
	 *   A
	 *  /
	 * B
	 */
	Graticule angle_45deg_rev = new Graticule(LATLON.LON, DIR.W, 100, new Line2D.Float(1,0,0,1));
	
	
	/*
	 * A
	 *  \
	 *   B
	 */
	Graticule angle_neg45deg = new Graticule(LATLON.LAT, DIR.N, 100, new Line2D.Float(0,0,1,1));
	
	/*
	 * B
	 *  \
	 *   A
	 */
	Graticule angle_neg45deg_rev = new Graticule(LATLON.LAT, DIR.S, 100, new Line2D.Float(1,1,0,0));
	
	@Before
	public void setUp() throws Exception {
	
	}
	
	/**
	 * Testing angle function
	 */
	@Test
	public void test_angle() {
		assertTrue(vert_origin.angle() == 0);
		assertTrue(vert_origin_rev.angle() == 0);
		assertTrue(horiz_origin.angle() == 0);
		assertTrue(horiz_origin_rev.angle() == 0);
		assertTrue(angle_45deg.angle() == Math.PI/4);
		assertTrue(angle_45deg_rev.angle() == Math.PI/4);
		assertTrue(angle_neg45deg.angle() == -Math.PI/4);
		assertTrue(angle_neg45deg_rev.angle() == -Math.PI/4);
	}
	
	/**
	 * Testing realValue function
	 */
	@Test
	public void test_realValue() {
		assertTrue(vert_origin.realValue() == 0);
		assertTrue(vert_origin_rev.realValue() == 0);
		assertTrue(horiz_origin.realValue() == 0);
		assertTrue(horiz_origin_rev.realValue() == 0);
		assertTrue(angle_45deg.realValue() == 100);
		assertTrue(angle_45deg_rev.realValue() == -100);
		assertTrue(angle_neg45deg.realValue() == 100);
		assertTrue(angle_neg45deg_rev.realValue() == -100);
	}
	
}
