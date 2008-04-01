SFE Compiler version 0.3
(C) 2005-2008 Louis Kruger
-------------------------------

To invoke:
compile program.txt
This will produce files called program.circ, program.fmt, and program.split

To run:
run_bob 5678 0 1 0 1 1...  (as many bits as Bob has as input)
run_alice localhost 5678 program.circ program.split 1 0 1 0...  (as many bits as Alice has as input)

Features:

It is a simple typed functional language
(look at the examples for details of language syntax)

- constant declarations
- integer/struct/array types
- struct dereferencing
- array indexing
- addition/subtraction/multiplication/division (+ - * /)
- variable assignment
- left and right shifting ( << >> )
- comparisions (== != < > <= >=)
- bitwise AND / OR / XOR ( & | ^ )
- if/else
- arrays
- for loops
- comments ( C-style /* */ and C++ style // )

===============================================================

Changelog:
Version 0.2:
+ Division "/" has been fixed.  Also fixed a bug in subtraction, an
circuit optimization bug, and several other miscellaneous bugs uncovered
during testing.

Version 0.1:
first release

===============================================================

Limitations:
- Can only define one main function called "output".  Subroutine calls will be supported
in a future version but I don't need them for current experiments.
- Error handling isn't great.  SFDL programs with an error will often produce "InternalCompilerError"
rather than a helpful error message.  I want to fix as many of these as possible but it is lower
priority.
- Bob gets all output bits.

Planned for future:
- signed integers (currently all are treated as unsigned)
- function calls
- mod operator
- bit permutations


notes:
there is no limit on type sizes, Theoretical you should be able to declare very large
integers.
