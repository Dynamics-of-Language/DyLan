%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% tree printer
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% modified by YS Feb 08 just the length of indent for unfixed / linked node

node_width( 10 ).
unfixed_node_indent( 80 ).%%% was 80, thought it too long....
linked_node_indent( 80 ).%%% same as above
trees_to_select( 4 ).
phpSyntaxTree_root( 'phpSyntaxTree/getgraph.png' ). % the URL for phpSyntaxTree

print_forest( Forest ) :-
	flag( print, all ),
	!,
	all_treetraces( Forest, AllTrees ),
	print_trees( AllTrees ).
print_forest( Forest ) :-
	flag( print, traces ),
	!,
	all_treetraces( Forest, AllTrees ),
	print_traces( AllTrees ).
print_forest( Forest ) :-
	flag( print, complete ),
	!,
	complete_treetraces( Forest, CompleteTrees ),
	print_trees( CompleteTrees ).
print_forest( Forest ) :-
	flag( print, best ),
	!,
	best_treetraces( Forest, BestTrees ),
	print_trees( BestTrees ).
print_forest( Forest ) :-
	flag( print, some ),
	!,
	complete_treetraces( Forest, CompleteTrees ),
	(
	  CompleteTrees = []
	->
	  all_treetraces( Forest, AllTrees ),
	  length( AllTrees, N ),
	  trees_to_select( S ),
	  (
	    N < S
	  ->
	    format( '(no complete trees, showing all ~w incomplete trees)', [N] ), newline,
	    print_trees( AllTrees )
	  ;
	    format( '(no complete trees, showing ~p incomplete trees of ~w)', [S, N] ), newline,
	    prefix( SomeTrees, AllTrees ),
	    length( SomeTrees, S ),
	    print_trees( SomeTrees )
	  )
	;
	  print( '(showing complete trees only)' ), newline,
	  print_trees( CompleteTrees )
	).
print_forest( Forest ) :-
	flag( print, none ),
	!,
	length( Forest, N ),
	complete_treetraces( Forest, CompleteTrees ),
	length( CompleteTrees, NC ),
	format( '~w trees, of which ~w complete.', [N, NC] ), newline.


print_trees( [] ) :-
	!,
	newline,
	print( '(no trees to print)' ), newline.
print_trees( Trees ) :-
	newline,
	print_trees( Trees, 0 ).

print_traces( [] ) :-
	!,
	newline,
	print( '(no trees to print)' ), newline.
print_traces( Trees ) :-
	newline,
	print_traces( Trees, 0 ).


print_trees( [], _N ).
print_trees( [H | T], N ) :-
	M is N + 1,
	print_tree( H, M ),
	!,
	print_trees( T, M ).

print_traces( [], _N ).
print_traces( [H | T], N ) :-
	M is N + 1,
	print_trace( H, M ),
	!,
	print_traces( T, M ).

print_tree( Tree-Trace-Prob, N ) :-
	print( 'Tree ' ), print( N ), print( ': ' ), newline,
	print( 'Trace: ' ), print( Trace ), newline,
	print( 'Prob: ' ), print( Prob ), newline,
	print_tree( Tree ).

print_trace( _Tree-Trace-Prob, N ) :-
	print( 'Tree ' ), print( N ), print( ': ' ), newline,
	print( 'Trace: ' ), print( Trace ), newline,
	print( 'Prob: ' ), print( Prob ), newline.


print_tree( Tree ) :-
	copy_term( Tree, Tree2 ),
	numbervars( Tree2, 0, _N ),
	root_node( R ),
	print_tree_copy( Tree2, R ).

print_tree_copy( tree( Nodes, Pointer ), R ) :-
	flag( format, php ),
	!,
	print_php_tree( R, Nodes, Pointer ).
print_tree_copy( tree( Nodes, Pointer ), R ) :-
	dtr( 0, R, Node0 ),
	count_leaves( Node0, Nodes, N0 ),
	node_width( W ),
	I is W * (N0+1),
	print_nodes( [I], [R], Nodes, Pointer ),
	!.
print_tree_copy( Tree, _R ) :-
	print( Tree ), newline.


print_nodes( [], [], _Nodes, _Pointer ) :-
	!.
