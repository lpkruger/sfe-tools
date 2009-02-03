:- dynamic(hasLearned/2).
:- dynamic(canLearn/2).

isParty(alice).
isParty(bob).
belongs(x, alice).
belongs(m, alice).
belongs(y, bob).
belongs(n, bob).
belongs(r1, alice).
belongs(r2, bob).

%mayUse(reEnc).
%mayUse(sfe).


%canLearn(_,_) :- fail.
%canLearn(mul(add(x,y),r1), alice).
%canLearn(add(x,y), alice).
%canLearn(mul(x,y), alice).
%canLearn(add(y,r1), bob).
%canLearn(add(x,add(y,r1)), alice).

test :- derive(add(x,y), alice).
%test :- derive(add(x,add(y,r1)), alice).



%:- table has/2.
%:- table sentTo/3.
%:- table calc/2.

init :- b_setval(out, []), nb_setval(out2, []).
appendGlobal(X) :-
	b_getval(out, R), 
	(memberchk(X, R) -> !, true ; 
	b_setval(out, [X | R]), b_getval(out, Q), nb_setval(out2, Q)).


writeCalcTerm(addEnc(enc(X,EE),enc(Y,EE))) :-
	Z = enc(add(X,Y),EE), writeProtoTerm(Z), write(' = addEnc('),
	Args = [ enc(X,EE), enc(Y,EE) ], writeCalcTermList(Args), writeln(');').
writeCalcTerm(subEnc(enc(X,EE),enc(Y,EE))) :-
	Z = enc(sub(X,Y),EE), writeProtoTerm(Z), write(' = subEnc('),
	Args = [ enc(X,EE), enc(Y,EE) ], writeCalcTermList(Args), writeln(');').
writeCalcTerm(mulEnc(enc(X,EE),enc(Y,EE))) :- EE=pubKey(E,eujin1), !,
	Z = enc(mul(X,Y),pubKey(E,eujin2)), writeProtoTerm(Z), write(' = mulEnc('),
	Args = [ enc(X,EE), enc(Y,EE) ], writeCalcTermList(Args), writeln(');').
writeCalcTerm(mulEnc(enc(X,EE),enc(Y,EE))) :-
	Z = enc(mul(X,Y),EE), writeProtoTerm(Z), write(' = mulEnc('),
	Args = [ enc(X,EE), enc(Y,EE) ], writeCalcTermList(Args), writeln(');').
writeCalcTerm(mulEnc(enc(X,EE),Y)) :- fail.    % -----> TODO
writeCalcTerm(reEnc(enc(X, E2), E1)) :-
	Z = enc(X,E2), writeProtoTerm(Z), write(' = reEnc('),
	Args = [ enc(X,E1), E1, E2 ], writeCalcTermList(Args), writeln(');').
writeCalcTerm(dec(enc(X,EE),DD)) :- 
	writeProtoTerm(X), write(' = dec('), 
	Args = [ enc(X,EE), DD], writeCalcTermList(Args), writeln(');').

writeCalcTerm(X) :- X =.. [Func | Args], writeProtoTerm(X), write(' = '), write(Func), write('('),
	writeCalcTermList(Args), writeln(');').

writeCalcTermList([]).
writeCalcTermList([H]) :- writeProtoTerm(H).
writeCalcTermList([H | R]) :-  writeProtoTerm(H), write(', '), writeCalcTermList(R).


writeProtoTerm(X) :- atomic(X), write(X).
writeProtoTerm(X) :- X =.. [Func | Args], writeProtoTermList([Func | Args]).
writeProtoTerm(X) :- nl, write('UNKNOWN: '), writeln(X).

writeProtoTermList([]).
writeProtoTermList([H | R]) :- writeProtoTerm(H), write('_'), writeProtoTermList(R).

writeProtoHeader(W) :- write('function '), write(W), write('() '), writeln('{').
writeProtoFooter(_) :- writeln('}').

outProto(W) :- !, writeln('-------\n'), nb_getval(out2, R), 
	writeProtoHeader(W), outProto(R, W), writeProtoFooter(W), !.

