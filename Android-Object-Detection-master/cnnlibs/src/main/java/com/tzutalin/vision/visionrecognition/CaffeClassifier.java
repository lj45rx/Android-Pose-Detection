/*
* abstract Base class for using caffe
* implemented in ObjectDetector and SceneClassifier
*/

package com.tzutalin.vision.visionrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public abstract class CaffeClassifier<T> {
    protected static boolean sInitialized;
    static {
        try {
            System.loadLibrary("objrek");
            System.loadLibrary("objrek_jni");
            jniNativeClassInit();
            sInitialized = true;
            android.util.Log.d("CaffeClassifier", "jniNativeClassInit success");
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.d("CaffeClassifier", "objrek objrek_jni library not found!");
        }
    }

    protected Context mContext;
    protected String mModelPath;
    protected String mWeightsPath;
    protected String mMeanPath;
    protected String mSynsetPath;
    protected String[] mSynsets;
    protected int mImgWidth;
    protected int mImgHeight;
    private String mSelectedLabel;

    /**
     * @param modelPath Caffe's model
     * @param wieghtsPath Caffe's trained wieght
     * @param manefile The file path of the image image
     * @param synsetFile The file path to load label's titles
     */
    protected CaffeClassifier(Context context, String modelPath, String wieghtsPath, String manefile, String synsetFile) {
        mContext = context;
        mModelPath = modelPath;
        mWeightsPath = wieghtsPath;
        mMeanPath = manefile;
        mSynsetPath = synsetFile;
    }

    //Init image width and height and start to load model, weight, allocate byte buffer, etc.
    public void init(int imgWidth, int imgHeight) {
        mSynsets = getSynsetsFromFile(mContext);
        mImgWidth = imgWidth;
        mImgHeight = imgHeight;
    }

    //release the resource, model, weight, deallocate the buffer
    public void deInit() {
    }


    public void setSelectedLabel(String label) {
        mSelectedLabel = label;
    }

    public void clearSelectedLabel() {
        mSelectedLabel = null;
    }

    private String[] getSynsetsFromFile(Context context) {
        if (!TextUtils.isEmpty(mSynsetPath)) {
            File file = new File(mSynsetPath);
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(file));
                String line;
                List<String> lines = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    String temp = line.substring(line.lastIndexOf("/") + 1);
                    lines.add(temp.split(" ")[0]);
                }
                return lines.toArray(new String[0]);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    private native static void jniNativeClassInit();
}