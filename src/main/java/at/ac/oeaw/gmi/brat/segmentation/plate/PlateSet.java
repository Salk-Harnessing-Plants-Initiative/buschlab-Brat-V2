package at.ac.oeaw.gmi.brat.segmentation.plate;

import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonNode;
import at.ac.oeaw.gmi.brat.segmentation.output.DataOutput;
import at.ac.oeaw.gmi.brat.segmentation.plants.Plant;
import at.ac.oeaw.gmi.brat.segmentation.plants.ng.PlantDetectorNG;
import at.ac.oeaw.gmi.brat.segmentation.seeds.SeedDetector;
import at.ac.oeaw.gmi.brat.segmentation.seeds.SeedingLayout;
import at.ac.oeaw.gmi.brat.utility.FileUtils;
import ij.IJ;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.io.Opener;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class PlateSet implements Runnable{
	private final static Logger log=Logger.getLogger(PlateSet.class.getName());
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private List<String> plateFilenames;
	private List<List<Plant>> plants;

	private SeedingLayout seedingLayout;
	
	public PlateSet(Set<String> plateFilenames){
		this.plateFilenames=new ArrayList<String>(plateFilenames);
		this.seedingLayout=new SeedingLayout(); // default seeding layout
		this.seedingLayout.setMarcoLayout(); //TODO alexandro layout inserted
	}
	
	public PlateSet(String[] plateFilenames){
		this.plateFilenames=new ArrayList<String>();
		for(String filename:plateFilenames){
			this.plateFilenames.add(filename);
			this.seedingLayout=new SeedingLayout(); // default seeding layout
			this.seedingLayout.setMarcoLayout(); //TODO alexandro layout inserted
		}
	}
	
	public void setSeedingLayout(SeedingLayout seedingLayout){
		this.seedingLayout=seedingLayout;
	}
	
	@Override
	public void run(){
		int nRows=seedingLayout.getExpectedRows();
		int nCols=seedingLayout.getExpectedColumns();
		List<List<Point2D>> seedCenters=seedingLayout.getSeedPositions();
		Shape plateShape=seedingLayout.getPlateShape();		
		
		plants=new ArrayList<List<Plant>>();
		for(int row=0,plantNr=0;row<nRows;++row){
			plants.add(new ArrayList<Plant>());
			for(int col=0;col<nCols;++col,++plantNr){
				if(prefs_simple.getBoolean("haveDayZeroImage",true)) {
					plants.get(row).add(null);
				}
				else{
					plants.get(row).add(new Plant(plantNr));
				}
			}
		}
		PlantDetectorNG plantDetector=new PlantDetectorNG(seedingLayout,plants);
		Opener opener=new Opener();
		ImageProcessor currentWorkIp;
		String baseDirectory = prefs_simple.get("baseDirectory",null);
		String moveDirectory = new File(baseDirectory,"processed-tif").getAbsolutePath();
		if(prefs_simple.getBoolean("haveDayZeroImage",true)) {
			String filePath=new File(baseDirectory,plateFilenames.get(0)).getAbsolutePath();
			System.out.println("day zero image: "+filePath);
			currentWorkIp =opener.openImage(filePath).getProcessor();

			if(prefs_simple.getBoolean("flipHorizontal",true)){
				currentWorkIp.flipHorizontal();
			}
			
			PlateDetector plateDetector=new PlateDetector(currentWorkIp,1.0,plateShape);
			plateDetector.detectInsideArea();
			currentWorkIp =plateDetector.getCorrectedIp();

			SeedDetector sd=new SeedDetector(currentWorkIp);
			sd.identifySeeds(prefs_expert.getDouble("seedMinimumSize",0.1),prefs_expert.getDouble("seedMaximumSize",0.7));
			List<List<Roi>> detectedSeeds=sd.getAssignedRois();
			seedCenters=sd.getAssignedRoiCenters();
			sd.drawSeedingLayout();
			new WaitForUserDialog("seeding layout").show();

			int plantNr=0;
			for(int row=0;row<nRows;++row){
				if(detectedSeeds.size()>row){
					for(int col=0;col<nCols;++col){
						if(detectedSeeds.get(row).get(col)!=null){
							IJ.log("plant "+row+","+col+": seed="+seedCenters.get(row).get(col).toString());
							Plant plant=new Plant(plantNr);
							plant.setSeedRoi(detectedSeeds.get(row).get(col));
							plant.setSeedCenter(seedCenters.get(row).get(col));
							plants.get(row).set(col,plant);
						}
						plantNr++;
					}
				}
			}
			try {
				FileUtils.moveFile(filePath,new File(moveDirectory,plateFilenames.get(0)).getAbsolutePath());
			} 
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			plateFilenames.remove(0);
		}

		for(int fileNr=0;fileNr<plateFilenames.size();++fileNr){
			try{
				String fileName = plateFilenames.get(fileNr);
				String filenameBase = FileUtils.removeExtension(fileName);

				log.info("working on file: '" + fileName + "'");
				currentWorkIp = opener.openImage(baseDirectory, fileName).getProcessor();

				if (prefs_simple.getBoolean("flipHorizontal", true)) {
					currentWorkIp.flipHorizontal();
				}

				IJ.log(currentWorkIp.toString());
				PlateDetector plateDetector = new PlateDetector(currentWorkIp, 1.0, plateShape);
				plateDetector.detectInsideArea();
				currentWorkIp = plateDetector.getCorrectedIp();
				IJ.log(currentWorkIp.toString());

				//TODO check if corrected ip is valid

				plantDetector.setIp(currentWorkIp);
				plantDetector.detectShoots(fileNr, 10.0);
				plantDetector.detectRootParts(fileNr);

				createTopologies(fileNr);
				calcTraits(fileNr);
				DataOutput.writeTraits(plants, fileNr, filenameBase);
				DataOutput.writeSinglePlantDiagnostics(currentWorkIp, plants, fileNr, filenameBase);
				DataOutput.writePlateDiags(currentWorkIp, plants, fileNr, filenameBase);
				try {
					DataOutput.writeCoordinates(plateDetector.getRotation(), plateDetector.getScalefactor(), plateDetector.getReferencePt(), plateDetector.getPlateShape(),
							plants, fileNr, filenameBase);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					FileUtils.moveFile(new File(baseDirectory, fileName).getAbsolutePath(), new File(moveDirectory, fileName).getAbsolutePath());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			catch(Exception e){
				IJ.log("unhandled exception!");
				e.printStackTrace();
			}
		}
	}
	
	//TODO: check ot used?
	public void createTopologies(){
		System.err.println("using old method");
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				plant.createTopologies(null);
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
				IJ.log("Plant "+plant.getPlantID()+": creating topology");
				SkeletonNode stNodeGuess=null;
				if(prefs_expert.get("startpointmethod","earliest").equals("earliest")){
					for(int i=0;i<time;++i){
						if((stNodeGuess=plant.getStartNode(i))!=null){
							break;
						}
					}
				}
				plant.createTopology(time,stNodeGuess);
			}
		}
	}
	
	public void calcTraits(int time){
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				IJ.log("Plant "+plant.getPlantID()+": calculating traits");
				plant.calcTraits(time);
			}
		}
	}
}