outProto([], _).
outProto([sendTo(X, W, B) | R], W) :- outProto(R, W),
	write('send("'), write(B), write('", '),
	writeProtoTerm(X), writeln(');').
outProto([sendTo(X, A, W) | R], W) :- outProto(R, W),
	writeProtoTerm(X), write(' = '),
	write('recv("'), write(A), writeln('");').

outProto([calc(X, W) | R], W) :- outProto(R, W),
	writeCalcTerm(X), nl.

outProto([_ | R], W) :- outProto(R, W).
outProto(X, _) :- nl, write('UNKNOWN: '), writeln(X).



output(all) :- !, writeln('-------\n'), nb_getval(out2, R), output(R), !.
output(W) :- isParty(W), !, writeln('-------\n'), nb_getval(out2, R), output(R, W), !.
	

output([], _).
output([sendTo(X, W, B) | R], W) :- output(R, W),
	write(W), write(': sendTo('), write(B), write(', '), write(X), write(')\n').
output([sendTo(X, A, W) | R], W) :- output(R, W),
	write(W), write(': readFrom('), write(A), write(', '), write(X), write(')\n').
	

output([calc(X, W) | R], W) :- output(R, W),
	write(W), write(': '), write(X), write('\n').
output([sfe(X, W) | R], W) :- output(R, W),
	write(W), write(': '), write('SFE('), write(X), write(')\n').
output([_ | R], W) :- output(R, W).

%special case for add:
%output(calc(add(X,Y), W)) :-
%  write(W), write(': '), write(X), write('+'), write(Y), write('\n').
%%%%
output([]).
output([sendTo(X, A, B) | R]) :- output(R),
  write(A), write(': sendTo('), write(B), write(', '), write(X),
  write(')\n').
output([calc(X, W) | R]) :- output(R),
  write(W), write(': '), write(X), write('\n').
output([sfe(X, W) | R]) :- output(R),
  write(W), write(': '), write(sfe(X)), write('\n').


%sentTo(X, A, B) :- has(X,B).
sentTo(X, A, B) :- isParty(A), isParty(B), A\==B, appendGlobal(sendTo(X, A, B)).

calc(add(X,Y),W) :- isParty(W), appendGlobal(calc(add(X,Y),W)).
calc(sub(X,Y),W) :- isParty(W), appendGlobal(calc(sub(X,Y),W)).

calc(X,W) :- isParty(W), appendGlobal(calc(X, W)).

isntEnc(X) :- not(X = enc(_,_)), not(X = pubKey(_,_)).

%isAdditive(paillier).
isAdditive(eujin1).
isAdditive(eujin2).
%isMultiplicative(elgamal).
isMultiplicative(_) :- fail.

isPubKey(EE) :- EE=pubKey(X, M), isParty(X), ( isAdditive(M) ; isMultiplicative(M) ).

% reuse computations
%has(X,W) :- hasLearned(X,W), !.

% ground rules: people have their own keys and inputs and constants
has(X,W) :- belongs(X,W).
has(X,W) :- integer(X).
has(privKey(A),A).
has(pubKey(A,M),A) :- isPubKey(pubKey(A,M)).
has(pubKey(A,M),W) :- isPubKey(pubKey(A,M)), sentTo(pubKey(A,M), A, W).

% simple arithmetic
has(add(X,Y),W) :- has(X, W), has(Y, W), calc(add(X,Y),W).
has(sub(X,Y),W) :- has(X, W), has(Y, W), calc(sub(X,Y),W).
has(mul(X,Y),W) :- has(X, W), has(Y, W), calc(mul(X,Y),W).

% can use public key to encrypt
has(enc(X, EE), W) :- isPubKey(EE), has(EE, W), has(X, W), calc(enc(X,EE), W).

% can send an encrypted value to someone if it's not their key
has(enc(X, EE), W) :- isPubKey(EE), EE=pubKey(E, M), W\==E, has(enc(X, EE), V), sentTo(enc(X, EE), V, W).

% can send an encrypted value to someone with their own key if they
% are allowed to learn the value
%
has(X, W) :- canLearn(X, W), has(X, V), writeln('*'), sentTo(X, V, W).

