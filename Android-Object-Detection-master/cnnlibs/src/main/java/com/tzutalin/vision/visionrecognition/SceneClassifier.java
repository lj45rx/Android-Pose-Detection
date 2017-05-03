/*
* Implements Pose detection. Done in two steps, first getting result vector from cnn, then applying
* weights from feature matrix and returning top results
*/

package com.tzutalin.vision.visionrecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

//Identifies the current scene in a android.graphics.Bitmap} graphic object
public class SceneClassifier extends CaffeClassifier<List<VisionDetRet>> {
    private static final String TAG = "SceneClassifier";
    private static final int MODEL_DIM = 227;
    private ByteBuffer _handler;

    //Creates a SceneClassifier, configured with its model path, trained weights, etc.
    //These parameters cannot be changed once the object is constructed.
    public SceneClassifier(Context context, String sceneModelPath, String sceneWieghtsPath, String sceneManefile, String sceneSynsetFile) throws IllegalAccessException {
        super(context, sceneModelPath, sceneWieghtsPath, sceneManefile, sceneSynsetFile);
        if (!new File(mModelPath).exists() ||
                !new File(mWeightsPath).exists() ||
                !new File(mSynsetPath).exists() ) {
            throw new IllegalAccessException("SceneClassifier cannot find model");
        }
    }

    //get cnn result vector - used as parameter for classify
    public float[] getErgVector(Bitmap bitmap){
        float[] propArray; //result vector

        // Check input
        if (bitmap == null) {
            Log.e(TAG, "classify. Invalid Input bitmap");
        }

        //get feature vektor
        storeBitmap(bitmap);
        propArray = jniClassifyBitmap(_handler);
        freeBitmap();

        return propArray;
    }

    //apply weights from feature matrix onto reslut vector
    //only return top x resluts
    public List<VisionDetRet> classify(float[] vector) {

        List<VisionDetRet> ret = new ArrayList<>(); //result

        //helper used to outsource functions
        IAP_Helper h = new IAP_Helper();

        //multiply vector with feature matrix which is loaded inside IAP_Helper to get final result
        float[] resultVector = h.localMartixByVectorFloat(vector);

        //get top results
        int noOfResults = 5;
        int[] resIndices = h.getTopIndices(resultVector, noOfResults); //indices
        float[] resValues = h.getConfidenceValues(resultVector, resIndices); //confidence values
        String[] resPaths = h.getImagePaths(resIndices); //image urls

        //add results to List<VisionDetRet> which will be returned
        for(int ii = 0; ii < noOfResults; ii++){
            //Log.i(TAG, "MAXIAP: ret:" + resIndices[ii] + ", " + resValues[ii]);

            //paths and confidence values (other variables unused and therefore = 0)
            VisionDetRet det = new VisionDetRet(resPaths[ii], resValues[ii], 0, 0, 0, 0);

            ret.add(det);
        }

        return ret;
    }

    //Functions to handle natural functions

    //(will reslut in error if file has different name then "SceneClassifier")
    @Override
    public void init(int imgWidth, int imgHeight) {
        super.init(imgWidth, imgHeight);
        jniLoadModel(mModelPath, mWeightsPath); //TODO crash here
        jniSetInputModelDim(MODEL_DIM, MODEL_DIM);
    }

    @Override
    public void deInit() {
        super.deInit();
        jniRelease();
    }

    private void storeBitmap(final Bitmap bitmap) {
        if (_handler != null)
            freeBitmap();
        _handler = jniStoreBitmapData(bitmap);
    }

    private void freeBitmap() {
        if (_handler == null)
            return;
        jniFreeBitmapData(_handler);
        _handler = null;
    }

    //Native Methods
    protected native int jniLoadModel(String modelPath, String weightsPath);

    protected native int jniSetInputModelDim(int width, int height);

    protected native int jniRelease();

    protected native float[] jniClassifyImgByPath(String imgPath);

    private native float[] jniClassifyBitmap(ByteBuffer handler);

    private native ByteBuffer jniStoreBitmapData(Bitmap bitmap);

    private native void jniFreeBitmapData(ByteBuffer handler);

    private native Bitmap jniGetBitmapFromStoredBitmapData(ByteBuffer handler);
}
