%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% context-handling library
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


:- dynamic context/1.

% clear context
reset_context :-
	retractall( context( _ ) ),
	assert( context( [] ) ).

% print context
print_context :-
	flag( context, none ).
print_context :-
	context( [] ),
	!,
	print( 'Context: (empty)' ), newline.
print_context :-
	context( Context ),
	print( 'Context: ' ), newline,
	print_forest( Context ).

% no context? do nothing
update_context( _Forest ) :-
	flag( context, none ).
% single turn? replace with complete forest
update_context( Forest ) :-
	flag( context, one ),
	retractall( context( _ ) ),
	complete_forest( Forest, Context ),
	assert( context( Context ) ).
% all turns? append
update_context( Forest ) :-
	flag( context, all ),
	context( Context1 ),
	retractall( context( _ ) ),
	complete_forest( Forest, Context ),
	append( Context, Context1, Context2 ),
	assert( context( Context2 ) ).

% any member of the context forest
tree_in_context( Tree ) :-
	context( Context ),
	member( twa( Tree, _Words, _Actions, _Trace, _Prob ), Context ).

% any node in a tree in context
node_in_context( Node ) :-
	tree_in_context( tree( Nodes, _Pointer ) ),
	member( Node, Nodes ).


% pronouns considered as always contextually available for generation
word_in_context( Word, Actions ) :-
	pron( Word, _, Type, _ ),
	Type \= whrel,
	Type \= whq,
	word( Word, Actions ).
% elliptical verbs always available
word_in_context( Word, Actions ) :-
	verb( _, Word, _, _, ellip ),
	word( Word, Actions ).
% otherwise any word/action pair in context
word_in_context( Word, [Action] ) :-
	context( Context ),
	member( twa( _Tree, Words, Actions, _Trace, _Prob ), Context ),
	nth( N, Words, Word ),
	nth( N, Actions, Action ).

% any sequence of word/action pairs in context
words_in_context( SubWords, SubActions ) :-
	context( Context ),
	member( twa( _Tree, Words, Actions, _Trace, _Prob ), Context ),
	sub_list( N, L, Words, SubWords ),
	sub_list( N, L, Actions, SubActions ).


% gets a sublist starting at N with length L>0
sub_list( N, L, List, SubList ) :-
	append( Prefix, Mid, List ),
	length( Prefix, P ),
	N is P + 1,
	append( SubList, _Suffix, Mid ),
	length( SubList, L ),
	L > 0.


% initial parse forest from context
forest_in_context( Forest ) :-
	findall( TWA,
		 (
		   %partial_forest( Forest1 ), % contextual partial trees for shared utterances
		   cr_twa( TWA ) % late-*-adjoined versions for CRs, extensions etc
		 ;
		   wh_twa( TWA ) % partial trees from wh-questions in context
		 ), Forest ).
		  

% initial forest for answer re-use give a wh-question tree in context
wh_twa( twa( Adjusted, [], [], [], 0.0 ) ) :-
	tree_in_context( Tree ),
	complete( Tree ),
	wh_adjust( Tree, Adjusted ).

% % any tree with a wh-metavariable as an argument of the root proposition
% % -- actually, this ignores embedded wh-questions, which we want to be able to answer
% wh_question( tree( Nodes, Pointer ) ) :-
% 	root( Node ),
% 	member( node( Node, Labels ), Nodes ),
% 	member( fo(Formula), Labels ),
% 	Formula =.. [_Pred | Args],
% 	member( wh(Wh), Args ),
% 	var( Wh ).

% move pointer to existing wh-node, remove wh-including formulae
wh_adjust( tree( Nodes, _Pointer ), tree( Nodes2, Node ) ) :-
	member( node( Node, Labels ), Nodes ),
	member( fo(wh(Wh)), Labels ),
	remove_wh( wh(Wh), Nodes, Nodes2 ).

% remove wh-including formulae, and change type to requirement
remove_wh( _Wh, [], [] ).
remove_wh( Wh, [node( Node, Labels1 ) | T1], [node( Node, [?ty(Type) | Labels2] ) | T2] ) :-
	select( fo(Formula), Labels1, Labels ),
	exact_sub_term( Wh, Formula ),
	!,
	select( ty(Type), Labels, Labels2 ),
	remove_wh( Wh, T1, T2 ).
remove_wh( Wh, [Node | T1], [Node | T2] ) :-
	remove_wh( Wh, T1, T2 ).

exact_sub_term( X1, X2 ) :-
	X1 == X2.
exact_sub_term( X1, Formula ) :-
	X1 \= Formula,
	Formula =.. [_Pred | Args],
	member( X2, Args ),
	exact_sub_term( X1, X2 ).


% initial forest for apposition, CRs, confirmations etc
cr_twa( twa( Adjusted, [], [], [], 0.0 ) ) :-
	tree_in_context( Tree ),
	complete( Tree ), % needs to be relaxed!
	cr_adjust( Tree, Adjusted ).

% move pointer to existing type-e node (actually should include at least e>t nodes)
% and open up the type requirement, allowing CRs, apposition etc via link-adjunction
cr_adjust( tree( Nodes, _Pointer ), Tree ) :-
	member( node( Node, Labels ), Nodes ),
	member( ty(e), Labels ),
	member( fo(_Formula), Labels ),
	rule( cr-link-adjunction, tree( Nodes, Node ), Tree ).
