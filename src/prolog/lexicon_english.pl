% Prolog lexicon for SHARDS, from OALD machine-readable dictionary
% Produced by asc2lex.perl, Matthew Purver 19/04/2001
%
% Manually edited for irregulars, CMTs, determiners etc.


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

verb( Stem, Word, pres, nons3, Type ) :-
	verb( Stem, Word, _, _, _, _, Cat, CatList ),
	verb_type( Cat, CatList, Type ).

verb( Stem, Word, pres, s3, Type ) :-
	verb( Stem, _, Word, _, _, _, Cat, CatList ),
	verb_type( Cat, CatList, Type ).

verb( Stem, Word, ing, person, Type ) :-
	verb( Stem, _, _, Word, _, _, Cat, CatList ),
	verb_type( Cat, CatList, Type ).

verb( Stem, Word, past, person, Type ) :-
	verb( Stem, _, _, _, Word, _, Cat, CatList ),
	verb_type( Cat, CatList, Type ).

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

verb( 'arrest', 'arrest', 'arrests', 'arresting', 'arrested', 'arrested', tran, ['6A','6D','7A','17','19B','19C','22'] ).
verb( 'arrive', 'arrive', 'arrives', 'arriving', 'arrived', 'arrived', intran, ['2A','2C','3A'] ).
verb( 'be', 'be', 'is', 'being', 'was', 'been', aux, ['1','4F'] ).
verb( 'be', 'am', '-', '-', '-', '-', aux, ['1','4F'] ).
verb( 'be', 'are', '-', '-', '-', '-', aux, ['1','4F'] ).
verb( 'begin', 'begin', 'begins', 'beginning', 'began', 'begun', _, ['2A','3A','6A','6D','7A'] ).
verb( 'believe', 'believe', 'believes', 'believing', 'believed', 'believed', _, ['3A','6A','9','25'] ).
verb( 'book', 'book', 'books', 'booking', 'booked', 'booked', tran, ['6A'] ).
verb( 'can', 'can', 'can', '-', 'could', '-', aux, ['5','6A'] ).
verb( 'dislike', 'dislike', 'dislikes', 'disliking', 'disliked', 'disliked', tran, ['6A','6C'] ).
verb( 'do', 'do', 'does', 'doing', 'did', 'done', aux, ['5'] ).
verb( 'fly', 'fly', 'flies', 'flying', 'flew', 'flew', _, ['2A','2B','2C','2D','4A','6A','15A','15B'] ).
verb( 'go', 'go', 'goes', 'going', 'went', 'gone', intran, ['2A','2B','2C','2D','2E','3A','4A','6A','15B'] ).
verb( 'give', 'give', 'gives', 'giving', 'gave', 'given', ditran, ['12A'] ).
verb( 'hate', 'hate', 'hates', 'hating', 'hated', 'hated', tran, ['6A','6D','7A','17','19C'] ).
verb( 'leave', 'leave', 'leaves', 'leaving', 'left', 'left', _, ['2A','2C','3A','6A','12B','13B','14','15A','15B','16A','19B','22','24B','25'] ).
verb( 'like', 'like', 'likes', 'liking', 'liked', 'liked', tran, ['6A','6D','7A','17','19B','19C','22'] ).
verb( 'may', 'may', 'may', '-', '-', '-', aux, ['5'] ).
verb( 'read', 'read', 'reads', 'reading', 'read', 'read', tran, ['6A','6D','7A','17','19B','19C','22'] ).
verb( 'sneeze', 'sneeze', 'sneezes', 'sneezing', 'sneezed', 'sneezed', intran, ['2A'] ).
verb( 'snore', 'snore', 'snores', 'snoring', 'snored', 'snored', intran, ['2A','2C'] ).
verb( 'start', 'start', 'starts', 'starting', 'started', 'started', _, ['2A','2C','3A','6A','6D','7A','15A','19B'] ).
verb( 'stay', 'stay', 'stays', 'staying', 'stayed', 'stayed', _, ['2A','2B','2C','4A','6A','15B'] ).
verb( 'think', 'think', 'thinks', 'thinking', 'thought', 'thought', _, ['2A','2B','2C','3A','6A','9','15B','22','25'] ).
verb( 'travel', 'travel', 'travels', 'travelling', 'travelled', 'travelled', _, ['2A','2B','2C','3A','4A'] ).
verb( 'want', 'want', 'wants', 'wanting', 'wanted', 'wanted', _, ['2A','3A','6A','6E','7A','17','19B','24A'] ).
verb( 'will', 'will', 'will', '-', '-', '-', aux, ['2A','5','6A','9','12A','13A','14','15A','17'] ).
verb( 'wonder', 'wonder', 'wonders', 'wondering', 'wondered', 'wondered', _, ['2A','3A','3B','8','10'] ).
verb( 'would', 'would', 'would', '-', 'would', '-', aux, ['5','6A'] ).

