/*
* Creates inctances of Object and Scene detectors, also manages file paths
* Add additional SD-card paths into "sdPaths" variable
*/

package com.tzutalin.vision.visionrecognition;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;

public final class VisionClassifierCreator {
    private final static String TAG = "VisonClassifierCreator";

    private static String path; //will be set during initialization
    private static boolean isInitialized = false;

    //possible paths to SD-Card
    private final static String[] sdPaths = {
            Environment.getExternalStorageDirectory().getPath(), //try to get path from environment
            "/sdcard1", // Sony M4 Aqua
            "ExamplePath1"
            };

    //paths to files necessary for detection/classification; set during initialization
    private static String POSE_MODEL_PATH;
    private static String POSE_WEIGHTS_PATH;
    private static String POSE_MEAN_FILE;
    private static String POSE_SYNSET_FILE;

    private static String DETECT_MODEL_PATH;
    private static String DETECT_WIEGHTS_PATH;
    private static String DETECT_MEAN_FILE;
    private static String DETECT_SYNSET_FILE;

    //constructor------------------------------------------------------------------------

    private VisionClassifierCreator() throws InstantiationException {
        throw new InstantiationException("This class is not for instantiation");
    }

    //methods----------------------------------------------------------------------------

    //check if correct path to Sd card can be found
    //implemented by checking if "deploy.prototxt" can be found
    //(called first when calling getPath() to load Matrix - 0.3 seconds slower than before but called in background thread)
    private static void initialize(){
        //only initialize once
        if(!isInitialized) {
            Log.i(TAG, "MAXIAP: checking SD-Path");
            //try path given by environment and manually set paths
            for (int ii = 0; ii < sdPaths.length + 1; ii++) {
                String testPath = sdPaths[ii] + "/phone_data/poses/deploy.prototxt";
                File file = new File(testPath);
                //if file found set initialized to true, set correct path and end loop
                if (file.exists()) {
                    Log.i(TAG, "MAXIAP: found SD-Path with " + sdPaths[ii]);
                    path = sdPaths[ii];
                    isInitialized = true;
                    break;
                } else {
                    Log.i(TAG, "MAXIAP: coundn't find SD-Path with " + sdPaths[ii]);
                }
            }

            //if valid SD-Path found set other paths
            if (isInitialized) {
                POSE_MODEL_PATH = path + "/phone_data/poses/deploy.prototxt";
                POSE_WEIGHTS_PATH = path + "/phone_data/poses/snap_iter_30000.caffemodel";
                POSE_MEAN_FILE = null;
                POSE_SYNSET_FILE = path + "/phone_data/vision_scene/mit/mit_category_table"; //needs to be non null

                DETECT_MODEL_PATH = path + "/phone_data/fastrcnn/deploy.prototxt";
                DETECT_WIEGHTS_PATH = path + "/phone_data/fastrcnn/caffenet_fast_rcnn_iter_40000.caffemodel";
                DETECT_MEAN_FILE = path + "/phone_data/fastrcnn/imagenet_mean.binaryproto";
                DETECT_SYNSET_FILE = path + "/phone_data/fastrcnn/fastrcnn_synset";
                Log.i(TAG, "MAXIAP: File paths successfully initialized");
            //if no valid path found print error message to log, PROGRAM WILL CRASH
            } else {
                Log.e(TAG, "MAXIAP: coundn't find SD-Path");
            }
        }
    }

    //returns path to sd-card root
    public static String getPath(){
        Log.i(TAG, "MAXIAP: getPath() called");
        initialize(); //make sure SD-path is correct
        return path;
    }

    //Create an instance using a default {@link SceneClassifier} instance
    @NonNull
    public static SceneClassifier createSceneClassifier(@NonNull Context context) throws IllegalAccessException {
        Log.i(TAG, "MAXIAP: createSceneClassifier() called");
        initialize(); //make sure SD-path is correct
        return new SceneClassifier(context, POSE_MODEL_PATH, POSE_WEIGHTS_PATH, POSE_MEAN_FILE, POSE_SYNSET_FILE);
    }

    //Create an instance using a default {@link ObjectDetector} instance
    @NonNull
    public static ObjectDetector createObjectDetector(@NonNull Context context) throws IllegalAccessException {
        Log.i(TAG, "MAXIAP: createObjectClassifier() called");
        initialize(); //make sure SD-path is correct
        return new ObjectDetector(context, DETECT_MODEL_PATH, DETECT_WIEGHTS_PATH, DETECT_MEAN_FILE, DETECT_SYNSET_FILE);
    }
}
