/***********************************************************************
 *
 * Auxiliary predicates needed by the shards components
 *
 ***********************************************************************/

% predicates exported by this module
% :- module( utils, [print_list/1, portray_list/1, prt/1, dbgprt/2,
% 		   split_list/4, read_in/1, charlist_to_atomlist/2] ).

% debug flag
:- asserta(debug_level(5)).

%%% for path renewal testing: there should be a better way
:- dynamic(tr/1),assert(lsttr(_)).

% for fresh-variable atoms
:- dynamic(varnum/2).

%%%%%%%%%%%%%%%%
%
% fresh-variable handling
%
%%%%%%%%%%%%%%%%

reset_vars :-
	retractall( varnum( _, _ ) ).

fresh_var( X ) :-
	fresh_var( x, X ).

fresh_var( Atom, X ) :-
	atom( Atom ),
	(
	  varnum( Atom, Num )
	->
	  true
	;
	  Num = 0
	),
	NewNum is Num + 1,
	atom_concat( Atom, NewNum, X ),
	retractall( varnum( Atom, _ ) ),
	assert( varnum( Atom, NewNum ) ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Output Predicates
%

%%% for record

record_input(Preds):-
	open('record.txt',append,S,[alias(record)]),
	set_output(S),
	record(Preds),
	close(S).

record([]):-nl,nl.
record([Pred|Preds]):-
	Pred=print_tree(_),!,call(Pred),record(Preds).
record([Pred|Preds]):-
	Pred=..[PredName,Pred1stArg|_],print(PredName),print('('),print(Pred1stArg),nl,
	call(Pred),record(Preds).
	
	


%%% testing pred. 


test_output(none,_,_,_):-!.
test_output(complex,_,_,_):-!.
test_output(actions,Rule,_,_):-
%	spy(test_output(actions,_,_,_)),print('Rule applied: '),print(Rule),leash(_).%tab(1),read(a).
	print('Rule applied: '),print(Rule),nl.

test_output(trees,_,Tree1,Tree2):-
	lsttr(Tree1),var(Tree1),!,retractall(lsttr(_)),
	print('Before: '),nl,print_tree(Tree1),nl,print('After: '),print_tree(Tree2),
	assert(lsttr(Tree2)).


test_output(trees,_,Tree1,Tree2):-
	lsttr(Tree1),!,retractall(lsttr(_)),
	%print('Before: '),nl,print_tree(Tree1),
	print('After: '),print_tree(Tree2),
	assert(lsttr(Tree2)).

test_output(trees,_,Tree1,Tree2):-
	print('New path entered'),nl,retractall(lsttr(_)),
	print('Before: '),nl,print_tree(Tree1),nl,print('After: '),print_tree(Tree2),
	assert(lsttr(Tree2)).

test_output(both,Rule,Tree1,Tree2):-
	test_output(actions,Rule,_,_),
	test_output(trees,_,Tree1,Tree2).


%
% print_list(+List)
%     -- prints all elements of List successively via prt.
%
print_list([]) :- !.
print_list([H|R]) :- 
	prt(H),
	prt(' '),
	print_list(R).

				%
% portray_list(+List)
%     -- portrays all elements of List successively via portray.
%
portray_list([]) :- !.
portray_list([H|R]) :-
	portray(H),
	nl, write('--------------------------------------------------'), nl,
	portray_list(R).


%
% prt(+Output)
%     -- writes Output. If a tcl interpreter is available the output
%        is written into the text field of the tcl window; writes to
%        stdout otherwise.
%

prt(X) :-
	% if we've got a tcl interpreter, use it for output
	shards_tcl_interp(Interp),
	!,
	atom_concat('.desk.nr1.text insert end "', X, A1),
        atom_concat(A1, '"', A2), 
	tcl_eval(Interp, A2, _).

prt(X) :-
        write(X).

%
% dpgprt(+DebugingOutput)
%     -- writes DebugingOutput if the debuging flag is set;
%        succeeds silently otherwise.
%
dbgprt(L, X) :-
	debug_level(Lev),
	L < Lev,
	!,
	write(X),
	nl.

dbgprt(_, _).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% split_list(+Sublist, +List, -Infront, -Behind)
%     -- find Sublist in List and remove it. Return remaining parts.
%        E.g. split_list([a,b,c,d,e,f,g], [c,d,e], [a,b], [f,g]) is
%        a succeeding instantiation.
%

split_list(Sublist, List, Infront, Behind) :-
	split_list(Sublist, List, [], Infront, Behind).

split_list(Sublist, List, InfAcc, Infront, Behind) :-
	append(Sublist, Behind, List),
	reverse(InfAcc, Infront).

split_list(Sublist, [H|TList], InfAcc, Infront, Behind) :-
	split_list(Sublist, TList, [H|InfAcc], Infront, Behind).


%
% flatten list List to FlatList
%
flatten(List, FlatList) :-
	flatten_aux(List, [], FlatList).

flatten_aux([H|T], ListAcc, FlatList) :-
	is_list(H),
	!,
	flatten(H, FlatH),
	append(ListAcc, FlatH, NewAcc),
	flatten_aux(T, NewAcc, FlatList).

flatten_aux([H|T], ListAcc, FlatList) :-
	append(ListAcc, [H], NewAcc),
	flatten_aux(T, NewAcc, FlatList).

flatten_aux([], FlatList, FlatList).


%
% Order a list given a list of indices
%
index_list( [], _List, [] ).

index_list( [IH | IT], List, [H | T] ) :-
	nth( IH, List, H ),
	index_list( IT, List, T ).



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%------------------------------------------------------
%% read_in/1
%% read_in( -S )
%%
%% Description:
%%   Program for reading in a sentence from the screen 
%%   to produce a list of words S.
%%   (modified) from Clocksin and Mellish ( 1984 )
%% Succeeds: 1
%% Side Effects: none
%%-------------------------------------------------------

% get first char, call restsent/3
read_in( S ) :-
	get0( C ), 
	restsent( C, S ).


%%------------------------------------------------------
%% restsent/2
%% restsent( +C, -S )
%%
%% Description:
%%   Given first char C of input, produces sentence S
%% Succeeds: 1
%% Side Effects: none
%%-------------------------------------------------------

% get word using read_word/3 and recurse (as long as we
% haven't seen an end-of-sentence punct)
restsent( C, [W | Ws] ) :-
	readword( C, W, C1 ),
	\+ W = [],
	!,
	restsent( C1, Ws ).

% base case: otherwise stop (not added)
restsent( _C, [] ).


%%------------------------------------------------------
%% readword/3
%% readword( +C, -W, -C2 )
%%
%% Description:
%%   Given first char C of input, produces entire word W
%%   and next char C2
%% Succeeds: 1
%% Side Effects: none
%%-------------------------------------------------------

% if end-of-line char, stop (NOT popping \n off stdin)
% empty list is just used here as a flag for restsent/2
readword( C, [], C ) :-
	end_of_line_char( C ),
	!.

% if end-of-sentence punct, stop (popping \n off stdin)
% empty list is just used here as a flag for restsent/2
readword( C, [], C ) :-
	end_of_sent_char( C ),
	get0( _ ),
	!.

% if alphabetic, get next char and finish word
readword( C, W, C2 ) :-
	in_word( C, NewC ),
	!, 
	get0( C1 ), 
	restword( C1, Cs, C2 ), 
	name( W, [NewC | Cs] ).

% otherwise (it's a space), get next char & try again
readword( _C, W, C2 ) :-
	get0( C1 ),
	readword( C1, W, C2 ).


%%------------------------------------------------------
%% restword/3
%% restword( +C, -W, -C2 )
%%
%% Description:
%%   Given char C of input, produces rest of word W
%%   and next char C2
%% Succeeds: 1
%% Side Effects: none
%%-------------------------------------------------------

% if alphabetic, get next char, add to word & recurse
restword( C, [NewC | Cs], C2 ) :-
	in_word( C, NewC ),
	!, 
	get0( C1 ), 
	restword( C1, Cs, C2 ).

% base case: non-alphabetic (punct or space) - not added to word
restword( C, [], C ).


%%------------------------------------------------------
%% in_word/2
%% in_word( +C, ?L )
%%
%% Description:
%%   Succeeds if char C is alphabetic. L differs from C
%%   at most in that it is lower-case
%% Succeeds: 0-1
%% Side Effects: none
%%-------------------------------------------------------

% lower-case letter: leave alone
in_word( C, C ) :-
	name( 'az', [LittleA, LittleZ] ),
	C >= LittleA,   % = 97
	C =< LittleZ.   % = 122

% upper-case letter: convert to lower-case
in_word( C, L ) :-
	name( 'AZa', [BigA, BigZ, LittleA] ),
	C >= BigA,      % = 65
	C =< BigZ,      % = 90
	Diff is LittleA - BigA,
	L is C + Diff.


%%------------------------------------------------------
%% end_of_sent_char/1
%% end_of_sent_char( ?C )
%%
%% Description:
%%   Succeeds if char C corresponds to an end-of-sentence
%%   punctuation character
%% Succeeds: 1*
%% Side Effects: none
%%-------------------------------------------------------

end_of_sent_char( 46 ).		% = '.'

end_of_sent_char( 63 ).		% = '?'

end_of_sent_char( 33 ).		% = '!'

end_of_line_char( 10 ).		% = '\n'

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
% Converts a list of char characters (ie a prolog string) into a list
% of atoms (like read_in does with the user input).
%

charlist_to_atomlist(CList, AtomList) :-
	char2atom(CList, [], [], AtomList).

char2atom([C|T], WordBuffer, AtomsSoFar, AtomList) :-
	wordseperator(C),
	!,
	reverse(WordBuffer, RevWordBuffer),
	name(Word, RevWordBuffer),
	char2atom(T, [], [Word|AtomsSoFar], AtomList).

char2atom([C|T], WordBuffer, AtomsSoFar, AtomList) :-
	in_word(C, Lower),
	char2atom(T, [Lower|WordBuffer], AtomsSoFar, AtomList).

char2atom([], [], RevAtomList, AtomList) :-
	reverse(RevAtomList, AtomList).

char2atom([], WordBuffer, RevAtomList, AtomList) :-
	reverse(WordBuffer, RevWordBuffer),
	name(Word, RevWordBuffer),
	reverse([Word|RevAtomList], AtomList).


wordseperator(32). % space
wordseperator(33). % !
wordseperator(44). % ,
wordseperator(59). % ;
wordseperator(58). % :
wordseperator(45). % -
wordseperator(63). % ?
wordseperator(46). % .

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%EOF%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
