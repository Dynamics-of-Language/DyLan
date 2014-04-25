% Prolog lexicon for SHARDS, from OALD machine-readable dictionary
% Produced by asc2lex.perl, Matthew Purver 19/04/2001
%
% Manually edited for irregulars, CMTs, determiners etc.

:- use_module( library( lists ), [memberchk/2] ).

:- dynamic noun/6, verb/5.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Morphological interface predicates
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% noun/6
% noun( ?Stem, ?Word, ?Type, ?Number, ?SemClass, ?Case )
%
% Morphological interface to noun/5 - relates word stem
% Stem to surface word Word.
% Number will be 'sing' or 'plur'
% SemClass a member of a defined hierarchy e.g. 'per', 'obj'
% Case currently always 'case' (i.e. any)
% Type will be 'mass' or 'count' (both may succeed)
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

noun( Stem, Word, Type, sing, SemClass, case ) :-
      noun( Stem, Word, _Plural, RawType, SemClass ),
      noun_type( RawType, Type ).

noun( Stem, Word, Type, plur, SemClass, case ) :-
      noun( Stem, _Sing, Word, RawType, SemClass ),
      noun_type( RawType, Type ).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% noun_type( +RawType, ?Type )
%
% Returns type as defined in lexicon, except for 'both'
% which gets converted to 'mass' or 'count'
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

noun_type( Type, Type ) :-
	\+ Type = both.
