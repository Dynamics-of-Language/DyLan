#!/usr/local/bin/perl -w
$| = 1;

# import subroutines
use CGI qw( param cgi_error header start_html end_html strong );
use IO::Socket::INET;

# set up defaults
$host = 'localhost';
$portfile = 'port.txt';
$endMessage = 'halt';
$turnMessage = 'next';
$genMessage = 'gen';
( $halt, $turn, $gen ) = ( 0, 0, 0 );
#$args = '"setflag(format,php)." "setflag(io,sock)."';
$args = '"setflag(io,sock)."';
$method = 'b_one';
$lex = 'no';

# start HTML
print header(), start_html( 'Dynamic Syntax' );

# trap errors
my $error = cgi_error();
$error and error_die( $error );

# get user parameters
my @names = param();
while ( my $name = shift( @names ) ) {
    my @param = param( $name );
    for ( @param ) {
	( $name =~ /sent/i ) and $sent = $_;
	( $name =~ /port/i ) and $port = $_;
	( $name =~ /halt/i ) and $halt = 1;
	( $name =~ /turn/i ) and $turn = 1;
	( $name =~ /gen/i ) and $gen = 1;
	( $name =~ /tree/i ) and $args .= " \"setflag(print,$_).\"";
	( $name =~ /context/i ) and $args .= " \"setflag(context,$_).\"";
	( $name =~ /format/i ) and $args .= " \"setflag(format,$_).\"";
	( $name =~ /method/i ) and $method = $_;
#	( $name =~ /lex/i ) and $lex = $_;
    }
}
$halt and ( $sent = $endMessage );
#$gen and ( $sent = "$genMessage $method\_$lex" );
$gen and ( $sent = "$genMessage $method" );
defined( $sent ) or error_die( 'No input to process!' );

# not been given a port? then start the prolog process and get the port
unless ( defined( $port ) ) {

    # spawn prolog as a daemon process
    system( "perl ds_daemon.pl $args &" );

    # give child time to start sicstus, then read port value
    sleep( 1 );
    open PORTFILE, "< $portfile" or error_die( "Can\'t open $portfile" );
    while ( <PORTFILE> ) {
	/(\d+)/ and ( $port = $1 );
    }
    close PORTFILE;
    defined( $port ) or error_die( 'No port!' );

}

# need a monospaced font, no line wrapping
print '<table><tr><td nowrap><tt>';

# do the business
if ( $turn ) {

# connect to prolog process
    ( $stream = IO::Socket::INET->new( "$host:$port" ) )
	or error_die( "Can\'t connect to $host:$port : $!" );
    autoflush STDOUT 1;
    autoflush $stream 1;

    print $stream "$turnMessage\n";
    while ( <$stream> ) {
	print;
    }
}

# connect to prolog process
( $stream = IO::Socket::INET->new( "$host:$port" ) )
    or error_die( "Can\'t connect to $host:$port : $!" );
autoflush STDOUT 1;
autoflush $stream 1;

# do the business
print $stream "$sent\n";
$ended = 0;
while ( <$stream> ) {
    print;
    /goodbye/i and $ended = 1;
}

# tidy up, passing the port as a hidden parameter for incremental parsing
print "</tt></td></tr></table>\n";

unless ( $ended ) {

    print<<END;
<hr>
<form action="ds.cgi" method="POST">
To add text incrementally to the current sentence parse tree, enter it here and press the "Parse" button.
To update context and start a new sentence, press the "Next Sentence" button:
<br><br>
<input type="hidden" name="port" value="$port">
<input name="sent" size=30></input>
<input type="submit" name="parse" value="Parse">
<input type="submit" name="turn" value="New Sentence">
<input type="submit" name="halt" value="Halt">
<input type="reset">
<br><br>
Please press the "Halt" button when you\'re finished!
<hr>
To attempt to generate all possible strings that match any current complete trees, press the "Generate" button:
<br>
<table><tr><td>
<input type="submit" name="gen" value="Generate"></td><td>
<input type="radio" name="method" value="b_one" checked> Breadth-first, shortest solutions only<br>
<input type="radio" name="method" value="b_all"> Breadth-first, all solutions (slow)</td><td>
<input type="radio" name="method" value="d_one"> Depth-first, first solution only<br>
<input type="radio" name="method" value="d_all"> Depth-first, all solutions (slow)
<!-- <input type="checkbox" name="lex" value="yes" checked> Use lexical selection -->
</td></tr></table>
<br>
</form>
END

}

print<<END;
<HR>

<table width=100%>
<tr>
<td width=50% align=left><i>This implementation &copy; <a href="http://www.dcs.qmul.ac.uk/~mpurver/">Matthew Purver</a>, 2002-9</i></td>
<td width=50% align=right><i>Problems, comments, suggestions: <a href=mailto:mpurver_AT_dcs.qmul.ac.uk>mpurver AT dcs.qmul.ac.uk</a></i></td>
</tr>
</table>
END

print end_html();
exit;

sub error_die {
    print start_html( 'Error' ), strong( @_ ), end_html();
    die;
}
