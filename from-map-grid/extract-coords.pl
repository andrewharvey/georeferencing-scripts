#!/usr/bin/perl -w

# Info: Given a specially crafted SVG file, make a CGP file for GDAL.
#       This script is NOT robust. I craft the SVG file as follows.
#           I opened the original JPG in Inkscape, and using tool to draw 
#           lines I,
#               for each line of latitude (horizontal) going from left to right,
#                   add a node in the line at the intersection with a line of
#                   longitude (vertical). That path is then named with the id
#                   lat${lat}_lon${startLon-endLon}. The -endLon is optional if
#                   you just want to count the first point in the path.
#
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 25 Oct 2010
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

use XML::XPath;
use XML::XPath::XMLParser;

my $debug = 0;

die "Usage: $0 image.svg grid_interval GCP.points\n" if (@ARGV < 2);
die "Can't find ".$ARGV[0]."\n" if (!-e $ARGV[0]);

my $interval = $ARGV[1];
die "Interval must be a positive integer.\n" if ($interval !~ /^\d+$/);

open GCP, ">".$ARGV[2];
print GCP "mapX,mapY,pixelX,pixelY,enable\n";

my $xp = XML::XPath->new(filename => $ARGV[0]);

my $nodeset = $xp->find('/svg/path'); # find all paths

my $gcpcount = 0;

foreach my $node ($nodeset->get_nodelist) {
    my $id = $node->findvalue('@id');
    my $d = $node->findvalue('@d');
    print "Path $id:\n" if ($debug);
    print " ", $d,"\n" if ($debug);

    my $lat;
    my $lon1;
    my $lon2;

    #examples of id format,
    #lat10_lon20-30    //two points 10,20 and 10,30
    #lat10_lon20       //just 1st point 10,20
    #lat-10_lon-20     //just 1st point at -10,-20
    #lat-10_lon-20--10 //two points -10,-20 and -10,-10

    if ($id =~ /lat(-?\d*)_lon(-?\d*)(-(-?\d*))?/) {
        $lat = $1;
        $lon1 = $2;
        $lon2 = $4;

        $lon2 = '' unless (defined $lon2);
        print "'$id' lat $lat, lon $lon1-$lon2\n" if ($debug);
    }else{
        print STDERR "ERR: did not match $id.\n";
        next;
    }

    $d =~ s/^\D\ //; #get rid of the 'm ' at the start
    $d =~ s/\s+\D\s+/ /; #eg. if we have some letters in the string. they have some special meaning for the SVG path, but we don't care.
    my @points = split / /, $d; #put the points into an array

    my $num_expected_points = ($lon2 ne '') ? (($lon2 - $lon1) / $interval) + 1 : 1;
    if (($lon2 ne '') && (@points != $num_expected_points)) {
        print STDERR "'$id': expecting ", $num_expected_points, " points, but found ",scalar @points,".\n";
        print STDERR " @points\n";
        next;
    }

    if ($lon2 ne '') {
        my $lastx = 0;
        my $lasty = 0;
        for (my $i = 0; $i < ((($lon2 - $lon1) / $interval) + 1); $i++) {
            my ($x, $y) = split /,/, $points[$i];
            #seems that the coor sys is
            #  +---> x
            #  |
            #  |
            #  v
            #   y
            #for the first point, and then the same coor system, but relative to
            #the last point for subsequent points

            $x += $lastx;
            $y += $lasty;

            print $x,",",$y," maps to ",$lat,", ",$lon1+($i*$interval),"\n" if ($debug);
            print GCP $lon1+($i*$interval),",$lat,$x,$y,1\n";
            $gcpcount++;

            $lastx = $x;
            $lasty = $y;
        }
    }else{
        if (@points == 0) {
            print STDERR "expected at least one point, but found none\n";
            next;
        }
        my ($x, $y) = split /,/, $points[0];
        print $x,",",$y," maps to ",$lat,", ",$lon1,"\n" if ($debug);
        print GCP "$lon1,$lat,$x,$y,1\n";
        $gcpcount++;
    }


    print "\n" if ($debug);
}

close GCP;

print "Exported $gcpcount GCP points.\n";
