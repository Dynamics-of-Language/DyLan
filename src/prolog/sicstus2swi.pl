/*
  Replacement for SICStus-only predicates
  */

% Replacement for SICStus substitute/4
% substitute(+X, +Xlist, +Y, ?Ylist)
% Xlist and Ylist are equal except for replacing *identical* occurrences of X by Y
substitute( _, [], _, [] ).
substitute( X, [H | XT], Y, [Y | YT] ) :-
	X == H,
	!,
	substitute( X, XT, Y, YT ).
substitute( X, [H | XT], Y, [H | YT] ) :-
	substitute( X, XT, Y, YT ).


% Replacement for SICStus prefix/2
% prefix(?Prefix, ?List)
% Prefix is a prefix of List
prefix( P, L ) :-
	append( P, _, L ).


% Replacement for SICStus remove_duplicates/2
% remove_duplicates(+List, ?Pruned)
% Pruned is the result of removing all *identical* duplicate elements in List.
remove_duplicates( L, P ) :-
	reverse( L, RL ),
	r_d( RL, RP ),
	reverse( RP, P ).

r_d( [], [] ).
r_d( [H | T1], T2 ) :-
	member( X, T1 ),
	H == X,
	!,
	r_d( T1, T2 ).
r_d( [H | T1], [H | T2] ) :-
	r_d( T1, T2 ).

% Replacement for SICStus variant/2
% - which is a built-in operator in SWI
variant( A, B ) :-
	A =@= B.


% SICStus nth/3 is called nth1/3 in SWI
nth( A, B, C ) :-
	nth1( A, B, C ).
