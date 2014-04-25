%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% computational action application
%
% Matthew Purver, 2009
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%% modified by YS for test-outputting, Jan 08

% adjust/4 - adjust( +Tree1, ?Tree2, -Trace, -Prob )
% Applies computational actions to Tree1, resulting in Tree2 (with Trace, NON-normalised Prob)

% forcibly apply as many thinning / elimination rules as possible
% (this isn't really necessary, it's just to cut down on the number of partial trees in memory)
adjust( Tree1, Tree3, [Rule-Pointer | Trace], Prob ) :-
	always( Tree1, Tree2, Rule ),
	pointer( Tree1, Pointer ),
	!,
	comp_prob( Rule, Tree1, Prob1 ),
%	print( 'Trace: ' ), print(Rule-Pointer), newline,
	adjust( Tree2, Tree3, Trace, Prob2 ),
	Prob is Prob1 + Prob2.
% optionally apply any number of other rules
adjust( Tree1, Tree3, [Rule-Pointer | Trace], Prob ) :-
	possible( Tree1, Tree2, Rule ),
	pointer( Tree1, Pointer ),
	comp_prob( Rule, Tree1, Prob1 ),
%	print( 'Trace: ' ), print(Rule-Pointer), newline,
	adjust( Tree2, Tree3, Trace, Prob2 ),
	Prob is Prob1 + Prob2.
% or do nothing
adjust( Tree, Tree, [], 0.0 ).


% for efficiency, distinction between those rules that should always apply if they can,
% and those that aren't necessarily desirable

always( Tree1, Tree2, Rule ) :-
	always_rules( Rules ),
	member( Rule, Rules ),
	rule( Rule, Tree1, Tree2 ),
	%%% test-print the process if the test flag is set
	%%% test_output/4 in utils.pl
	flag(test,T),test_output(T,Rule,Tree1,Tree2).


possible( Tree1, Tree2, Rule ) :-
	possible_rules( Rules ),
	member( Rule, Rules ),
	rule( Rule, Tree1, Tree2 ),
	%%% test-print the process if the test flag is set
	flag(test,T),test_output(T,Rule,Tree1,Tree2).



rename_nodes( _Old, _New, [], [] ).
rename_nodes( Old, New, [node( O, Labels ) | OT], [node( N, Labels ) | NT] ) :-
	rename_node( Old, New, O, N ),
	rename_nodes( Old, New, OT, NT ).

rename_node( Old, _New, O, O ) :-
	\+ (
	     atom_concat( Mid, _End, O ),
	     atom_concat( _Start, Old, Mid )
	   ).
rename_node( Old, New, O, N ) :-
	atom_concat( Mid1, End, O ),
	atom_concat( Start, Old, Mid1 ),
	atom_concat( Start, New, Mid2 ),
	atom_concat( Mid2, End, N ).

% "CORRECT" version - with type-checking, currently unusable as typechecking prevents
% conjoining cn & t formulae as is required in restrictive relatives
%
% % same type? just conjunction
% conjoin( Type:Formula1, Type:Formula2, Type:(Formula1 & Formula2) ).
% % not same type? recurse inside lambda-abstraction
% conjoin( Type1:Formula1, (ArgType>Type2):(Var^Formula2), (ArgType>Type):(Var^Formula) ) :-
% 	conjoin( Type1:Formula1, Type2:Formula2, Type:Formula ).

% "INCORRECT" version - no type-checking, just ensures conjunction done within all
% lambda-abstracted variables
%
% lambda-abstract? recurse inside lambda-abstraction
conjoin( Type1:Formula1, Type2:(Var^Formula2), Type:(Var^Formula) ) :-
	!,
	conjoin( Type1:Formula1, Type2:Formula2, Type:Formula ).
% run out of variables? just conjunction
conjoin( _Type1:Formula1, Type:Formula2, Type:(Formula1 & Formula2) ).
