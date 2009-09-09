/*
 * sillymain.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYMAIN_H_
#define SILLYMAIN_H_

typedef int (*main_ptr)(int,char**);

#ifdef ISMAINPROG
#define MAIN(name) int main(int argc, char **argv) { return _main(argc, argv); }
void* add_main(const char* name, main_ptr main_f) {}
#else
extern void* add_main(const char* name, main_ptr main_f);
#define MAIN(name) static void* p_main __attribute__ ((unused)) = add_main(name, &_main);
#define MAINF(name, f) static void* p_main_##f __attribute__ ((unused)) = add_main(name, &f);
#endif

#endif /* SILLYMAIN_H_ */
