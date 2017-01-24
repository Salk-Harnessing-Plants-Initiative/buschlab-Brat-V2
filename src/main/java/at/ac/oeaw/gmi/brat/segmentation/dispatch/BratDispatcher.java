package at.ac.oeaw.gmi.brat.segmentation.dispatch;

import at.ac.oeaw.gmi.brat.gui.swing.SegmentationGUI;
import at.ac.oeaw.gmi.brat.gui.fx.log.BratFxLogHandler;
import at.ac.oeaw.gmi.brat.gui.fx.log.LogQueue;
import at.ac.oeaw.gmi.brat.gui.swing.log.DebugLogFormatter;
import at.ac.oeaw.gmi.brat.gui.swing.log.SingleLineFormatter;
import at.ac.oeaw.gmi.brat.segmentation.plate.PlateSet;
import at.ac.oeaw.gmi.brat.utility.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Scene;


public class BratDispatcher{
	private final static String version = "2.0.1";
	private final static ClassLoader classloader = BratDispatcher.class.getClassLoader();
	private final static Logger log=Logger.getLogger(BratDispatcher.class.getName());
	private final static Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final static Preferences prefs_expert = prefs_simple.node("expert");

	private SegmentationGUI gui;

	private Map<String,SortedSet<String>> filesets;
    private DoubleProperty fontSize = new SimpleDoubleProperty(10);
	private IntegerProperty reds = new SimpleIntegerProperty(200);
	private IntegerProperty greens = new SimpleIntegerProperty(200);
	private IntegerProperty blues = new SimpleIntegerProperty(255);
	private Scene scene;
	private LogQueue logQueue;

	public BratDispatcher(){
	}

	public void runHeadless(){
		log.config("starting headless run.");
		String strBaseDir=System.getenv("BRAT_BASEDIR");
		log.config(String.format("ENV: BRAT_BASEDIR=%s",strBaseDir));
		prefs_simple.put("baseDirectory",strBaseDir);

		String strFlipHorizontal=System.getenv("BRAT_FLIPHORIZONTAL");
		log.config(String.format("ENV: BRAT_FLIPHORIZONTAL=%s",strFlipHorizontal));
		prefs_simple.put("flipHorizontal",strFlipHorizontal);

		String strFilesetNr=System.getenv("BRAT_FILESETNR");
		log.config(String.format("ENV: BRAT_FILESETNR=%s",strFilesetNr));
		int filesetNr = Integer.parseInt(System.getenv("BRAT_FILESETNR"));

		String strHaveDayZero=System.getenv("BRAT_HAVEDAYZERO");
		log.config(String.format("ENV: BRAT_HAVEDAYZERO=%s",strHaveDayZero));
		prefs_simple.put("haveDayZeroImage",strHaveDayZero);

		readBaseDirectory();
		SortedSet<String> workSet = new ArrayList<SortedSet<String>>(filesets.values()).get(filesetNr);
		for(String f:workSet){
			log.config(String.format("prepared for working on: %s",f));
		}

		PlateSet plateSet=new PlateSet(workSet);
		plateSet.run();

	}

