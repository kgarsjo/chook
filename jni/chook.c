#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <unistd.h>
#include <jni.h>

#define SENTINEL "endJNImain\n"

// Inter-layer communication file-descriptors
int fd_out;
int fd_in;

// Temp buffers to hold Strings from the Java environment
const char *in;
const char *out;

/*-----------------------------------------------------//
| makeFIFO() - Creates two named pipes for in/out, or
|	ensures they exist already.
|
| JNIEnv *env, jobject obj - Java JNI Overhead variables
| jstring inFile  - The path to pull C input from
| jstring outFile - The path to push C output to
-------------------------------------------------------*/
JNIEXPORT int JNICALL Java_com_os_chook_CHook_makeFIFO(JNIEnv *env, jobject obj, jstring inFile, jstring outFile) {

	// Convert java-formatted strings to c-strings
	in  = (*env)->GetStringUTFChars(env, inFile, 0);
	out = (*env)->GetStringUTFChars(env, outFile, 0);

	// Check on FIFO's (0666 <=> S_IRUSR | S_IWUSR)
	int res = mkfifo(in, 0666);
	if (res < 0 && errno != 17) { return errno; }
	res = mkfifo(out, 0666);
	if (res < 0 && errno != 17) { return errno; }

	// Free java strings
	(*env)->ReleaseStringUTFChars(env, inFile, in);
	(*env)->ReleaseStringUTFChars(env, outFile, out);

	return 0;
}


/*-----------------------------------------------------//
| openFIFO() - Opens the STDOUT pipe to Java
|
| JNIEnv *env, jobject obj - Java JNI Overhead variables
| jstring outFile - The path to push C output to
-------------------------------------------------------*/
JNIEXPORT int JNICALL Java_com_os_chook_CHook_openFIFO(JNIEnv *env, jobject obj, jstring outFile) {

	// Clear all outgoing file-descriptors of clutter
	fflush(NULL);

	// Open the pipe
	fd_out = open("/data/data/com.os.chook/files/in-fifo", O_WRONLY);
	if (fd_out < 0) { return 0; }

	// When STDOUT is closed, dup() will reassign the given file
	// descriptor to the lowest availible file descriptor (being STDOUT,
	// as it == 1).
	close(STDOUT_FILENO);
	dup(fd_out);

	// Prevent STDOUT from buffering and holding onto output unneccessarily
	setvbuf(stdout, NULL, _IOLBF, 0);

	// Now, printf(), write(STDOUT_FILENO,...), etc, can be used like normal.
	// C is configured to pipe all STDOUT through the named pipe to the Java side.

	return 0;
}


/*-----------------------------------------------------//
| jniMain() - Execution Method for C!
-------------------------------------------------------*/
int Java_com_os_chook_CHook_jniMain() {
	// TODO: Application logic goes below.



	// All jniMain() calls must end by printing the
	// SENTINEL and returning. 
	printf(SENTINEL);
	return 0;
}
