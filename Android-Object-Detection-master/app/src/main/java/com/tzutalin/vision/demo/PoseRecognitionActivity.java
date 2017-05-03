/*
* This Class implements the backbone of our pose detection. The outer Class provides an Activity to
* show the results while the PredictTask Subclass given the path to the input file detects the best
* fitting poses and draws the results
*/
package com.tzutalin.vision.demo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import com.tzutalin.vision.visionrecognition.IAP_Helper;
import com.tzutalin.vision.visionrecognition.ObjectDetector;
import com.tzutalin.vision.visionrecognition.SceneClassifier;
import com.tzutalin.vision.visionrecognition.R;
import com.tzutalin.vision.visionrecognition.VisionClassifierCreator;
import com.tzutalin.vision.visionrecognition.VisionDetRet;
import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.provider.BigImageCardProvider;
import com.dexafree.materialList.view.MaterialListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PoseRecognitionActivity extends Activity {

    private final static String TAG = "PoseRecognitionAct";
    //massage shown during execution, will change according to progress
    private ProgressDialog mmDialog;

    MaterialListView mListView;
    private ObjectDetector mObjectDet; //to detect objects first
    private SceneClassifier mClassifier;//to detect poses
    private String imgPath;
    private IAP_Helper helper;

    //create PredictTask and run detection
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_scene_recognition);
        mListView = (MaterialListView) findViewById(R.id.material_listview);
        final String key = Camera2BasicFragment.KEY_IMGPATH;
        imgPath = getIntent().getExtras().getString(key);
        if (!new File(imgPath).exists()) {
            Toast.makeText(this, "No file path", Toast.LENGTH_SHORT).show();
            this.finish();
            return;
        }
        helper = new IAP_Helper();
        PredictTask task = new PredictTask();
        task.execute(imgPath);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClassifier != null) {
            mClassifier.deInit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scene_recognition, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Tasks inner class----------------------------------------------------------------------------
    private class PredictTask extends AsyncTask<String, Void, List<VisionDetRet>> {
        //initialize ProgressDialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mmDialog = ProgressDialog.show(PoseRecognitionActivity.this, getString(R.string.dialog_wait),getString(R.string.dialog_scene_decscription), true);
        }

        @Override //MAXIAP most action happens here
        protected List<VisionDetRet> doInBackground(String... strings) {
            //variables to meassure execution times
            long startTime = System.currentTimeMillis();
            long endTime;

            final String filePath = strings[0];

            //--------------------------------------------------------------------------------------
            //first run object detection------------------------------------------------------------
            changeMessage("object detection");
            Log.d(TAG, "DetectTask filePath:" + filePath);

            //initialize ObjectDetector
            if (mObjectDet == null) {
                try {
                    mObjectDet = VisionClassifierCreator.createObjectDetector(getApplicationContext());
                    mObjectDet.init(0, 0);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            //TODO delete - for test purposes only
            /*
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    String retImgPath = filePath;
                    Bitmap bitmap = BitmapFactory.decodeFile(retImgPath, options);
                    Drawable d = new BitmapDrawable(getResources(), bitmap);

                    Card card = new Card.Builder(PoseRecognitionActivity.this)
                            .withProvider(BigImageCardProvider.class)
                            .setDrawable(d)
                            .setDescription("Input image - for test purposes")
                            .endConfig()
                            .build();
                    mListView.add(card);
                }
            });
            */

            //Run Object detection
            //will only return resluts with label= "person"
            List<VisionDetRet> detectedHumans = new ArrayList<>(); //was objRet
            if (mObjectDet != null) {
                Log.d(TAG, "Start objDetect");
                detectedHumans.addAll(mObjectDet.classifyByPath(filePath));
                Log.d(TAG, "end objDetect");
                //calculate and show how long Object detection took
                endTime = System.currentTimeMillis();
                final double diffTime = (double) (endTime - startTime) / 1000;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PoseRecognitionActivity.this, "Object Detection took " + diffTime + " second", Toast.LENGTH_LONG).show();
                    }
                });
            }
            mObjectDet.deInit();

            //--------------------------------------------------------------------------------------
            //then check for detected humans--------------------------------------------------------
            int[] coords; //coordinates will be saved here if person was found

            //if no humans detected abort-------------------------------------------
            if(detectedHumans.size() == 0){
                showToastMessage("No humans detected - aborting");
                Log.i(TAG, "MAXIAP: No humans detected");

                //draw input image
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        Bitmap bm = BitmapFactory.decodeFile(imgPath, options);
                        Drawable d = new BitmapDrawable(getResources(), bm);

                        Card card = new Card.Builder(PoseRecognitionActivity.this)
                                .withProvider(BigImageCardProvider.class)
                                .setDrawable(d)
                                .setDescription("Input image - no Person detected")
                                .endConfig()
                                .build();
                        mListView.add(card);
                    }
                });

                return null;
            //if humans detected----------------------------------------------------
            } else {
                //----------------------------------------------------
                //draw input image with highlighted humans
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        String retImgPath = "/sdcard/temp.jpg";
                        Bitmap bitmap = BitmapFactory.decodeFile(retImgPath, options);
                        Drawable d = new BitmapDrawable(getResources(), bitmap);

                        Card card = new Card.Builder(PoseRecognitionActivity.this)
                                .withProvider(BigImageCardProvider.class)
                                .setDrawable(d)
                                .setDescription("Input image - Person detected")
                                .endConfig()
                                .build();
                        mListView.add(card);
                    }
                });
                //----------------------------------------------------

                //detectedHumans might contain multiple results
                //find best match and its coordinates
                VisionDetRet bestHit;
                //if multiple humans detected get best match
                if(detectedHumans.size() > 1){
                    bestHit = helper.getBestHumanMatch(detectedHumans);
                //if only one use that
                } else {
                    bestHit = detectedHumans.get(0);
                }
                showToastMessage("Human detected");
                //save coordinates of best match to coords
                coords = helper.getCoordinates(bestHit);
            }

            //--------------------------------------------------------------------------------------
            //then run pose detection---------------------------------------------------------------
            changeMessage("detecting poses");
            Log.i(TAG, "MAXIAP: PoseRecognitionActivity startTime:" + startTime);

            //initialize PoseDetector
            initCaffeMobile();
            Log.e(TAG, "MAXIAP: initCaffeMobile finished");
            List<VisionDetRet> rets = new ArrayList<>(); //will contain detected poses
            Log.d(TAG, "PredictTask filePath:" + filePath);
            if (mClassifier != null) {
                changeMessage("searching closest match");
                //----------------------------------------------------
                //draw Card with cropped image
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Log.d(TAG, "format:" + options.inPreferredConfig);
                Bitmap bitmapImg = BitmapFactory.decodeFile(filePath, options);
                final Bitmap croppedBitmap = Bitmap.createBitmap(bitmapImg, coords[0],coords[1],coords[2], coords[3]);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Drawable d = new BitmapDrawable(getResources(), croppedBitmap);

                        Card card = new Card.Builder(PoseRecognitionActivity.this)
                                .withProvider(BigImageCardProvider.class)
                                .setDrawable(d)
                                .setDescription("Cropped image")
                                .endConfig()
                                .build();
                        mListView.add(card);
                    }
                });
                //----------------------------------------------------

                //get result vector from cnn over cropped image
                float[] erg_vector = mClassifier.getErgVector(croppedBitmap);

                //check if matrix is loaded - if not wait until it is loaded
                if(!helper.isLoaded()){
                    changeMessage("loading matrix");
                    while(!helper.isLoaded()){
                        //do nothing until matrix is loaded
                    }
                }

                //get best matching poses from result vector and feature matrix
                changeMessage("calculating closest match");
                List<VisionDetRet> ret; //list with results
                ret = mClassifier.classify(erg_vector);
                rets.addAll(ret);
            //----------------------------------------------------------------------------------

                //change message and show execution time
                Log.i(TAG, "MAXIAP: PoseRecognitionActivity classify()finished");
                endTime = System.currentTimeMillis();
                final double diffTime = (double) (endTime - startTime) / 1000;
                showToastMessage("Finished in " + diffTime + " seconds");
            }

            //clean up
            File beDeletedFile = new File(filePath);
            if (beDeletedFile.exists()) {
                beDeletedFile.delete();
            } else {
                Log.d(TAG, "file does not exist " + filePath);
            }

            //return list of results
            return rets;
        }

        //MAXIAP change message of ProgressDialog
        private void changeMessage(final String text){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mmDialog.setMessage(text);
                }
            });
        }

        //MAXIAP create toast-message
        private void showToastMessage(final String text){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PoseRecognitionActivity.this, text, Toast.LENGTH_LONG).show();
                }
            });
        }

        //draw resluts and clean up
        @Override
        protected void onPostExecute(List<VisionDetRet> resultList) {
            super.onPostExecute(resultList);
            //dismiss message
            if (mmDialog != null) {
                mmDialog.dismiss();
            }

            //if no humans were detected abort
            if(resultList == null){
                return;
            }

            //If humans detected create cards with matching poses
            //first get picture urls(label) and confidence values from "rets" list
            String[] pictures = new String[resultList.size()];
            String[] confidence = new String[resultList.size()];
            int h = 0;
            for (VisionDetRet singleResult : resultList) {
                confidence[h] = "" + singleResult.getConfidence();
                pictures[h] = singleResult.getLabel(); //get picture urls
                h += 1;
            }

            //draw cards of best results
            Log.i(TAG, "MAXIAP: PoseRecognitionActivity: onPostExecute() creating cards");

            for(int ii = 0; ii < resultList.size(); ii++){
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 1;
                Bitmap bm = BitmapFactory.decodeFile(pictures[ii]);
                Drawable d = new BitmapDrawable(getResources(), bm);

                Card card = new Card.Builder(PoseRecognitionActivity.this)
                        .withProvider(BigImageCardProvider.class)
                        .setTitle("Top " + (ii+1))
                        .setDescription(confidence[ii])
                        .setDrawable(d)
                        .endConfig()
                        .build();
                mListView.add(card);
            }
        }
    }

    // Private methods------------------------------------------------------------------------------
    //initialize CaffeModel
    private void initCaffeMobile() {
        if (mClassifier == null) {
            try {
                mClassifier = VisionClassifierCreator.createSceneClassifier(getApplicationContext());
                Log.d(TAG, "Start Load model");
                mClassifier.init(224,224);  // init once
                Log.d(TAG, "End Load model");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
}
