#!/usr/bin/perl -w

# Info: Will try to gather georeferencing info from gdalinfo output, to generate
#       a KML file.
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 26 Oct 2010
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

die "Usage: $0 overlay_name raster_href gdalinfo_file overlay.kml\n" if (@ARGV < 4);
die "Can't find ".$ARGV[2]."\n" if (!-e $ARGV[2]);


my $north = undef;
my $south = undef;
my $east = undef;
my $west = undef;

my $name = $ARGV[0];
my $href = $ARGV[1];
my $gdalinfo = $ARGV[2];
my $kmlfile = $ARGV[3];

open GDALINFO, "$gdalinfo";
open KMLFILE, ">$kmlfile";

while (<GDALINFO>) {
    if ($_ =~ /Upper Left\s*\(\s*([+-]?\d+\.?\d*),\s*([+-]?\d+\.?\d*)\)/) {
        print "Upper Left:",$1,",",$2,"\n";
        $west = $1;
        $north = $2;
    }
    if ($_ =~ /Lower Right\s*\(\s*([+-]?\d+\.?\d*),\s*([+-]?\d+\.?\d*)\)/) {
        print "Lower Right:",$1,",",$2,"\n";
        $east = $1;
        $south = $2;
    }
}

close GDALINFO;

if ((!defined $north) ||
    (!defined $south) ||
    (!defined $east) ||
    (!defined $west)) {
    die "Error reading gdalinfo_file, $gdalinfo.\n";
}

print KMLFILE <<EOF;
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns=" http://www.opengis.net/kml/2.2">
    <GroundOverlay>
        <name>$name</name>
        <Icon>
            <href>$href</href>
        </Icon>
        <LatLonBox>
            <north>$north</north>
            <south>$south</south>
            <east>$east</east>
            <west>$west</west>
            <rotation>0.0</rotation>
        </LatLonBox>
    </GroundOverlay>
</kml>
EOF

close KMLFILE;

