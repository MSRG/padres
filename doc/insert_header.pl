#!/usr/bin/perl

$headerdelim = "// =============================================================================";

$file = $ARGV[0];

open (HEADER, "<LICENSE_HEADER");

while (<HEADER>) {
	$header .= $_;
}

close(HEADER);

open (F, "$file");

while (<F>) {
	$f .= $_;
}

close (F);

$i = index($f, $headerdelim);
if ($i == -1) {
	$f = $header . $f;
} else {
	$i = index($f, $headerdelim, $i+1);
	$i = index($f, $headerdelim, $i+1);
	$endheader = $i + length($headerdelim) + 1;
	$tempf = substr($f, $endheader);
	$f = $header . $tempf;
}

#print $f;

open (F, ">$file");
print F $f;
close (F);

