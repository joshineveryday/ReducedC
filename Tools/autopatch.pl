#!/usr/bin/perl

$usr = "$ARGV[0].patch";

`dos2unix $usr $usr.new 2>/dev/null`;
`mv $usr.new $usr`;
print "Applying patch for: $ARGV[0]\n";
open INFILE, "<$usr" or die $!;
open OUTFILE, ">$usr\.new" or die $!; 

$count = 0;
$addcount = 0;
$delcount = 0;
$modcount = 0;
while (<INFILE>) {
    $new = $_;
    if ($new =~ m/^---/) {
        ($x,$y,$z) = split(/\s/, $new, 3);
        $new = "$x $y.new $z";
    }
    if ($new =~ m/^\+\s+[^\s]+/) {
        $addcount++;
        print $new
    }
    if ($new =~ m/^-\s+[^\s]+/) {
        $delcount++;
        print $new
    }
    if ($new =~ m/^!\s+[^\s]+/) {
        $modcount++;
        print $new
    }
    print OUTFILE $new;
}

$count = $addcount + $delcount + ($modcount/2);
close INFILE;
close OUTFILE;
`mv $usr\.new $usr`;
print "Number of lines of change: $count\n";
if ($count > 20) {
    print "Greater than 20 lines of diff\n";
} else {
    print "Converting to UNIX format...\n";
    if ( -d "original/src" ) {
        system("for i in \`ls original/src\` ; do echo \$i; dos2unix original/src/\$i original/src/\$i 2>/dev/null; done");
    } else {
        system("for i in \`ls original\` ; do echo \$i; dos2unix original/\$i original/\$i 2>/dev/null; done");
    }
    print "Applying patch...\n";
    `patch -b -p0 < $usr`;
}
