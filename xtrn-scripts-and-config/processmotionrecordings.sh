#!/bin/bash

export path=$1
export dir=$(dirname $path)
export basename=$(basename ${path%.*})

ffmpeg -i $1 -c copy -level 3.0 -start_number 0 -hls_time 10 -hls_list_size 0 -f hls $dir/${basename}_.m3u8

# Processing done so remove the recording file generated by motion
rm $1
# Remove oldest recording files
find dir -mmin +60 -exec rm {} \;
