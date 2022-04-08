/*
 * key.c -- common stuff used in keyboard simulators.
 */

#include <stdio.h>
#include <string.h>

#include "key.h"

volatile unsigned char keybits[256];
volatile unsigned char currentkey;
