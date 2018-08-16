package au.edu.rmit.bdm.Torch.base.helper;

import au.edu.rmit.bdm.Torch.base.model.TrajEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class provides basic function for geo-distance calculation.
 */
public class GeoUtil {

    private static Logger logger = LoggerFactory.getLogger(GeoUtil.class);

    public static double distance(TrajEntry v1, TrajEntry v2){
        return distance(v1.getLat(), v2.getLat(), v1.getLng(), v2.getLng());
    }

    /**
     * Calculate geo-distance between two points in latitude and longitude.
     * <p>
     *
     * @param lat1 latitude of point1 GPS position
     * @param lat2 latitude of point2 GPS position
     * @param lon1 longitude of point1 GPS position
     * @param lon2 longitude of point2 GPS position
     * @return Distance in Meters
     */
    public static double distance(double lat1, double lat2, double lon1, double lon2) {
        return distance(lat1, lat2, lon1, lon2, 0.0, 0.0);
    }

    /**
     * Calculate geo-distance between two points in latitude and longitude taking
     * into account height difference. Uses Haversine method as its base.
     * <p>
     * lat1, lon1 Start candidatePoint lat2, lon2 End candidatePoint el1 Start altitude in meters
     * el2 End altitude in meters
     *
     * @return Distance in Meters
     */

    private static double distance(double lat1, double lat2, double lon1, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    /**
     * increase the value of the current latitude.
     *
     * @param lat the latitude to be increased
     * @param meters specify how much in meters should the latitude increase
     * @return new latitude.
     */
    public static double increaseLat(double lat, double meters) {
        double ret;
        double coef = meters * 0.0000089;
        ret = lat + coef;
        if (ret > 90) ret = 90;
        if (ret < -90) ret = -90;
        return ret;
    }

    public static double decreaseLat(double lat, double meters){
        return increaseLat(lat, -meters);
    }

    /**
     * increase the value of the current longitude.
     *
     * @param lat the longitude to be increased
     * @param meters specify how much in meters should the longitude increase
     * @return new latitude.
     */
    public static double increaseLng(double lat, double lon, double meters) {
        double ret;
        double coef = meters * 0.0000089;
        ret = lon + coef / Math.cos(lat * 0.018);
        if (ret > 180) ret -= 360;
        if (ret < -180) ret += 360;

        return ret;
    }

    public static double decreaseLng(double lat, double lng, double meters){
        return increaseLng(lat, lng, -meters);
    }
}
