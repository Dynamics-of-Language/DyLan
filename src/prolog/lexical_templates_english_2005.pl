%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% lexical action definitions for English
% (following Cann, Kempson & Marten 2005)
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


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

word( Word, Action ) :-
	prep( Word, _Mode ),
	Word \= '-',
	prep_action( Word, Action ).

word( Word, Action ) :-
	conj( Word, _Operator ),
	Word \= '-',
	conj_action( Word, Action ).

word( Word, Action ) :-
	misc( Word, interj, Message ),
	Word \= '-',
	misc_action( Message, Action ).

word( Word, Action ) :-
	misc( Word, too, _Message ),
	Word \= '-',
	too_action( Word, Action ).

word( Word, Action ) :-
	misc( Word, comp, _Message ),
	Word \= '-',
	comp_action( Word, Action ).


translate_per( sing, s3 ) :- !.
translate_per( P, P ).


% action for proper noun
noun_action( proper, Name, Per, Class, [(
					 if [ ?ty(e) ],
					 then [ freshput( x, Var, fo(Entity) ),
						put( ty(e) ) ,
						put( +Per ),
						put( +Class ),
						put( ! ) ],
					 else [ abort ]
					)] ) :-
	Entity =.. [Name, Var].

% action for mass noun
noun_action( mass, Name, Per, Class, [(
				       if [ ?ty(e) ],
				       then [ put( fo(Name) ),
					      put( ty(e) ) ,
					      put( +Per ),
					      put( +Class ),
					      put( ! ) ],
				       else [ abort ]
				      )] ).

% action for count noun
noun_action( count, Pred, Per, Class, [(
					if [ ?ty(cn) ],
					then [ make( [\/0] ),
					       go( [\/0] ),
					       freshput( x, Var, fo(Var) ),
					       put( ty(e) ),
					       put( +Per ),
					       put( +Class ),
					       put( ! ),
					       go( [/\0] ),
					       go( [/\0] ),
					       put( +Class ),
					       %put( ?scope( X ) ),
					       go( [\/0] ),
					       make( [\/1] ),
					       go( [\/1] ),
					       put( fo(X^Expr) ),
					       put( ty(e>cn) ),
					       put( ! ),
					       go( [/\1] ),
					       go( [\/0] ) ],
					else [ abort ]
				       )] ) :-
	Expr =.. [Pred, X].


% action for relative pronoun
pron_action( whrel, _Case, Class, [(
				    if [ ?ty(e),
					 ?fixed, %this is problematic
					 ([/\(*),/\2],fo(X)),
					 ([/\(*),/\2],+Class) ],
				    then [ put( fo(X) ),
					   put( ty(e) ),
					   put( ! ) ],
				    else [ abort ]
				   )] ).

% pron_action( whq, _Case, Class, [(
% 				  if [ ?ty( e ) ],
% 				  then [ (
% 					   if [ ([/\(*)],?ty( t )),
% 						([/\(*)],?fixed) ],
% 					   then [ put( fo( wh(Var) ) ),
% 						  put( ty( e ) ),
% 						  put( +Class ),
% 						  go( [/\(*)] ),
% 						  put( +q ) ],
% 					   else [ put( fo( wh(Var) ) ),
% 						  put( ty( e ) ),
% 						  put( +Class ) ]
% 					 ) ],
% 				  else [ abort ]
% 				 )] ).

% action for wh-question pronoun
pron_action( whq, _Case, Class, [(
				  if [ ?ty(e) ],
				  then [ put( fo(wh(_Var)) ),  % approximating a typed version
					 %put( fo(_Var) ), % untyped version
					 put( ty(e) ),
					 put( +Class ),
					 put( ([/\0],+q) ), % need this as overall subj is now always unfixed
					 (
					   if [ ?fixed ],
					   then [ gofirst( [/\(*)], ?ty(t) ),
						  (
						    if [ root ],
						    then [ put( ?(+q) ) ],
						    else [ put( +q ) ]
						  ) ],
					   else []
					 ) ],
				  else [ abort ]
				 )] ).

