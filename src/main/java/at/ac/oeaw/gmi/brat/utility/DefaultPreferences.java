package at.ac.oeaw.gmi.brat.utility;

import java.awt.*;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by crisoo on 10/15/16.
 */
public class DefaultPreferences {
    private final static Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
    private final static Preferences prefs_expert = prefs_simple.node("expert");

    public static void create() throws BackingStoreException {
        /*
         * simple
         */
        prefs_simple.put("baseDirectory",System.getProperty("user.home"));
        prefs_simple.put("fileExtension","tif");
        prefs_simple.put("flipHorizontal","true");
        prefs_simple.putBoolean("useSets",true);
        prefs_simple.putBoolean("haveDayZeroImage",true);
        prefs_simple.putDouble("numThreads",1.0);

        prefs_simple.flush();


        /*
	     * general
	     */
        prefs_expert.put("experimentIdentifier","^(.*?_)");
        prefs_expert.put("setIdentifier","set\\d+");
        prefs_expert.put("dayIdentifier","day\\d+");
        prefs_expert.put("plateIdentifier","\\d{3}$");
        prefs_expert.put("logLevel","INFO");
        prefs_expert.putInt("resolution",1200);
//        prefs_expert.putDouble("mmPerPixel",25.4/1200.0);
//
//        prefs_expert.put("outputDirectory",new File(System.getProperty("user.home"),"processed").getAbsolutePath());
//        prefs_expert.put("moveDirectory",new File(System.getProperty("user.home"),"processed-tif").getAbsolutePath());
//        prefs_expert.put("dayZeroIdentifier","day0");

        /*
         * seed detection
         */
        prefs_expert.putDouble("seedMinimumSize",0.10); // minimum seed diameter [mm]
        prefs_expert.putDouble("seedMaximumSize",0.70); // maximum seed diameter [mm]

        /*
         * shoot detection
         */
        prefs_expert.putDouble("shootMinThickness",0.1); // minimum thickness of shoot parts [mm]
        prefs_expert.putInt("shootWidthStep",200); // shoot area width increase
        prefs_expert.putInt("shootHeightStep",200); // shoot area height increase

        /*
         * root detection
         */
        prefs_expert.putInt("PLANT_TREE_MAXLEVEL",10);
        prefs_expert.putInt("PLANT_TRACKBACK_SIZE",10); // number of pixels included in calculation of tracking properties (e.g. tracking angle)
        prefs_expert.putInt("plantWidthStep",200);
        prefs_expert.putInt("plantHeightStep",200);
        prefs_expert.putDouble("plantMinThickness",0.1);
        prefs_expert.putDouble("plantMinHeight",0.1);

        /*
         * diagnostics
         */
        prefs_expert.put("SHOOTROI_COLOR","#00FF00"); //green
        prefs_expert.put("ROOTROI_COLOR","#1E90FF"); //blue
        prefs_expert.put("SKELETON_COLOR","#808080"); //gray
        prefs_expert.put("MAINROOT_COLOR","#FF00FF"); //magenta
        prefs_expert.put("STARTPT_COLOR","#FFB366"); //orange
        prefs_expert.put("ENDPT_COLOR","#1E90FF"); //blue new Color(65280); //green Color.red;
        prefs_expert.put("SHOOTCM_COLOR","#FF0000"); //red
        prefs_expert.put("GENERAL_COLOR","#FF0000"); //red
        prefs_expert.putInt("LABEL_SIZE",30);
        prefs_expert.put("LABEL_FONT","sansserif-BOLD-30");
        prefs_expert.putInt("CIRCLEDIAMETER",10);
        prefs_expert.putInt("DEFAULTNUMBERSIZE",150);
        prefs_expert.put("DEFAULTNUMBERFONT","sansserif-BOLD-150");

        prefs_expert.flush();
    }

    private static int colorToInt(Color color){
        return color.getRed()<<16 + color.getGreen()<<8 +color.getBlue();
    }
    private static String colorToHex(Color color) { return String.format("#%02X%02X%02X",color.getRed()&0xff, color.getGreen()&0xff, color.getBlue()&0xff); }
    private static String intToHex(int i){ return String.format("#%06X",i & 0xffffff); }

    public static void main(String[] args){
        try {
            DefaultPreferences.create();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
        Preferences prefs = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2/expert");
        System.out.println("plantMinThickness = "+prefs.getDouble("plantMinThickness",99999));
    }
}
