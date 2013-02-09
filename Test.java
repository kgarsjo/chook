/*-------------------------------------------//
//	Message Passing Prototype
//		(Read Side)
//
//	Author: Kevin Garsjo
//	Class:	UO CIS 415, Operating Systems
//	
//	Def:	Opens a named pipe and reads
//	its contents forever.
//
//-------------------------------------------*/

import java.io.*;

static String buffline;

class Test {

	public static void main( String[] args ) {
		
		Thread t = new Thread(new Runnable() {
		    public void run() {
			      
		    	try  {
					BufferedReader inFile = new BufferedReader( new FileReader("./PIPE") );
			
					while (true) {
						String line = inFile.readLine();
						if (line != null) {
							buffline = "Message Received ::> " + line;
						}
					}
				} catch (FileNotFoundException e) {
					System.err.println("File not found!");
				} catch (IOException e) {
					System.err.println("IO Exception!");
				}
		      
		    }
		  })
		
		t.start();
	}

}
