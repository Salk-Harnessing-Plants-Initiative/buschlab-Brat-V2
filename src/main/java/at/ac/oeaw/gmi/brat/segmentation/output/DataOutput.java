package at.ac.oeaw.gmi.brat.segmentation.output;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonNode;
import at.ac.oeaw.gmi.brat.segmentation.plants.Plant;
import at.ac.oeaw.gmi.brat.utility.ExceptionLog;
import at.ac.oeaw.gmi.brat.utility.FileUtils;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.io.FileSaver;
import ij.process.Blitter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class DataOutput {
	private final static Logger log= Logger.getLogger(DataOutput.class.getName());
	private static final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private static final Preferences prefs_expert = prefs_simple.node("expert");
	private static final String outputDirectory = new File(prefs_simple.get("baseDirectory",null),"processed").getAbsolutePath();

	public static void writePlateDiags(ImageProcessor srcIp,List<List<Plant>> plants,int time,String filenamePart){
		ImageProcessor diagIp=srcIp.duplicate();
		for(List<Plant> plantsRow:plants){
			for(Plant plant:plantsRow){
				if(plant==null) continue;
				
				Roi shootRoi=plant.getShootRoi(time);
				if(shootRoi!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("SHOOTROI_COLOR","#00FF00")));
					diagIp.draw(shootRoi);
				}

				Roi rootRoi=plant.getRootRoi(time);
				if(rootRoi!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("ROOTROI_COLOR","#1E90FF")));
					diagIp.draw(rootRoi);
				}
				
				SkeletonNode startPt=plant.getStartNode(time);
				int circleDiameter=prefs_expert.getInt("CIRCLEDIAMETER",10);
				if(startPt!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("STARTPT_COLOR","#FFB366")));
					diagIp.fillOval(startPt.getX()-circleDiameter/2,startPt.getY()-circleDiameter/2,circleDiameter,circleDiameter);
				}

				SkeletonNode endPt=plant.getEndNode(time);
				if(endPt!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("ENDPT_COLOR","#1E90FF")));
					//				diagIp.drawOval(endPt.x-PlateSet.CIRCLEDIAMETER/2,endPt.y-PlateSet.CIRCLEDIAMETER/2,PlateSet.CIRCLEDIAMETER,PlateSet.CIRCLEDIAMETER);
					diagIp.fillOval(endPt.getX()-circleDiameter/2,endPt.getY()-circleDiameter/2,circleDiameter,circleDiameter);
					diagIp.setColor(Color.decode(prefs_expert.get("GENERAL_COLOR","#FF0000")));
					diagIp.setFont(Font.decode(prefs_expert.get("LABEL_FONT","sansserif-BOLD-30")));
					diagIp.drawString(plant.getPlantID(),endPt.getX(),endPt.getY()+10+prefs_expert.getInt("LABEL_SIZE",30));
				}

				List<SkeletonNode> skeleton=plant.getRootMainPath(time);
				if(skeleton!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("SKELETON_COLOR","#808080")));
					for (SkeletonNode aSkeleton : skeleton) {
						diagIp.drawPixel(aSkeleton.getX(), aSkeleton.getY());
					}
				}

				List<SkeletonNode> mainPath=plant.getRootMainPath(time);
				if(mainPath!=null){
					diagIp.setColor(Color.decode(prefs_expert.get("MAINROOT_COLOR","#FF00FF")));
					for (SkeletonNode aMainPath : mainPath) {
						diagIp.drawPixel(aMainPath.getX(), aMainPath.getY());
					}
				}
			}
			String savePath=new File(outputDirectory,String.format("Object_Diagnostics_%s.jpg",filenamePart)).getAbsolutePath();
			writeDiagnosticImage(savePath,diagIp);
		}
	}

	public static void writeTraits(List<List<Plant>> plants,Integer time,String filenamePart) {
		String outputPath=new File(outputDirectory,String.format("Object_Measurements_%s.txt",filenamePart)).getAbsolutePath();
		DecimalFormat f = new DecimalFormat("####0.000");

		BufferedWriter output=null;
		try{
			output = new BufferedWriter(new FileWriter(outputPath));

			String newline = System.getProperty("line.separator");
			String nullStr="null";
			String nanStr="nan";

			for(List<Plant> plantRow:plants){
				for(Plant plant:plantRow){
					if(plant==null)
						continue;
					
					List<Object> traitList=plant.getTraitsAsList(time);
					if(traitList==null)
						continue;
					
					StringBuilder sb=new StringBuilder();
					sb.append(plant.getPlantID());
					for (Object aTraitList : traitList) {
						sb.append("\t");
						if (aTraitList == null) {
							sb.append(nullStr);
						} else if (aTraitList instanceof Double) {
							if (((Double) aTraitList).isNaN()) {
								sb.append(nanStr);
							} else {
								sb.append(f.format(aTraitList));
							}
						} else if (aTraitList instanceof String) {
							sb.append(aTraitList);
						}
					}
					sb.append(newline);
					output.write(sb.toString());
					output.flush();
				}
			}
		}
		catch(IOException e)
		{
//			e.printStackTrace();
//			StringWriter sw = new StringWriter();
//			e.printStackTrace(new PrintWriter(sw));
//			String stackTrace = sw.toString();
			log.severe(String.format("Error writing file!\n%s",ExceptionLog.StackTraceToString(e)));
		}
		finally{
			if(output!=null){
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void writeSinglePlantDiagnostics(ImageProcessor srcIp,List<List<Plant>> plants,Integer time,String filenamePart){
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				String savePath=new File(outputDirectory,String.format("Plant_%s_Object_Diagnostics_%s.jpg",plant.getPlantID(),filenamePart)).getAbsolutePath();
				ImageProcessor diagIp=createSinglePlantDiagnostic(srcIp,plant,time,100,1000,plants.get(0).size(),plants.size());
				if(diagIp==null)
					continue;
				writeDiagnosticImage(savePath,diagIp);
			}
		}
	}
	
	private static ImageProcessor createSinglePlantDiagnostic(ImageProcessor srcIp,Plant plant,int time,int border,int newHeight,int plantsPerRow,int plantRows){
		if(plant.getRootRoi(time)==null)
			return null;
		
		ImageProcessor diagIp=srcIp.duplicate();
		
		ShapeRoi shootRoi=new ShapeRoi(plant.getShootRoi(time));
		if(shootRoi!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("SHOOTROI_COLOR","#00FF00")));
			diagIp.draw(shootRoi);
		}
		
		ShapeRoi rootRoi=new ShapeRoi(plant.getRootRoi(time)); 
		if(shootRoi!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("ROOTROI_COLOR","#1E90FF")));
			diagIp.draw(rootRoi);
		}
		
		SkeletonNode sNode=plant.getStartNode(time);
		int circleDiameter=prefs_expert.getInt("CIRCLEDIAMETER",10);
		if(sNode!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("STARTPT_COLOR","#FFB366")));