print_nodes( I, N, Nodes, Pointer ) :-
	print_node_names( 0, I, N, Nodes, Pointer ),
	print_node_reqs( 0, I, N, Nodes ),
	print_node_sems( 0, I, N, Nodes ),
	extend_branches( 0, I, N, Nodes ),
	extend( I, N, Nodes, I2, N2 ),
	print_nodes( I2, N2, Nodes, Pointer ).


print_node_names( _Cursor, [], [], _Nodes, _Pointer ) :-
	newline.
print_node_names( Cursor, [I | IT], [N | NT], Nodes, N ) :-
	member( node( N, _Labels ), Nodes ),
	!,
	atom_concat( N, ' <>', NP ),
	In is I - Cursor,
	indent( In ), print( NP ),
	atom_length( NP, L ),
	NewCursor is I + L,
	print_node_names( NewCursor, IT, NT, Nodes, N ).
print_node_names( Cursor, [I | IT], [N | NT], Nodes, Pointer ) :-
	member( node( N, _Labels ), Nodes ),
	In is I - Cursor,
	indent( In ), print( N ),
	atom_length( N, L ),
	NewCursor is I + L,
	print_node_names( NewCursor, IT, NT, Nodes, Pointer ).


print_node_reqs( _Cursor, [], [], _Nodes ) :-
	newline.
print_node_reqs( Cursor, [I | IT], [N | NT], Nodes ) :-
	member( node( N, Labels ), Nodes ),
	( select( ty( _Ty ), Labels, Labels2 ); Labels2 = Labels ),
	( select( fo( _Fo ), Labels2, Labels3 ); Labels3 = Labels2 ),
	In is I - Cursor,
	indent( In ), print( Labels3 ),
	(
	  prolog_version( sicstus )
	->
	  write_to_chars( Labels3, RC ),
	  length( RC, L )
	;
	  sformat( RC, '~w', [Labels3] ),
	  string_length( RC, L )
	),
	NewCursor is I + L,
	print_node_reqs( NewCursor, IT, NT, Nodes ).


print_node_sems( _Cursor, [], [], _Nodes ) :-
	newline.
print_node_sems( Cursor, [I | IT], [N | NT], Nodes ) :-
	member( node( N, Labels ), Nodes ),
	( member( ty( Ty ), Labels ); Ty = _ ),
	( member( fo( Fo ), Labels ); Fo = _ ),
	In is I - Cursor,
	indent( In ), print( Ty:Fo ),
	(
	  prolog_version( sicstus )
	->
	  write_to_chars( Ty:Fo, SC ),
	  length( SC, L )
	;
	  sformat( SC, '~w', Ty:Fo ),
	  string_length( SC, L )
	),
	NewCursor is I + L,
	print_node_sems( NewCursor, IT, NT, Nodes ).


extend( [], [], _Nodes, [], [] ).
% no daughters
extend( [I | IT], [N | NT], Nodes, ID, ND ) :-
	extend( IT, NT, Nodes, IT2, NT2 ),
	find_dtrs( I, N, Nodes, IT2, NT2, ID, ND ).

find_dtrs( I, N, Nodes, I1, N1, I2, N2 ) :-
 	find_unfixed_dtr( I, N, Nodes, IUs, NUs ),
 	append( I1, IUs, I1aa ),
 	append( N1, NUs, N1aa ),
	find_linked_dtr( I, N, Nodes, I2s, N2s ),
	append( I1aa, I2s, I1a ),
	append( N1aa, N2s, N1a ),
	find_dtr( 1, I, N, Nodes, I1s, N1s ),
	append( I1s, I1a, I1b ),
	append( N1s, N1a, N1b ),
	find_dtr( 0, I, N, Nodes, I0s, N0s ),
	append( I0s, I1b, I2 ),
	append( N0s, N1b, N2 ).

find_dtr( D, _I, N, Nodes, [], [] ) :-
	dtr( D, N, ND ),
	\+ member( node( ND, _ ), Nodes ).
find_dtr( 0, I, N, Nodes, [ID], [ND] ) :-
	dtr( 0, N, ND ),
	member( node( ND, _ ), Nodes ),
	node_width( W ),
	ID is I - W.
