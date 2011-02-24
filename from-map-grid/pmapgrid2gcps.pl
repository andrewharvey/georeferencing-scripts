#!/usr/bin/perl -w

# Info: Given a specially crafted SVG file, make a CGP file for GDAL.
#       The SVG file should consist of paths with two nodes per path. The path
#       id should be the label on the parish map. eg. "w220" or "s400".
#       In the case of zero, use either h0 or v0 for horizontal or vertical.
#
#       This script will find the intersection of these lines and output those
#       points of intersection as GCPs.
#
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 30 Oct 2010
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

# we use this to parse the SVG file
use XML::XPath;
use XML::XPath::XMLParser;

use Geo::Proj4;

use Data::Dumper;

my $debug = 0;

# switch to use source spatial reference system in the output .points file
my $use_s_srs = 1;

# finds the intersection point of two lines
## line 1 (L1), line 2 (L2)
## line 1 has two points (x1, y1) and (x2, y2)
## line 1 has two points (x3, y3) and (x4, y4)
sub intersection($$$$$$$$) {
    my ($x1, $y1, $x2, $y2, $x3, $y3, $x4, $y4) = @_;

    my ($px, $py);

    #I've cheated and used the formula at http://en.wikipedia.org/wiki/Line-line_intersection
    $px = (((($x1 * $y2) - ($y1 * $x2)) * ($x3 - $x4)) - (($x1 - $x2) * (($x3 * $y4) - ($y3 * $x4)))) /
          ((($x1 - $x2) * ($y3 - $y4)) - (($y1 - $y2) * ($x3 - $x4)));

    $py = (((($x1 * $y2) - ($y1 * $x2)) * ($y3 - $y4)) - (($y1 - $y2) * (($x3 * $y4) - ($y3 * $x4)))) /
          ((($x1 - $x2) * ($y3 - $y4)) - (($y1 - $y2) * ($x3 - $x4)));

    return ($px, $py);
}

# if the first argument is -h or --help
if ( (@ARGV < 4) || 
     ((@ARGV >= 1) && ($ARGV[0] eq "-h" || $ARGV[0] eq "--help")) ) {
    print "Usage: $0 long_trig lat_trig image.svg GCP.points\n";
    print "\n";
    print "   long_trig is the longitude of the reference trig station in WGS84 LL,\n";
    print "   lat_trig is the latitude. These are both in decimal degrees.\n";
    print "\n";
    print "   The image.svg file must consist of paths along the lines of longitude and\n";
    print "   latitude. They must have an id in the form w220 for West 200 units. If the\n";
    print "   line is passing through the reference trig station just use either n0 or e0.\n";
    exit;
}

# basic argument count check, and check svg file exists
die "Can't find ".$ARGV[2]."\n" if (!-e $ARGV[2]);

my $datumx = $ARGV[0];
my $datumy = $ARGV[1];
# assume source units to be chains.
# 1chain = 20.1168 meters
my $scale = 20.1168;

# convert the reference trig station from LL to projected coordinates
my $proj = Geo::Proj4->new(proj => "utm", south => undef, zone => 56, ellps => "WGS84" );
($datumx, $datumy) = $proj->forward($datumy, $datumx);

open GCP, ">".$ARGV[3] or die("Unable to open ".$ARGV[3]."\n");
print GCP "mapX,mapY,pixelX,pixelY,enable\n";

my $xp = XML::XPath->new(filename => $ARGV[2]);

my $nodeset = $xp->find('/svg/path'); # find all paths

my $gcpcount = 0;

my @vert_lines;
my @horiz_lines;

#for each SVG path...
foreach my $node ($nodeset->get_nodelist) {
    my $id = $node->findvalue('@id');
    my $d = $node->findvalue('@d');
    print "Path $id:\n" if ($debug);
    print " ", $d,"\n" if ($debug);

    my $direction;
    my $value;
    if ($id =~ /^([nsewvh])(\d+)$/i) {
        $direction = $1;
        $value = $2;
    }else{
        print STDERR "WARNING: found a path in svg with id $id.\n";
        next;
    }

    $d =~ s/^(\D)\ ?//; #get rid of any 'm ' at the start
    my $abs = ($1 =~ /[A-Z]/); #M means absolute, m means relative

    $d =~ s/\s+\D\s+/ /; #eg. if we have some letters in the string. they have some special meaning for the SVG path, but we don't care.
    my @points = split / /, $d; #put the points into an array

    my $num_expected_points = 2;
    if (@points != $num_expected_points) {
        print STDERR "WARNING: path '$id' expecting ", $num_expected_points, " points, but found ",scalar @points,".\n";
        print STDERR "   @points\n";
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
        print "new horizontal line,";
        push @horiz_lines, \@line;
    }elsif ($direction =~ /^[ewv]$/i) {
        print "new vertical line,";
        push @vert_lines, \@line;
    }
    print " ", $direction, " ", $value, "\n";
}

print "Found ", scalar @horiz_lines, " horizontal lines, and ", scalar @vert_lines, " vertical lines.\n";

foreach my $h (@horiz_lines) {
    foreach my $v (@vert_lines) {
        my ($horiz_direction, $horiz_value, $horiz_x1, $horiz_y1, $horiz_x2, $horiz_y2) = @$h;
        my ($vert_direction, $vert_value, $vert_x1, $vert_y1, $vert_x2, $vert_y2) = @$v;
        
        #find intersection of lines (not the segments, but the inf length lines)
        my ($ix, $iy) = intersection($horiz_x1, $horiz_y1, $horiz_x2, $horiz_y2,
                                       $vert_x1, $vert_y1, $vert_x2, $vert_y2,);

        #if the segments intersect...
        #if intersection x is within line 1 x range AND within y range
        my $inL1 = ((($ix < $horiz_x2) ^ ($ix < $horiz_x1)) &&
                   (($iy < $horiz_y2) ^ ($iy < $horiz_y1)));
        my $inL2 = ((($ix < $vert_x2) ^ ($ix < $vert_x1)) &&
                   (($iy < $vert_y2) ^ ($iy < $vert_y1)));

        if ($inL1 && $inL2) {
            print $horiz_direction, $horiz_value, ",", $vert_direction, $vert_value, " intersect at ", $ix, ",", $iy, "\n";
            my ($easting, $northing);
            
            if ($use_s_srs) {
                $easting = ($horiz_direction eq "s" ? -$horiz_value : $horiz_value);
                $northing = ($vert_direction eq "w" ? -$vert_value : $vert_value);
            }else{
                my $px = $datumx + ($horiz_value*$scale);
                my $py = $datumy + ($vert_value*$scale);
                # convert the projected coordinates back to LL
                ($northing, $easting) = $proj->inverse($px, $py);
            }
            print GCP $easting, ",", $northing, "," ,"$ix,$iy,1\n";
            $gcpcount++;
        }else{
            print $horiz_direction, $horiz_value, ",", $vert_direction, $vert_value, " don't intersect.\n";
        }
        
    }
}

close GCP;

print "Exported $gcpcount GCP points.\n";
