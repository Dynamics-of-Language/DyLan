%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% lexical action definitions for Japanese
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

:- use_module( library( lists ), [member/2, append/3] ).

% decompose suffix if present
word( Words, Actions ) :-
	suffix( Suffix, Class, Data ),
	atom_concat( Word, Suffix, Words ),
	suffix_action( Class, Data, SAction ),
	word( Word, WAction ),
	append( WAction, SAction, Actions ).

% standard versions
word( Word, Action ) :-
	suffix( Word, Class, Data ),
	suffix_action( Class, Data, Action ).

word( Word, Action ) :-
	noun( Stem, Word, Type, Per1, Class, _Case ),
	Word \= '-',
	translate_per( Per1, Per2 ),
	noun_action( Type, Stem, Per2, Class, Action ).

word( Word, Action ) :-
	verb( Stem, Word, Mode, Per, Type ),
	Word \= '-',
	verb_action( Type, Stem, Mode, Per, Action ).

word( Word, Action ) :-
	pron( Word, Case, Type, Class ),
	Word \= '-',
	pron_action( Type, Case, Class, Action ).

word( Word, Action ) :-
	det( Word, Type, Rel ),
	Word \= '-',
	det_action( Type, Rel, Action ).

word( Word, Action ) :-
	adj( _Stem, Word, _Mode ),
	Word \= '-',
	adj_action( Word, Action ).


translate_per( sing, s3 ) :- !.
translate_per( P, P ).


noun_action( proper, Name, Per, Class, [(
					 if [ ?ty(e) ],
					 then [ put( fo(Name) ),
						put( ty(e) ) ,
						put( +Per ),
						put( +Class ) ],
					 else [ abort ]
					)] ).

noun_action( mass, Name, Per, Class, [(
				       if [ ?ty(e) ],
				       then [ put( fo(Name) ),
					      put( ty(e) ) ,
					      put( +Per ),
					      put( +Class ) ],
				       else [ abort ]
				      )] ).

noun_action( count, Pred, Per, Class, [(
					if [ ?ty(e) ],
					then [ make( [\/1] ),
					       go( [\/1] ),
					       put( fo(X^eps(X)) ),
					       put( ty(cn>e) ),
					       go( [/\1] ),
					       make( [\/0] ),
					       go( [\/0] ),
					       put( ?ty(cn) ),
					       make( [\/0] ),
					       go( [\/0] ),
					       put( fo(e(_Var)) ),
					       put( ty(e) ),
					       put( +Per ),
					       put( +Class ),
					       go( [/\0] ),
					       go( [/\0] ),
					       put( +Class ),
					       %put( ?scope( X ) ),
					       go( [\/0] ),
					       make( [\/1] ),
					       go( [\/1] ),
					       put( fo(X^Expr) ),
					       put( ty(e>cn) ) ],
					else [ abort ]
				       )] ) :-
	Expr =.. [Pred, X].



pron_action( whrel, _Case, Class, [(
				    if [ ?ty(e),
					 ?fixed,
					 ([/\*,/\2],fo(X)),
					 ([/\*,/\2],+Class) ],
				    then [ put( fo(X) ),
					   put( ty(e) ) ],
				    else [ abort ]
				   )] ).

% pron_action( whq, _Case, Class, [(
% 				  if [ ?ty( e ) ],
% 				  then [ (
% 					   if [ ([/\*],?ty( t )),
% 						([/\*],?fixed) ],
% 					   then [ put( fo( wh(Var) ) ),
% 						  put( ty( e ) ),
% 						  put( +Class ),
% 						  go( [/\*] ),
% 						  put( +q ) ],
% 					   else [ put( fo( wh(Var) ) ),
% 						  put( ty( e ) ),
% 						  put( +Class ) ]
% 					 ) ],
% 				  else [ abort ]
% 				 )] ).

