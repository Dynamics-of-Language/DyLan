%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% computational action application for English
% (following Kempson, Meyer-Viol & Gabbay, 2001 - roughly)
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% for efficiency, distinction between those rules that should always apply if they can,
% and those that aren't necessarily desirable
always_rules( [thinning, elimination, conjunction] ).
possible_rules( [completion, introduction, prediction, pointer-movement,
		 merge, star-adjunction, link-adjunction, resolution] ).


% rule/3 - rule( ?RuleName, +Tree1, ?Tree2 )
% Defines the computational action RuleName in terms of input tree Tree1 and
% resulting output tree Tree2

% INTRODUCTION (English version)
% given ?ty(t), introduces requirements for suitable ?ty(e), ?ty(e>t) daughters
rule( introduction,
      tree( Nodes, Pointer ), tree( [node( Pointer, [R0, R1 | Labels] )
				    | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	member( ?ty(t), Labels ),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	R0 = ?([\/0],ty(e)),
	R1 = ?([\/1],ty(e>t)),
	\+ member( node( D0, _ ), Nodes ),
	\+ member( node( D1, _ ), Nodes ),
	\+ member( R0, Labels ),
	\+ member( R1, Labels ).

% % PREDICTION (general version)
% rule( prediction,
%       tree( Nodes, Pointer ), tree( [node( D, [?Req] ), node( Pointer, Labels2 )
% 				    | Nodes2], D ) ) :-
% 	select( node( Pointer, Labels ), Nodes, Nodes2 ),
% 	select( ?([\/N],Req), Labels, Labels2 ),
% 	dtr( N, Pointer, D ),
% 	\+ member( node( D, _ ), Nodes ).

% PREDICTION (English version for subject)
% given requirement for a 0 daughter, adds the node
rule( prediction,
      tree( Nodes, Pointer ), tree( [node( D0, [?Req0] ),
				     node( Pointer, Labels2 ) | Nodes2], D0 ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?([\/0],Req0), Labels, Labels2 ),
%	member( ?([\/1],_Req1), Labels ),
	dtr( 0, Pointer, D0 ),
%	dtr( 1, Pointer, D1 ),
%	unfixed_dtr( Pointer, UD ),
	\+ member( node( D0, _ ), Nodes ). % check not already done
%	\+ member( node( D1, _ ), Nodes ), % DON'T check no predicate (may be auxiliary)
%	member( node( UD, _ ), Nodes ),    % check unfixed node there (just to stop multiple derivations)

% PREDICTION (English version for predicate)
% given requirement for a 1-daughter, adds the node (if 0-daughter already present)
rule( prediction,
      tree( Nodes, Pointer ), tree( [node( D1, [?Req1] ),
				     node( Pointer, Labels2 ) | Nodes2], D1 ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?([\/1],Req1), Labels, Labels2 ),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	member( node( D0, _ ), Nodes ),    % check subject already there
	\+ member( node( D1, _ ), Nodes ). % check not already done

% PREDICTION (English version for predicate after auxiliary)
% given a suitable 1-daughter, allows pointer movement to it
rule( prediction,
      tree( Nodes, Pointer ), tree( Nodes, D1 ) ) :-
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	member( node( D0, _ ), Nodes ),  % check subject already there
	member( node( D1, L1 ), Nodes ), % predicate node already there?
	member( ?fo(_), L1 ).            % then check it was an auxiliary

% *-ADJUNCTION
% given a ?ty(t) node, adds an unfixed ?ty(e) node below it
rule( star-adjunction,
      tree( Nodes, Pointer ), tree( [node( Node, [?ty( e ), ?fixed] ) | Nodes], Node ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ?ty( t ), Labels ),
	%%% root_node( Pointer ), % currently restricted to root-node only
	                          %%% removed. with this object relative woudn't work
	\+ member( ?([\/0],ty(e)), Labels ), % (i.e. no introduction has taken place)
%	( Pointer = '0'; atom_concat( _, '2', Pointer ) ), % can't check singleton directly ...
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	unfixed_dtr( Pointer, Node ),
	\+ member( node( D0, _ ), Nodes ),
	\+ member( node( D1, _ ), Nodes ),
	\+ member( node( Node, _ ), Nodes ).

% LINK-ADJUNCTION
% given a ty(e) node, adds a LINK to a new ?ty(t) node
rule( link-adjunction,
      tree( Nodes, Pointer ), tree( [node( LD, [?ty( t ), ?([\/(*)],fo( Formula ))] ) | Nodes], LD ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ty( e ), Labels ),
	member( fo( Formula ), Labels ),
	linked_dtr( Pointer, LD ),
	\+ member( node( LD, _ ), Nodes ).


% THINNING
% removes a satisfied (local) requirements
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?Req, Labels, Labels2 ),
	check( Req, tree( Nodes, Pointer ) ).

% THINNING
% removes a satisfied (remote) requirements
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?(Dir,Req), Labels, Labels2 ),
	check( (Dir,Req), tree( Nodes, Pointer ) ).

% ELIMINATION
% given suitable fo() values at 0- and 1-daughters, builds new fo() value by beta-reduction
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
	nonvar( Formula ). % do we still need this?


% CONJUNCTION (yes, all right, I just made this one up)
% LINK compilation
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
	conjoin( TypeL:FormulaL, Type:Formula, CType:CFormula ).


% COMPLETION
% given a node with no outstanding requirements, allows pointer movement up to mother
rule( completion,
      tree( Nodes, Pointer ), tree( Nodes, Mother ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ty(_Type), Labels ),
	%\+ member( ?fo(_), Labels ),
	\+ ( member( ?Req, Labels ), Req \= fixed ),
	%member( fo(_Formula), Labels ), % check formal definition
	(
	  dtr( _, Mother, Pointer );
	  linked_dtr( Mother, Pointer );
	  unfixed_dtr( Mother, Pointer )
	).

% MERGE
% merges an unfixed node with another compatible fixed node
rule( merge,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels ) | Nodes4], Pointer ) ) :-
	select( node( Pointer, FLabels ), Nodes, Nodes2 ),
	\+ member( ?fixed, FLabels ),
	\+ member( fo( _ ), FLabels ),
	select( node( Node, ULabels ), Nodes2, Nodes3 ),
	select( ?fixed, ULabels, ULabels2 ),
	member( fo( _ ), ULabels2 ),
	find_node( Pointer, [/\(*),\/(*)], Node ), % check no links in between, i.e. same subtree
	union( FLabels, ULabels2, Labels ),
	compatible( Labels ),
	rename_nodes( Node, Pointer, Nodes3, Nodes4 ).


% POINTER MOVEMENT
% version 1 for ditransitive verbs (in the 'john gives mary ... a present' case)
% (the 'john gives a present ... to mary' case is assumed
% to be taken care of via the lexical entry for 'to')
rule( pointer-movement,
      tree( Nodes, Pointer ), tree( Nodes, Node0 ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ?ty(e>t), Labels ),
	find_node( Pointer, [\/1,\/1], Node11 ),
	member( node( Node11, Labels11 ), Nodes ),
	member( ty(e>(e>(e>t))), Labels11 ),
	find_node( Pointer, [\/1], Node1 ),
	member( node( Node1, Labels1 ), Nodes ),
	member( ty(e>(e>t)), Labels1 ),
	find_node( Pointer, [\/0], Node0 ),
	member( node( Node0, Labels0 ), Nodes ),
	member( ?ty(e), Labels0 ).



% RESOLUTION
% resolves an outstanding fo() value at a typed node from another node in context
% i.e. strict readings of pronouns & VP ellipsis
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
	Formula \= e(_Var), % don't want to take eps-term variable, only whole term
	union( Labels1, Labels2, ULabels ),
	compatible( ULabels ).

% % RESOLUTION
% % old intrasentential version
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
% 	compatible( Labels ).

% RESOLUTION (for subformulae - i.e. anaphoric element within e.g. determiner)
rule( resolution,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels1 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?subfo(SType,SLabels,SFormula), Labels, Labels1 ),
	(
	  member( node( Node, Labels2 ), Nodes ),
	  Node \= Pointer; % check not resolving to own super-formula
	  node_in_context( node( Node, Labels2 ) )
	),
	member( ty( SType ), Labels2 ),
	member( fo( SFormula ), Labels2 ),
	SFormula \= e(_Var), % don't want to take eps-term variable, only whole term
	compatible_features( SLabels, Labels2 ).

% RESOLUTION
% resolves an outstanding fo() value at a typed node by reusing actions in context
% i.e. sloppy readings of pronouns & VP ellipsis
rule( resolution,
      tree( Nodes, Pointer ), tree( Nodes2, Pointer2 ) ) :-
	flag( ellip, all ),
	member( node( Pointer, Labels ), Nodes ),
	member( ?fo(_), Labels ),
	member( ty( Type ), Labels ),
	words_in_context( Words, Actions ),
	Actions = [(if If, then Then, else Else) | A],
	select( ?ty( Type ), If, If2 ),
	apply( Words, [(if If2, then Then, else Else) | A],
	       tree( Nodes, Pointer ), tree( Nodes2, Pointer2 ), _Trace, _Prob ),
	member( node( Pointer, Labels2 ), Nodes2 ),
	member( ty( Type ), Labels2 ).

/*
% ABSTRACTION
% Dalrymple, Shieber, Pereira-style VP ellipsis: abstraction from previous node in context
% NOT CURRENTLY USED - prefer action re-use instead
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
	lambda_abstract( Formula3, Formula2, Formula ).
*/
