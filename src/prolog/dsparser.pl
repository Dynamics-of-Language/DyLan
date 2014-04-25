%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Dynamic Syntax
%
% parser
%
% Matthew Purver, 2004
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


% parse/3 - parse( +Forest1, +WordList, ?Forest2 )
% Adds words in WordList one by one, proceeding from the input forest Forest1
% to the resulting output forest Forest2
parse( Forest, [], Forest ).
parse( Forest1, [H | T], Forest3 ) :-
	parse_word( Forest1, H, Forest2 ),
	parse( Forest2, T, Forest3 ).

% parse_word( +Forest1, +Word, -Forest2 )
% Adds a single word, extending the input forest Forest1 to the resulting
% possible output forest Forest2
parse_word( _Forest1, Word, [] ) :-
	atom( Word ),
	\+ word( Word, _Action ),
	print( 'unknown word: ' ), print( Word ), newline.
parse_word( Forest1, Word, Forest2 ) :-
	flag( test, complex ),
	atom( Word ),
	print( 'word: ' ), print( Word ), newline,
	print( 'Trees before: ' ), length( Forest1, N1 ), print( N1 ), newline,
 	findall( TmpTree,
		 (
		   member( twa( Tree1, Words1, Actions1, Trace1, Prob1 ), Forest1 ),
		   word( Word, Actions ),
		   apply_lex( Actions, Tree1, TmpTree )
		 ), TmpForest ),
	print( 'Trees after lex (all): ' ), length( TmpForest, TmpN ), print( TmpN ), newline,
	remove_variants( TmpForest, TmpForest2 ),
	print( 'Trees after lex (unique): ' ), length( TmpForest2, TmpN2 ), print( TmpN2 ), newline,
	findall( twa( Tree2, Words2, Actions2, Trace2, Prob2 ),
		 (
		   member( twa( Tree1, Words1, Actions1, Trace1, Prob1 ), Forest1 ),
		   word( Word, Actions ),
		   apply( [Word], Actions, Tree1, Tree2, Trace, Prob ),
		   append( Words1, [Word], Words2 ),
		   append( Actions1, Actions, Actions2 ),
		   append( Trace1, Trace, Trace2 ),
		   update_prob( Prob, Trace1, Prob1, Trace2, Prob2 )
		 ), Forest ),
	print( 'Trees after (all): ' ), length( Forest, N ), print( N ), newline,
	remove_variants( Forest, Forest2 ),
	print( 'Trees after (unique): ' ), length( Forest2, N2 ), print( N2 ), newline.
parse_word( Forest1, Word, Forest2 ) :-
	\+ flag( test, complex ),
	atom( Word ),
	print( 'word: ' ), print( Word ), newline,
	findall( twa( Tree2, Words2, Actions2, Trace2, Prob2 ),
		 (
		   member( twa( Tree1, Words1, Actions1, Trace1, Prob1 ), Forest1 ),
		   word( Word, Actions ),
		   apply( [Word], Actions, Tree1, Tree2, Trace, Prob ),
		   append( Words1, [Word], Words2 ),
		   append( Actions1, Actions, Actions2 ),
		   append( Trace1, Trace, Trace2 ),
		   update_prob( Prob, Trace1, Prob1, Trace2, Prob2 )
		 ), Forest ),
	remove_variants( Forest, Forest2 ).

% apply/6 - apply( +Words, +Actions, +Tree1, ?Tree2, ?Trace, ?Prob )
% Applies list of Actions to Tree1 resulting in Tree2, with resulting additional Trace and Prob

% recurse through a list of actions, allowing computational actions
apply( [], [], Tree, Tree, [], 0.0 ).
apply( [HW | TW], [HA | TA], Tree1, Tree4, Trace, Prob ) :-
	copy_term( HA, A ), % preventing unification of stored actions with metavariable values
	exec( A, Tree1, Tree2 ),
	lex_prob( HW, HA, Tree1, HLexProb ),
	adjust( Tree2, Tree3, HTrace, HCompProb ),
	pointer( Tree1, Pointer1 ),
	append( [HW-Pointer1 | HTrace], TTrace, Trace ),
	apply( TW, TA, Tree3, Tree4, TTrace, TProb ),
	Prob is ( HLexProb + HCompProb + TProb ).

% a version which applies only the lexical action - just for measuring complexity
apply_lex( [], Tree, Tree ).
apply_lex( [H | T], Tree1, Tree3 ) :-
	copy_term( H, A ),
	exec( A, Tree1, Tree2 ),
	apply_lex( T, Tree2, Tree3 ).


% remove_variants/2
% remove_variants(+List, ?Pruned)
% Pruned is the result of removing all *variant* (identical modulo variables) elements in List.
remove_variants( [], [] ).
remove_variants( [H | T1], T2 ) :-
	member( X, T1 ),
	variant( X, H ),
	!,
	remove_variants( T1, T2 ).
remove_variants( [H | T1], [H | T2] ) :-
	remove_variants( T1, T2 ).
