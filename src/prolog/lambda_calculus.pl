%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% lambda calculus utilities
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% beta_reduce( ?Function, ?Argument, ?Result )
% copy, apply and check unbound vars
beta_reduce( (AT>PT):F, AT:A, PT:P ) :-
	nonvar( F ),
	copy_term( F, A^P ),
	unbound_vars( F, [], V ),
	unbound_vars( A^P, [], V ).


% unbound_vars( +LambdaTerm, +BoundVarList, ?UnboundVarList )
% deal with vars first - if bound, ignore ...
unbound_vars( V, Bound, [] ) :-
	member( B, Bound ),
	V == B,
	!.
% ... otherwise list as unbound
unbound_vars( V, _Bound, [V] ) :-
	var( V ),
	!.
% base case
unbound_vars( [], _Bound, [] ) :-
	!.
% recurse through arguments, collecting unbound list
unbound_vars( [H | T], Bound, Unbound ) :-
	!,
	unbound_vars( H, Bound, U1 ),
	unbound_vars( T, Bound, U2 ),
	append( U1, U2, Unbound ).
% start here: collect bound vars
unbound_vars( A^B, Bound, Unbound ) :-
	!,
	unbound_vars( B, [A | Bound], Unbound ).
% once all bound vars collected, examine arguments
unbound_vars( Expr, Bound, Unbound ) :-
	Expr =.. [_F | Args],
	unbound_vars( Args, Bound, Unbound ).


% lambda_abstract( ?Argument, ?Function, ?Result )
lambda_abstract( A, F1, X^F2 ) :-
	F1 =.. [Func | Args1],
	lambda_subst( A, X, Args1, Args2 ),
	F2 =.. [Func | Args2].

lambda_subst( _, _, [], [] ).
lambda_subst( A, X, [H | T1], [X | T2] ) :-
	A == H,
	!,
	lambda_subst( A, X, T1, T2 ).
lambda_subst( A, X, [H1 | T1], [H2 | T2] ) :-
	compound( H1 ),
	!,
	lambda_abstract( A, H1, X^H2 ),
	lambda_subst( A, X, T1, T2 ).
lambda_subst( A, X, [H | T1], [H | T2] ) :-
	lambda_subst( A, X, T1, T2 ).
