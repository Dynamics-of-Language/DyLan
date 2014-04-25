%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% main module
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% define operators
:- op( 900, fx, [?, /\, \/, if, then, else] ).
:- op( 800, xf, * ).
:- op( 1000, yfx, [&] ).

% load general DS modules
:- ensure_loaded( [lexical_actions, computational_actions, lambda_calculus, dstrees, dsprob] ).
:- ensure_loaded( [dsparser, dsgenerator, dscontext, dsio, utils, dsprinter, dsflags] ).

% load language-specific modules
%:- ensure_loaded( [lexicon_english, lexical_templates_english_2009ttr, computational_actions_english_2009ttr] ).
:- ensure_loaded( [lexicon_english, lexical_templates_english_2009, computational_actions_english_2009] ).
%:- ensure_loaded( [lexicon_english, lexical_templates_english_2005, computational_actions_english_2005] ).
%:- ensure_loaded( [lexicon_english, lexical_templates_english_2001, computational_actions_english_2001] ).
%:- ensure_loaded( [lexicon_japanese, lexical_templates_japanese, computational_actions_japanese] ).

:- default_flags.%%% just to make sure they are set without running ds

% check for Prolog compatibility
prolog_version( sicstus ) :-
	prolog_flag( version, V ),
	atom_concat( 'SICS', _, V ),
	!.
prolog_version( swi ) :-
	prolog_flag( version, V ),
	atom_concat( 'SWI', _, V ),
	!.
prolog_version( unknown ) :-
	print( 'error - unknown Prolog' ),
	fail.

% load Prolog-specific modules
:-
	prolog_version( swi ),
	ensure_loaded( sicstus2swi ),
	use_module( library( socket ) )
	;
	prolog_version( sicstus ),
	use_module( library( lists ), [is_list/1, member/2, reverse/2, append/3, prefix/2, select/3,
				       substitute/4, memberchk/2, nth/3, remove_duplicates/2] ),
	use_module( library( charsio ), [read_from_chars/2, write_to_chars/2] ),
	use_module( library( terms ), [variant/2] ),
	use_module( library( sockets ) ).


% ds/0
% Top-level - sets up initial forest and starts
ds :-
	default_flags,
	process_args,
	startup( Socket ),
	reset_context,
	reset_vars,
	root_forest( Forest ),
%	print_forest( Forest ),
	ds( Socket, Forest ),
	shutdown( Socket ).


% ds/2 - ds( ?Socket, +TreeList )
% Gets input from user and passes to ds/3
% Socket should be instantiated if io(sock) true
% TreeList should be the current parse forest
ds( Socket, Forest1 ) :-
	get_input( Socket, Words ),
	ds( Socket, Words, Forest1 ).

% ds/3 - ds( ?Socket, +WordList, +TreeList )
% Checks for system message, calls parser or generator as required
% Succeeds when: told to stop / told to generate / parse forest becomes empty
% stop: succeed
ds( _Socket, Words, _Forest ) :-
	end_message( Words ),
	!.
% reset: start again
ds( Socket, Words, _Forest ) :-
	reset_message( Words ),
	!,
	reset_context,
	reset_vars,
	print_context,
	root_forest( Forest ),
	ds( Socket, Forest ).
% next turn: update context & start again
ds( Socket, Words, Forest1 ) :-
	turn_message( Words ),
	!,
	print( 'Next sentence:' ), newline,
	close_streams,
	update_context( Forest1 ),
	print_context,
	root_forest( Forest2 ),
	ds( Socket, Forest2 ).
% generate: call generate/3
ds( _Socket, Words, Forest ) :-
	gen_message( Words, Method ),
	!,
	generate( Forest, Strings, Method ),
	newline, write( 'gen> ' ), print( Strings ), newline.
% otherwise: call parse/3
ds( Socket, Words, Forest1 ) :-
	print_context,
	statistics( runtime, _ ),
	parse( Forest1, Words, Forest2 ),
	statistics( runtime, [_,R] ),
	format( "Runtime ~w~n", [R] ),
	(
	  Forest2 = []
	->
	  print( 'No derivations' ), newline
	;
	  print_forest( Forest2 ),
	  close_streams,
	  ds( Socket, Forest2 )
	).


% message to halt parsing
end_message( [halt] ).
end_message( [exit] ).
end_message( [quit] ).
% message to start generating
gen_message( [gen, Method, Number], [Method, Number] ).
gen_message( [gen], [Method, Number] ) :-
	flag( search, Method ),
	flag( gen, Number ).
% message to empty current forest
reset_message( [reset] ).
turn_message( [next] ).

close_streams :-
	current_output( OutStream ),
	close( OutStream ),
        current_input( InStream ),
        close( InStream ).

prepare_for_web :-
	setflag( io, sock ),
	setflag( format, html ),
	(
	    prolog_version( sicstus ),
	    save_program( ds, ds )
	    % use_module( library( system ) ),
	    % exec( '../../bin/spld --output=ds --static ds.sav', [null,null,null], _PID )
	;
	    prolog_version( swi ),
	    qsave_program( ds, [goal(ds), stand_alone(true)] )
	).
