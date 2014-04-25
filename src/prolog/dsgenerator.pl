%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% generator
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% some predefined trees for testing
gen_trees( [
	    ( snore(Man), tree([node('0',[+pres,ty(t),fo(snore(Man))]),node('01',[ty(e>t),fo(A^snore(A))]),node('00',[+(male),ty(e),fo(Man)])],'0') ),
	    ( like(Man,Woman), tree([node('0',[+pres,ty(t),fo(like(Man,Woman))]),node('01',[ty(e>t),fo(A^like(A,Woman))]),node('010',[+(female),ty(e),fo(Woman)]),node('011',[ty(e>(e>t)),fo(B^C^like(C,B))]),node('00',[+(male),ty(e),fo(Man)])],'0') )
	   ] ).

% top-level: pull out the complete trees and generate them with gen/5
generate( GoalForest, Strings, Method ) :-
	complete_trees( GoalForest, GoalTrees ),
	root_forest( RootForest ),
	gen( GoalTrees, RootForest, Method, Strings, _Forest ).

generate( GoalForest, Strings, Method, Forest ) :-
	complete_trees( GoalForest, GoalTrees ),
	root_forest( RootForest ),
	gen( GoalTrees, RootForest, Method, Strings, Forest ).

generate( GoalForest, StartForest, Strings, Method, Forest ) :-
	complete_trees( GoalForest, GoalTrees ),
%	root_forest( RootForest ),
%	append( StartForest, RootForest, InputForest ),
	gen( GoalTrees, StartForest, Method, Strings, Forest ).


% base case
gen( [], _Forest1, _Method, [], _Forest2 ).
% depth-first, one solution
gen( [H | _T], Forest1, [d, one], [String], Forest2 ) :-
	lexical_selection( H, W ),
	d_gen( H, W, Forest1, Forest2, String ).
% depth-first, all solutions
gen( [H | _T], Forest1, [d, all], Strings, Forest2 ) :-
	lexical_selection( H, W ),
	findall( String, d_gen( H, W, Forest1, Forest2, String ), Strings ).
% breadth-first
gen( [H | _T], Root, [b, Number], Strings, _Forest2 ) :-
	lexical_selection( H, W ),
	b_gen( H, W, [Root], Strings, Number ).


% main generation predicate. Simple or what?
main_gen( GoalTree, Word, Actions, Forest1, Forest2 ) :-
	findall( twa( Tree2, Words2, Actions2, Trace2, Prob2 ),
		 (
		   member( twa( Tree1, Words1, Actions1, Trace1, Prob1 ), Forest1 ),
		   apply( [Word], Actions, Tree1, Tree2, Trace, Prob ),
		   subsumes( GoalTree, Tree2 ),
		   append( Words1, [Word], Words2 ),
		   append( Actions1, Actions, Actions2 ),
		   append( Trace1, [Word | Trace], Trace2 ),
		   update_prob( Prob, Trace1, Prob1, Trace2, Prob2 )
		 ), Forest2 ),
	Forest2 \= [],
	Forest2 = [twa(_,Words2,_,_,_) | _],
	print( Words2 ), newline.


% d_gen/2 - d_gen( +LogicalForm, ?String )
% just for testing
d_gen( Sem, String ) :-
	gen_trees( GT ),
	member( ( Sem, GoalTree ), GT ),
	root_forest( Forest ),
	d_gen( GoalTree, Forest, String ).

% d_gen/6 - gen( +GoalTree, ?WordBag, +Forest1, ?Forest2, ?FinalString )
% depth-first search
d_gen( _GoalTree, _WordBag, Forest, Forest, [] ) :-
	complete_forest( Forest, [twa( _Tree, Words, _Actions, _Trace, _Prob ) | _] ),
	newline, print( Words ), newline.
d_gen( GoalTree, WordBag, Forest1, Forest3, [Word | Words] ) :-
	choose_word( Word, Actions, WordBag ),
	main_gen( GoalTree, Word, Actions, Forest1, Forest2 ),
	d_gen( GoalTree, WordBag, Forest2, Forest3, Words ).


% b_gen/2 - b_gen( +LogicalForm, ?StringList )
% just for testing
b_gen( Sem, StringList, Number ) :-
	gen_trees( GT ),
	member( ( Sem, GoalTree ), GT ),
	root_forest( Forest ),
	b_gen( GoalTree, Forest, StringList, Number ).