has(enc(X, EE), W) :- isPubKey(EE), EE=pubKey(E, _), W==E, canLearn(X, W), has(enc(X, EE), V), writeln('*'), sentTo(enc(X, EE), V, W).

% can decrypt an encrypted value if they have the private key
% D=W assumes no sharing of priv keys
has(X,W) :- D=W, EE=pubKey(D, M), isPubKey(EE), isntEnc(X), has(enc(X,EE), W), 
	DD=privKey(D, M), calc(dec(enc(X,EE),DD),W).

%%% additive homomorphic rules
% can add two values encrypted with E's key
has(enc(add(X,Y),EE), W) :- EE=pubKey(_,M), isAdditive(M), isPubKey(EE),
	has(enc(X,EE),W), has(enc(Y,EE),W),
  	calc(addEnc(enc(X,EE),enc(Y,EE)), W).  
has(enc(sub(X,Y),EE), W) :- EE=pubKey(_,M), isAdditive(M), isPubKey(EE),
	has(enc(X,EE),W), has(enc(Y,EE),W),
  	calc(subEnc(enc(X,EE),enc(Y,EE)), W).  

% can multiply an encrypted value by a non-encrypted value
has(enc(mul(X,Y),EE), W) :- EE=pubKey(_,M), isAdditive(M), isPubKey(EE),
	has(enc(X,EE),W), has(Y,W), calc(mulEnc(enc(X,EE),Y), W).
%has(enc(mul(Y,X),EE), W) :- EE=pubKey(_,M), isAdditive(m), isPubKey(EE), 
%	has(enc(X,EE),W), has(Y,W), calc(mulEnc(enc(X,EE),Y), W).

has(enc(mul(Y,X),EE), W) :- has(enc(mul(X,Y), EE), W).
	
%%% multiplicative homomorphic rule
has(enc(mul(X,Y),EE), W) :- EE=pubKey(_,M), isMultiplicative(M), isPubKey(EE),
	has(enc(X,EE),W), has(enc(Y,EE),W),
  	calc(mulEnc(enc(X,EE),enc(Y,EE)), W).  
has(enc(exp(X,Y),EE), W) :- EE=pubKey(_,M), isMultiplicative(M), isPubKey(EE),
	has(enc(X,EE),W), has(Y,W), calc(expEnc(enc(X,EE),Y), W).
%
%%% special Eu-Jin version
has(enc(mul(X,Y),E2), W) :- E2=pubKey(E,eujin2), isParty(E), E1=pubKey(E,eujin1),
	has(enc(X,E1),W), has(enc(Y,E1),W),
  	calc(mulEnc(enc(X,E1),enc(Y,E1)), W).  
%

% can create random blinding value
has(mul(X,R), W) :- has(X, W), isNewBlind(R), calc(random(R)).

% can send blinded value to another party
has(mul(X,R), W) :- isBlind(R), has(mul(X,R), A), sendTo(R, A, W).

% division can cancel multiplication
has(div(X,Y), W) :- has(mul(X,R), W), has(mul(Y,R), W), calc(div(mul(X,R),mul(Y,R))).

% catch all using reencryption
%has(enc(add(x,y), pubKey(_, multiplicative)), _) :- trace, fail.
has(enc(X, E2), W) :- mayUse(reEnc), E2=pubKey(E, M2), isPubKey(E2),
	E1=pubKey(E, M1), isPubKey(E1), M1\=M2, has(enc(X, E1), W),
	has(E2, W), has(E1, W),
	calc(reEnc(enc(X, E2), E1), W).
% catch all using SFE
has(X,W) :- mayUse(sfe), canLearn(X,W), appendGlobal(sfe(X,W)), true.

% For YAP
%call_with_depth_limit(A,B,_) :- depth_bound_call(A,B).
/*
derive1(X, W, L) :- call_with_depth_limit(has(X, W), L, Z), 
  Z \== depth_limit_exceeded.

derive(X, W) :- derive0(X, W, 1) , !.
*/

