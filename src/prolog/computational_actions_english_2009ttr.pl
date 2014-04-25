%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% computational action definitions for English
% (following recent developments on DynDial project)
%
% Matthew Purver, 2009
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% for efficiency, distinction between those rules that should always apply if they can,
% and those that aren't necessarily desirable
always_rules( [thinning, elimination, link-evaluation] ).
possible_rules( [completion, introduction, prediction, anticipation, empty-complementiser,
		 merge, link-adjunction, substitution, star-adjunction, late-star-adjunction] ).


% rule/3 - rule( ?RuleName, +Tree1, ?Tree2 )
% Defines the computational action RuleName in terms of input tree Tree1 and
% resulting output tree Tree2


% INTRODUCTION (English version, for ?ty(t) node only)
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


% PREDICTION (English version, for ?ty(t) node only)
% given requirements for 0/1 daughters, adds the nodes and moves pointer to 0-daughter
rule( prediction,
      tree( Nodes, Pointer ), tree( [node( D0, [?Req0] ),
				     node( D1, [?Req1] ),
				     node( Pointer, Labels2 ) | Nodes2], D0 ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	member( ?ty(t), Labels ),
	select( ?([\/0],Req0), Labels, Labels1 ),
	select( ?([\/1],Req1), Labels1, Labels2 ),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	\+ member( node( D0, _ ), Nodes ), % check not already done
	\+ member( node( D1, _ ), Nodes ), % check no predicate
	unfixed_dtr( Pointer, UD ),
	member( node( UD, _ ), Nodes ).    % check unfixed node there (just to stop multiple derivations)

% PREDICTION (English version, for ?ty(t) node only) - version for use after sentence-initial auxiliary
% given requirement for 0 daughter only, adds the node and moves pointer to 0-daughter
rule( prediction,
      tree( Nodes, Pointer ), tree( [node( D0, [?Req0] ),
				     node( Pointer, Labels2 ) | Nodes2], D0 ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	member( ?ty(t), Labels ),
	select( ?([\/0],Req0), Labels, Labels2 ),
	\+ member( ?([\/1],_Req1), Labels ),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	\+ member( node( D0, _ ), Nodes ), % check not already done
	member( node( D1, _ ), Nodes ). % check predicate already there (from auxiliary)


% ANTICIPATION
% given a daughter with an unsatisfied requirement, moves pointer down to it
rule( anticipation,
      tree( Nodes, Pointer ), tree( Nodes, Dtr ) ) :-
	member( node( Pointer, _Labels ), Nodes ),
	dtr( _, Pointer, Dtr ),
	member( node( Dtr, DLabels ), Nodes ),
	member( ?_Req, DLabels ).


% THINNING
% removes a satisfied (local) requirement
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?Req, Labels, Labels2 ),
	check( Req, tree( Nodes, Pointer ) ).

% THINNING
% removes a satisfied (remote) requirement
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?(Dir,Req), Labels, Labels2 ),
	check( (Dir,Req), tree( Nodes, Pointer ) ).

% THINNING
% moves a remote label for later satisfaction
rule( thinning,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels2 ), node( OtherNode, [Req | OtherLabels] ) | Nodes3], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( (Dir,Req), Labels, Labels2 ),
	find_node( Pointer, Dir, OtherNode ),
	select( node( OtherNode, OtherLabels ), Nodes2, Nodes3 ).
	%member( ?(Req), OtherLabels ).


% COMPLETION
% given a node with no outstanding requirements, allows pointer movement up to mother
rule( completion,
      tree( Nodes, Pointer ), tree( Nodes, Mother ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ty(_Type), Labels ),
	%\+ member( ?fo(_), Labels ), % no formula req?: not in formal definition
	%member( fo(_Formula), Labels ), % require formula?: not in formal definition
	\+ ( member( ?Req, Labels ), Req \= fixed ), % no req except unfixed?: not in formal definition
%	\+ ( member( ?Req, Labels ), Req \= fixed, Req \= fo(_) ), % no req except unfixed or formula?: not in formal definition
	(
	  dtr( _, Mother, Pointer );
	  linked_dtr( Mother, Pointer );
	  unfixed_dtr( Mother, Pointer )
	).


% ELIMINATION
% given suitable ty(),fo() values and no outstanding requirements at 0- and 1-daughters,
% builds new fo() value by beta-reduction and bg() concatenation
rule( elimination,
      tree( Nodes, Pointer ), tree( [node( Pointer, [ty(Type), fo(Formula), bg(Bg) | Labels2] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?ty(Type), Labels, Labels2 ),
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	member( node( D0, Labels0 ), Nodes ),
	member( ty(Type0), Labels0 ),
	member( fo(Formula0), Labels0 ),
	( member( bg(Bg0), Labels0 ) ; Bg0 = [] ),
	\+ member( ?_Req0, Labels0 ),
	member( node( D1, Labels1 ), Nodes ),
	member( ty(Type1), Labels1 ),
	member( fo(Formula1), Labels1 ),
	( member( bg(Bg1), Labels1 ) ; Bg1 = [] ),
	\+ member( ?_Req1, Labels1 ),
	beta_reduce( Type1:Formula1, Type0:Formula0, Type:Formula ),
	append( Bg0, Bg1, Bg ).


% *-ADJUNCTION
% given a ?ty(t) node, adds an unfixed ?ty(e) node below it
rule( star-adjunction,
      tree( Nodes, Pointer ), tree( [node( Node, [?ty(e), ?fixed] ) | Nodes], Node ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ?ty(t), Labels ),
	\+ member( ?([\/0],ty(e)), Labels ), % (i.e. no introduction has taken place)
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	unfixed_dtr( Pointer, Node ),
	\+ member( node( D0, _ ), Nodes ), % no existing 0-daughter
	\+ member( node( D1, _ ), Nodes ), % no existing 1-daughter
	\+ member( node( Node, _ ), Nodes ), % no existing unfixed daughter
	\+ ( member( node( UNode, ULabels ), Nodes ),
	       member( ?fixed, ULabels ),  % only one unfixed node in this sub-tree
	       find_node( Pointer,  [/\(*),\/(*)], UNode ) ).


% MERGE
% merges an unfixed node with another compatible fixed node
rule( merge,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels ) | Nodes4], Pointer ) ) :-
	select( node( Pointer, FLabels ), Nodes, Nodes2 ),
	\+ member( ?fixed, FLabels ),
	\+ member( fo(_), FLabels ),
	select( node( Node, ULabels ), Nodes2, Nodes3 ),
	select( ?fixed, ULabels, ULabels2 ),
	member( fo(_), ULabels2 ),
	find_node( Pointer, [/\(*),\/(*)], Node ), % check no links in between, i.e. same subtree
	union( FLabels, ULabels2, Labels ),
	compatible( Labels ),
	rename_nodes( Node, Pointer, Nodes3, Nodes4 ).


% LINK-ADJUNCTION
% given a ty(e) node, adds a LINK to a new ?ty(t) node
rule( link-adjunction,
      tree( Nodes, Pointer ), tree( [node( LD, [?ty(t), ?([\/(*)],fo(Formula))] ) | Nodes], LD ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ty(e), Labels ),
	member( fo(Formula), Labels ),
	linked_dtr( Pointer, LD ),
	\+ member( node( LD, _ ), Nodes ).

% CR-LINK-ADJUNCTION
% given a ty(e) node, adds a LINK to a new ?ty(e) node
rule( cr-link-adjunction,
      tree( Nodes, Pointer ), tree( [node( LD, [?ty(e), ?fo(Formula)] ) | Nodes], LD ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	member( ty(e), Labels ),
	member( fo(Formula), Labels ),
	linked_dtr( Pointer, LD ),
	\+ member( node( LD, _ ), Nodes ).


% EMPTY-COMPLEMENTISER
% (in the book, this is a lexical entry for a null string)
% given an unfixed node below a LINK from a ty(e) node, copies formula
rule( empty-complementiser,
      tree( Nodes, Pointer ), tree( [node( Pointer, [ty(e), fo(Formula) | Labels3] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?ty(e), Labels, Labels3 ),
	member( ?fixed, Labels ),
	member( node( Node1, Labels1 ), Nodes2 ),
	find_node( Pointer, [/\(*),/\2], Node1 ),
	member( fo(Formula), Labels1 ),
	member( node( Node2, Labels2 ), Nodes2 ),
	find_node( Pointer, [/\(*),/\2,/\0], Node2 ),
	member( ?ty(cn), Labels2 ).


% LATE-*-ADJUNCTION
% given a ty(e/t) node, adds an unfixed ?ty(e/t) node below it
rule( late-star-adjunction,
      tree( Nodes, Pointer ), tree( [node( Node, [?ty(Type), ?fixed] ) | Nodes], Node ) ) :-
	member( node( Pointer, Labels ), Nodes ),
	root_node( Root ),
	find_node( Pointer, [/\(*)], Root ), % not on a LINKed subtree
	member( ty(Type), Labels ),
	member( Type, [e, t] ), % book allows general type
	dtr( 0, Pointer, D0 ),
	dtr( 1, Pointer, D1 ),
	unfixed_dtr( Pointer, Node ),
	\+ member( node( D0, _ ), Nodes ), % no existing 0-daughter
	\+ member( node( D1, _ ), Nodes ), % no existing 1-daughter
	\+ member( node( Node, _ ), Nodes ), % no existing unfixed daughter
	\+ ( member( node( UNode, ULabels ), Nodes ),
	       member( ?fixed, ULabels ),  % only one unfixed node in this sub-tree
	       find_node( Pointer,  [/\(*),\/(*)], UNode ) ).


% LINK-EVALUATION
% at a node with a LINKed subtree, identify fo() and concatenate bg()
rule( link-evaluation,
      tree( Nodes, Pointer ), tree( [node( Pointer, [bg(Bg), +linked | Labels0] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	\+ member( +linked, Labels ), % to stop this happening more than once
	member( ty(Type), Labels ), % book restricts to ty(t) or ty(cn)
	member( fo(Formula), Labels ),
	( select( bg(Bg0), Labels,  Labels0 ) ; ( Bg0 = [], Labels0 = Labels ) ),
	linked_dtr( Pointer, DL ),
	member( node( DL, LabelsL ), Nodes ),
	member( ty(Type), LabelsL ), % book restricts to ty(t) or ty(cn)
	member( fo(Formula), LabelsL ),
	( member( bg(BgL), LabelsL ) ; BgL = [] ),
	append( Bg0, BgL, Bg ).


% SUBSTITUTION
% resolves an outstanding fo() value at a typed node from another node in context
% i.e. strict readings of pronouns & VP ellipsis
rule( substitution,
      tree( Nodes, Pointer ), tree( [node( Pointer, [fo(Formula) | Labels1] ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?fo(FoVar), Labels, Labels1 ),
	var( FoVar ),
	member( ty(Type), Labels1 ),
	(
	  member( node( _Node, Labels2 ), Nodes );
	  node_in_context( node( _Node, Labels2 ) )
	),
	member( ty(Type), Labels2 ),
	member( fo(Formula), Labels2 ),
	Formula \= e(_Var), % don't want to take eps-term variable, only whole term
	union( Labels1, Labels2, ULabels ),
	compatible( ULabels ).

% SUBSTITUTION (for subformulae - i.e. anaphoric element within e.g. determiner)
rule( substitution,
      tree( Nodes, Pointer ), tree( [node( Pointer, Labels1 ) | Nodes2], Pointer ) ) :-
	select( node( Pointer, Labels ), Nodes, Nodes2 ),
	select( ?subfo(SType,SLabels,SFormula), Labels, Labels1 ),
	(
	  member( node( Node, Labels2 ), Nodes ),
	  Node \= Pointer; % check not resolving to own super-formula
	  node_in_context( node( Node, Labels2 ) )
	),
	member( ty(SType), Labels2 ),
	member( fo(SFormula), Labels2 ),
	SFormula \= e(_Var), % don't want to take eps-term variable, only whole term
	compatible_features( SLabels, Labels2 ).

% SUBSTITUTION
% resolves an outstanding fo() value at a typed node by reusing actions in context
% i.e. sloppy readings of pronouns & VP ellipsis
rule( substitution,
      tree( Nodes, Pointer ), tree( Nodes2, Pointer2 ) ) :-
	flag( ellip, all ),
	member( node( Pointer, Labels ), Nodes ),
	member( ?fo(FoVar), Labels ),
	var( FoVar ),
	member( ty(Type), Labels ),
	words_in_context( Words, Actions ),
	Actions = [(if If, then Then, else Else) | A],
	select( ?ty(Type), If, If2 ),
	apply( Words, [(if If2, then Then, else Else) | A],
	       tree( Nodes, Pointer ), tree( Nodes2, Pointer2 ), _Trace, _Prob ),
	member( node( Pointer, Labels2 ), Nodes2 ),
	member( ty(Type), Labels2 ).
