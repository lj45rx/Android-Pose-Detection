/*
* Implements Object detection. Capable of detecting multiple categories of objects but set to only
* return detected Humans.
*/

package com.tzutalin.vision.visionrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetector extends CaffeClassifier <List<VisionDetRet>>{
    private static final String TAG = "ObjectDetector";

    //Creates a ObjectDetector, configured with its model path, trained weights, etc.
    //These parameters cannot be changed once the object is constructed.
    public ObjectDetector(Context context, String modelPath, String wieghtsPath, String manefile, String synsetFile) throws IllegalAccessException {
        super(context, modelPath, wieghtsPath, manefile, synsetFile);
        if (!new File(mModelPath).exists() ||
                !new File(mWeightsPath).exists() ||
                !new File(mSynsetPath).exists() ) {
            throw new IllegalAccessException("ObjectDetector cannot find model");
        }
    }

    @Override
    public void init(int imgWidth, int imgHeight) {
        Log.i(TAG, "MAXIAP init() called");
        super.init(imgWidth, imgHeight);
        jniLoadModel(mModelPath, mWeightsPath, mMeanPath, mSynsetPath);
    }

    @Override
    public void deInit() {
        Log.i(TAG, "MAXIAP deInit() called");
        super.deInit();
        jniRelease();
    }

    @Override
    public void setSelectedLabel(String label) {
        super.setSelectedLabel(label);
        jniSetSelectedLabel(label);
    }

    @Override
    public void clearSelectedLabel() {
        super.clearSelectedLabel();
        jniSetSelectedLabel("");
    }

    //Detect and locate objects according to the given image path
    //detects several object categories but only returns detected humans
    public List<VisionDetRet> classifyByPath(String imgPath) {
        Log.i(TAG, "MAXIAP classifyByPath() called");
        List<VisionDetRet> resultList = new ArrayList<>();

        if (TextUtils.isEmpty(imgPath) || !new File(imgPath).exists()) {
            Log.e(TAG, "classifyByPath. Invalid Input path");
            return resultList;
        }

        //number of detected "objects" (including "background")
        int numObjs = jniClassifyImgByPath(imgPath);
        Log.i(TAG, "MAXIAP classifyByPath numObjs: " + numObjs);

        //put results into resultList and return
        for (int i = 0; i != numObjs; i++) {
            VisionDetRet result = new VisionDetRet();
            int success = jniGetDetRet(result, i);
            //only return detcted persons - others are not important for us
            if (success >= 0 && result.getLabel().equals("person"))
                resultList.add(result);
        }
        return resultList;
    }

    //native Methods
    protected native int jniLoadModel(String modelPath, String weightsPath, String meanfilePath, String sysetPath);

    protected native int jniRelease();

    protected native int jniSetSelectedLabel(String label);

    protected native int jniClassifyImgByPath(String imgPath);

    private native int jniClassifyBitmap(ByteBuffer handler);

    private native int jniGetDetRet(VisionDetRet det, int index);

    private native ByteBuffer jniStoreBitmapData(Bitmap bitmap);

    private native void jniFreeBitmapData(ByteBuffer handler);
}