find_dtr( 1, I, N, Nodes, [ID], [ND] ) :-
	dtr( 1, N, ND ),
	member( node( ND, _ ), Nodes ),
	node_width( W ),
	ID is I + W.

find_linked_dtr( _I, N, Nodes, [], [] ) :-
	linked_dtr( N, ND ),
	\+ member( node( ND, _ ), Nodes ).
find_linked_dtr( I, N, Nodes, [ID], [ND] ) :-
	linked_dtr( N, ND ),
	member( node( ND, _ ), Nodes ),
	linked_node_indent( W ),
	ID is I + W.

find_unfixed_dtr( _I, N, Nodes, [], [] ) :-
	unfixed_dtr( N, ND ),
	\+ member( node( ND, _ ), Nodes ).
find_unfixed_dtr( I, N, Nodes, [ID], [ND] ) :-
	unfixed_dtr( N, ND ),
	member( node( ND, _ ), Nodes ),
	unfixed_node_indent( W ),
	ID is I + W.


extend_branches( Cursor, I, N, Nodes ) :-
 	find_links( I, N, Nodes, ILS, ILE ),
	( ILS = [] -> true; print_links( ILS ) ),
 	find_unfixed_branches( I, N, Nodes, IUS, IUE ),
 	( IUS = [] -> true; print_unfixed_branches( IUS ) ),
	find_branches( I, N, Nodes, I2 ),
	minus( ILE, MIL ),
	minus( IUE, MIU ),
	append( I2, MIL, I3 ),
	append( I3, MIU, I4 ),
	extend_branches( Cursor, I4 ).

find_links( [], [], _Nodes, [], [] ).
find_links( [I | IT], [N | NT], Nodes, I2T, E2T ) :-
	find_linked_dtr( I, N, Nodes, [], _ ),
	find_links( IT, NT, Nodes, I2T, E2T ).
find_links( [I | IT], [N | NT], Nodes, [I | I2T], [E | E2T] ) :-
	find_linked_dtr( I, N, Nodes, [E], _ ),
	find_links( IT, NT, Nodes, I2T, E2T ).

print_links( [] ).
print_links( [H | T] ) :-
	linked_node_indent( W ),
	print_link( H, W, '=' ),
	print_links( T ).

find_unfixed_branches( [], [], _Nodes, [], [] ).
find_unfixed_branches( [I | IT], [N | NT], Nodes, I2T, E2T ) :-
	find_unfixed_dtr( I, N, Nodes, [], _ ),
	find_unfixed_branches( IT, NT, Nodes, I2T, E2T ).
find_unfixed_branches( [I | IT], [N | NT], Nodes, [I | I2T], [E | E2T] ) :-
	find_unfixed_dtr( I, N, Nodes, [E], _ ),
	find_unfixed_branches( IT, NT, Nodes, I2T, E2T ).

print_unfixed_branches( [] ) :-
	newline.
print_unfixed_branches( [H | T] ) :-
	unfixed_node_indent( W ),
	print_link( H, W, '-' ),
	print_unfixed_branches( T ).

find_branches( [], [], _Nodes, [] ).
find_branches( [_I | IT], [N | NT], Nodes, I2T ) :-
	is_leaf( N, Nodes ),
	!,
	find_branches( IT, NT, Nodes, I2T ).
find_branches( [I | IT], [_N | NT], Nodes, [I | I2T] ) :-
	find_branches( IT, NT, Nodes, I2T ).

extend_branches( _Cursor, [] ) :-
	!,
	newline.
extend_branches( Cursor, List ) :-
	extend_branches( Cursor, List, 0 ),
	extend_branches( Cursor, List, 1 ),
	extend_branches( Cursor, List, 2 ),
	extend_branches( Cursor, List, 3 ).


extend_branches( _Cursor, [], _N ) :-
	newline.
extend_branches( Cursor, [H | T], N ) :-
	H < 0,
	I is -H,
	In is I - Cursor,
	indent( In ),
	print( '|' ),
	NewCursor is In + 1,
	extend_branches( NewCursor, T, N ).
extend_branches( Cursor, [H | T], N ) :-
	I is H - Cursor,
	In is I - N,
	indent( In ),
	S is N * 2,
	print( '/' ), indent( S ), print( '\\' ),
	NewCursorA is I + N,
	NewCursor is NewCursorA + 1,
	extend_branches( NewCursor, T, N ).


