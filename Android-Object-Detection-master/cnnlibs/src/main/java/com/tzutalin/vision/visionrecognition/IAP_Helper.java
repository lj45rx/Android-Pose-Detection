/**
 * Set of helper functions for other classes
 * also handels loading of feature matrix
 */

package com.tzutalin.vision.visionrecognition;

import java.io.FileNotFoundException;
import java.io.FileReader;

import android.util.Log;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class IAP_Helper {

    private final String SDPath = VisionClassifierCreator.getPath() + "/phone_data";
    private static final String TAG = "IAP_Helper";
    //matrix is loaded in onCreate() Method of CameraActivity.java
    private static float[][] matrix; //feature matrix
    private static boolean isInitialized = false; //make sure matrix is loaded only once
    private static boolean isLoaded = false; // only return matrix if it is fully loaded

    public IAP_Helper(){
    }

    //start loading matrix using class IAP_Thread with desired number of Subthreads
    public static void loadMatrix(int noOfThreads){
        if(!isInitialized){
            Log.i(TAG, "MAXIAP: loadMatrix() called");
            isInitialized = true; //only start loading once
            IAP_Thread thread = new IAP_Thread(noOfThreads);
            thread.start();
        } else {
            Log.i(TAG, "MAXIAP: loadMatrix() called - but was already loaded");
        }
    }

    //allow IAP_Thread to set matrix
    public void setMatrix(float[][] data){
        //only allow matrix to be set once
        if(!isLoaded) {
            matrix = data;
            isLoaded = true;
        }else{
            Log.e(TAG, "MAXIAP: setMatrix() called - but was already set");
        }
    }

    //check if matrix is loaded
    public boolean isLoaded(){
        return isLoaded;
    }

    //return matrix - if not loaded return [[0]]
    public float[][] returnMatrix(){
        if(isLoaded){
            return matrix;
        }else{
            return new float[1][1];
        }
    }
    
    //multiply vector with matrix loaded and saved in this class 
    //only works if matrix has been loaded
    public float[] localMartixByVectorFloat(float[] vector){

        //matrix dimensions
        int noOfRows = matrix.length;
        int noOfColumns = matrix[0].length;

        float[] result = new float[noOfRows];
        //basic matrix multiplication
        for(int ii=0; ii < noOfRows; ii++){
            int temp = 0;
            for(int jj=0; jj < noOfColumns; jj++){
                temp += matrix[ii][jj] * vector[jj];
            }
            result[ii] = temp;
        }
        return result;
    }

    //get indixes of top x values is vector
    public int[] getTopIndices(float[] vector, int noOfResults){
        int[] result = new int[noOfResults];
        float[] temp = new float[noOfResults];
        //go through all elements in vector
        for(int ii = 0; ii < vector.length; ii++){
            //if current element has relevant size
            //for example if noOfResults = 5, if element bigger than 5th largest element
            if(vector[ii]>temp[noOfResults-1]){

                //search for index where it should go
                int h_index = 0;
                for(int jj = 0; jj < noOfResults; jj++){
                    //index des groessten wertes kleiner vector[ii] suchen
                    //search index of biggest value smaller than current value
                    if(temp[jj]<vector[ii]){
                        h_index = jj;
                        break;
                    }
                }
                //rearrange and insert new value
                for(int jj = noOfResults-2; jj >= h_index ; jj--){
                    temp[jj+1] = temp[jj];
                    result[jj+1] = result[jj];
                }
                temp[h_index] = vector[ii];
                result[h_index] = ii;

            }
        }
        return result;
    }

    //return confidence values of desired results
    public float[] getConfidenceValues(float[] vector, int[] indices){
        float[] result = new float[indices.length];

        for(int ii = 0; ii< indices.length; ii++){
            result[ii] = vector[indices[ii]];
        }

        return result;
    }

    //load desired image paths
    public String[] getImagePaths(int[] indices){
        String folderUrl = SDPath +"/poses/long_jump/";
        String[]  result = new String[indices.length];

        //load image paths from file
        Gson gson = new Gson();
        //vtry to open file
        JsonReader reader = null;
        try {
            reader = new JsonReader(new FileReader(SDPath +"/poses/selected_feature_images.json"));
        } catch (FileNotFoundException e) {
            Log.i(TAG, "MAXIAP: getImagePaths file not found ");
            e.printStackTrace();
        }
        //temporarily save all image paths
        String temp[] = gson.fromJson(reader, String[].class);

        //pick desired paths
        for(int ii = 0; ii<indices.length; ii++){
            result[ii] = folderUrl + temp[indices[ii]];
        }

        return result;
    }

    //find best reslut if object detection returns multiple humans
    public VisionDetRet getBestHumanMatch(List<VisionDetRet> detectedHumans) {
        float conf = 0;
        int pos = 0;

        //go through all detected humans - memorize best match
        for ( VisionDetRet item : detectedHumans) {
            if(item.getConfidence() > conf){
                conf = item.getConfidence();
                pos = detectedHumans.indexOf(item);
            }
        }
        //return best match
        return detectedHumans.get(pos);
    }

    //get coordinates from VisionDetRet Object and make sure they are in correct bounds
    public int[] getCoordinates(VisionDetRet detectedHuman){
        int[] coords = new int[4];
        coords[0] = detectedHuman.getLeft();
        coords[1] = detectedHuman.getTop();
        coords[2] = detectedHuman.getRight() -coords[0]; //width
        coords[3] = detectedHuman.getBottom() -coords[1]; //height
        //because sample size = 4
        for(int ii = 0; ii < 4; ii++){
            coords[ii] = coords[ii]/4;
            //make sure coordinates are in bounds
            if(ii<2 && coords[ii]<0){
                coords[ii] = 0;
            } else if(ii == 2 && (coords[2]+coords[0])>1032){
                coords[2] = 1032-coords[0];
            } else if(ii == 3 && (coords[3]+coords[1])>774){
                coords[3] = 774-coords[1];
            }
        }
        //Log.i(TAG, "MAXIAP: left " + coords[0] + " top " + coords[1]);
        //Log.i(TAG, "MAXIAP: width " + coords[2] + " height " + coords[3]);

        return coords;
    }

    //functions to normalize vector
        //2 private helper functions to get mean and standard deviation
    private double mean(float[] inputLine){
        double sum = 0;
        for(int ii = 0; ii < inputLine.length; ii++){
            sum += inputLine[ii];
        }
        return sum/inputLine.length;
    }

    private double stdDeviation(float[] inputLine, double mean){
        double sum = 0;
        for(int ii = 0; ii < inputLine.length; ii++){
            double temp = inputLine[ii]-mean;
            sum += temp*temp;
        }
        sum /= inputLine.length;
        return Math.sqrt(sum);
    }

    public float[] normalize(float[] inputLine){
        double mean = mean(inputLine);
        double stdDev = stdDeviation(inputLine, mean);
        for(int ii = 0; ii < inputLine.length; ii++){
            inputLine[ii] = (float)((inputLine[ii]-mean)/stdDev);
        }
        return inputLine;
    }
}