pron_action( whq, _Case, Class, [(
				  if [ ?ty(e) ],
				  then [ put( fo(wh(_Var)) ),
					 put( ty(e) ),
					 put( +Class ),
					 (
					   if [ ?fixed ],
					   then [ gofirst( [/\*], ?ty(t) ),
						  (
						    if [ root ],
						    then [ put( ?(+q) ) ],
						    else [ put( +q ) ]
						  ) ],
					   else []
					 ) ],
				  else [ abort ]
				 )] ).

pron_action( Type, Case, Class, [(
				  if [ ?ty(e) ],
				  then [ %put( fo(_Var) ),
					 put( ?fo(_) ),
					 put( ty(e) ),
					 put( +Type ), % Type is person here
					 put( +Class ),
					 put( ?([/\],?ty(t)) ) ],
				  else [ abort ]
				 )] ) :-
	\+ member( Type, [whrel, whq, whmod] ),
	Case \= acc.

pron_action( Type, Case, Class, [(
				  if [ ?ty(e),
				       ([/\],?ty(e>t))],
				  then [ %put( fo(_Var) ),
					 put( ?fo(_) ),
					 put( ty(e) ),
					 put( +Type ), % Type is person here
					 put( +Class ),
					 put( ?([/\],?ty(e>t)) ) ],
				  else [ abort ]
				 )] ) :-
	\+ member( Type, [whrel, whq, whmod] ),
	Case \= nom.


%% ruth
% verb_action( aux, _Pred, Tense, Per, [(
% 				       if [ ?ty(t) ],
% 				       then [( if [ ([\/],ty(e>t)) ],
% 					       then [ abort ],
% 					       else [( if [ ([\/0],ty(e)) ],
% 						       then [],
% 						       else [( if [ root ],
% 							       then [ abort],
% 							       else [ put( q ) ]
% 							     )]
% 						     ),
% 						     put( +Tense ),
% 						     make( [\/1] ),
% 						     go( [\/1] ),
% 						     %put( fo(U) ),
% 						     put( ty(e>t) ),
% 						     put( ?fo(_) ) ]
% 					     )],
% 				       else [ abort ]
% 				      )] ) :-
% 	\+ member( Tense, [inf,ing] ).

verb_action( aux, _Pred, Tense, Per, [(
				       if [ ?ty(t) ],
				       then [( if [ ([\/],ty(e>t)) ],
					       then [ abort ],
					       else [ put( +Tense ),
						      put( +Per ),
						      make( [\/1] ),
						      go( [\/1] ),
						      %put( fo(U) ),
						      put( ty(e>t) ),
						      put( ?fo(_) ),
						      go( [/\1] ),
						      ( if [ ([\/0],ty(e)) ],
							  then [],
							  else [( if [ root ],
								  then [ put( +q ) ],
								  else [ abort ]
								)]
						      )]
					     )],
				       else [ abort ]
				      )] ) :-
	\+ member( Tense, [inf,ing] ).

% finite
verb_action( intran, Pred, _Tense, _Per, [(
					   if [ ?ty(t) ],
					   then [ replace( [\/1] ),
						  go( [\/1] ),
						  put( fo(X^Expr) ),
						  put( ty(e>t) ),
						  (
						    if [ ([/\1,\/0],ty(e)) ],
						    then [],
						    else [ make( [/\1,\/0] ),
							   go( [/\1,\/0] ),
							   put( fo(_Subj) ),
							   put( ty(e) ) ]
						  )],
					   else [ abort ]
					)] ) :-
	Expr =.. [Pred, X].

% finite
verb_action( tran, Pred, _Tense, _Per, [(
					 if [ ?ty(t) ],
					 then [ replace( [\/1] ),
						go( [\/1] ),
						replace( [\/1] ),
						go( [\/1] ),
						put( fo(Y^(X^Expr)) ),
						put( ty(e>(e>t)) ),
						go( [/\] ),
						(
						  if [ ([\/0],ty(e)) ],
						  then [],
						  else [ make( [\/0] ),
							 go( [\/0] ),
							 put( fo(_Obj) ),
							 put( ty(e) ),
							 go( [/\] ) ]
						),
						(
						  if [ ([/\1,\/0],ty(e)) ],
						  then [],
						  else [ make( [/\1,\/0] ),
							 go( [/\1,\/0] ),
							 put( fo(_Subj) ),
							 put( ty(e) ) ]
						)],
					 else [ abort ]
					)] ) :-
	Expr =.. [Pred, X, Y].

