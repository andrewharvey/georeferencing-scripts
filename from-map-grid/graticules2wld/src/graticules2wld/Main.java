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

/* 
 * This program is designed to aid with georefencing raster images, by creating a world file.
 * 
 * The source csv file should have a format like,

lonlat,dir,value,x1,y1,x2,y2
lon,w,500,619.32204,221.18643,1348.0085,4434.7881
lon,v,0,3370.0212,202.13982,4112.2246,4406.5254
lon,e,100,3920.5297,195.99575,4246.1653,2055.8051
lat,h,0,125.33898,1128.0508,4235.4131,427.62711
lat,s,100,128.71822,1673.6441,4239.3748,970.1907
lat,s,700,3200.4449,4413.2839,4283.6441,4230.4979
lon,w,600,120.42373,493.36863,799.82466,4442.1075
lon,w,700,248.50609,4448.8415,138.15548,3790.2134

 * We assume that projected coordinates have units in meters.
 */

package graticules2wld;

import graticules2wld.Graticule.LATLON;
import graticules2wld.Graticule.DIR;

import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.cli.*;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class Main {
	
	static double unitsToMeters = 1; // use 1/20.1168 for chains
	static boolean debug = false;

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		/* parse the command line arguments */
		// create the command line parser
		CommandLineParser parser = new PosixParser();

		// create the Options
		Options options = new Options();
		options.addOption("x", "originx", true, "x component of projected coordinates of upper left pixel");
		options.addOption("y", "originy", true, "y component of projected coordinates of upper left pixel");
		options.addOption("u", "tometers", true, "multiplication factor to get source units into meters");
		options.addOption("h", "help", false, "prints this usage page");
		options.addOption("d", "debug", false, "prints debugging information to stdout");
		
        double originNorthing = 0;
        double originEasting = 0;
        
        String inputFileName = null;
        String outputFileName = null;

		try {
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );
		    
		    if (line.hasOption("help"))
		    	printUsage(0); // print usage then exit using a non error exit status
		    
		    if (line.hasOption("debug"))
		    	debug = true;
		    
		    // these arguments are required
		    if (!line.hasOption("originy") || !line.hasOption("originx"))
		    	printUsage(1);
		    
		    originNorthing = Double.parseDouble(line.getOptionValue("originy"));
		    originEasting = Double.parseDouble(line.getOptionValue("originx"));
		    
		    if (line.hasOption("tometers"))
		    	unitsToMeters = Double.parseDouble(line.getOptionValue("tometers"));
		    
		    // two args should be left. the input csv file name and the output wld file name.
			String[] iofiles = line.getArgs();
			if (iofiles.length < 2) {
				printUsage(1);
			}
			
			inputFileName = iofiles[0];
			outputFileName = iofiles[1];
		}
		catch( ParseException exp ) {
		    System.err.println( "Unexpected exception:" + exp.getMessage() );
		    System.exit(1);
		}
		
		// try to open the input file for reading and the output file for writing
		File graticulesCsvFile;
		BufferedReader csvReader = null;
		
		File wldFile;
        BufferedWriter wldWriter = null;
        
		try {
			graticulesCsvFile = new File(inputFileName);
			csvReader = new BufferedReader(new FileReader(graticulesCsvFile));
		}catch( IOException exp ) {
			System.err.println("Could not open input file for reading: " + inputFileName);
			System.exit(1);
		}
		
		try {
			wldFile = new File(outputFileName);
        	wldWriter = new BufferedWriter(new FileWriter(wldFile));
		}catch( IOException exp ) {
			System.err.println("Could not open output file for writing: " + outputFileName);
			System.exit(1);
		}
		
        // list of lon graticules and lat graticules
        ArrayList<Graticule> lonGrats = new ArrayList<Graticule>();
        ArrayList<Graticule> latGrats = new ArrayList<Graticule>();
        
        // read the source CSV and convert its information into the two ArrayList<Graticule> data structures
        readCSV(csvReader, lonGrats, latGrats);

        // we now need to start finding the world file paramaters
        DescriptiveStatistics stats = new DescriptiveStatistics();

        // find theta and phi
        for (Graticule g : latGrats) {
        	stats.addValue(g.angle());
        }

        double theta = stats.getMean(); // we use the mean of the lat angles as theta
        if (debug)
        	System.out.println("theta range = " +  Math.toDegrees(stats.getMax() - stats.getMin()));
        stats.clear();

        for (Graticule g : lonGrats) {
        	stats.addValue(g.angle());
        }

        double phi = stats.getMean(); // ... and the mean of the lon angles for phi
        if (debug)
        	System.out.println("phi range = " +  Math.toDegrees(stats.getMax() - stats.getMin()));
        stats.clear();
        
        // print these if in debug mode
        if (debug) {
        	System.out.println("theta = " + Math.toDegrees(theta) + "deg");
        	System.out.println("phi = " + Math.toDegrees(phi) + "deg");
        }
        
        
        // find x and y (distance beteen pixels in map units)
        Collections.sort(latGrats);
        Collections.sort(lonGrats);
        int prevMapValue = 0; //fixme: how to stop warning about not being initilised?
        Line2D prevGratPixelSys = new Line2D.Double();
        
        boolean first = true;
        for (Graticule g : latGrats) {
        	if (!first) {
        		int deltaMapValue = Math.abs(g.realValue() - prevMapValue);
        		double deltaPixelValue = (g.l.ptLineDist(prevGratPixelSys.getP1()) + (g.l.ptLineDist(prevGratPixelSys.getP2()))) / 2;
        		
        		double delta = deltaMapValue / deltaPixelValue;
        		stats.addValue(delta);
        	}else{
        		first = false;
        		prevMapValue = g.realValue();
        		prevGratPixelSys = (Line2D) g.l.clone();
        	}
        }
        
        double y = stats.getMean();
        if (debug)
        	System.out.println("y range = " + (stats.getMax() - stats.getMin()));
        stats.clear();
        
        first = true;
        for (Graticule g : lonGrats) {
        	if (!first) {
        		int deltaMapValue = g.realValue() - prevMapValue;
        		double deltaPixelValue = (g.l.ptLineDist(prevGratPixelSys.getP1()) + (g.l.ptLineDist(prevGratPixelSys.getP2()))) / 2;
        		
        		double delta = deltaMapValue / deltaPixelValue;
        		stats.addValue(delta);
        	}else{
        		first = false;
        		prevMapValue = g.realValue();
        		prevGratPixelSys = (Line2D) g.l.clone();
        	}
        }
        
        double x = stats.getMean();
        if (debug)
        	System.out.println("x range = " + (stats.getMax() - stats.getMin()));
        stats.clear();
        
        if (debug) {
        	System.out.println("x = " + x);
        	System.out.println("y = " + y);
        }
        
        // C, F are translation terms: x, y map coordinates of the center of the upper-left pixel
        for (Graticule g : latGrats) {
            // find perp dist to pixel space 0,0
        	Double perpPixelDist = g.l.ptLineDist(new Point2D.Double(0,0));
        	
        	// find the map space distance from this graticule to the center of the 0,0 pixel
        	Double perpMapDist = perpPixelDist * y; // perpMapDist / perpPixelDist = y
        	
        	stats.addValue(perpMapDist);
        }
        
        double F = stats.getMean();
        F += originNorthing;
        stats.clear();
        
        for (Graticule g : lonGrats) {
        	// find perp dist to pixel space 0,0
        	Double perpPixelDist = g.l.ptLineDist(new Point2D.Double(0,0));
        	
        	// find the map space distance from this graticule to the center of the 0,0 pixel
        	Double perpMapDist = perpPixelDist * x; // perpMapDist / perpPixelDist = x
        	
        	stats.addValue(perpMapDist);
        }
        
        double C = stats.getMean();
        C += originEasting;
        
        stats.clear();
        
        
        // calculate the affine transformation matrix elements
        double D = -1 * x * unitsToMeters * Math.sin(theta);
        double A = x * unitsToMeters * Math.cos(theta);
        double B = y * unitsToMeters * Math.sin(phi); // if should be negative, it'll formed by negative sin
        double E = -1 * y * unitsToMeters * Math.cos(phi);

        /*
         * Line 1: A: pixel size in the x-direction in map units/pixel
         * Line 2: D: rotation about y-axis
         * Line 3: B: rotation about x-axis
         * Line 4: E: pixel size in the y-direction in map units, almost always negative[3]
         * Line 5: C: x-coordinate of the center of the upper left pixel
         * Line 6: F: y-coordinate of the center of the upper left pixel
         */
        if (debug) {
	        System.out.println("A = " + A);
	        System.out.println("D = " + D);
	        System.out.println("B = " + B);
	        System.out.println("E = " + E);
	        System.out.println("C = " + C);
	        System.out.println("F = " + F);
	        
	        // write the world file
	        System.out.println();
	        System.out.println("World File:");
	        System.out.println(A);
	        System.out.println(D);
	        System.out.println(B);
	        System.out.println(E);
	        System.out.println(C);
	        System.out.println(F);
        }
        
        // write to the .wld file
        wldWriter.write(A + "\n");
        wldWriter.write(D + "\n");
        wldWriter.write(B + "\n");
        wldWriter.write(E + "\n");
        wldWriter.write(C + "\n");
        wldWriter.write(F + "\n");
        
        wldWriter.close();
	}

	/**
	 * Abstracts the source CSV file format, and exposes the data as two ArrayList<Graticule> for the rest of the program.
	 * @param csvReader
	 * @param lonGrats
	 * @param latGrats
	 * @throws Exception
	 */
	private static void readCSV(BufferedReader csvReader,
			ArrayList<Graticule> lonGrats, ArrayList<Graticule> latGrats) throws Exception {

        // chew the header line and check it is what we expect
        String line = csvReader.readLine();

        if (!line.equals("lonlat,dir,value,x1,y1,x2,y2"))
        	System.exit(1);

        // read each line of the CSV file and build our internal data structures
        for (line = csvReader.readLine(); line != null; line = csvReader.readLine()) {
        	String[] l = line.split(",");
        	if (l.length != 7)
        		throw new Exception("Source file has bad format. Each line should have 7 columns, but we found a line with " + l.length + "columns.");

        	LATLON latlon;
        	if (l[0].equals("lon"))
        		latlon = LATLON.LON;
        	else if (l[0].equals("lat"))
        		latlon = LATLON.LAT;
        	else
        		throw new Exception("Either 'lat' or 'lon' expected, found " + l[0]);

        	DIR dir;
        	if (l[1].equals("w"))
        		dir = DIR.W;
        	else if (l[1].equals("e"))
        		dir = DIR.E;
        	else if (l[1].equals("n"))
        		dir = DIR.N;
        	else if (l[1].equals("s"))
        		dir = DIR.S;
        	else if (l[1].equals("v"))
        		dir = DIR.E;
        	else if (l[1].equals("h"))
        		dir = DIR.N;
        	else
        		throw new Exception("Either n,s,e,w,h,v expected, found " + l[1]);

        	Graticule graticule = new Graticule(latlon,
        			dir,
        			Integer.parseInt(l[2]),
        			new Line2D.Float(Float.parseFloat(l[3]), Float.parseFloat(l[4]), Float.parseFloat(l[5]), Float.parseFloat(l[6])));

        	if (latlon.equals(LATLON.LAT))
        		latGrats.add(graticule);
        	else
        		lonGrats.add(graticule);
        }
		
	}

	private static void printUsage(int status) {
		//                  <----                               80 chars                               ---->		
		System.out.println("graticules2wld [options] input.csv output.wld");
		System.out.println();
		System.out.println("    Options:");
		System.out.println("    -h, --help        prints this message");
		System.out.println("    -x, --originx     x component of projected coordinates of upper left pixel");
		System.out.println("    -y, --originy     y component of projected coordinates of upper left pixel");
		System.out.println("    -u, --tometers    multiplication factor to get source units into meters");
		
		System.exit(status);
	}

}
