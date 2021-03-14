ffmpeg -i $1 -c copy -level 3.0 -start_number 0 -hls_time 10 -hls_list_size 0 -f hls $2
# Processing done so remove the nginx generated recording file
rm $1
# Remove oldest recording files
find $3 -mmin +4320 | xargs rm

# Remove the oldest motion event files
find /home/www-data/motion-log -mmin +4320 | xargs rm

#echo $1 $2 $3 $4 > $3/didit.txt

# Create master m3u8 file
# filepath=$3
# basename=$4

# output_file=$filepath/"master.m3u8"
# first=1


# for file in `ls -rt $filepath/${basename%-*}*.m3u8`; do 
#    if [ $first -eq 1 ] 
#    then
#       head -n -1 $file > $output_file
#       first=0
#    else
#       echo "#EXT-X-DISCONTINUITY" >> $output_file
#       tail -n +`grep -n -m 1 "#EXTINF" $file | sed 's/\([0-9]*\).*/\1/'` $file | head -n -1 >> $output_file
#    fi
# done

# echo "#EXT-X-ENDLIST" >> $output_file