:- dynamic shownProto/2.
seenProto(X) :- nb_getval(out2, R), shownProto(X, R).
assertProto(X) :- nb_getval(out2, R), asserta(shownProto(X, R)).
derive(X, W) :- retractall(hasLearned(_,_)), retractall(shownProto(X, _)), 
	affirm(canLearn(X,W)), init, clause_tree(has(X,W), []), 
	not(seenProto(X)), output(all), output(alice), output(bob), assertProto(X).

derive0(X, W, Proto) :- derive(X,W), nb_getval(out2, Proto).
derive(X, W, Set) :- setof(Proto, derive0(X, W, Proto), Set).

deriveBest(X, W) :- derive(X, W, Set), shortest(Set, R), nb_setval(out2, R), 
	output(all), outProto(alice), outProto(bob).

clear :- retractall(canLearn(_,_)), retractall(shownProto(_,_)).

%% LL is a list of lists, L is the shortest list
shortest(LLL, L) :- LLL = [L0 | LL], shortest0(LL, L0, L).
shortest0([], L, L).
shortest0([ L0 | LL ], L, LOut) :- length(L0, N0), length(L, N1), 
	(N0<N1 -> shortest0(LL, L0, LOut) ; shortest0(LL, L, LOut)).

%%%%%%%%%%% Meta-Interpreter %%%%%%%%%%%%%

subst([],[]).
subst([H|R], Z) :- var(H), !, subst(R,RR), Z=[H|RR].
subst([has(X,Y)|R], Z) :- subst(R,RR), Z=[has(X,Y,In,Out)|RR].
subst([H|R], Z) :- subst(R,RR), Z=[H|RR].
subst0(In,Out) :- In =.. InList, subst(InList, OutList), 
  Out =.. OutList.

/*
xform(Z) :- clause(has(X,W),C),
   subst0(C, CC), Z=(has(X,W,In,Out) :- CC), asserta(Z).
   
gsub([],[]).
gsub([H|R], Z) :- var(H), !, gsub(R,RR), Z=[_|RR].
gsub([H|R], Z) :- compound(H), !, gsub(H,HH), gsub(R,RR), Z=[HH|RR].
gsub([H|R], Z) :- gsub(R,RR), Z=[H|RR].
gsub(In,Out) :- In =.. InList, gsub(InList, OutList), Out =.. OutList.
*/

clause_tree(true,_) :- !. 
clause_tree((G,R),Trail) :-
   !, 
   clause_tree(G,Trail),
   clause_tree(R,Trail). 
%clause_tree(G,Trail) :- G=has(X,W), write(G), write('  '), writeln(Trail), fail.
%clause_tree(G,Trail) :- G=has(enc(add(x, y), alice), bob), trace, fail.
%clause_tree(G,Trail) :- G=has(enc(x,alice), alice), trace, fail.
clause_tree(G,Trail) :- G=appendGlobal(A), writeln([A | Trail]), fail.
clause_tree(G,Trail) :-
   inner_loop_detect([G | Trail]),
   !, fail.
% Let prolog handle built-ins
clause_tree(G,_) :- predicate_property(G,built_in), !, call(G).
clause_tree(G,Trail) :- 
   (G=has(X,W) -> isParty(W) ; true),
   clause(G,Body),
   %once(gsub(G, GG)),
   clause_tree(Body, [G|Trail]),
   (G=has(X,W) -> affirm(hasLearned(X,W)) ; true).


affirm(X) :- clause(X, true), !.
affirm(X) :- asserta(X).

inner_loop_detect([H|R]) :- loop_detect(H,R).
inner_loop_detect([_|R]) :- inner_loop_detect(R).

loop_detect(G,[G1|_]) :- G == G1.
loop_detect(G,[_|R])  :- loop_detect(G,R). 

%loop_detect(G, L) :- trace, setof(X,X==G,Z), length(Z,N), loop_detect(G,L,N).
%loop_detect(G, L, 0).
%loop_detect(G,[G1|R],N) :- G =%= G1, !, NN is N-1, loop_detect(G,R,NN).
%loop_detect(G,[_|R],N)  :- loop_detect(G,R,N). 

%list_length(L,N).

can_unify(G, G1) :- not(not(G=G1)).

l(X,Y) :- not(not(X=Y)), belongs(X,Y).