//			diagIp.drawOval(this.startPt.x-Parameters.CIRCLEDIAMETER/2,this.startPt.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);
			diagIp.fillOval(sNode.getX()-circleDiameter/2,sNode.getY()-circleDiameter/2,circleDiameter,circleDiameter);
		}

		SkeletonNode eNode=plant.getEndNode(time);
		if(eNode!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("ENDPT_COLOR","#1E90FF")));
//			diagIp.drawOval(rootEndNode.x-Parameters.CIRCLEDIAMETER/2,rootEndNode.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);
			diagIp.fillOval(eNode.getX()-circleDiameter/2,eNode.getY()-circleDiameter/2,circleDiameter,circleDiameter);
			diagIp.setColor(Color.decode(prefs_expert.get("GENERAL_COLOR","#FF0000")));
			diagIp.setFont(Font.decode(prefs_expert.get("LABEL_FONT","sansserif-bold-30")));
			diagIp.drawString(plant.getPlantID(),eNode.getX(),eNode.getY()+10+prefs_expert.getInt("LABEL_SIZE",30));
		}
		
		Collection<SkeletonNode> skeleton=plant.getRootSkeleton(time);
		if(skeleton!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("SKELETON_COLOR","#808080")));
			for(SkeletonNode node:skeleton){
				diagIp.drawPixel(node.getX(),node.getY());
			}
		}
		
		List<SkeletonNode> rootMainPath=plant.getRootMainPath(time);
		if(rootMainPath!=null){
			diagIp.setColor(Color.decode(prefs_expert.get("MAINROOT_COLOR","#FF00FF")));
			for(int i=0;i<rootMainPath.size();++i){
				diagIp.drawPixel(rootMainPath.get(i).getX(),rootMainPath.get(i).getY());
			}
		}
		
