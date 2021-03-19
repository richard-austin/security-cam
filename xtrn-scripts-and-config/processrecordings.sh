ffmpeg -i $1 -c copy -level 3.0 -start_number 0 -hls_time 10 -hls_list_size 0 -f hls $2
# Processing done so remove the nginx generated recording file
rm $1
# Remove oldest recording files (older than 2 weeks)
find $3 -mmin +20160 -exec rm {} \;
