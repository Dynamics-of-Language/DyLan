%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% tree handling
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% forest definitions
all_trees( Forest, Trees ) :-
	findall( Tree,
		 member( twa( Tree, _Words, _Actions, _Trace, _Prob ), Forest ),
		 Trees ).

complete_trees( Forest, CompleteTrees ) :-
	findall( Tree,
		 (
		   member( twa( Tree, _Words, _Actions, _Trace, _Prob ), Forest ),
		   complete( Tree )
		 ), CompleteTrees ).

all_treetraces( Forest, TreeTraces ) :-
	findall( Tree-Trace-Prob,
		 member( twa( Tree, _Words, _Actions, Trace, Prob ), Forest ),
		 TreeTraces ).

complete_treetraces( Forest, CompleteTreeTraces ) :-
	findall( Tree-Trace-Prob,
		 (
		   member( twa( Tree, _Words, _Actions, Trace, Prob ), Forest ),
		   complete( Tree )
		 ), CompleteTreeTraces ).

best_treetraces( [], [] ).
best_treetraces( Forest, [Tree-Trace-Prob] ):-
	best( Forest, twa( Tree, _Words, _Actions, Trace, Prob ) ).

complete_forest( Forest, CompleteForest ) :-
	findall( twa( Tree, Words, Actions, Trace, Prob ),
		 (
		   member( twa( Tree, Words, Actions, Trace, Prob ), Forest ),
		   complete( Tree )
		 ), CompleteForest ).


% completeness definition:
% pointer at root node, no outstanding requirements
complete( tree( Nodes, Pointer ) ) :-
	root_node( Pointer ),
	\+ (
	     member( node( _Node, Labels ), Nodes ),
	     ( member( ?_Req, Labels ); member( ?(_Dir,_Req), Labels ) )
	   ).

