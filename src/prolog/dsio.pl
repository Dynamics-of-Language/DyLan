%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% input/output library
%
% Matthew Purver, 2002
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% initialisation
startup( _Socket ) :-
	flag( io, term ).
 % for sicstus
startup( Socket ) :-
	flag( io, sock ),
	prolog_version( sicstus ),
	socket( 'AF_INET', Socket ),
	socket_bind( Socket, 'AF_INET'( localhost, Port ) ),
	flag( portfile, PortFile ),
	open( PortFile, write, PortFileStream ),
	print( PortFileStream, Port ), nl( PortFileStream ),
	close( PortFileStream ),
	socket_listen( Socket, 1 ).
 % for SWI
startup( Socket ) :-
	flag( io, sock ),
	prolog_version( swi ),
	tcp_socket( Socket ),
	tcp_bind( Socket, Port ),
	flag( portfile, PortFile ),
	open( PortFile, write, PortFileStream ),
	print( PortFileStream, Port ), nl( PortFileStream ),
	close( PortFileStream ),
	tcp_listen( Socket, 5 ).

% shutdown
shutdown( _Socket ) :-
	flag( io, term ),
	newline, print( 'Goodbye!' ), newline,
	abort.
shutdown( Socket ) :-
	flag( io, sock ),
	prolog_version( sicstus ),
	newline, print( 'Goodbye!' ), newline,
	current_output( Stream ),
	close( Stream ),
	socket_close( Socket ),
	halt.
shutdown( Socket ) :-
	flag( io, sock ),
	prolog_version( swi ),
	newline, print( 'Goodbye!' ), newline,
	current_output( Stream ),
	close( Stream ),
	tcp_close_socket( Socket ),
	halt.

get_input( _Socket, Words ) :-
	flag( io, term ),
	write( 'ds> ' ), flush_output,
	read_in( Words ).
get_input( Socket, Words ) :-
	flag( io, sock ),
	prolog_version( sicstus ),
	socket_accept( Socket, _Client, SockStream ),
	set_input( SockStream ),
	set_output( SockStream ),
	read_in( Words ).
get_input( Socket, Words ) :-
	flag( io, sock ),
	prolog_version( swi ),
	tcp_accept( Socket, ClientSocket, _ClientIP ),
	tcp_open_socket( ClientSocket, InStream, OutStream ),
	set_input( InStream ),
	set_output( OutStream ),
	read_in( Words ).
