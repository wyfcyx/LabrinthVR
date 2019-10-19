package com.shinbokuow.labyrinthvr;

import android.util.Log;

import java.util.ArrayList;

public class SegmentTriggerSystem {
    private ArrayList<SegmentTrigger> triggers;
    private static final String TAG = "SegmentTriggerSystem";

    public SegmentTriggerSystem() {
        triggers = new ArrayList<>();
    }

    public void addSegmentTrigger(SegmentTrigger trigger) {
        triggers.add(trigger);
    }

    public void reset() {
        triggers.clear();
    }
    public SegmentTrigger getTrigger(Segment2D moveSegment) {
        SegmentTrigger nearestTrigger = null;
        double distance = 1e8, nDistance;
        //Log.i(TAG, "getTriggerBegin");
        //Log.i(TAG, "move=" + moveSegment.toString());


        for (SegmentTrigger trigger: triggers) {
            //Log.i(TAG, "this trigger is=" + trigger.getSegment().toString());
            if (trigger.tryIntersection(moveSegment)) {
                nDistance = Point2D.distance(
                        moveSegment.getStartPoint(),
                        trigger.getIntersection()
                );
                if (nearestTrigger == null || nDistance < distance) {
                    distance = nDistance;
                    nearestTrigger = trigger;
                }
            }
        }
        //Log.i(TAG, "getTriggerEnd");

        return nearestTrigger;
    }
}
