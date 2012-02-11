if [ -z "$1" ]
then
    echo "Please specify the folder. Usage: $0 [folder]";
    exit -1;
fi

folder="$1"

cd $folder

# counting number of message generated  per sec (1000ms)
echo "Extracting generation msg rate"
for x in `ls delays_runtopology*0`; 
do 
    echo $x; 
    awk '{ts=int(($2)/1000);ts_arr[ts]++;} END{i=asorti(ts_arr, sorted_ts_arr); for(j=1; j<=i; j++){ts=sorted_ts_arr[j]; printf("%5d %15d %10d\n", j, ts, ts_arr[ts]);} }' $x > $x.gener.rate; 
done

# counting number of message delivered  per sec (1000ms)
echo "Extracting delivery msg rate"
for x in `ls delays_runtopology*0`; 
do 
    echo $x; 
    awk '{ts=int(($3)/1000); ts_arr[ts]++;} END{i=asorti(ts_arr, sorted_ts_arr);for(j=1; j<=i; j++) {ts=sorted_ts_arr[j]; printf("%5d %15d %10d\n", j, ts, ts_arr[ts]);} }' $x >$x.deliv.rate; 
done 

# extracting average delay per sec (1000ms) (based on delivery time)
echo "Extracting average delays"
for x in `ls delays_runtopology*0`; 
do 
    echo $x; 
    awk '{ts=int(($3)/1000); ts_count[ts]++; ts_sum[ts]+=$4;} END{i=asorti(ts_count, sorted_indices); for(j=1; j<=i; j++) {ts=sorted_indices[j]; printf("%5d %15d %10.3f\n", j, ts, (ts_sum[ts]/ts_count[ts]));} }' $x >$x.avg_delay; 
done 
