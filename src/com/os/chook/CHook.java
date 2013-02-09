package com.os.chook;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import java.io.*;
import java.util.*;

public class CHook extends Activity {
	
	// Constant global strings
	final static String separator 	= "------------------------------------------------------------------------\n";
	final static String OUTFILE 	=  "/out-fifo";	// Pipes java output to c input
	final static String INFILE		=  "/in-fifo";	// Pipes c output to java input
	final static String SENTINEL = "endJNImain";
	
	// Android app-specific data path 
	// ( usually /data/data/com.os.chook/files )
	static File dataPath;
	
	// Static IPC-related variables; need to be
	// static for various threading reasons
	static String buffline;
	static int proc_return = 0;
	static boolean proc_finished = false;
	static BufferedReader inFile;
	
	// Android View Elements
	static TextView title;
	static EditText descript;
	static Button runButton;
	

	// Load the Dynamic Library; must be done before anything else
	static {
		System.loadLibrary("chook");
	}
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	// Android Overhead, Instantiate the Views 'n stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get the data path for pipe creating / reading / writing
        dataPath = this.getApplicationContext().getFilesDir();
        
        // Instantiate title TextView
        title = (TextView) findViewById(R.id.welcomeText);
      
        // Instantiate EditText view, show the readme, and disable interactivity
        descript = (EditText) findViewById(R.id.descripText);
		String str = readFromResource(R.raw.readme);
        descript.setText(str, TextView.BufferType.NORMAL);
        descript.setKeyListener(null);
        
        // Instantiate Run Button and define its onClick functionality
        runButton = (Button) findViewById(R.id.runButton);
        runButton.setOnClickListener( new OnClickListener() {
        	public void onClick(View v) {
        		
        		// Reconfigure visual environment
        		title.setText("Displaying C-Hook output:");
        		descript.setText("");
        		runButton.setVisibility(8);
        		
        		// Ensure the named pipes exist or are created
    			int ret = makeFIFO(dataPath.getPath() + OUTFILE, dataPath.getPath() + INFILE);
    			if (ret != 0) {
    				return;
    			}
    			
    			// Thread actual JNI execution separately from the UI thread
    			runtimeThread.start();
    			  
    			// Thread buffer-checking separate from the UI thread
    			ipcThread.start();
    			
        	}
        }); // End setOnClickListener();
        
    } // End onCreate();
    
    
    /*------------------------------------------//
     * 	readFromResource(int id) - Convenience
     * 		method for grabbing whole resource
     * 		text files into a string.
     * 
     * 	int id - The R.id to open
     //-----------------------------------------*/
    public String readFromResource(int id) {
        InputStream iStream = getResources().openRawResource(id);
		Scanner scan = new Scanner(iStream);
		String str = new String();
		
		while (scan.hasNextLine()) {
			str += scan.nextLine() + "\n";
		}
		
		return str;
    }
    
    
    /*------------------------------------------//
     * 	runtimeThread - Executes the C-side of 
     * 		the application
     //-----------------------------------------*/
    Thread runtimeThread = new Thread(new Runnable() {
	    public void run() {
		    
	    	//Attempt to open the named pipe on the read side
	    	int ret = openFIFO(dataPath.getPath() + INFILE);
	    	if (ret != 0) {
	    		// Some error handling goes here eventually
	    	} else {
	    		// Store the return value for printing by the IPC
	    		// thread, once all other c stdout has been received
	    		proc_return = jniMain();
	    	}
	    	
	    }
	  });
    
    
    /*------------------------------------------//
     * 	ipcThread - Constantly listens for C output
     * 		until the sentinel is received
     //-----------------------------------------*/
    Thread ipcThread = new Thread(new Runnable() {
	    public void run() {
	    	
	    	try {	    		
	    		inFile = new BufferedReader( new FileReader(dataPath.getPath() + INFILE) );

	    		while (!proc_finished) {	// Run until sentinel is caught and flag is set
	    			
	    			// Make sure readLine won't block before starting a new thread
	    			// (this reduces the overhead of creating a thread every 10ms)
	    			if (inFile.ready()) {
	    				
	    				// Post a new thread update to the EditText view
	    				descript.post(new Runnable() {
	    					public void run() {
	    						try {
	    							// Check again readLine not blocking hasn't changed
	    							// since the thread's creation
	    							if (inFile.ready()) {
	    								buffline = inFile.readLine();
	    								
	    								if (buffline.equals(SENTINEL)) { // Check for sentinel flag
	    									proc_finished = true;
	    								} else if (buffline != null) {
	    									descript.append(buffline + "\n");	// Append text to View
	    								}
	    							}
	    						} catch (IOException e) {
	    							// TODO Auto-generated catch block
	    							e.printStackTrace();
	    						}
	    						
	    					}
	    				});
	    			}
	    			Thread.sleep(10); // Sleep so a thread isn't being made every few clock cycles
	    		}
	    		inFile.close();
	    	
	    	// Some exception handling for original try{} ...
	    		
	    	} catch (IOException e) {
	    		Log.e("[IN IOEXCEPTION FOR THREAD2]  ", e.getMessage());
	    		descript.post(new Runnable() {
	    			public void run() {
	    				descript.append("[IOEXCEPTION]\n");
	    			}
	    		});
	    	} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    	
	    	// ... and now print the jniMain return value given by the runtimeThread
	    	descript.post(new Runnable() {
    			public void run() {
    				descript.append("\n" + separator + "\t\tReturn: " + proc_return);
    			}
    		});
	    	
	    }
	});
    
    // The following methods are implemented in chook.c, which are compiled into
    // a dynamic library by the ndk-build command and included with the Android app.
    
    public native int makeFIFO(String outFile, String inFile);
    public native int openFIFO(String inFile);
    public native int jniMain();
}