% subsumption definition:
% tree 2 has no node for which we cannot find a compatible node in tree 1
% (note that this means we're actually checking tree2 subsumes tree1)
subsumes( tree( Nodes1, _Pointer1 ), tree( Nodes2, _Pointer2 ) ) :-
	\+ var( Nodes1 ),
	\+ (
	     member( Node2, Nodes2 ),
	     \+ (
		  member( Node1, Nodes1 ),
		  compatible_nodes( Node1, Node2 )
		)
	   ).
% special case: variable tree1 will trivially be subsumed by everything -
% not formally correct but handy for random corpus generation!
subsumes( Tree1, _ ) :-
	var( Tree1 ).


compatible_nodes( node( Name1, Labels1 ), node( Name2, Labels2 ) ) :-
	(
	  Name1 = Name2;
	  (
%	    member( ?fixed, Labels2 ),
	    unfixed_dtr( U ),
	    sub_atom( Name2, _Before, _Length, _After, U ),
	    find_node( Name1, [/\(*),\/(*)], Name2 ) % check no links in between, i.e. same subtree
	  )
	),
	union( Labels1, Labels2, Labels ),
	compatible( Labels ),
	\+ extra_features( Labels1, Labels2 ).

	

% recursive type definition
type( e ).
type( t ).
type( cn ).
type( A > B ) :-
	type( A ),
	type( B ).


% node names
root_node( '0' ).
fixed_dtrs( [0, 1] ).
linked_dtr( '2' ).
unfixed_dtr( '*' ).

% initial root-only tree
root( tree( [node( N, [?ty(t)] )], N ) ) :-
	root_node( N ).

% initial forest (both new root, and forest given by context)
root_forest( Forest ) :-
	root( Root ),
	findall( twa( Tree, [], [], Trace, Prob ),
		 adjust( Root, Tree, Trace, Prob ),
		 RootForest ),
	forest_in_context( ContextForest ),
	append( RootForest, ContextForest, Forest ).

% just for convenience
pointer( tree( _Nodes, Pointer ), Pointer ).

% mother-daughter relation definition
dtr( Num, Atom1, Atom2 ) :-
	fixed_dtrs( Dtrs ),
	member( Num, Dtrs ),
	number_chars( Num, Chars ),
	atom_chars( Char, Chars ),
	atom_concat( Atom1, Char, Atom2 ),
	atom_length( Atom1, L ),
	L > 0.

% link mother-daughter relation definition
linked_dtr( Num, Atom1, Atom2 ) :-
	number_chars( Num, Chars ),
	atom_chars( Dtr, Chars ),
	linked_dtr( Dtr ),
	atom_concat( Atom1, Dtr, Atom2 ),
	atom_length( Atom1, L ),
	L > 0.
linked_dtr( Atom1, Atom2 ) :-
	linked_dtr( Dtr ),
	atom_concat( Atom1, Dtr, Atom2 ),
	atom_length( Atom1, L ),
	L > 0.

% unfixed mother-daughter relation definition
unfixed_dtr( Atom1, Atom2 ) :-
	unfixed_dtr( Dtr ),
	atom_concat( Atom1, Dtr, Atom2 ),
	atom_length( Atom1, L ),
	L > 0.


% node handling
find_node( N, [], N ).
find_node( N1, [Dir | T], N3 ) :-
	(
	  var( N1 );
	  member( Dir, [\/(*), \/_N*] )
	),
	!,
	find_node( N2, T, N3 ),
	find_node( N1, Dir, N2 ).
find_node( N1, [Dir | T], N3 ) :-
	find_node( N1, Dir, N2 ),
	find_node( N2, T, N3 ).

find_node( N1, \/Dir, N2 ) :-
	find_node( N2, /\Dir, N1 ).
find_node( N1, /\, N2 ) :-
	dtr( _, N2, N1 ).

find_node( N1, /\N, N2 ) :-
	var( N ),
	!,
	(
	  dtr( _D, N2, N1 );
	  unfixed_dtr( N2, N1 )
	).
find_node( N1, /\N, N2 ) :-
	number( N ),
	(
	  dtr( N, N2, N1 );
	  linked_dtr( N, N2, N1 )
	).

% % /\* is an atom in SICStus, but upsets SWI as it appears to contain a comment symbol
% find_node( N1, /\*, N2 ) :-
% 	find_node( N1, /\ *, N2 ).
% % as is \/*
% find_node( N1, \/*, N2 ) :-
% 	find_node( N1, \/ *, N2 ).
% this version interprets arrow* as "arrow kleene star"
find_node( N1, /\(*), N2 ) :-
	find_node( N1, /\_N*, N2 ).
% this version interprets arrow* as "arrow unfixed dtr"
%find_node( N1, /\(*), N2 ) :-
%	unfixed_dtr( N2, N1 ).
find_node( N1, /\_N*, N1 ).
find_node( N1, /\N*, N3 ) :-
	nonvar( N1 ),
	find_node( N1, /\N, N2 ),
	find_node( N2, /\N*, N3 ).
find_node( N1, /\N*, N3 ) :-
	var( N1 ),
	find_node( N2, /\N*, N3 ),
	find_node( N1, /\N, N2 ).


union( L, R, U ) :-
	member( M, L ),
	select( M, R, R2 ),
	!,
	union( L, R2, U ).
union( L, R, U ) :-
	append( L, R, U ).

/*
subset( [], _B ).
subset( S, B ) :-
	member( M, B ),
	select( M, S, S2 ),
	subset( S2, B ).
*/

% tree check: if a bottom restriction at the pointer, check no lower nodes
bottom_check( tree( Nodes, Pointer ) ) :-
	\+ (
	     member( node( Pointer, Labels1 ), Nodes ),
	     member( !, Labels1 ),
	     member( node( Node2, _Labels2 ), Nodes ),
	     find_node( Pointer, \/(*), Node2 ),
	     Node2 \= Pointer
	   ).

% node check: no downwards labels together with a bottom restriction
bottom_check( Labels ) :-
	is_list( Labels ),
	\+ (
	     member( !, Labels ), % bottom restriction label ...
	     member( ( \/(_Dir), _Label ), Labels ) % ... and a remote downwards label
	   ).

% check for e.g. +q present when not wanted
extra_features( Labels1, Labels2 ) :-
	member( +Label, Labels2 ),
	\+ member( +Label, Labels1 ).

% check for incompatible pairs
compatible( [] ).
compatible( Labels ) :-
	bottom_check( Labels ),
	compatible_types( Labels ),
	compatible_formulae( Labels ),
	compatible_features( Labels, Labels ).

% no two type labels/requirements with different types
compatible_types( Labels ) :-
	\+ (
	     ( member( ?ty( Type1 ), Labels ); member( ty( Type1 ), Labels ) ),
	     ( member( ?ty( Type2 ), Labels ); member( ty( Type2 ), Labels ) ),
	     Type1 \= Type2
	   ).

% no two non-unifiable formula labels
compatible_formulae( Labels ) :-
	\+ (
	     member( fo( Formula1 ), Labels ),
	     member( fo( Formula2 ), Labels ),
	     Formula1 \= Formula2
	   ).

% no two incompatible +features
compatible_features( Labels1, Labels2 ) :-
	\+ (
	     member( +Feature1, Labels1 ),
	     member( +Feature2, Labels2 ),
	     (
	       incompatible_pair( Feature1, Feature2 );
	       incompatible_pair( Feature2, Feature1 )
	     )
	   ).

% semantic class
incompatible_pair( male, female ).
incompatible_pair( per, obj ).
incompatible_pair( per, loc ).
incompatible_pair( per, tim ).
incompatible_pair( per, trans ).
incompatible_pair( tim, loc ).
incompatible_pair( tim, trans ).
incompatible_pair( loc, trans ).
incompatible_pair( addr, _ ).
incompatible_pair( spkr, _ ).
% tense
incompatible_pair( pres, past ).
incompatible_pair( fin, inf ).
incompatible_pair( fin, ing ).
incompatible_pair( inf, ing ).
% person
incompatible_pair( sing, plur ).
incompatible_pair( s3, nons3 ).
% deal with hierarchy
incompatible_pair( X, Y ) :-
	isa( X, X1 ),
	incompatible_pair( X1, Y ).
incompatible_pair( X, Y ) :-
	isa( Y, Y1 ),
	incompatible_pair( X, Y1 ).

isa( X, Y ) :-
	isa0( X, Y ).
isa( X, Z ) :-
	isa0( X, Y ),
	isa( Y, Z ).

% semantic class
isa0( male, per ).
isa0( female, per ).
isa0( addr, per ).
isa0( spkr, per ).

% tense
isa0( pres, fin ).
isa0( past, fin ).

% person
isa0( s1, sing ).
isa0( s2, sing ).
isa0( s3, sing ).
isa0( s1, nons3 ).
isa0( s2, nons3 ).
isa0( plur, nons3 ).
isa0( s3, person ).
isa0( nons3, person ).
