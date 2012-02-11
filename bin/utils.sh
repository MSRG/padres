find_ip() {
    hostname='localhost'
    ifconfig=`whereis -b ifconfig | cut -d " " -f2 -s`
    if [ ! -z $ifconfig ]
    then
	ip_list=$($ifconfig | awk '/inet addr:/{print $2}' | cut -d: -f2)
	for ip in $ip_list
	do
	    hostname=$ip
	    break
	done
    fi
    #ip_list=`ifconfig | awk '/inet addr/{print $2}' | cut -d: -f2 -s`
    echo $hostname
}
