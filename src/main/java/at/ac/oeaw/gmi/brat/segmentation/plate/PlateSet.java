package at.ac.oeaw.gmi.brat.segmentation.plate;

import at.ac.oeaw.gmi.brat.segmentation.output.DataOutput;
import at.ac.oeaw.gmi.brat.segmentation.plants.Plant;
import at.ac.oeaw.gmi.brat.segmentation.plants.PlantDetector;
import at.ac.oeaw.gmi.brat.segmentation.seeds.SeedDetector;
import at.ac.oeaw.gmi.brat.segmentation.seeds.SeedingLayout;
import at.ac.oeaw.gmi.brat.utility.ExceptionLog;
import at.ac.oeaw.gmi.brat.utility.FileUtils;
import ij.gui.Roi;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class PlateSet implements Runnable{
	final private static Logger log=Logger.getLogger(PlateSet.class.getName());
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private List<String> plateFilenames;
	private List<List<Plant>> plants;

	private SeedingLayout seedingLayout;
	
	public PlateSet(Set<String> plateFilenames){
		this.plateFilenames=new ArrayList<String>(plateFilenames);
		this.seedingLayout=new SeedingLayout(); // default seeding layout
//		this.seedingLayout.setDelyLayout();
//		this.seedingLayout.setAlexandroLayout(); //TODO alexandro layout inserted
	}
	
	public PlateSet(String[] plateFilenames){
		this.plateFilenames=new ArrayList<String>();
		for(String filename:plateFilenames){
			this.plateFilenames.add(filename);
			this.seedingLayout=new SeedingLayout(); // default seeding layout
//			this.seedingLayout.setDelyLayout();
//			this.seedingLayout.setAlexandroLayout(); //TODO alexandro layout inserted
		}
	}
	
	public void setSeedingLayout(SeedingLayout seedingLayout){
		this.seedingLayout=seedingLayout;
	}
	
	@Override
	public void run(){
		int nRows=seedingLayout.getExpectedRows();
		int nCols=seedingLayout.getExpectedColumns();
		List<List<Point2D>> seedCenters;
		Shape plateShape=seedingLayout.getPlateShape();
		
		plants=new ArrayList<List<Plant>>();
		for(int row=0, plantNr=0;row<nRows;++row){
			plants.add(new ArrayList<Plant>());
			for(int col=0;col<nCols;++col, ++plantNr){
				plants.get(row).add(new Plant(plantNr));
			}
		}
		PlantDetector plantDetector=new PlantDetector(seedingLayout,plants);
		Opener opener=new Opener();
		ImageProcessor currentWorkIp;
		String baseDirectory = prefs_simple.get("baseDirectory",null);
		String moveDirectory = new File(baseDirectory,"processed-tif").getAbsolutePath();

		if(prefs_simple.getBoolean("haveDayZeroImage",true)) {
			String filePath=new File(baseDirectory,plateFilenames.get(0)).getAbsolutePath();
			log.info(String.format("processing day zero image: %s", plateFilenames.get(0)));
			currentWorkIp =opener.openImage(filePath).getProcessor();
			
			if(prefs_simple.getBoolean("flipHorizontal",true)){
				currentWorkIp.flipHorizontal();
			}
			
			PlateDetector plateDetector=new PlateDetector(currentWorkIp,1.0,plateShape);
			int origWidth = currentWorkIp.getWidth();
			int origHeight = currentWorkIp.getHeight();
			boolean done = false;
			int maxColorRise = 3;
			while(!done && maxColorRise<6) {
				plateDetector.detectInsideArea(maxColorRise);
				currentWorkIp = plateDetector.getCorrectedIp();
				if(currentWorkIp.getWidth()/origWidth < 0.5 || currentWorkIp.getHeight()/origHeight < 0.5){
					++maxColorRise;
				}
				else{
					done = true;
				}
			}

			SeedDetector sd=new SeedDetector(currentWorkIp);
			sd.identifySeeds(prefs_expert.getDouble("seedMinimumSize",0.1),prefs_expert.getDouble("seedMaximumSize",0.7));
			List<List<Roi>> detectedSeeds=sd.getAssignedRois();
			seedCenters=sd.getAssignedRoiCenters();
//			sd.drawSeedingLayout();
			
			for(int row=0;row<nRows;++row){
				for(int col=0;col<nCols;++col){
					if(detectedSeeds.get(row).get(col)!=null){
						log.fine("plant "+row+","+col+": seed="+seedCenters.get(row).get(col).toString());
						Plant plant=plants.get(row).get(col);
						plant.setSeedRoi(detectedSeeds.get(row).get(col));
						plant.setSeedCenter(seedCenters.get(row).get(col));
					}
				}
			}
			FileUtils.moveFile(filePath,new File(moveDirectory,plateFilenames.get(0)).getAbsolutePath());
			plateFilenames.remove(0);
		}

		for(int fileNr=0;fileNr<plateFilenames.size();++fileNr){
			String fileName=plateFilenames.get(fileNr);
			String filenameBase=FileUtils.removeExtension(fileName);
			log.info(String.format("processing image: %s", fileName));

			try{
//				IJ.log("working on file: '"+fileName+"'");
				currentWorkIp =opener.openImage(baseDirectory,fileName).getProcessor();

				if(prefs_simple.getBoolean("flipHorizontal",true)){
					currentWorkIp.flipHorizontal();
				}

//				IJ.log(currentWorkIp.toString());
				int origWidth = currentWorkIp.getWidth();
				int origHeight = currentWorkIp.getHeight();
				boolean done = false;
				int maxColorRise = 3;
				PlateDetector plateDetector = null;
				while(!done && maxColorRise<5) {
					plateDetector=new PlateDetector(currentWorkIp,1.0,plateShape);
					plateDetector.detectInsideArea(maxColorRise);
					currentWorkIp =plateDetector.getCorrectedIp();
					if((double)currentWorkIp.getWidth()/origWidth < 0.5 || (double)currentWorkIp.getHeight()/origHeight < 0.5){
						++maxColorRise;
					}
					else{
						done = true;
					}
				}
//				IJ.log(currentWorkIp.toString());

				//TODO check if corrected ip is valid

				plantDetector.setIp(currentWorkIp);
				plantDetector.detectShootParts(fileNr);
				plantDetector.detectRootParts(fileNr);

				createTopologies(fileNr);
				calcTraits(fileNr);
				DataOutput.writeTraits(plants,fileNr,filenameBase);
				DataOutput.writeSinglePlantDiagnostics(currentWorkIp,plants,fileNr,filenameBase);
				DataOutput.writePlateDiags(currentWorkIp, plants, fileNr, filenameBase);
				DataOutput.writeCoordinates(plateDetector.getRotation(), plateDetector.getScalefactor(), plateDetector.getReferencePt(), plateDetector.getPlateShape(),
						plants, fileNr, filenameBase);
				FileUtils.moveFile(new File(baseDirectory,fileName).getAbsolutePath(),new File(moveDirectory,fileName).getAbsolutePath());
			}
			catch(Exception e){
				StringWriter sw = new StringWriter();
				PrintWriter pw =new PrintWriter(sw);
				e.printStackTrace(pw);
				log.severe(String.format("unhandled exception processing file %s\n%s!",fileName, sw.toString()));
//				e.printStackTrace();
			}
		}
	}
	
	public void createTopologies(){
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				plant.createTopologies();
			}
		}
	}
	
	public void createTopologies(int time){
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				if(plant.getRootRoi(time)==null){
					continue;
				}
				log.info(String.format("Plant %s: calculating topology",plant.getPlantID()));
				plant.createTopology(time);
			}
		}
	}
	
	public void calcTraits(int time){
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				log.info(String.format("Plant %s: calculating traits",plant.getPlantID()));
				plant.calcTraits(time);
			}
		}
	}
}
