%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% computational action application for Japanese
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% for efficiency, distinction between those rules that should always apply if they can,
% and those that aren't necessarily desirable
always_rules( [thinning, elimination, conjunction] ).
possible_rules( [completion, merge, star-adjunction, link-adjunction,
		 resolution, abstraction] ).


% rule/3 - rule( ?RuleName, +Tree1, ?Tree2 )
% Defines the computational action RuleName in terms of input tree Tree1 and
% resulting output tree Tree2

% % INTRODUCTION (English version)
% rule( introduction,
%       tree( Nodes, Pointer ), tree( [node( Pointer, [R0, R1 | Labels] )
% 				    | Nodes2], Pointer ) ) :-
% 	select( node( Pointer, Labels ), Nodes, Nodes2 ),
% 	member( ?ty(t), Labels ),
% 	dtr( 0, Pointer, D0 ),
% 	dtr( 1, Pointer, D1 ),
% 	R0 = ?([\/0],ty(e)),
% 	R1 = ?([\/1],ty(e>t)),
% 	\+ member( node( D0, _ ), Nodes ),
% 	\+ member( node( D1, _ ), Nodes ),
% 	\+ member( R0, Labels ),
% 	\+ member( R1, Labels ),
% 	dbg( 'comp: introduction at node ' ), dbg( Pointer ), dbgnl.

% % PREDICTION (general version)
% rule( prediction,
%       tree( Nodes, Pointer ), tree( [node( D, [?Req] ), node( Pointer, Labels2 )
% 				    | Nodes2], D ) ) :-
% 	select( node( Pointer, Labels ), Nodes, Nodes2 ),
% 	select( ?([\/N],Req), Labels, Labels2 ),
% 	dtr( N, Pointer, D ),
% 	\+ member( node( D, _ ), Nodes ),
% 	dbg( 'comp: prediction of ' ), dbg( D ), dbgnl.

% *-ADJUNCTION
rule( star-adjunction,
      tree( Nodes, Pointer ), tree( [node( Node, [?ty( e ), ?fixed] ) | Nodes], Node ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ?ty( t ), Labels ),
	unfixed_dtr( Pointer, Node ),
	\+ member( node( Node, _ ), Nodes ),
	dbg( 'comp: *-adjunction of ' ), dbg( Node ), dbgnl.

% % LINK-ADJUNCTION
% rule( link-adjunction,
%       tree( Nodes, Pointer ), tree( [node( LD, [?ty( t ), ?([\/ *],fo( Formula ))] ) | Nodes], LD ) ) :-
% 	member( node( Pointer, Labels ), Nodes ),
% 	member( ty( e ), Labels ),
% 	member( fo( Formula ), Labels ),
% 	linked_dtr( Pointer, LD ),
% 	\+ member( node( LD, _ ), Nodes ),
% 	dbg( 'comp: link-adjunction of ' ), dbg( LD ), dbgnl.


% THINNING
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?Req, Labels, Labels2 ),
	check( Req, tree( Nodes, Pointer ) ),
	dbg( 'comp: thin ' ), dbg( ?Req ), dbg( ' from ' ), dbg( Pointer ), dbgnl.

% THINNING
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?(Dir,Req), Labels, Labels2 ),
	check( (Dir,Req), tree( Nodes, Pointer ) ),
	dbg( 'comp: thin ' ), dbg( ?(Dir,Req) ), dbg( ' from ' ), dbg( Pointer ), dbgnl.

