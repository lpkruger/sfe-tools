/*
 * SSH_Dummy.cpp
 *
 *  Created on: Sep 5, 2009
 *      Author: louis
 */

#include <stdio.h>

// dummy implementation.  This file is not linked with dropbear
bool ssh_writePacket(const unsigned char *buf, int length) {
	printf("Writing a packet of length %d", length);
	return true;
}
