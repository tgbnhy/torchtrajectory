package au.edu.rmit.trajectory.torch.queryEngine.model;

import au.edu.rmit.trajectory.torch.base.helper.GeoUtil;
import au.edu.rmit.trajectory.torch.base.model.Coordinate;
import au.edu.rmit.trajectory.torch.base.model.TorPoint;

/**
 * SearchWindow class models a window used for window query
 */
public class SearchWindow {

    public final TorPoint middle;
    public final double squareRadius;
    public final double lowerLat;
    public final double upperLat;
    public final double leftLng;
    public final double rightLng;

    public SearchWindow(TorPoint middle, double squareRadius ){

        this.middle = middle;
        this.squareRadius = squareRadius;

        upperLat =GeoUtil.increaseLat(middle.getLat(), squareRadius);
        lowerLat = GeoUtil.increaseLat(middle.getLat(), -squareRadius);
        leftLng = GeoUtil.increaseLng(middle.getLat(),middle.getLng(), -squareRadius);
        rightLng = GeoUtil.increaseLng(middle.getLat(), middle.getLng(), squareRadius);
    }
}