noun_type( both, mass ).
noun_type( both, count ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% verb/5
% verb( ?Stem, ?Word, ?VForm, ?Number, ?Type )
%
% Morphological interface to verb/8 - relates word stem
% Stem to surface word Word.
% VForm currently 'inf', 'pres' or 'past'
% Number currently 's3', 'nons3' or 'person' (i.e. undefined)
% Type will be 'intran', 'tran', 'ditran', etc.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

verb( Stem, Word, inf, person, Type ) :-
	verb( Stem, Word, _, _, _, _, Cat, CatList ),
	verb_type( Cat, CatList, Type ).

% verb( Stem, Word, pres, nons3, Type ) :-
% 	verb( Stem, Word, _, _, _, _, Cat, CatList ),
% 	verb_type( Cat, CatList, Type ).

% verb( Stem, Word, pres, s3, Type ) :-
% 	verb( Stem, _, Word, _, _, _, Cat, CatList ),
% 	verb_type( Cat, CatList, Type ).

% verb( Stem, Word, ing, person, Type ) :-
% 	verb( Stem, _, _, Word, _, _, Cat, CatList ),
% 	verb_type( Cat, CatList, Type ).

% verb( Stem, Word, past, person, Type ) :-
% 	verb( Stem, _, _, _, Word, _, Cat, CatList ),
% 	verb_type( Cat, CatList, Type ).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% verb_type( +RawType, +CatList, ?Type )
%
% Type is a verb subcategory type determined from the
% RawType atom and CatList list of numbers defined in
% the lexicon
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

verb_type( intran, CatList, intran ) :-
	memberchk( '2A', CatList ).
verb_type( tran, CatList, tran ) :-
	memberchk( '6A', CatList );
	memberchk( '6B', CatList ).
verb_type( _, CatList, ditran ) :-
	memberchk( '12A', CatList );
	memberchk( '12B', CatList );
	memberchk( '12C', CatList ).
verb_type( _, CatList, p_stran ) :-
	memberchk( '9', CatList ).
verb_type( _, CatList, q_stran ) :-
	memberchk( '10', CatList ).
verb_type( _, CatList, pp_to ) :-
	memberchk( '3A', CatList ).
verb_type( _, CatList, pp_for ) :-
	memberchk( '3A', CatList ).
verb_type( _, CatList, subjraise ) :-
	memberchk( '4E', CatList ).
verb_type( _, CatList, subjcon ) :-
	memberchk( '7A', CatList ).
verb_type( _, CatList, aux ) :-
	memberchk( '5', CatList ).
verb_type( _, CatList, be ) :-
	memberchk( '1', CatList ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% adj/3
% adj( ?Stem, ?Word, ?Type )
%
% Morphological interface to adj/4 - relates word stem
% Stem to surface word Word.
% Type will be 'simple', 'comparative' or 'superlative'
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

adj( Word, Word, simple ) :-
	adj( Word, _, _, _ ).

adj( Stem, Word, comparative ) :-
	adj( Stem, Word, _, _ ).

adj( Stem, Word, superlative ) :-
	adj( Stem, _, Word, _ ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% General predicates
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% word( ?Word )
%
% Succeeds if Word is a word defined in the lexicon
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
word( Word ) :-
	noun( _, Word, _, _, _, _ );
	pron( Word, _, _, _ );
	verb( _, Word, _, _, _ );
	adj( _, Word, _ );
	adv( Word, _ );
	prep( Word, _ );
	conj( Word, _ );
	det( Word, _, _ );
	misc( Word, _, _ );
	polar( Word, _, _ ).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% open_class_word( ?Word )
%
% Succeeds if Word is a noun/verb/adj/adv in the lexicon
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
open_class_word( Word ) :-
	noun( _, Word, _, _, _, _ );
	verb( _, Word, _, _, _ );
	adj( _, Word, _ );
	adv( Word, _ ).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% unknown_word( +Word )
%
% Succeeds if Word is NOT defined in the lexicon
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
unknown_word( Word ) :-
	\+ word( Word ).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% Lexicon
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

verb( 'can', 'can', 'can', '-', 'could', '-', aux, ['5','6A'] ).
verb( 'eat', 'tabe', '-', '-', '-', '-', tran, ['6A'] ).
verb( 'peel', 'mui', '-', '-', '-', '-', tran, ['6A'] ).
verb( 'say', 'it', '-', '-', '-', '-', p_stran, ['9'] ).
verb( 'snore', 'snore', '-', '-', '-', '-', intran, ['2A'] ).

noun( 'bill', 'bill', '-', proper, male ).
noun( 'john', 'john', '-', proper, male ).
noun( 'mary', 'mary', '-', proper, female ).
noun( 'german', 'doitugo', '-', proper, obj ).

noun( 'apple', 'ringo', 'ringo', count, obj ).

noun( 'beer', 'biiru', 'biiru', count, obj ).
noun( 'natu', 'hotel', 'hotels', count, obj ).
noun( 'boy', 'boy', 'boys', count, male ).
noun( 'girl', 'girl', 'girls', count, female ).
noun( 'student', 'gakusei', 'gakusei', count, per ).
noun( 'teacher', 'sensei', 'sensei', count, per ).

adj( 'blue', 'bluer', 'bluest', normal ).
adj( 'red', 'redder', 'reddest', normal ).
adj( 'green', 'greener', 'greenest', normal ).

adv( -, - ).

pron( 'i', nom, s1, spkr ).
pron( 'he', nom, s3, male ).
pron( 'her', acc, s3, female ).
pron( 'him', acc, s3, male ).
pron( 'it', case, s3, obj ).
pron( 'me', acc, s1, spkr ).
pron( 'she', nom, s3, female ).
pron( 'what', case, whq, obj ).
pron( 'what', case, whrel, obj ).
pron( 'which', case, whrel, obj ).
pron( 'that', case, whrel, obj ).
pron( 'who', case, whq, per ).
pron( 'who', case, whrel, per ).
%pron( 'whose', case, person, _ ).
pron( 'you', case, s2, addr ).
pron( 'them', acc, plur, per ).
pron( 'they', nom, plur, per ).

pron( 'how', case, whmod, trans ).
pron( 'when', case, whmod, tim ).
pron( 'where', case, whmod, loc ).

det( 'sannin', quant, three ).
det( 'subete', quant, forall ).

prep( 'by', trans ).
prep( 'from', loc ).
prep( 'in', loc ).
prep( 'in', tim ).
prep( 'on', tim ).
prep( 'to', loc ).

conj( -, - ).

misc( 'konnichiwa', interj, greet ).
misc( 'sayonara', interj, close ).

polar( 'hai', plus, true ).
polar( 'no', plus, untrue ).

suffix( 'ga', noun, '0' ).
suffix( 'wa', noun, '0' ).
suffix( 'o', noun, '10' ).
suffix( 'ni', noun, '110' ).

suffix( 'ru', verb, pres ).
suffix( 'ta', verb, past ).
