%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% reading lexical & computational actions from file
%
% Matthew Purver, 2009
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% for efficiency, distinction between those rules that should always apply if they can,
% and those that aren't necessarily desirable
:- dynamic always_rules/1, possible_rules/1.

% word/2: lexical actions (i.e. lexicon)
% defined as word( WORD, ACTION )
:- dynamic word/2.

% rule/3: computational actions (i.e. grammar)
% defined as rule( NAME, ACTION )
:- dynamic rule/2.

% rule/3 - rule( ?RuleName, +Tree1, ?Tree2 )
% Defines the computational action RuleName in terms of input tree Tree1 and
% resulting output tree Tree2. Kept for compatibility with old definitions in .pl files
rule( Name, Tree1, Tree2 ) :-
	rule( Name, Action ),
	exec( Action, Tree1, Tree2 ).


get_grammar( Dir ) :-
	open( 'Dir/computational-actions.txt', read, FileStream ),
	set_input( FileStream ),
	get_file( FileStream, List ),
	close( FileStream ).


get_file( Stream, [] ) :-
	at_end_of_stream( Stream ).
get_file( Stream, [H | T] ) :-
	get_line( Stream, Line ),
	get_file_line( Stream, Line ),
	get_file( Stream, T ).


get_file_line( Stream, Line ) :-
	empty_line( _, Line ).
get_file_line( Stream, ['I', 'F' | Line] ) :-
	nl.


get_line( Stream, [] ) :-
	at_end_of_line( Stream );
	at_end_of_stream( Stream ).
get_line( Stream, Line ) :-
	get_char( Stream, H ),
	(
	  H = '/',
	  peek_char( Stream, '/' )
	->
	  get_char( Stream, '/' ),
	  (
	    peek_char( Stream, '*' )
	  ->
	    go_to_end_comment( Stream ),
	    get_line( Stream, Line )
	  ;
	    get_line( Stream, _Ignore ),
	    Line = []
	  )
	;
	  Line = [H | T],
	  get_line( Stream, T )
	).


go_to_end_comment( Stream ) :-
	at_end_of_stream( Stream ).
go_to_end_comment( Stream ) :-
	get_char( Stream, '*' ),
	get_char( Stream, '/' ),
	get_char( Stream, '/' ).
go_to_end_comment( Stream ) :-
	get_char( Stream, _ ),
	go_to_end_comment( Stream ).


empty_line( [] ).
empty_line( [' ' | T] ) :-
	empty_line( T ).
