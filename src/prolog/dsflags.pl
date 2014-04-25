%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% flags
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% modified Jan 2008 by YS -- marked with %%%comment

:- dynamic flag/2.

%%% possible values for flags (default marked with /**/)

% do we print to the terminal or to network sockets?
flag_values( io, [/**/term,sock] ).

% which file do we write the network socket port info to?
flag_values( portfile, [/**/'port.txt',_] ).

% do we print all trees in forest, or only the complete/best ones? 
flag_values( print, [all,/**/some,complete,best,traces,none] ).

% do we print in plain text, HTML with plain text trees, or HTML with phpSyntaxTrees?
flag_values( format, [/**/txt,html,php] ).

% how many sentences of context do we maintain?
flag_values( context, [/**/one,none,all] ).

% how do we search during generation? (d=depth-first/b=breadth-first)
flag_values( search, [/**/d,b] ).

% how many generation alternatives do we try for? 
flag_values( gen, [/**/one,all] ).

% do we use lexical pre-selection?
flag_values( lexbag, [true,/**/false] ).

% allowed VP ellipsis types
flag_values( ellip, [/**/strict,all] ).

%%% test output / messages during the parse
flag_values( test, [/**/none,trees,actions,both,complex] ).

%%% statistics
flag_values( statistics, [on,/**/off] ).


default_flags :-
	default_flag( print, some ),
	default_flag( format, txt ),
	default_flag( io, term ),
	default_flag( portfile, 'port.txt' ),
	default_flag( context, one ),
	default_flag( search, d ),
	default_flag( gen, one ),
	default_flag( lexbag, false ),
	default_flag( ellip, strict ),
	default_flag( test, none ),
	default_flag( statistics, off ).


flags :-
	flag( Flag, Value ),
	print( Flag ), print('='), print( Value ),nl,
	fail.
flags.


setflag( Flag, Value ) :-
	check_flag( Flag, Value ), %%% check flag/value validity
	retractall( flag( Flag, _ ) ),
	assert( flag( Flag, Value ) ).

%%% check flag validity
%%% first check if the flag exists
check_flag( Flag, Value ) :-
	flag_values( Flag, Values ),
	!,
	check_value( Value, Values ).
check_flag( _Flag, _Value ) :-
	print('no such flag'), !, fail.
%%% then if its value is correct
check_value( Value, Values ) :-
	member( Value, Values ), !.
check_value( _Value, _Values ) :-
	print( 'value invalid' ), !, fail.
	





default_flag( Flag, Value ) :-
	(
	  flag( Flag, _ )
	->
	  true
	;
	  setflag( Flag, Value )
	).


% process_args/0
% Gets command-line arguments or sets defaults for display format etc.
process_args :-
	prolog_version( sicstus ),
	current_prolog_flag( argv, ArgList ),
	process_args( ArgList ).
process_args :-
	prolog_version( swi ),
	current_prolog_flag( argv, [_Command | ArgList] ),
	process_args( ArgList ).

% process_args/1 - process_args( +ArgList )
% Asserts any suitable command-line arguments
process_args( [] ).
process_args( [H | T] ) :-
	(
	  prolog_version( sicstus )
	->
	  name( H, Chars ),
	  read_from_chars( Chars, Term )
	;
	  atom_to_term( H, Term, _ )
	),
	(
	  callable( Term )
	->
	  call( Term )
	;
	  true
	),
	process_args( T ).