% b_gen/5 - breadth-first version with lexical pre-selection
b_gen( _GoalTree, _WordBag, [], [], _Number ).
b_gen( GoalTree, WordBag, Forests1, Strings, Number ) :-
	findall( Forest2,
		 (
		   member( Forest1, Forests1 ),
		   member( Word, WordBag ),
		   word( Word, Actions ),
		   main_gen( GoalTree, Word, Actions, Forest1, Forest2 )
		 ), Forests2 ),
	findall( String,
		 (
		   member( Forest, Forests2 ),
		   complete_forest( Forest, [twa( _Tree, String, _Actions, _Trace, _Prob ) | _] ),
		   newline, print( String ), newline
		 ), Strings2 ),
	(
	  Number = one,
	  Strings2 \= []
	->
	  Strings = Strings2
	;
	  b_gen( GoalTree, WordBag, Forests2, Strings3, Number ),
	  append( Strings2, Strings3, Strings )
	).


% if NOT doing lexical pre-selection (i.e. strictly incremental)
choose_word( Word, Actions, WordBag ) :-
	var( WordBag ),
	(
	  word_in_context( Word, Actions );
	  word( Word, Actions )
	).
% if doing lexical preselection
choose_word( Word, Actions, WordBag ) :-
	nonvar( WordBag ),
	(
	  member( Word, WordBag ),
	  word_in_context( Word, Actions );
	  member( Word, WordBag ),
	  word( Word, Actions )
	).


% choose a multiset of words
% NOT currently enabled by default - although it speeds up generation, it is not strictly incremental
% (can't handle online goal tree modification) and doesn't fit well with alignment model
lexical_selection( tree( Nodes, _Pointer ), WordBag ) :-
	(
	  flag( lexbag, true )
	->
	  lexical_selection( Nodes, WordBag1 ),
	  remove_duplicates( WordBag1, WordBag ),
	  print( 'Bag: ' ), print( WordBag ), newline
	;
	  true
	).

% hack to pre-select
% wh-relatives could be chosen properly on basis of LINK structure in goal tree
% pronouns, ellipsis auxs could be chosen on the fly given labels
lexical_selection( [], [] ).
%lexical_selection( [], [to] ).
%lexical_selection( [], [who, which, that, he, she, him, her, does] ).

lexical_selection( [node( _N, Labels ) | NTail], WordBag ) :-
	findall( Word, (
			 member( fo( Formula ), Labels ),
			 Formula \= e(_),
			 word( Word, Action ),
			 associated( Action, fo( Formula ) ),
			 \+ (
			      member( +Label, Labels ),
			      \+ associated( Action, +Label )
			    )
		       ), Words ),
	lexical_selection( NTail, WTail ),
	append( Words, WTail, WordBag ).


associated( put( Label1 ), Label2 ) :-
	variant( Label1, Label2 ).

associated( ( if _List1, then List2, else List3 ), Label ) :-
	associated( List2, Label );
	associated( List3, Label ).

associated( [Head | Tail], Label ) :-
	associated( Head, Label );
	associated( Tail, Label ).


% random generator: to generate a random corpus of grammatical sentences
rand_gen( Num ) :-
	open( 'corpus.txt', write, FileStream ),
	rand_gen( Num, FileStream ).

rand_gen( 0, FileStream ) :-
	close( FileStream ).
rand_gen( Num, FileStream ) :-
	Num > 0,
	root_forest( RootForest ),
	get_random_lexicon( WordList ),
	rand_gen( WordList, RootForest, EndForest, String ),
	(
	  EndForest = []
	->
	  NewNum = Num
	;
	  current_output( OldStream ),
	  set_output( FileStream ),
	  print(Num), newline,
	  print(String), newline,
	  set_output( OldStream ),
	  NewNum is Num - 1
	),
	rand_gen( NewNum, FileStream ).

% rand_gen( +Forest1, ?Forest2, ?FinalString )
% like d_gen/6 - depth-first search, one string only: but random word choice, no goal tree
% have a complete tree? then stop
rand_gen( _WordList, Forest, Forest, [] ) :-
	complete_forest( Forest, [twa( _Tree, Words, _Actions, _Trace, _Prob ) | _] ),
	newline, print( 'got complete tree' ), print( Words ), newline.
% have run out of words? then stop
rand_gen( [], _Forest, [], [] ) :-
	newline, print( 'failed to get complete tree' ), newline.
% pop a word off stack, adding it to string if successfully parsed
rand_gen( [word( Word, Actions ) | WordList], Forest1, Forest3, [Word | Words] ) :-
	main_gen( _GoalTree, Word, Actions, Forest1, Forest2 ),
	!,
	rand_gen( WordList, Forest2, Forest3, Words ).
% must keep the option of skipping words in seq when order is ungrammatical
rand_gen( [word( _Word, _Actions ) | WordList], Forest1, Forest2, Words ) :-
	rand_gen( WordList, Forest1, Forest2, Words ).


% this is for random corpus generation: don't use context!
get_random_lexicon( WordList ) :-
	%use_module( library( random ) ),
	findall( word( Word, Action ), word( Word, Action ), AllWords ),
	length( AllWords, N ),
	randseq( N, N, IndexList ),
	index_list( IndexList, AllWords, WordList ).