% action for standard pronoun (nominative)
pron_action( Type, Case, Class, [(
				  if [ ?ty(e) ],
				  then [ %put( fo(_Var) ),
					 put( ?fo(_) ),
					 put( ty(e) ),
					 put( +Type ), % Type is person here
					 put( +Class ),
					 put( ! ),
					 put( ?([/\],?ty(t)) ) ],
				  else [ abort ]
				 )] ) :-
	\+ member( Type, [whrel, whq, whmod, det] ),
	Case \= acc.

% action for standard pronoun (accusative)
pron_action( Type, Case, Class, [(
				  if [ ?ty(e),
				       ([/\],?ty(e>VType))],  % could be e>t or e>(e>t)
				  then [ %put( fo(_Var) ),
					 put( ?fo(_) ),
					 put( ty(e) ),
					 put( +Type ), % Type is person here
					 put( +Class ),
					 put( ! ),
					 put( ?([/\],?ty(e>VType)) ) ],
				  else [ abort ]
				 )] ) :-
	\+ member( Type, [whrel, whq, whmod, det] ),
	Case \= nom.


% action for anaphoric determiner (his etc)
pron_action( det, _Case, Class, [(
			  if [ ?ty(e) ],
			  then [ make( [\/1] ),
				 go( [\/1] ),
				 put( fo(X^Expr) ),
				 put( ty(cn>e) ),
				 put( ! ),
				 go( [/\1] ),
				 put( ?subfo(e,[+Class],Y) ),
				 make( [\/0] ),
				 go( [\/0] ),
				 put( ?ty(cn) ) ],
			  else [ abort ]
			 )] ) :-
	quant_term( the, Pred ),
	Expr =.. [Pred, Y, X].


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

% action for auxiliary verb
verb_action( aux, _Pred, Tense, Per, [(
				       if [ ?ty(t) ],
				       then [( if [ ([\/],ty(e>t)) ],
					       then [ abort ],
					       else [( if [ ([\/1],_) ],
						       then [ abort ],
						       else [ put( +Tense ),
							      make( [\/1] ),
							      go( [\/1] ),
							      %put( fo(U) ),
							      put( ty(e>t) ),
							      put( ?fo(_) ),
							      put( +Per ),
							      go( [/\1] ),
							      ( if [ ([\/0],ty(e)) ],
								  then [],
								  else [( if [ root ],
									  then [ put( +q ) ],
									  else [ abort ]
									)]
							      ) ]
						     )]
					     )],
				       else [ abort ]
				      )] ) :-
	\+ member( Tense, [inf,ing] ).

/*
% action for VP ellipsis auxiliary
% NOT CURRENTLY USED - prefer computational action instead
verb_action( aux, _Pred, Tense, Per, Actions ) :-
	\+ member( Tense, [inf,ing] ),
	words_in_context( _Words, CActions ),
	append( [(
		  if [ ?ty(e>t) ],
%		  ([/\1,\/0],+Per) ],
		  then [ go( [/\1] ),
			 put( +Tense ),
			 go( [\/1] ),
			 put( +Per ) ],
		  else [ abort ]
		 ) | CActions],
		[(
		  if [ ty(e>t) ],
		  then [],
		  else [abort]
		 )], Actions ).
*/


% finite intransitive
verb_action( intran, Pred, Tense, Per, [(
					 if [ ?ty(e>t) ],
%					      ([/\1,\/0],+Per) ],
					 then [ go( [/\1] ),
						put( +Tense ),
						go( [\/1] ),
						put( +Per ),
						put( fo(X^Expr) ),
						put( ty(e>t) ),
						put( ! ) ],
					 else [ abort ]
					)] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X].
% non-finite intransitive
verb_action( intran, Pred, inf, _Per, [(
				       if [ ty(e>t),
					    ?fo(_) ],
				       then [ put( fo(X^Expr) ),
					      put( ! ) ],
				       else [ abort ]
				      )] ) :-
	Expr =.. [Pred, X].

% finite transitive
verb_action( tran, Pred, Tense, Per, [(
				       if [ ?ty(e>t) ],
%					    ([/\1,\/0],+Per) ],
				       then [ go( [/\1] ),
					      put( +Tense ),
					      go( [\/1] ),
					      make( [\/1] ),
					      go( [\/1] ),
					      put( +Per ),
					      put( fo(Y^(X^Expr)) ),
					      put( ty(e>(e>t)) ),
					      put( ! ),
					      go( [/\] ),
					      make( [\/0] ),
					      go( [\/0] ),
					      put( ?ty(e) ) ],
				       else [ abort ]
				      )] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y].
