package com.timshinlee.tagviewgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/1/9.
 */

public class DirectionUtil {
    private static DirectionUtil mInstance;
    private List<DIRECTION[][]> mDirections = new ArrayList<>();
    /**
     * 四种变换模式
     */
    private DIRECTION[][] oneTagMode;
    private DIRECTION[][] twoTagMode;
    private DIRECTION[][] threeTagMode;

    private DirectionUtil() {
        oneTagMode = new DIRECTION[][]{
                {DIRECTION.RIGHT_CENTER},
                {DIRECTION.LEFT_TOP_TILT},
                {DIRECTION.RIGHT_BOTTOM_TILT},
                {DIRECTION.LEFT_CENTER}};
        twoTagMode = new DIRECTION[][]{
                {DIRECTION.RIGHT_BOTTOM_TILT, DIRECTION.RIGHT_CENTER},
                {DIRECTION.LEFT_BOTTOM_TILT, DIRECTION.RIGHT_CENTER},
                {DIRECTION.RIGHT_TOP_TILT, DIRECTION.LEFT_CENTER},
                {DIRECTION.LEFT_TOP_TILT, DIRECTION.LEFT_CENTER}};
        threeTagMode = new DIRECTION[][]{
                {DIRECTION.RIGHT_TOP_TILT, DIRECTION.RIGHT_CENTER, DIRECTION.RIGHT_BOTTOM_TILT},
                {DIRECTION.LEFT_TOP_TILT, DIRECTION.RIGHT_CENTER, DIRECTION.LEFT_BOTTOM_TILT},
                {DIRECTION.RIGHT_TOP_TILT, DIRECTION.LEFT_CENTER, DIRECTION.RIGHT_BOTTOM_TILT},
                {DIRECTION.LEFT_TOP_TILT, DIRECTION.LEFT_CENTER, DIRECTION.LEFT_BOTTOM_TILT}};
        mDirections.add(oneTagMode);
        mDirections.add(twoTagMode);
        mDirections.add(threeTagMode);
    }

    public static DirectionUtil getInstance() {
        if (mInstance == null) {
            mInstance = new DirectionUtil();
        }
        return mInstance;
    }

    /**
     * 三种标签数的模式
     */
    public List<DIRECTION[][]> getModes() {
        return mDirections;
    }
}
