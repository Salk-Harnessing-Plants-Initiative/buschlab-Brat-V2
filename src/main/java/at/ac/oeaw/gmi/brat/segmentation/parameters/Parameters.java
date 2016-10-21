package at.ac.oeaw.gmi.brat.segmentation.parameters;

import java.awt.Color;
import java.awt.Font;
import java.util.logging.Level;

public class Parameters {
	/*
	 * general
	 */
	public static Level logLevel=Level.OFF;
	public static int numInstances=1;
	public static int resolution=1200;
	public static double mmPerPixel=25.4/resolution;
	
	public static String baseDirectory=null;
	public static String outputDirectory=null;
	public static String moveDirectory=null;
	public static String experimentIdentifier="^(.*?_)";
	public static String setIdentifier="set\\d+";
	public static String dayIdentifier="day\\d+";
	public static String plateIdentifier="\\d{3}$";
	public static String fileExtension="tif";
	public static Boolean flipHorizontal=true;
	public static String dayZeroIdentifier="day0";
	public static Boolean haveDayZeroImage=true;
	
	/*
	 * seed detection
	 */
	public static double seedMinimumSize=0.10; // minimum seed diameter [mm]
	public static double seedMaximumSize=1.00; // maximum seed diameter [mm]
	
	/*
	 * shoot detection
	 */
	public static double shootMinThickness=0.1; // minimum thickness of shoot parts [mm]
	public static int shootWidthStep=200; // shoot area width increase
	public static int shootHeightStep=200; // shoot area height increase
	/*
	 * root detection
	 */
	public static final int STPT_DEFAULT=0;
	public static final int STPT_EARLIEST=1;
	public static final int STPT_SEEDPT=3;
	public static double startPointMethod=STPT_DEFAULT;
	
	public static int PLANT_TREE_MAXLEVEL=10;
	public static int PLANT_TRACKBACK_SIZE=10; // number of pixels included in calculation of tracking properties (e.g. tracking angle)
	public static int plantWidthStep=200;
	public static int plantHeightStep=200;
	public static double plantMinThickness=0.1;
	public static double plantMinHeight=0.1;
	
	/*
	 * diagnostics
	 */
	public static final Color SHOOTROI_COLOR = new Color(65280); //green
	public static final Color ROOTROI_COLOR = new Color(2003199); //blue
	public static final Color SKELETON_COLOR = Color.GRAY;
	public static final Color MAINROOT_COLOR = Color.magenta;
	public static final Color STARTPT_COLOR = new Color(16747520); //orange
	public static final Color ENDPT_COLOR = new Color(2003199); //blue new Color(65280); //green Color.red;
	public static final Color SHOOTCM_COLOR = Color.red;
	public static final Color GENERAL_COLOR = Color.red;
	public static final int LABEL_SIZE = 30;
	public static final Font LABEL_FONT = new Font("sansserif",Font.BOLD,LABEL_SIZE);
	public static final int CIRCLEDIAMETER = 10;
	public static final int DEFAULTNUMBERSIZE = 150;
	public static final Font DEFAULTNUMBERFONT=new Font("sansserif",Font.BOLD,DEFAULTNUMBERSIZE);
	
}
