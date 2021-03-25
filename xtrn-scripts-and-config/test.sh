#!/bin/bash

# Script to continuously start and stop recordings to test for no returned path
# (and hence failure) from the curl call. This problem, seen several times has been
# (hopefully) addressed by setting the worker process count to one, as mentioned
# here https://github.com/arut/nginx-rtmp-module/wiki/Control-Module

# set -x

start ()
{
	
	result=`curl "http://localhost:8083/control/record/start?app=live3&name=cam3&rec=live3_rec"`
	echo "curl \"http://localhost:8083/control/record/start?app=live3&name=cam3&rec=live3_rec\" >> ${result}" 
}

stop ()
{
	result=`curl "http://localhost:8083/control/record/stop?app=live3&name=cam3&rec=live3_rec"`
	echo "curl \"http://localhost:8083/control/record/stop?app=live3&name=cam3&rec=live3_rec\" >> ${result}" 
}

while :
do
	#while :
	#do
	#	sleep 2
		start
	#	[[ $result == "" ]] || break
	#	sleep 1
	#	stop
	#	sleep 1
	#done

	sleep 6 
	
	#while :
	#do
		stop
	#	[[ $result == "" ]] || break
		sleep 1 
	#done
done