% ELIMINATION
rule( elimination,
      tree( Nodes, Pointer ), tree( [node( Pointer, [ty(Type), fo(Formula) | Labels3] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	(
	  \+ member( ty(_Type), Labels ),
	  Labels = Labels2
	;
	  select( ty(Type), Labels, Labels2 ),
	  member( ?fo(_), Labels2 )
	),
	(
	  \+ member( fo(_Formula), Labels2 ),
	  Labels3 = Labels2
	;
	  select( fo(Formula), Labels2, Labels3 ),
	  var( Formula )	% allow metavariable
	),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	member( node( D0, Labels0 ), Nodes ),
	member( ty(Type0), Labels0 ),
	member( fo(Formula0), Labels0 ),
	member( node( D1, Labels1 ), Nodes ),
	member( ty(Type1), Labels1 ),
	member( fo(Formula1), Labels1 ),
	beta_reduce( Type1:Formula1, Type0:Formula0, Type:Formula ),
	nonvar( Formula ), % do we still need this?
	dbg( 'comp: eliminate at ' ), dbg( Pointer ), dbg( ' -> ' ), dbg( Formula ), dbgnl.


% CONJUNCTION (yes, all right, I just made this one up)
rule( conjunction,
      tree( Nodes, Pointer ), tree( [node( Pointer, [ty( CType ), fo( CFormula ), +linked | Labels2] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	\+ member( +linked, Labels ), % to stop this happening more than once
	select( ty( Type ), Labels, Labels1 ),
	select( fo( Formula ), Labels1, Labels2 ),
	dtr( 0, Pointer, D0 ),
	member( node( D0, _Labels0 ), Nodes ),
	linked_dtr( D0, DL ),
	member( node( DL, LabelsL ), Nodes ),
	member( ty( TypeL ), LabelsL ),
	member( fo( FormulaL ), LabelsL ),
	conjoin( TypeL:FormulaL, Type:Formula, CType:CFormula ),
	dbg( 'comp: conjoin at ' ), dbg( Pointer ), dbg( ' -> ' ), dbg( Formula ), dbgnl.


% COMPLETION
rule( completion,
      tree( Nodes, Pointer ), tree( Nodes, Mother ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	\+ member( ?_Req, Labels ),
	(
	  dtr( _, Mother, Pointer );
	  linked_dtr( Mother, Pointer );
	  unfixed_dtr( Mother, Pointer )
	),
	dbg( 'comp: complete, up to ' ), dbg( Mother ), dbgnl.

% MERGE
rule( merge,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels ) | Nodes4], Pointer ) ) :-
	select( node( Pointer, FLabels ), Nodes, Nodes2 ),
	\+ member( ?fixed, FLabels ),
	\+ member( fo( _ ), FLabels ),
	select( node( Node, ULabels ), Nodes2, Nodes3 ),
	select( ?fixed, ULabels, ULabels2 ),
	member( fo( _ ), ULabels2 ),
	find_node( Pointer, [/\ *,\/ *], Node ), % check no links in between, i.e. same subtree
	union( FLabels, ULabels2, Labels ),
	compatible( Labels ),
	rename_nodes( Node, Pointer, Nodes3, Nodes4 ),
	dbg( 'comp: merge ' ), dbg( Node ), dbg( ' with ' ), dbg( Pointer ), dbgnl.

% RESOLUTION
rule( resolution,
      tree( Nodes, Pointer ), tree( [node( Pointer, [fo( Formula ) | Labels1] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?fo(_), Labels, Labels1 ),
	member( ty( Type ), Labels1 ),
	(
	  member( node( _Node, Labels2 ), Nodes );
	  node_in_context( node( _Node, Labels2 ) )
	),
	member( ty( Type ), Labels2 ),
	member( fo( Formula ), Labels2 ),
	union( Labels1, Labels2, ULabels ),
	compatible( ULabels ),
 	dbg( 'comp: resolve ' ), dbg( Pointer ), dbg( ' to ' ), dbg( Formula ), dbgnl.

% % RESOLUTION
% rule( resolution,
%       tree( Nodes, Pointer ), tree( Nodes, Pointer ) ) :-
% 	member( node( Pointer, Labels1 ), Nodes ),
% 	\+ member( ?fixed, Labels1 ), % stop false ambiguity merge/resolve vs resolve/merge
% 	member( ty( Type ), Labels1 ),
% 	member( fo( Var ), Labels1 ),
%  	var( Var ),
% 	member( node( _Node, Labels2 ), Nodes ),
% 	member( ty( Type ), Labels2 ),
% 	member( fo( Var ), Labels2 ),
%  	nonvar( Var ),
% 	union( Labels1, Labels2, Labels ),
% 	compatible( Labels ),
%  	dbg( 'comp: resolve ' ), dbg( Pointer ), dbg( ' to ' ), dbg( Var ), dbgnl.

% ABSTRACTION
rule( abstraction,
      tree( Nodes, Pointer ), tree( [node( Pointer, [fo( Formula ) | Labels1] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?fo(_), Labels, Labels1 ),
	member( ty( e>t ), Labels1 ), % could generalise
	node_in_context( node( Node2, Labels2 ) ),
	member( ty( t ), Labels2 ),
	member( fo( Formula2 ), Labels2 ),
	dtr( 0, Node2, Node3 ),
	node_in_context( node( Node3, Labels3 ) ),
	member( ty( e ), Labels3 ),
	member( fo( Formula3 ), Labels3 ),
	lambda_abstract( Formula3, Formula2, Formula ),
 	dbg( 'comp: abstract-resolve ' ), dbg( Pointer ), dbg( ' to ' ), dbg( Formula ), dbgnl.
