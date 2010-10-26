#!/usr/bin/perl -w

# Info: Given an SVG path's data string e.g. "m 10,20 -2,2"... and a matching
#       string of coordinates e.g. "150.90,-34.40 150.92,-34.45"... reconstruct
#       the SVG path, and paste it together with the coordinates to make a GCPs
#       .points file.
#
#       The intended usage is to trace around some control points on a map in
#       inkscape and trace matching points on another map from say using
#       a libchamplain app to easily get a GCPs .points file.
#
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 26 Oct 2010
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

my $debug = 0;

die "Usage: $0 svg_path_string coordinates_string GCP.points\n" if (@ARGV < 3);

my $svg_path = $ARGV[0];
my $coordinates_string = $ARGV[1];

open GCP, ">".$ARGV[2] or die("Cannot write to ".$ARGV[2]."\n");
print GCP "mapX,mapY,pixelX,pixelY,enable\n";

$svg_path =~ s/^\D\ //; #get rid of any 'm ' at the start
$svg_path =~ s/\s+\D\s+/ /; #eg. if we have some letters in the string. they have some special meaning for the SVG path, but we don't care.
my @image_points = split / /, $svg_path; #put the image points into an array

my @geo_points = split / /, $coordinates_string; #put the geo points into an array

if (scalar @image_points != scalar @geo_points) {
    die "svg_path has ",scalar @image_points," points, but coordinates_string has ",scalar @geo_points," points.\n";
}

my $lastix = 0;
my $lastiy = 0;

for (my $i = 0; $i < scalar @image_points; $i++) {
    my ($ix, $iy) = split /,/, $image_points[$i];
    my ($gx, $gy) = split /,/, $geo_points[$i];
    $ix += $lastix;
    $iy += $lastiy;
    print $ix,",",$iy," maps to ",$gx,",",$gy,"\n" if ($debug);
    print GCP "$gx,$gy,$ix,$iy,1\n";

    $lastix = $ix;
    $lastiy = $iy;
}

close GCP;

print "Exported ",scalar @image_points," GCP points.\n";