print_link( A, B, Char ) :-
	A1 is A - 1,
	indent( A1 ),
	print( '\\' ),
	B1 is B - 1,
	print_links( B1, Char ),
	print( '\\' ),
	newline.

print_links( N, _Char ) :-
	N < 1,
	!.
print_links( N, Char ) :-
	write( Char ),
	M is N - 1,
	print_links( M, Char ).
	

is_leaf( Node, Nodes ) :-
	member( node( Node, _ ), Nodes ),
	dtr( 0, Node, Node0 ),
	dtr( 1, Node, Node1 ),
	\+ member( node( Node0, _ ), Nodes ),
	\+ member( node( Node1, _ ), Nodes ).


count_leaves( Node, Nodes, 0 ) :-
	\+ member( node( Node, _ ), Nodes ),
	!.
count_leaves( Node, Nodes, 1 ) :-
	is_leaf( Node, Nodes ),
	!.
count_leaves( Node, Nodes, N ) :-
	dtr( 0, Node, Node0 ),
	count_leaves( Node0, Nodes, N0 ),
	dtr( 1, Node, Node1 ),
	count_leaves( Node1, Nodes, N1 ),
	N is N0 + N1.


print_php_tree( Root, Nodes, Pointer ) :-
	php_tree_string( [Root], Nodes, Pointer, [String] ),
	phpSyntaxTree_root( PhpSyntaxTree ),
	format( '<img src="~p?data=~p&color=1&antialias=1&fontsize=10" />', [PhpSyntaxTree, String] ), newline.

php_tree_string( [], _Nodes, _Pointer, [] ).
php_tree_string( [N | Ns], Nodes, Pointer, [[S] | Str] ) :-
	find_dtrs( 0, N, Nodes, [], [], [], [] ), % leaf?
	!,
	php_node_string( N, Nodes, Pointer, S ),
	php_tree_string( Ns, Nodes, Pointer, Str ).
php_tree_string( [N | Ns], Nodes, Pointer, [[S | DtrStr] | Str] ) :-
	find_dtrs( 0, N, Nodes, [], [], _I2, Nodes2 ), % non-leaf?
	!,
	php_node_string( N, Nodes, Pointer, S ),
	php_tree_string( Nodes2, Nodes, Pointer, DtrStr ),
	php_tree_string( Ns, Nodes, Pointer, Str ).


php_node_string( N, Nodes, Pointer, Str ) :-
	php_node_name( N, Nodes, Pointer, Name ),
	php_node_sem( N, Nodes, Name, Sem ),
	php_node_reqs( N, Nodes, Sem, Str ).

php_node_name( N, Nodes, N, NP ) :-
	member( node( N, _Labels ), Nodes ),
	!,
	atom_concat( N, '<>', NP ).
php_node_name( N, Nodes, _Pointer, N ) :-
 	member( node( N, _Labels ), Nodes ).

php_node_sem( N, Nodes, In, Out ) :-
 	member( node( N, Labels ), Nodes ),
	( member( ty( Ty ), Labels ); Ty = _ ),
	( member( fo( Fo ), Labels ); Fo = _ ),
	sformat( Out, '~p<br>~p:~p', [In, Ty, Fo] ).

php_node_reqs( N, Nodes, In, Out ) :-
	member( node( N, Labels ), Nodes ),
	( select( ty( _Ty ), Labels, Labels2 ); Labels2 = Labels ),
	( select( fo( _Fo ), Labels2, Labels3 ); Labels3 = Labels2 ),
	sformat( Out, '~p<br>~p', [In, Labels3] ).


indent( N ) :-
	N < 1,
	!.
indent( N ) :-
	(
	  ( flag( format, html ); flag( format, php ) )
	->
	  write( '&nbsp;' )
	;
	  write( ' ' )
	),
	M is N - 1,
	indent( M ).

minus( [], [] ).
minus( [H | T], [MH | MT] ) :-
	MH is -H,
	minus( T, MT ).

newline :-
	(
	  ( flag( format, html ); flag( format, php ) )
	->
	  print( '<br>' ),
	  nl
	;
	  nl
	).
