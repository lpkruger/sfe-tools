There are three protocols that can be used
to compute edit distance:  Protocol 1, protocol 2, and protocol 3.

-----------------------------------
To run protocol 1:

First, prepare a circuit for the problem size you will be evaluating.  For example, if Alice's string is length 2 and Bob's string is length 3, then the circuit is prepared as follows:
$ cd editdist
$ mkcirc.sh 2 3 > circ_8_2_3.txt

Compile the circuit:
compile.sh circ_8_2_3.txt

It should output the files: circ_8_2_3.circ and circ_8_2_3.txt.fmt

Now, the protocol can be run as follows:

Start Bob first:
bob1.sh "BBB"

Then start Alice.  If you are running on difference machines, substitute the machine name for Bob instead
of "localhost"

alice1.sh localhost "AA"

-----------------------------------
To run protocol 2:

Start Bob first:
bob2c.sh "BBB"

Then start Alice.
alice2c.sh localhost "AA"

-----------------------------------
To run protocol 3:

First, prepare a circuit for the block size you will be evaluating.  For
example, if the block size is 4x4 then you can create the block circuit
like this:
$ cd editdist
$ mkcirc3.sh 4 4 > block_8_4_4.txt

Compile the circuit:
compile.sh -c block_8_4_4.txt

It should output the files: block_8_4_4.txt.circ and block_8_4_4.fmt

Assume Alice's string is length 8 and Bob's string is length 8.  Boths
lengths must be a multiple of the block size.  You can use padding to make
sure this is true.

Start Bob first:
bob3.sh "BBBBBBBB" 4

Then start Alice.
alice3.sh localhost "AAAAAAAA" 4


Smith-Waterman
