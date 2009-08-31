/*
 * sillymain.h
 *
 *  Created on: Aug 23, 2009
 *      Author: louis
 */

#ifndef SILLYMAIN_H_
#define SILLYMAIN_H_

typedef int (*main_ptr)(int,char**);
extern void* add_main(const char* name, main_ptr main_f);
#define MAIN(name) static void* p_main = add_main(name, &_main);
#define MAINF(name, f) static void* p_main_##f = add_main(name, &f);


#endif /* SILLYMAIN_H_ */
