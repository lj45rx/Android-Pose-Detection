/**
 * Implements multithreaded loading of matrix in background
 * called by and returns reslut to IAP_Helper
 */

package com.tzutalin.vision.visionrecognition;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class IAP_Thread extends Thread {
    private final static String TAG = "IAP_Thread";
    private final String SDPath = VisionClassifierCreator.getPath() + "/phone_data";
    private String path = SDPath + "/poses/selected_feature_lines/selected_features_";
    private static float[][] data;				//result saved in here
    private int nextLine;						//next line to be read
    private int maxNoOfThreads;					//max allowed SubThreads
    private static volatile int noOfSubThreads;	//current Subthreads

    long startTime = System.nanoTime(); //used to time how long loading takes

    //constructor------------------------------------------------------------------------

    public IAP_Thread(int passedNoOfThreads) {
        startTime = System.nanoTime();
        maxNoOfThreads = passedNoOfThreads;
    }

    //IAP_Thread methods-----------------------------------------------------------------

    public void run() {
        Log.i(TAG, "MAXIAP: started loading Matrix with " + maxNoOfThreads + " Threads");
        //initialize variables
        data = new float[348][];
        nextLine = 0;
        noOfSubThreads = 0;

        //load SubThreads until whole matrix is loaded
        while(nextLine<348){
            //if less than desired threads running start new one
            if(noOfSubThreads<maxNoOfThreads){
                //start new SubThread and keep track of how many are running
                incrementThread();
                SubThread subThread = new SubThread(getNext());
                subThread.start();
            }
        }

        //make sure all subthreads are finished
        while(noOfSubThreads != 0){
            //do nothing
        }

        //pass matrix to IAP_Helper
        IAP_Helper helper = new IAP_Helper();
        helper.setMatrix(data);
        float elapsedTime = (System.nanoTime() - startTime)/1000000000;
        Log.i(TAG, "MAXIAP: Matrix loaded , took " + elapsedTime + " seconds");
    }

    //SubThread methods------------------------------------------------------------------

    //have SubThread get matrix line from file
    private void getLine(int lineNumber){
        //try to open file
        Gson gson = new Gson();
        JsonReader reader = null;
        try {
            String fileName = path + lineNumber + ".json";
            reader = new JsonReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "MAXIAP: file not found");
            e.printStackTrace();
        }

        //get line from the file and save it into data
        float res[] = gson.fromJson(reader, float[].class);
        writeLine(lineNumber, res);
    }

    //write line into "data",
    private synchronized void writeLine(int lineNumber, float[] line){
        data[lineNumber] = line;
        //if multiple of 50 write into log
        //better with counter instead of line number for parallel execution but good enough in this case
        if(lineNumber%50 == 0){
            Log.i(TAG, "MAXIAP: writing line "+ lineNumber);
        }
    }

    //synchronized method to get next position to load
    private synchronized int getNext(){
        nextLine += 1;
        return nextLine-1;
    }

    //synchronized methods to keep track of number of running subthreads
    private synchronized void incrementThread(){
        noOfSubThreads += 1;
    }

    private synchronized void decrementThread(){
        noOfSubThreads -= 1;
    }

    //nested SubThread class-------------------------------------------------------------

    private class SubThread extends Thread{

        private int lineNumber;	//linenumber this SubThread will load

        //constructor--------------------------------------------------------------------

        SubThread(int passedNumber){
            lineNumber = passedNumber;
        }

        //methods------------------------------------------------------------------------

        //get line and notify when you "die"
        public void run(){
            getLine(lineNumber);
            decrementThread(); //notify that finished
        }
    }
    //-----------------------------------------------------------------------------------
}