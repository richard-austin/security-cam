#!/bin/bash

path=hls/
output_file=$path/"master.m3u8"
first=1


for file in `ls -rt $path/porch*.m3u8`; do 
   if [ $first -eq 1 ] 
   then
      head -n -1 $file > $output_file
      first=0
   else
      echo "#EXT-X-DISCONTINUITY" >> $output_file
      tail -n +`grep -n -m 1 "#EXTINF" $file | sed 's/\([0-9]*\).*/\1/'` $file | head -n -1 >> $output_file
   fi
done

echo "#EXT-X-ENDLIST" >> $output_file