% non-finite transitive
verb_action( tran, Pred, inf, _Per, [(
				      if [ ty(e>t),
					   ?fo(_) ],
				      then [ make( [\/1] ),
					     go( [\/1] ),
					     put( fo(Y^(X^Expr)) ),
					     put( ty(e>(e>t)) ),
					     put( ! ),
					     go( [/\] ),
					     make( [\/0] ),
					     go( [\/0] ),
					     put( ?ty(e) ) ],
				      else [ abort ]
				     )] ) :-
	Expr =.. [Pred, X, Y].

% finite ditransitive (direct object first)
verb_action( ditran, Pred, Tense, Per, [(
					 if [ ?ty(e>t) ],
%					    ([/\1,\/0],+Per) ],
					 then [( if [ ([\/1,\/1],ty(e>(e>(e>t)))) ],
						 then [ abort ],
						 else [ go( [/\1] ),
							put( +Tense ),
							go( [\/1] ),
							make( [\/1] ),
							go( [\/1] ),
							put( ?ty(e>(e>t)) ),
							make( [\/1] ),
							go( [\/1] ),
							put( +Per ),
							put( fo(Z^(Y^(X^Expr))) ),
							put( ty(e>(e>(e>t))) ),
							put( ! ),
							go( [/\] ),
							make( [\/0] ),
							go( [\/0] ),
							put( ?ty(e) ),
							go( [/\,/\] ),
							make( [\/0] ),
							go( [\/0] ),
							put( ?ty(e) ) ] )],
					 else [ abort ]
					)] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y, Z].
% non-finite ditransitive (direct object first)
verb_action( ditran, Pred, inf, _Per, [(
					if [ ty(e>t),
					     ?fo(_) ],
					then [( if [ ([\/1,\/1],ty(e>(e>(e>t)))) ],
						then [ abort ],
						else [ make( [\/1] ),
						       go( [\/1] ),
						       put( ?ty(e>(e>t)) ),
						       make( [\/1] ),
						       go( [\/1] ),
						       put( fo(Z^(Y^(X^Expr))) ),
						       put( ty(e>(e>(e>t))) ),
						       put( ! ),
						       go( [/\] ),
						       make( [\/0] ),
						       go( [\/0] ),
						       put( ?ty(e) ),
						       go( [/\,/\] ),
						       make( [\/0] ),
						       go( [\/0] ),
						       put( ?ty(e) ) ] )],
					else [ abort ]
				       )] ) :-
	Expr =.. [Pred, X, Y, Z].

% finite ditransitive (indirect object first)
verb_action( ditran, Pred, Tense, Per, [(
					 if [ ?ty(e>t) ],
%					    ([/\1,\/0],+Per) ],
					 then [( if [ ([\/1,\/1],ty(e>(e>(e>t)))) ],
						 then [ abort ],
						 else [ go( [/\1] ),
							put( +Tense ),
							go( [\/1] ),
							make( [\/0] ),
							go( [\/0] ),
							put( ?ty(e) ),
							go( [/\] ),
							make( [\/1] ),
							go( [\/1] ),
							put( ?ty(e>(e>t)) ),
							make( [\/1] ),
							go( [\/1] ),
							put( +Per ),
							put( fo(Z^(Y^(X^Expr))) ),
							put( ty(e>(e>(e>t))) ),
							put( ! ),
							go( [/\] ),
							make( [\/0] ),
							go( [\/0] ),
							put( ?ty(e) ) ] )],
					 else [ abort ]
					)] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y, Z].
% non-finite ditransitive (indirect object first)
verb_action( ditran, Pred, inf, _Per, [(
					if [ ty(e>t),
					     ?fo(_) ],
					then [( if [ ([\/1,\/1],ty(e>(e>(e>t)))) ],
						then [ abort ],
						else [ make( [\/0] ),
						       go( [\/0] ),
						       put( ?ty(e) ),
						       go( [/\] ),
						       make( [\/1] ),
						       go( [\/1] ),
						       put( ?ty(e>(e>t)) ),
						       make( [\/1] ),
						       go( [\/1] ),
						       put( fo(Z^(Y^(X^Expr))) ),
						       put( ty(e>(e>(e>t))) ),
						       put( ! ),
						       go( [/\] ),
						       make( [\/0] ),
						       go( [\/0] ),
						       put( ?ty(e) ) ] )],
					else [ abort ]
				       )] ) :-
	Expr =.. [Pred, X, Y, Z].

