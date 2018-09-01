
#ifndef __UTILS_H__
#define __UTILS_H__

#include "defs.h"
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <unistd.h>

#define RETROSTORE_HOST "retrostore.org"
#define RETROSTORE_PORT 80

#define LOG(msg) printf("%s\n", msg)

bool connect_server(int* fd);
bool skip_to_body(int fd);

#ifdef ANDROID
#if defined(__BIONIC_FORTIFY)
#  define bcopy(b1, b2, len) \
    (void)(__builtin___memmove_chk((b2), (b1), (len), __bos0(b2)))
#  define bzero(b, len) \
    (void)(__builtin___memset_chk((b), '\0', (len), __bos0(b)))
#else
# define bcopy(b1, b2, len) (void)(__builtin_memmove((b2), (b1), (len)))
# define bzero(b, len) (void)(__builtin_memset((b), '\0', (len)))
#endif
#endif

#endif