//		Point shootCoM=this.topology.getShootCoM();
//		if(shootCoM!=null){
//			diagIp.setColor(Parameters.SHOOTCM_COLOR);
//			diagIp.drawOval(shootCoM.x-Parameters.CIRCLEDIAMETER/2,shootCoM.y-Parameters.CIRCLEDIAMETER/2,Parameters.CIRCLEDIAMETER,Parameters.CIRCLEDIAMETER);			
//		}
		
		ShapeRoi combinedRoi=shootRoi.or(rootRoi);
		Rectangle roiRect=combinedRoi.getBounds();
		Rectangle drawRect=new Rectangle(roiRect.x-border,roiRect.y-border,roiRect.width+2*border,roiRect.height+2*border);

		int dx=drawRect.x+drawRect.width-diagIp.getWidth();
		if(dx>0) drawRect.width-=dx;
		if(drawRect.x<0) drawRect.x=0;
		int dy=drawRect.y+drawRect.height-diagIp.getHeight();
		if(dy>0) drawRect.height-=dy;
		if(drawRect.y<0) drawRect.y=0;
		
		ImageProcessor dstIp1=srcIp.duplicate();
		dstIp1.setColor(Color.red);
		dstIp1.fill(new Roi(drawRect));

		//draw default numbers
		int numberOffsetX1=0;
		int numberOffsetY1=dstIp1.getHeight()/16;//drawRect.y-(int)(TopoDiags.DEFAULTNUMBERSIZE*1.1);
		int numberOffsetY2=dstIp1.getHeight()/2-(int)(prefs_expert.getInt("DEFAULTNUMBERSIZE",150)*1.1);
		int deltaX=(dstIp1.getWidth()-2*numberOffsetX1)/(plantsPerRow+1);
		int deltaY=(dstIp1.getHeight()-2*numberOffsetY1)/(plantRows);
		dstIp1.setColor(Color.white);
		dstIp1.setFont(Font.decode(prefs_expert.get("DEFAULTNUMBERFONT","sansserif-BOLD-150")));
		for(int j=0;j<plantRows;++j){
			for(int i=0;i<plantsPerRow;++i){
				dstIp1.drawString(Integer.toString(i+1+(j*plantsPerRow)),numberOffsetX1+(i+1)*deltaX-prefs_expert.getInt("DEFAULTNUMBERSIZE",150)/2,numberOffsetY1+(j*deltaY),Color.blue);
				//dstIp1.drawString(Integer.toString(i+1+12),numberOffsetX1+(i+1)*deltaX-TopoDiags.DEFAULTNUMBERSIZE/2,numberOffsetY2,Color.blue);
			}
		}
		int newWidth=(int)((double)newHeight/(double)diagIp.getHeight()*(double)(diagIp.getWidth()));
		dstIp1=dstIp1.resize(newWidth);

		ImageProcessor dstIp2=diagIp.duplicate();
		dstIp2.setRoi(drawRect);
