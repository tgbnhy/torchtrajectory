package au.edu.rmit.bdm.Torch.queryEngine.model;

import au.edu.rmit.bdm.Torch.base.helper.GeoUtil;
import au.edu.rmit.bdm.Torch.base.model.Coordinate;
import au.edu.rmit.bdm.Torch.base.model.TrajEntry;

/**
 * SearchWindow class models a window used for window query.txt
 */
public class SearchWindow implements Geometry{

    public final Coordinate middle;
    public final double lowerLat;
    public final double upperLat;
    public final double leftLng;
    public final double rightLng;

    public SearchWindow(Coordinate middle, double squareRadius ){

        this.middle = middle;

        upperLat =GeoUtil.increaseLat(middle.getLat(), squareRadius);
        lowerLat = GeoUtil.increaseLat(middle.getLat(), -squareRadius);
        leftLng = GeoUtil.increaseLng(middle.getLat(),middle.getLng(), -squareRadius);
        rightLng = GeoUtil.increaseLng(middle.getLat(), middle.getLng(), squareRadius);
    }

    public SearchWindow(TrajEntry upperLeft, TrajEntry lowerRight) {

        upperLat = upperLeft.getLat();
        lowerLat = lowerRight.getLat();
        leftLng = upperLeft.getLng();
        rightLng = lowerRight.getLng();

        this.middle = new Coordinate((upperLat + lowerLat) / 2., (leftLng + rightLng) / 2.);
    }
}
