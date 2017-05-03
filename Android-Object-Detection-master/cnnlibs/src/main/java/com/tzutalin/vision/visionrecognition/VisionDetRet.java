/*
* Class to contain result objects with label (image url), confidence value and coordinates
*/

package com.tzutalin.vision.visionrecognition;

public final class VisionDetRet {
    private String mLabel;
    private float mConfidence;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;

    VisionDetRet() {
    }

    public VisionDetRet(String label, float confidence, int l, int t, int r, int b) {
        mLabel = label;
        mLeft = l;
        mTop = t;
        mRight = r;
        mBottom = b;
        mConfidence = confidence;
    }

    //return the X coordinate of the left side of the result
    public int getLeft() {
        return mLeft;
    }

    //return the Y coordinate of the top of the result
    public int getTop() {
        return mTop;
    }

    //return the X coordinate of the right side of the result
    public int getRight() {
        return mRight;
    }

    //return the Y coordinate of the bottom of the result
    public int getBottom() {
        return mBottom;
    }

    //return confidence value
    public float getConfidence() {
        return mConfidence;
    }

    //return label of the result
    public String getLabel() {
        return mLabel;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Left:")
                .append(mLeft)
                .append(", Top:")
                .append(mTop)
                .append(", Right:")
                .append(mRight)
                .append(", Bottom:")
                .append(mBottom)
                .append(", Label:")
                .append(mLabel).toString();
    }
}