noun( 'bill', 'bill', '-', proper, male ).
noun( 'john', 'john', '-', proper, male ).
noun( 'mary', 'mary', '-', proper, female ).

noun( 'europe', 'europe', '-', proper, loc ).
noun( 'london', 'london', '-', proper, dest ).
noun( 'paris', 'paris', '-', proper, dest ).

noun( 'aeroplane', 'aeroplane', 'aeroplanes', both, trans ).
noun( 'airplane', 'airplane', 'airplanes', both, trans ).
noun( 'boat', 'boat', 'boats', both, trans ).
noun( 'car', 'car', 'cars', both, trans ).
noun( 'plane', 'plane', 'planes', both, trans ).
noun( 'train', 'train', 'trains', both, trans ).

noun( 'book', 'book', 'books', count, obj ).
noun( 'hotel', 'hotel', 'hotels', count, obj ).
noun( 'room', 'room', 'rooms', count, obj ).
noun( 'table', 'table', 'tables', count, obj ).
noun( 'ticket', 'ticket', 'tickets', count, obj ).
noun( 'trip', 'trip', 'trips', count, obj ).

noun( 'boy', 'boy', 'boys', count, male ).
noun( 'girl', 'girl', 'girls', count, female ).
noun( 'student', 'student', 'students', count, per ).
noun( 'policeman', 'policeman', 'policemen', count, male ).
noun( 'right', 'right', 'rights', count, obj ).

noun( 'sugar', 'sugar', 'sugars', both, obj ).
noun( 'travel', 'travel', 'travels', both, obj ).

noun( 'january', 'january', '-', proper, month ).
noun( 'february', 'february', '-', proper, month ).

noun( 'bank_1', 'bank', 'banks', count, obj ).
noun( 'bank_2', 'bank', 'banks', count, loc ).
noun( 'bank_1', 'financial-institution', 'financial-institution', count, obj ).
noun( 'bank_2', 'riverside', 'riversides', count, loc ).

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

pron( 'his', case, det, male ).
pron( 'her', case, det, female ).

det( 'a', quant, exists ).
det( 'an', quant, exists ).
det( 'the', quant, the ).
% manual additions
det( 'every', quant, forall ).
det( 'no', quant, notexists ).
det( 'some', quant, exists ).
det( 'which', whq, _ ).

prep( 'by', trans ).
prep( 'from', loc ).
prep( 'in', loc ).
prep( 'in', tim ).
prep( 'on', tim ).
prep( 'to', loc ).

conj( 'and', '&' ).

misc( 'to', partcl, '-' ).

misc( 'that', comp, '-' ).

misc( 'too', too, '-' ).

misc( 'hallo', interj, greet ).
misc( 'hello', interj, greet ).
misc( 'hi', interj, greet ).
misc( 'hullo', interj, greet ).
misc( 'bye', interj, close ).
misc( 'exit', interj, close ).
misc( 'quit', interj, close ).
misc( 'thank-you', interj, thank ).
misc( 'thanks', interj, thank ).

polar( 'yes', plus, true ).
polar( 'ok', plus, true ).
polar( 'no', plus, untrue ).
polar( 'probably', plus, probable ).
