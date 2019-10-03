package com.shinbokuow.labyrinthvr;

public class SegmentTrigger {
    public static enum TriggerType {
        BOUND_TRIGGER,
        DEAD_TRIGGER,
        EXIT_TRIGGER
    };
    private TriggerType triggerType;
    private Segment2D segment;
    /* outVec: the direction you can move forward but can't move backward */
    private Point2D outVec;
    private Point2D intersection;
    public SegmentTrigger(Segment2D _segment, Point2D _outVec, TriggerType _triggerType) {
        segment = _segment;
        outVec = _outVec;
        triggerType = _triggerType;
    }

    public Segment2D getSegment() {
        return segment;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public boolean tryIntersection(Segment2D moveSegment) {
        if (segment.onSegment(moveSegment.getStartPoint())) {
            // near the wall
            if (Segment2D.dcmp(moveSegment.getDirection().dot(outVec)) > 0)
                return false;
            intersection = moveSegment.getStartPoint();
            return true;
        }
        Segment2D.getSegmentIntersection(moveSegment, segment);
        if (Segment2D.getIntersect() == false)
            return false;
        intersection = Segment2D.getIntersection();
        return true;
    }
    public Point2D getIntersection() {
        return intersection;
    }
}