//		ImageProcessor dstIp2=tmpIp2.crop();
		dstIp2=dstIp2.crop();
		newWidth=(int)((double)newHeight/(double)dstIp2.getHeight()*(double)(dstIp2.getWidth()));
		dstIp2=dstIp2.resize(newWidth);
		
		
		ImageProcessor dstIp3=srcIp.duplicate();
		dstIp3.setRoi(drawRect);
		dstIp3=dstIp3.crop();
		dstIp3=dstIp3.resize(newWidth);
		
		ImageProcessor dstIp=new ColorProcessor(dstIp1.getWidth()+dstIp2.getWidth()+dstIp3.getWidth(),newHeight);
		dstIp.copyBits(dstIp1, 0, 0, Blitter.COPY);
		dstIp.copyBits(dstIp2, dstIp1.getWidth(), 0, Blitter.COPY);
		dstIp.copyBits(dstIp3, dstIp1.getWidth()+dstIp2.getWidth(),0,Blitter.COPY);

		return dstIp;
	}
	
	public static void writeCoordinates(double plateRotation,double scalefactor,Point2D refPt,Shape plateShape,List<List<Plant>> plants,Integer time,String filenamePart){
		PlateCoordinates pc=new PlateCoordinates();
		pc.rotation=plateRotation;
		pc.scalefactor=scalefactor;
		pc.refPt=refPt;
		pc.plateShape=plateShape;
		
		for(List<Plant> plantRow:plants){
			for(Plant plant:plantRow){
				if(plant==null){
					continue;
				}
				String plantID=plant.getPlantID();
				List<Point> shootPixels=null;
				if(plant.getShootRoi(time)!=null){
					ImageProcessor mask=plant.getShootRoi(time).getMask();
					Rectangle bounds=plant.getShootRoi(time).getBounds();
					shootPixels=new ArrayList<Point>();
					for(int y=0;y<bounds.height;++y){
						for(int x=0;x<bounds.width;++x){
							if(mask.get(x,y)>0){
								shootPixels.add(new Point(x+bounds.x,y+bounds.y));
							}
						}
					}
				}
				
				List<Point> rootPixels=null;
				if(plant.getRootRoi(time)!=null){
					ImageProcessor mask=plant.getRootRoi(time).getMask();
					Rectangle bounds=plant.getRootRoi(time).getBounds();
					rootPixels=new ArrayList<Point>();
					for(int y=0;y<bounds.height;++y){
						for(int x=0;x<bounds.width;++x){
							if(mask.get(x,y)>0){
								rootPixels.add(new Point(x+bounds.x,y+bounds.y));
							}
						}
					}
				}
				
				pc.plantCoordinates.put(plantID,new ArrayList<Object>());
				pc.plantCoordinates.get(plantID).add(shootPixels);
				pc.plantCoordinates.get(plantID).add(rootPixels);
				pc.plantCoordinates.get(plantID).add(plant.getRootMainPathPoints(time));
			}
		}
		
		String outputPath=new File(outputDirectory,String.format("Object_Coordinates_%s.ser",filenamePart)).getAbsolutePath();
		ObjectOutputStream oos=null;
		try{
			oos=new ObjectOutputStream(new FileOutputStream(outputPath));
			oos.writeObject(pc);
		} catch (IOException e) {
			log.warning(String.format("Could not write Coordinates.\n%s",e.getMessage()));
//					e1.printStackTrace();
		}
		finally {
			try {
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void writeDiagnosticImage(final String diagPath,final ImageProcessor diagIp){
		ImagePlus diagImage = new ImagePlus("Plant Diag",diagIp);
		//diagImage.show();
		if(diagPath!=null)
		try {
			FileUtils.assertFolder(new File(diagPath).getParent());
			FileSaver filesaver = new FileSaver(diagImage);
			//filesaver.saveAsTiff(diagPath);
			filesaver.saveAsJpeg(diagPath);
		} catch (IOException e) {
			log.severe(String.format("Error writing diagnostic image %s.\n%s", diagPath, ExceptionLog.StackTraceToString(e)));
		}
//		diagImage.close();
	}

//	private Font getFontFromConfigString(String strConfig){
//		String[] strCols = strConfig.split("-");
//		String type=strCols[0];
//		int style = Font.
//		return new Font(strCols[0],strCols[1],strCols[2]);
//	}
}

class PlateCoordinates implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -2826357954582508727L;
	public double rotation;
	public double scalefactor;
	public Point2D refPt;
	public Shape plateShape;
	
	public Map<String,List<Object>> plantCoordinates=new HashMap<String,List<Object>>();
}