% finite
verb_action( p_stran, Pred, Tense, Per, [(
					  if [ ?ty(e>t) ],
					  %([/\1,\/0],+Per) ],
					  then [ go( [/\1] ),
						 put( +Tense ),
						 go( [\/1] ),
						 make( [\/1] ),
						 go( [\/1] ),
						 put( +Per ),
						 put( fo(Y^(X^Expr)) ),
						 put( ty(t>(e>t)) ),
						 go( [/\] ),
						 make( [\/0] ),
						 go( [\/0] ),
						 put( ?ty(t) ) ],
					  else [ abort ]
					 )] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y].
% non-finite
verb_action( p_stran, Pred, inf, _Per, [(
					 if [ ty(e>t),
					      ?fo(_) ],
					 then [ make( [\/1] ),
						go( [\/1] ),
						put( fo(Y^(X^Expr)) ),
						put( ty(t>(e>t)) ),
						go( [/\] ),
						make( [\/0] ),
						go( [\/0] ),
						put( ?ty(t) ) ],
					 else [ abort ]
					)] ) :-
	Expr =.. [Pred, X, Y].

% finite
verb_action( q_stran, Pred, Tense, Per, [(
					  if [ ?ty(e>t) ],
					  %([/\1,\/0],+Per) ],
					  then [ go( [/\1] ),
						 put( +Tense ),
						 go( [\/1] ),
						 make( [\/1] ),
						 go( [\/1] ),
						 put( +Per ),
						 put( fo(Y^(X^Expr)) ),
						 put( ty(t>(e>t)) ),
						 go( [/\] ),
						 make( [\/0] ),
						 go( [\/0] ),
						 put( ?ty(t) ),
						 put( ?(+q) ) ],
					  else [ abort ]
					 )] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y].
% non-finite
verb_action( q_stran, Pred, inf, _Per, [(
					 if [ ty(e>t),
					      ?fo(_) ],
					 then [ make( [\/1] ),
						go( [\/1] ),
						put( fo(Y^(X^Expr)) ),
						put( ty(t>(e>t)) ),
						go( [/\] ),
						make( [\/0] ),
						go( [\/0] ),
						put( ?ty(t) ),
						put( ?(+q) ) ],
					 else [ abort ]
					)] ) :-
	Expr =.. [Pred, X, Y].



det_action( quant, Rel, [(
			  if [ ?ty(e) ],
			  then [ make( [\/1] ),
				 go( [\/1] ),
				 put( fo(X^Expr) ),
				 put( ty(cn>e) ),
				 go( [/\1] ),
				 make( [\/0] ),
				 go( [\/0] ),
				 put( ?ty(cn) ) ],
			  else [ abort ]
			 )] ) :-
	quant_term( Rel, Pred ),
	Expr =.. [Pred, X].

quant_term( exists, eps ).
quant_term( forall, tau ).
quant_term( the, eta ).
quant_term( notexists, noteps ).


adj_action( Pred, [(
		    if [ ?ty(cn) ],
		    then [ make( [\/1] ),
			   go( [\/1] ),
			   put( fo(X^Expr) ),
			   put( ty(cn>cn) ),
			   go( [/\1] ),
			   make( [\/0] ),
			   go( [\/0] ),
			   put( ?ty(cn) ) ],
		    else [ abort ]
		   )] ) :-
	Expr =.. [Pred, X].


suffix_action( verb, Tense, [(
			      if [ ty(t) ],
			      then [ put( +Tense ) ],
			      else [ abort ]
			     )] ).

suffix_action( noun, Dir, [(
			    if [ ty(e) ],
			    then [ fix( Dir ) ],
			    else [ abort ]
			   )] ).
