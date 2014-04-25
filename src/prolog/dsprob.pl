%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% probability handling
%
% Matthew Purver, 2009
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

% getting best things
best_tree( Forest, Tree, Prob ) :-
	best( Forest, twa( Tree, _Words, _Actions, _Trace, Prob ) ).

best_twa( Forest, TWA, Prob ) :-
	best( Forest, TWA ),
	TWA = twa( _Tree, _Words, _Actions, _Trace, Prob ).



% beam maintenance
trim_forest( Forest1, Forest2, BeamWidth ) :-
	best( Forest1, Best ),
	findall( TWA,
		 (
		   member( TWA, Forest1 ),
		   better( Best, TWA, BeamWidth )
		 ), Forest2 ).

	
best( [H | T], Best ) :-
	best( T, H, Best ).

best( [], A, A ).
best( [H | T], BestSoFar, Best ) :-
	better( H, BestSoFar ),
	!,
	best( T, H, Best ).
best( [_H | T], BestSoFar, Best ) :-
	best( T, BestSoFar, Best ).


% A more probable than B
better( twa( _Tree1, _Words1, _Actions1, _Trace1, Prob1 ),
	twa( _Tree2, _Words2, _Actions2, _Trace2, Prob2 ) ) :-
	Prob1 > Prob2.

% A more probable than B by at least Diff
better( twa( _Tree1, _Words1, _Actions1, _Trace1, Prob1 ),
	twa( _Tree2, _Words2, _Actions2, _Trace2, Prob2 ), Diff ) :-
	( Prob1 - Prob2 ) > Diff.


% conditional (log) prob of lexical action application given word, current partial tree
lex_prob( _Word, _Action, _Tree, Prob ) :-
	Prob = -0.1.

% conditional (log) prob of computational action given current partial tree
comp_prob( _Rule, _Tree, Prob ) :-
	Prob = 0.0.

% add (log) prob, normalising by trace length
update_prob( Prob, Trace1, Prob1, Trace2, Prob2 ) :-
	length( Trace1, L1 ),
	length( Trace2, L2 ),
	Prob2 is ( ( (Prob1 * L1) + Prob ) / L2 ).
