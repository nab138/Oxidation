package me.nabdev.oxidation.util;

import org.json.JSONObject;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

/**
 * Utility class for handling JSON objects related to robot geometry.
 * Provides methods to convert between JSON objects and Pose2d/Translation2d.
 */
public class JSONUtils {
    /**
     * Converts a JSONObject to a Pose2d object.
     * 
     * @param obj the JSONObject containing x, y, and optional rot keys
     * @return Pose2d
     */
    public static Pose2d getPose2d(JSONObject obj) {
        if (!obj.has("x") || !obj.has("y"))
            throw new IllegalArgumentException("JSONObject does not contain x and y keys");

        return new Pose2d(obj.getDouble("x"), obj.getDouble("y"),
                Rotation2d.fromDegrees(obj.has("rot") ? obj.getDouble("rot") : 0));
    }

    /**
     * Converts a JSONObject to a Translation2d object.
     * 
     * @param obj the JSONObject containing x and y keys
     * @return Translation2d
     */
    public static Translation2d getTranslation2d(JSONObject obj) {
        if (!obj.has("x") || !obj.has("y"))
            throw new IllegalArgumentException("JSONObject does not contain x and y keys");

        return new Translation2d(obj.getDouble("x"), obj.getDouble("y"));
    }

    /**
     * Converts a Pose2d object to a JSONObject.
     * 
     * @param translation the Pose2d object to convert
     * @return JSONObject containing x, y, and rot keys
     */
    public static JSONObject fromTranslation2d(Translation2d translation) {
        JSONObject obj = new JSONObject();
        obj.put("x", translation.getX());
        obj.put("y", translation.getY());
        return obj;
    }

    /**
     * Create a JSONObject from an x and y coordinate.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @return JSONObject containing x and y keys
     */
    public static JSONObject fromCoords(double x, double y) {
        JSONObject obj = new JSONObject();
        obj.put("x", x);
        obj.put("y", y);
        return obj;
    }
}
