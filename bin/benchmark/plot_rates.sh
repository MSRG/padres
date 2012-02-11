#!/bin/bash

if [ $# -lt 3 ]
then
    echo "Usage $0 <data_dir> <topology#> <msg_rate>"
    exit 1
fi
data_dir=$1

if [ $2 == "1" ]
then
    replace_str="$2_$3"
else
    if [ $# -lt 4 ]
    then
	echo "Usage $0 <data_dir> <topology#> <msg_rate> <client#>"
	exit 1
    fi
    replace_str="$2_c$4_$3"
fi
deliv_file="$data_dir/delays_runtopology${replace_str}.deliv.rate"
gener_file="$data_dir/delays_runtopology${replace_str}.gener.rate"
start_time_g=`head -n 1 $gener_file | awk '{print $1}'`
start_time_d=`head -n 1 $deliv_file | awk '{print $1}'`

fig_dir="figures"
if [ ! -d $fig_dir ]; then
    mkdir $fig_dir
fi

protocol=`basename $data_dir`
eps_filename=${protocol}_$replace_str

# sed -e s:socket_60000:$eps_filename:g -e s:data:$data_dir:g -e s/1_900/$replace_str/ -e s/\$1-[0-9]\\+/\$1-$start_time_g/ plot_rates.gp > plot_rates.tmp.gp
sed -e s:socket_60000:$eps_filename:g -e s:data:$data_dir:g -e s/1_900/$replace_str/ plot_rates.gp > plot_rates.tmp.gp

gnuplot plot_rates.tmp.gp

rm plot_rates.tmp.gp


