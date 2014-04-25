%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% lexical action application
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%% modifired by YS, Feb 08, testing output

% exec/3 - exec( +Action, +Tree1, ?Tree2 )
% Executes Action on Tree1, resulting in Tree2

% recurse through a list of actions
exec( [], Tree, Tree ).
exec( [H | T], Tree1, Tree3 ) :-
	exec( H, Tree1, Tree2 ),
       %%% test output
	flag(test,Fl),test_output(Fl,H,Tree1,Tree2),
	exec( T, Tree2, Tree3 ).

% if-then-else
exec( ( if If, then Then, else Else ), Tree1, Tree2 ) :-
	(
	  check( If, Tree1 )
	->
	  exec( Then, Tree1, Tree2 )
	;
	  exec( Else, Tree1, Tree2 )
	).

% make node
exec( make( Dir ), tree( Nodes, Pointer ), tree( [node( Node, [] ) | Nodes], Pointer )  ) :-
	find_node( Pointer, Dir, Node ),
	( ( Dir = \/(_) ) -> bottom_check( tree( [node( Node, [] ) | Nodes], Pointer ) ); true ).

% go to node
exec( go( Dir ), tree( Nodes, Pointer1 ), tree( Nodes, Pointer2 )  ) :-
	member( node( Pointer2, _ ), Nodes ),
	find_node( Pointer1, Dir, Pointer2 ).

% go to first satisfactory node
exec( gofirst( Dir, Cond ), tree( Nodes, Pointer1 ), tree( Nodes, Pointer2 )  ) :-
	member( node( Pointer2, _ ), Nodes ),
	find_node( Pointer1, Dir, Pointer2 ),
	check( Cond, tree( Nodes, Pointer2 ) ).

% put label
exec( put( Label ), tree( Nodes1, Pointer ), tree( Nodes2, Pointer ) ) :-
	\+ is_list( Label ),
	member( node( Pointer, Labels ), Nodes1 ),
	compatible( [Label | Labels] ), % check we're not e.g. putting +male on a +female node
	remove_duplicates( [Label | Labels], Labels2 ),
	substitute( node( Pointer, Labels ), Nodes1,
		    node( Pointer, Labels2 ), Nodes2 ),
	( ( Label = ! ) -> bottom_check( tree( Nodes2, Pointer ) ) ; true ).

% put list of labels recursively
exec( put( [] ), Tree, Tree ).
exec( put( [H | T] ), Tree1, Tree3 ) :-
	exec( put( H ), Tree1, Tree2 ),
	exec( put( T ), Tree2, Tree3 ).

% put label at node
exec( put( Dir, Label ), tree( Nodes1, Pointer ), tree( Nodes2, Pointer ) ) :-
	find_node( Pointer, Dir, TempPointer ),
	exec( put( Label ), tree( Nodes1, TempPointer ), tree( Nodes2, TempPointer ) ).

% get fresh variable and put
exec( freshput( Type, Var, Label ), Tree1, Tree2 ) :-
	fresh_var( Type, Var ),
	exec( put( Label ), Tree1, Tree2 ).

% abort
exec( abort, _, _ ) :-
	fail.


% check/2 - check( +Tree, +Expr )
% Succeeds if Expr holds of Tree

% stop annoying infinite recursion when checking for any label existence
check( X, _Tree ) :-
	var( X ),
	!.

% recurse through a list of expressions
check( [], _Tree ).
check( [H | T], Tree ) :-
	check( H, Tree ),
	check( T, Tree ).

% check local label
check( Label, tree( Nodes, Pointer ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( Label, Labels ).

% check local feature allowing for feature hierarchy
check( +Feat, tree( Nodes, Pointer ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( +Feat2, Labels ),
	isa( Feat2, Feat ).

% check absence of remote node
check( ( Dir, Label ), tree( Nodes, Pointer ) ) :-
	\+ var( Label ),
	Label = !,
	!,
	\+ (
	     member( node( Node, _ ), Nodes ),
	     find_node( Pointer, Dir, Node )
	   ).

% check remote label
check( ( Dir, Label ), tree( Nodes, Pointer ) ) :-
	member( node( Node, _ ), Nodes ), % to prevent infinite finding ...
	find_node( Pointer, Dir, Node ),
	check( Label, tree( Nodes, Node ) ).

% hack for root node identification (as nodes not strictly labelled)
check( root, tree( _Nodes, Pointer ) ) :-
	root_node( Pointer ).

% SHOULD be able to do this, but we're actually using ! to represent [\/]! ...
% bottom never holds
%check( !, _Tree ) :-
%	fail.