set term push
set terminal postscript eps enhanced 
set output "figures/socket_60000.eps"

set ylabel "message rate (msg/sec)"
set y2label "message delay (ms)"
set xlabel "run time (sec.)"
# set x2label "msg. count (x1000)"

set ytics nomirror
set y2tics nomirror
set xtics nomirror
# set x2tics nomirror

#set key right center
set key below

plot "data/delays_runtopology1_900.gener.rate" u 1:3 t "generation rate" w p,\
     "data/delays_runtopology1_900.deliv.rate" u 1:3 t "delivery rate" w p 2 4,\
     "data/delays_runtopology1_900.avg_delay" u 1:3 t "delivery delay" axes x1y2 w p 
# "data/delays_runtopology1_900" u ($1/1000):($4 < 50? $4:1/0) t "delivery delay" axes x2y2 w p

#3 6

set terminal pop

replot

pause -1