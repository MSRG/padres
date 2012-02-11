#!/usr/bin/perl
#
# PADRES Demo Script
#
# takes a demo configuration file with lines of the following type:
#   B <host> <run directory> <properties filename>
#   C <host> <run directory> <run script command>

if (@ARGV < 1) {
	print "Must specify a demo configuration file\n";
	exit(1);
}

setpgrp(0,0);

$conffile = $ARGV[0];
open(CONF, "< $conffile") or die "Can't open $conffile : $!";
@conflines = <CONF>;
close CONF;

chomp($mydir = `pwd`);
$cnum = 0;

@remotehosts = ();

foreach $confline (@conflines) {
	next if $confline =~ /^#/;
	chomp $confline;
	($type, $host, $dir, $param, @otherparams) = split (' ', $confline);
	(my $PADRES_ROOT = $dir) =~ s-/[^/]*$--;

	chdir($mydir);
	if (uc($type) =~ /^B/) {
		# BROKER
		$logfile = "$mydir/B-$host-$param.log";
		%props = &readProperties($param);
		if (&isLocal($host)) {
			print "Executing local broker with properties file $param\n";
			system("cp -f $param $dir/$param");
			chdir($dir);
			runForked("rmiregistry " . $props{"padres.port"});
			runForked("java -Djava.class.path=\"main:lib/jess.jar:lib/openjms-0.7.5.jar:lib/j2ee.jar:lib/mysql-connector.jar:lib/exolabcore-0.3.5.jar:lib/pg73jdbc3.jar\" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=\"file://$PADRES_ROOT/build/padres-0.2.jar file://$PADRES_ROOT/build/lib/openjms-0.7.5.jar file://$PADRES_ROOT/build/lib/j2ee.jar\" -Dpadres.build=\"$PADRES_ROOT/build\" brokercore.BrokerCore $param > $logfile");
		} else {
			print "Executing remote broker on $host with properties file $param\n";
#			if (!grep($host, @remotehosts)) {
				push(@remotehosts, $host);
#			}
			system("scp $param $host:$dir");
			runForked("ssh $host /j2sdk1.4.2_05/bin/rmiregistry " . $props{"padres.port"});
#			runForked("ssh $host 'cd $dir; java -Djava.class.path=\"main:lib/jess.jar:lib/openjms-0.7.5.jar:lib/j2ee.jar:lib/mysql-connector.jar:lib/exolabcore-0.3.5.jar:lib/pg73jdbc3.jar\" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=\"//$PADRES_ROOT/build/padres-0.2.jar //$PADRES_ROOT/build/lib/openjms-0.7.5.jar //$PADRES_ROOT/build/lib/j2ee.jar\" -Dpadres.build=\"$PADRES_ROOT/build\" brokercore.BrokerCore $param' > $logfile");
			runForked("ssh $host 'cd $dir; java -Djava.class.path=\"main;lib/jess.jar;lib/openjms-0.7.5.jar;lib/j2ee.jar;lib/mysql-connector.jar;lib/exolabcore-0.3.5.jar;lib/pg73jdbc3.jar\" -Djava.security.policy=etc/java.policy -Djava.rmi.server.codebase=\"file://$PADRES_ROOT/build/padres-0.2.jar file://$PADRES_ROOT/build/lib/openjms-0.7.5.jar file://$PADRES_ROOT/build/lib/j2ee.jar\" -Dpadres.build=\"$PADRES_ROOT/build\" brokercore.BrokerCore $param' > $logfile");
		}
	} elsif (uc($type) =~ /^C/) {
		# CLIENT
		$logfile = "$mydir/C-$host-$cnum.log";
		if (&isLocal($host)) {
			print "Executing local client with command $param " . join(" ", @otherparams) . "\n";
			chdir($dir);
			runForked("$param " . join(" ", @otherparams) . " > $logfile");
		} else {
			print "Executing remote client on $host with command $param\n";
#			if (!grep($host, @remotehosts)) {
				push(@remotehosts, $host);
#			}
			runForked("ssh $host 'cd $dir; $param " . join(" ", @otherparams) . "' > $logfile");
		}
		$cnum++;
	} else {
		print "invalid configuration line: $confline\n";
		exit(1);
	}

	sleep(2);
}

$stophosts = join(" ", @remotehosts);
open(STOPSCRIPT, "> stop_demo.sh");
print STOPSCRIPT <<END;
#!/bin/sh
for host in $stophosts
do
ssh \$host 'killall java; killall rmiregistry'
done
END
close(STOPSCRIPT);
chmod(0755, "stop_demo.sh");

print "System is running, press <ENTER> to terminate\n";
<STDIN>;
print "Killing child processes\n";
local $SIG{TERM} = 'IGNORE';
kill SIGTERM, -$$;
print "Exiting\n";
exit(0);


sub isLocal {
	my $host = shift(@_);
	if ($host =~ /127\./ | $host =~ /localhost/) {
		return 1;
	}
	return 0;
}


sub readProperties {
	my %props = ();
	my $propsfile = shift(@_);
	open(PF, "< $propsfile") or die "Can't open $propsfile : $!";
	while (<PF>) {
		next if m/^\s*$/;
		next if m/^#/;
		chomp;
		my ($key, $value) = split(/=/);
		$props{$key} = $value;
	}
	close(PF);
	return %props;
}


sub runForked {
	my $command = shift;
	my $pid = fork();
	die "fork() failed: $!" unless defined $pid;
	if ($pid) {
		return $pid;
	}
	else {
		exec($command);
	}
}

