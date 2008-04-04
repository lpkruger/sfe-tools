:- dynamic(has/2).
:- dynamic(canLearn/2).

isParty(alice).
isParty(bob).
belongs(x, alice).
belongs(m, alice).
belongs(y, bob).
belongs(n, bob).
belongs(r1, alice).
belongs(r2, bob).

canLearn(_,_) :- fail.
canLearn(mul(add(x,y),r1), alice).
canLearn(add(x,y), alice).
canLearn(add(y,r1), bob).
canLearn(add(x,add(y,r1)), alice).

%test :- derive(add(x,y), alice).
test :- derive(add(x,add(y,r1)), alice).



%:- table has/2.
%:- table sentTo/3.
%:- table calc/2.

init :- b_setval(out, []), nb_setval(out2, []).
appendGlobal(X) :-
  b_getval(out, R), b_setval(out, [X | R]),
  b_getval(out, Q), nb_setval(out2, Q).
output :- writeln('-------\n'), nb_getval(out2, R), output(R).


output([]).
output([sendTo(X, A, B) | R]) :- output(R),
  write(A), write(': sendTo('), write(B), write(', '), write(X), write(')\n').

%special case for add:
%output(calc(add(X,Y), W)) :-
%  write(W), write(': '), write(X), write('+'), write(Y), write('\n').

output([calc(X, W) | R]) :- output(R),
  write(W), write(': '), write(X), write('\n').

%sentTo(X, A, B) :- has(X,B).
sentTo(X, A, B) :- isParty(A), isParty(B), A\==B, appendGlobal(sendTo(X, A, B)).

calc(add(X,Y),W) :- isParty(W), appendGlobal(calc(add(X,Y),W)).
calc(sub(X,Y),W) :- isParty(W), appendGlobal(calc(sub(X,Y),W)).

calc(X,W) :- isParty(W), appendGlobal(calc(X, W)).

isntEnc(X) :- not(X = enc(_,_)).

% ground rules: people have their own keys and inputs and constants
has(X,W) :- belongs(X,W).
has(X,W) :- integer(X).
has(privKey(A),A).
has(pubKey(A),A).
has(pubKey(A),W) :- sentTo(pubKey(A), A, W).

% simple arithmetic
has(add(X,Y),W) :- has(X, W), has(Y, W), calc(add(X,Y),W).
has(sub(X,Y),W) :- has(X, W), has(Y, W), calc(sub(X,Y),W).
has(mul(X,Y),W) :- has(X, W), has(Y, W), calc(mul(X,Y),W).

% can use public key to encrypt
has(enc(X, E), W) :- has(pubKey(E), W), has(X, W), calc(enc(X,E), W).

% can send an encrypted value to someone if it's not their key
has(enc(X, E), W) :- W\==E, has(enc(X, E), V), sentTo(enc(X, E), V, W).

% can send an encrypted value to someone with their own key if they
% are allowed to learn the value
has(enc(X, E), W) :- canLearn(X, W), has(enc(X, E), V), writeln('*'), sentTo(enc(X, E), V, W).

% can decrypt an encrypted value if they have the private key
% D=W assumes no sharing of priv keys
has(X,W) :- D=W, isntEnc(X), has(enc(X,D), W), calc(dec(enc(X,D),D),W).

% can add two values encrypted with E's key
has(enc(add(X,Y),E), W) :- has(enc(X,E),W), has(enc(Y,E),W),
  calc(addEnc(enc(X,E),enc(Y,E)), W).  
has(enc(sub(X,Y),E), W) :- has(enc(X,E),W), has(enc(Y,E),W),
  calc(subEnc(enc(X,E),enc(Y,E)), W).  

% can multiply an encrypted value by a non-encrypted value
has(enc(mul(X,Y),E), W) :- has(enc(X,E),W), has(Y,W),
  calc(mulEnc(enc(X,E),Y), W).
has(enc(mul(Y,X),E), W) :- has(enc(X,E),W), has(Y,W),
  calc(mulEnc(enc(X,E),Y), W).

% For YAP
%call_with_depth_limit(A,B,_) :- depth_bound_call(A,B).
/*
derive1(X, W, L) :- call_with_depth_limit(has(X, W), L, Z), 
  Z \== depth_limit_exceeded.

derive(X, W) :- derive0(X, W, 1) , !.
*/

derive(X, W) :- init, clause_tree(has(X,W), []), output.



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
   (G=has(X,W) -> affirm(G) ; true).


affirm(X).
%affirm(X) :- clause(X,true), !.
%affirm(X) :- asserta(X).

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