% finite propositional attitude
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
						 put( ! ),
						 go( [/\] ),
						 make( [\/0] ),
						 go( [\/0] ),
						 put( ?ty(t) ) ],
					  else [ abort ]
					 )] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y].
% non-finite propositional attitude
verb_action( p_stran, Pred, inf, _Per, [(
					 if [ ty(e>t),
					      ?fo(_) ],
					 then [ make( [\/1] ),
						go( [\/1] ),
						put( fo(Y^(X^Expr)) ),
						put( ty(t>(e>t)) ),
						put( ! ),
						go( [/\] ),
						make( [\/0] ),
						go( [\/0] ),
						put( ?ty(t) ) ],
					 else [ abort ]
					)] ) :-
	Expr =.. [Pred, X, Y].

% finite question-embedding
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
						 put( ! ),
						 go( [/\] ),
						 make( [\/0] ),
						 go( [\/0] ),
						 put( ?ty(t) ),
						 put( ?(+q) ) ],
					  else [ abort ]
					 )] ) :-
	\+ member( Tense, [inf,ing] ),
	Expr =.. [Pred, X, Y].
% non-finite question-embedding
verb_action( q_stran, Pred, inf, _Per, [(
					 if [ ty(e>t),
					      ?fo(_) ],
					 then [ make( [\/1] ),
						go( [\/1] ),
						put( fo(Y^(X^Expr)) ),
						put( ty(t>(e>t)) ),
						put( ! ),
						go( [/\] ),
						make( [\/0] ),
						go( [\/0] ),
						put( ?ty(t) ),
						put( ?(+q) ) ],
					 else [ abort ]
					)] ) :-
	Expr =.. [Pred, X, Y].


% action for standard determiner
det_action( quant, Rel, [(
			  if [ ?ty(e) ],
			  then [ make( [\/1] ),
				 go( [\/1] ),
				 put( fo(X^Expr) ),
				 put( ty(cn>e) ),
				 put( ! ),
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


% action for adjective
adj_action( Pred, [(
		    if [ ?ty(cn) ],
		    then [ make( [\/1] ),
			   go( [\/1] ),
			   put( fo(X^Expr) ),
			   put( ty(cn>cn) ),
			   put( ! ),
			   go( [/\1] ),
			   make( [\/0] ),
			   go( [\/0] ),
			   put( ?ty(cn) ) ],
		    else [ abort ]
		   )] ) :-
	Expr =.. [Pred, X].

% action for preposition (pointer-movement for 'to' only at present)
prep_action( 'to', [(
		    if [ ?ty(e>t),
			 ([\/1,\/0],?ty(e)) ],
		    then [ go( [\/1,\/0] ) ],
		    else [ abort ]
		   )] ).

% action for conjunction
conj_action( _Word, [(
		    if [ ty(X) ],
		    then [ make( [\/2] ),
			   go( [\/2] ),
			   put( ?ty(X) ) ],
		    else [ abort ]
		   )] ).

% action for complentiser 'that'
comp_action( _Word, [(
		    if [ ?ty(t) ],
		    then [ (if [ ([/\],!) ], % at root node
			    then [ abort ],
			    else [ put( +fin ) ]) ], % hacky way of phrasing tense requirement
		    else [ abort ]
		   )] ).

% action for 'empty' dialogue moves (greetings etc)
misc_action( Move, [(
		     if [ ?ty(t),
			  ([/\2],!) ], % we're not on a LINKed tree
		     then [ put( fo(Move) ),
			    put( ty(t) ),
			    put( ! ) ],
		     else [ abort ]
		    )] ).

% placeholder action for 'too'
too_action( _Word, [(
		    if [ ty(t) ],
		    then [ (if [ +too ],
			       then [ abort ],
			       else [ put( +too ) ]) ],
		    else [ abort ]
		   )] ).
