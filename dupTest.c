/*-------------------------------------------------//
//	Message Passing Prototype
//		(Write Side)
//
//	Author:	Kevin Garsjo
//	Class:	UO CIS 415, Operating Systems
//
//	Def:	Opens a named pipe and writes a
//	constant string per second n times.
//
//-------------------------------------------------*/


#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>

#define TRUE  1
#define FALSE 0


int main() {

	int res = mkfifo("./PIPE", 0666);
	if (res == -1 && errno != EEXIST) {
		perror("FIFO: ");
	}

	//pid_t pid = fork();

	//if (pid < 0) {	// Error case
	//	perror("Fork: ");

	//} else if (pid == 0){	// Child case
		int fd = open("./PIPE", O_WRONLY);
		close(STDOUT_FILENO);
		dup(fd);
		setvbuf(stdout, NULL, _IOLBF, 0);

		int i;
		for (i = 0; i < 5; i++) {
			printf("Hello\n");
			sleep(1);
		}

		exit(0);


	/*} else {	// Parent Case
		int fd = open("./PIPE", O_RDONLY);
		int bytes;
		char buf[1024];

		while (TRUE) {
			bytes = read(fd, buf, 1024);
			if (bytes > 0 ) {
				write(STDOUT_FILENO, "Message Received ::> ", 20);
				write(STDOUT_FILENO, buf, bytes);
			}
		}

	}*/

	return 0;
}
