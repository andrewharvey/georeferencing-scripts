#!/usr/bin/perl -w

# Info: Given a raster image and a GCP file, write out a shell script to use
#       gdal to warp the raster.
#       The script we produce is one like Quantum GIS can generate.
# Author: Andrew Harvey (http://andrewharvey4.wordpress.com/)
# Date: 25 Oct 2010
#
# To the extent possible under law, the person who associated CC0
# with this work has waived all copyright and related or neighboring
# rights to this work.
# http://creativecommons.org/publicdomain/zero/1.0/

use strict;

die "Usage: $0 rasterimage GCP.points gdal_script.sh\n" if (@ARGV < 3);
die "Can't find ".$ARGV[0]."\n" if (!-e $ARGV[0]);
die "Can't find ".$ARGV[1]."\n" if (!-e $ARGV[1]);

my $rasterfile = $ARGV[0];
open GCP, $ARGV[1];
open SH, ">".$ARGV[2];

print SH "#!/bin/sh\n\n";

print SH "#gdal_translate. We tell it the source and target coordinate system.\n";
print SH "#VRT is a virtual raster format, which is a file containing metadata,\n",
         "#not the actual raster image data.\n",
         "#EPSG:4326 is WGS84. #-a_srs 'EPSG:4326'\n";
print SH "echo 'gdal_translate... to create VRT'\n";
print SH "gdal_translate -of VRT ";

<GCP>; #chew the header line

while (<GCP>) {
    my ($mapX,$mapY,$pixelX,$pixelY) = split /,/;
    print SH " -gcp $pixelX $pixelY $mapX $mapY";
}
close GCP;

print SH " \"$rasterfile\" \"$rasterfile.vrt\"\n";
print SH "\n";

#
print SH "#gdalwarp. actually warp the raster into a new raster\n",
         "#you will need to change the -order to a lower value if you have not\n",
         "#many GCPs.\n";
print SH "echo 'gdalwarp... to tiff'\n";
print SH "gdalwarp -of GTiff -co 'TFW=YES' -t_srs '+proj=utm +zone=56 +datum=WGS84' -r cubic -multi \"$rasterfile.vrt\" \"${rasterfile}_warped.tif\"\n";
print SH "\n";


#convert the GTiff to JPEG
print SH "#convert tif to jpg\n";
print SH "echo 'gdal_translate... tiff to jpg'\n";
print SH "gdal_translate -of JPEG -co 'WORLDFILE=YES' -co 'QUALITY=75' \"${rasterfile}_warped.tif\" \"${rasterfile}_warped.jpg\"\n";
print SH "\n";

print SH "#create the gdalinfo file so we can grab extents later (say for \n",
         "#creating a KML file) after we deleate the tif which has this extra\n",
         "#metadata embeded in it.\n";
print SH "echo 'gdalinfo...'\n";
print SH "gdalinfo \"${rasterfile}_warped.tif\" > ${rasterfile}.gdalinfo\n";
print SH "\n";

#deleate the GTiff
print SH "#delete the tiff file\n";
print SH "rm -f \"${rasterfile}_warped.tif\"\n";

close SH;

chmod 0755, $ARGV[2];
