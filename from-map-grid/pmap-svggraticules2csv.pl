#!/usr/bin/perl -w

# Info: Given a specially crafted SVG file, make an intermediate csv file which
#       can be feed to graticules2wld.
#
#       The SVG file should consist of paths with two nodes per path. The path
#       id should be the label on the parish map. eg. "w220" or "s400".
#       In the case of zero, just use either (n or s) or (e or w).
#
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 07 Feb 2011
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

# we use this to parse the SVG file
use XML::XPath;
use XML::XPath::XMLParser;

# if the first argument is -h or --help, then print a help message
if ( (@ARGV < 2) || 
     ((@ARGV >= 1) && ($ARGV[0] eq "-h" || $ARGV[0] eq "--help")) ) {
    print "Usage: $0 image.svg graticules.csv\n";
    print "\n";
    print "   The image.svg file must consist of paths along the lines of longitude and\n";
    print "   latitude. They must have an id in the form w220 for West 200 units. If the\n";
    print "   line is passing through the reference trig station just use either n0 or e0.\n";
    exit;
}

# check svg file exists
die "Can't find ".$ARGV[0]."\n" if (!-e $ARGV[0]);

open CSV, ">".$ARGV[1] or die("Unable to open ".$ARGV[1]."\n");

my $xp = XML::XPath->new(filename => $ARGV[0]);

my $nodeset = $xp->find('/svg/path'); # find all paths

my ($lat_count, $lon_count);

print CSV "lonlat,dir,value,x1,y1,x2,y2\n";

# parse the svg file to extract our graticules
#for each SVG path...
foreach my $node ($nodeset->get_nodelist) {
    my $id = $node->findvalue('@id');
    my $d = $node->findvalue('@d');

    my $direction;
    my $value;
    if ($id =~ /([nsewvh])(\d+)/i) {
        $direction = $1;
        $value = $2;
    }else{
        print STDERR "ERR: did not match $id.\n";
        next;
    }

    $d =~ s/^(\D)\ ?//; #get rid of any 'm ' at the start
    my $abs = ($1 =~ /[A-Z]/); #M means absolute, m means relative

    $d =~ s/\s+\D\s+/ /; #eg. if we have some letters in the string. they have some special meaning for the SVG path, but we don't care.
    my @points = split / /, $d; #put the points into an array

    my $num_expected_points = 2;
    if (@points != $num_expected_points) {
        print STDERR "'$id': expecting ", $num_expected_points, " points, but found ",scalar @points,".\n";
        print STDERR " @points\n";
        next;
    }

    my ($x1, $y1) = split /,/, $points[0];
    my ($x2, $y2) = split /,/, $points[1];
    if (!$abs) {
        $x2 += $x1;
        $y2 += $y1;
    }
    
    my @line = ($direction, $value, $x1, $y1, $x2, $y2);
    if ($direction =~ /^[nsh]$/i) {
        print CSV "lat,";
        $lat_count++;
    }elsif ($direction =~ /^[ewv]$/i) {
        print CSV "lon,";
        $lon_count++;
    }
    print CSV "$direction,$value,$x1,$y1,$x2,$y2\n";
}

print "Found ", $lat_count, " horizontal lines, and ", $lon_count, " vertical lines.\n";

close CSV;