	public void runFromGUI(){
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				log.config("starting gui run.");
				readBaseDirectory();
				if(filesets.size()==0){
					log.warning("No images found. Terminating.");
					return;
				}
				int numThreads = prefs_simple.getInt("numThreads",1);
				log.info("Starting thread pool.");
				ThreadPoolExecutor executorPool = new ThreadPoolExecutor(numThreads,numThreads,Long.MAX_VALUE, TimeUnit.NANOSECONDS,new LinkedBlockingQueue<Runnable>());

				for(SortedSet<String> workSet:filesets.values()){
					PlateSet plateSet = new PlateSet(workSet);
					executorPool.execute(plateSet);
				}

				while(executorPool.getCompletedTaskCount()<executorPool.getTaskCount()){
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						executorPool.shutdown();
						break;
					}
				}
				log.info("All threads terminated. Shutting down.");
				executorPool.shutdown();
			}
		});
		thread.start();
	}

	public void initLogger(LogQueue logQueue) {
		Logger rootLog = LogManager.getLogManager().getLogger("");
		rootLog.setLevel(Level.ALL);

		Handler[] curHandlers = rootLog.getHandlers();
		for (Handler h : curHandlers) {
			rootLog.removeHandler(h);
		}

		Logger log = Logger.getLogger("at.ac.oeaw.gmi.brat");

		Level logLevel = Level.parse(prefs_expert.get("logLevel", "ALL"));
		log.setLevel(Level.ALL);

		curHandlers = log.getHandlers();
		for (Handler h : curHandlers) {
			if (h instanceof ConsoleHandler || h instanceof BratFxLogHandler) {
				log.removeHandler(h);
			}
		}

		Handler h = new ConsoleHandler();
		h.setLevel(logLevel);
		if (logLevel.intValue() >= Level.FINE.intValue()) {
			h.setFormatter(new SingleLineFormatter());
		} else {
			h.setFormatter(new DebugLogFormatter());
		}
		h.setLevel(Level.ALL);
		log.addHandler(h);

		if (logQueue != null) {
			BratFxLogHandler guiLogHandler = new BratFxLogHandler(logQueue);
			guiLogHandler.setLevel(Level.ALL);
			log.addHandler(guiLogHandler);
		}
		log.info("Brat Logging started.");
		log.info(String.format("Brat version %s", version));
	}

	private void readBaseDirectory(){
		log.info("Analyzing base directory.");
		filesets=new TreeMap<String,SortedSet<String>>();

		File dir=new File(prefs_simple.get("baseDirectory",null));
		String[] filenames=dir.list(new ImageFilter());

		Pattern expPattern=Pattern.compile(prefs_expert.get("experimentIdentifier",null));
		Pattern setPattern=Pattern.compile(prefs_expert.get("setIdentifier",null));
		Pattern platePattern=Pattern.compile(prefs_expert.get("plateIdentifier",null));
		assert filenames != null;
		int minFilesetSize=Integer.MAX_VALUE;
		int maxFilesetSize=Integer.MIN_VALUE;
		for(String filename:filenames){
			String trimmedName=FileUtils.removeExtension(filename);
			String setID="";
			Matcher matcher=expPattern.matcher(trimmedName);
			if(matcher.find()){
				setID+=matcher.group();
			}
			else {
				continue;
			}
			setID+=":";
			matcher=setPattern.matcher(trimmedName);
			if(matcher.find()){
				setID+=matcher.group();
			}
			else {
				continue;
			}
			setID+=":";
			matcher=platePattern.matcher(trimmedName);
			if(matcher.find()){
				setID+=matcher.group();
			}
			else {
				continue;
			}

			if(!filesets.containsKey(setID)){
				filesets.put(setID,new TreeSet<String>(new DayComparator()));
			}
			filesets.get(setID).add(filename);
		}
		for(SortedSet<String> set:filesets.values()){
			int curSetSize=set.size();
			if(minFilesetSize>curSetSize){
				minFilesetSize=curSetSize;
			}
			if(maxFilesetSize<curSetSize){
				maxFilesetSize=curSetSize;
			}
		}
		log.info(String.format("Found %d image sets. (min/max set size = %d/%d)", filesets.size(),
				minFilesetSize!=Integer.MAX_VALUE ? minFilesetSize : 0,
				maxFilesetSize!=Integer.MIN_VALUE ? maxFilesetSize : 0));
	}


	private class ImageFilter implements FilenameFilter{
		String fileExt;

		ImageFilter(){
			fileExt=prefs_simple.get("fileExtension",null);
			if(!fileExt.startsWith(".")){
				fileExt="."+fileExt;
			}
		}

		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(fileExt);
		}
	}

	private class DayComparator implements Comparator<String>{
		Pattern dayPattern=Pattern.compile(prefs_expert.get("dayIdentifier",null));
		Pattern intPattern=Pattern.compile("\\d+");

		@Override
		public int compare(String o1, String o2) {
			Matcher m=dayPattern.matcher(o1);
			Integer day1=null;
			if(m.find()){
				Matcher m2=intPattern.matcher(m.group());
				if(m2.find()){
					day1=Integer.parseInt(m2.group());
				}

			}
			if(day1==null){
				return 1;
			}
			m=dayPattern.matcher(o2);
			Integer day2=null;
			if(m.find()){
				Matcher m2=intPattern.matcher(m.group());
				if(m2.find()){
					day2=Integer.parseInt(m2.group());
				}

			}
			if(day2==null){
				return -1;
			}

			return day1.compareTo(day2);
		}

	}}

