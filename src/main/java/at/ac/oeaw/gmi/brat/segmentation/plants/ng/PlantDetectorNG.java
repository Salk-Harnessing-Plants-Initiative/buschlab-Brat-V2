package at.ac.oeaw.gmi.brat.segmentation.plants.ng;

import at.ac.oeaw.gmi.brat.math.PlaneFit;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.ColorSpaceConverter;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.EdgeFilter;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.PixelUtils;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow.GradientVectorFlow;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow.RidgeDetector;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow.RidgeRoi;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonGraph;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonNode;
import at.ac.oeaw.gmi.brat.segmentation.plants.Plant;
import at.ac.oeaw.gmi.brat.segmentation.seeds.SeedingLayout;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.WaitForUserDialog;
import ij.gui.Wand;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.EDM;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.*;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;

public class PlantDetectorNG {
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private final double mmPerPixel = 25.4/prefs_expert.getDouble("resolution",1200);
	private final List<List<Plant>> plants;
	private final SeedingLayout seedingLayout;
	
	private ImageProcessor origIp;
	private int origWidth;
	private int origHeight;
	
	private List<RidgeRoi> ridgeRois;
	
//	List<List<Roi>> detectedShootRois;
//	List<List<Roi>>	detectedPlantRois;
	
//	List<List<Point>> trackingStartPts;
	
	public PlantDetectorNG(SeedingLayout seedingLayout,List<List<Plant>> plants){
		this.seedingLayout=seedingLayout;
		this.plants=plants;
	}
	
	public void setIp(ImageProcessor ip){
		this.origIp=ip;
		this.origWidth=ip.getWidth();
		this.origHeight=ip.getHeight();
	}
	
//	public List<List<Roi>> getShootRois(){
//		return detectedShootRois;
//	}
	
//	public List<List<Roi>> getPlantRois(){
//		return detectedPlantRois;
//	}
	
	public void drawDebugImage(){
		ImageProcessor dbgIp=origIp.duplicate();
		dbgIp.setColor(Color.red);
		for(RidgeRoi roi:ridgeRois){
			int cgreen=Color.green.getRGB();
			int cwhite=Color.white.getRGB();
			for(int idx:roi.getShootPixelIndices()){
				dbgIp.set(idx,cgreen);
			}
			for(int idx:roi.getRootPixelIndices()){
				dbgIp.set(idx,cwhite);
			}
		}
		for(List<Plant> lp:plants){
			for(Plant p:lp){
				Point2D pt=p.getSeedCenter();
				dbgIp.fillOval((int)pt.getX(),(int)pt.getY(),10,10);
			}
		}
		new ImagePlus("plant detector debug",dbgIp).show();
		
	}
	
	public ImageProcessor blueRemoval(ImageProcessor ip){
		for(int i=0;i<ip.getPixelCount();++i){
			int pixVal=ip.get(i);
			int[] rgb={(pixVal>>16)&0xff,(pixVal>>8)&0xff,pixVal&0xff};
			
			int min=Integer.MAX_VALUE;
			int max=Integer.MIN_VALUE;
			for(int j=0;j<3;++j){
				if(rgb[j]>max){
					max=rgb[j];
				}
				if(rgb[j]<min){
					min=rgb[j];
				}
			}
			if(rgb[2]-rgb[0]>10 && rgb[2]-rgb[1]>10){
				ip.set(i,0);
			}
		}
		
//		new ImagePlus("plant det blue",origIp).show();
		return ip;
	}
	
	public void detectShoots(int timePt,double gravityEpsilon){
		ColorAnalyzer ca=new ColorAnalyzer(origWidth);
		ca.calcWeights((int[])origIp.getPixels());
		List<ShootRegion> shootRegions=ca.getShootRegions(30);
		
		for(int row=0;row<plants.size();++row){
			for(int col=0;col<plants.get(row).size();++col){
				Plant plant=plants.get(row).get(col);

				Point2D seedCenter=plant.getSeedCenter();
				double searchRange=0;
				searchRange=seedingLayout.getSearchWidth(row,col); //TODO get searchRange from real seed centers
				
				if(seedCenter==null){
					seedCenter=seedingLayout.getSeedPosition(row,col);
				}
				
				ShootRegion assignedReg=null;
				double maxGravity=0;
				for(ShootRegion reg:shootRegions){
					double dist=reg.getDistance(seedCenter.getX(),seedCenter.getY());
					if(dist<=searchRange){
						double mass=reg.area();
						double gravity=mass/(dist*dist+gravityEpsilon);
						if(gravity>maxGravity){
							maxGravity=gravity;
							assignedReg=reg;
						}
					}
				}
				
				plant.setShootRoi(timePt,assignedReg.getRoiOutline());
			}
		}
		
	}
	
	
	public void detectPlants(int timePt){
		float[] shootRegions=new float[origWidth*origHeight];
		float[] rootRegions=new float[origWidth*origHeight];
		
		ImageProcessor dbgIp=new ColorProcessor(origWidth,origHeight);
		ImagePlus dbgImg=new ImagePlus("rois",dbgIp);
		dbgImg.show();
		for(int roiNr=0;roiNr<ridgeRois.size();++roiNr){
			RidgeRoi rr=ridgeRois.get(roiNr);
			if(rr.getShootPixelIndices().size()>0){
				for(int pixIdx:rr.getShootPixelIndices()){
					shootRegions[pixIdx]=roiNr+1;
					dbgIp.set(pixIdx,Color.green.getRGB());
				}
				for(int pixIdx:rr.getRootPixelIndices()){
					rootRegions[pixIdx]=roiNr+1;
					dbgIp.set(pixIdx,Color.white.getRGB());
				}
				dbgImg.updateAndDraw();
//				new WaitForUserDialog("next").show();
			}
		}
		
		for(int row=0;row<plants.size();++row){
			for(int col=0;col<plants.get(row).size();++col){
				Plant plant=plants.get(row).get(col);

//				Rectangle searchArea=null;
				Point2D seedCenter=plant.getSeedCenter();
				double searchRange=0;
				searchRange=seedingLayout.getSearchWidth(row,col); //TODO get searchRange from real seed centers
				
				if(seedCenter==null){
					seedCenter=seedingLayout.getSeedPosition(row,col);
				}
				
				
				
				Roi shootRoi=findNearestShootRegion(shootRegions,(int)seedCenter.getX(),(int)seedCenter.getY(),(int)searchRange/2);
				dbgIp.setColor(Color.red);
				dbgIp.draw(shootRoi);
				dbgImg.updateAndDraw();
				
			}
		}
	}
	
		private Roi findNearestShootRegion(float[] regions,int xc,int yc,int maxRange){
		int curRange=1;
		int[] assignedRoi=null;
	
		
		while(assignedRoi==null && curRange<=maxRange){
			// top row
			int y=yc-curRange;
			if(y>=0 && y<origHeight){
				for(int x=xc-curRange;x<=xc+curRange;++x){
					if(x<0 || x>=origWidth){
						continue;
					}
					int idx=y*origWidth+x;
					if(regions[idx]>0){
						assignedRoi=new int[]{x,y};
						break;
					}
				}
			}
			if(assignedRoi!=null){
				break;
			}
			
			// bottom row
			y=yc+curRange;
			if(y>=0 && y<origHeight){
				for(int x=xc-curRange;x<=xc+curRange;++x){
					if(x<0 || x>=origWidth){
						continue;
					}
					int idx=y*origWidth+x;
					if(regions[idx]>0){
						assignedRoi=new int[]{x,y};
						break;
					}
				}
			}
			if(assignedRoi!=null){
				break;
			}
			
			// left column
			int x=xc-curRange;
			if(x>=0 && x<origWidth){
				for(y=yc-curRange+1;y<=yc+curRange-1;y++){
					if(yc<0 || yc>=origHeight){
						continue;
					}
					int idx=y*origWidth+x;
					if(regions[idx]>0){
						assignedRoi=new int[]{x,y};
						break;
					}
				}
			}
			if(assignedRoi!=null){
				break;
			}
			
			// right column
			x=xc+curRange;
			if(x>=0 && x<origWidth){
				for(y=yc-curRange+1;y<=yc+curRange-1;y++){
					if(yc<0 || yc>=origHeight){
						continue;
					}
					int idx=y*origWidth+x;
					if(regions[idx]>0){
						assignedRoi=new int[]{x,y};
						break;
					}
				}
			}
			
			curRange++;
		}
		
		Roi roi=null;
		if(assignedRoi!=null){
			ImageProcessor roiIp=new FloatProcessor(origWidth,origHeight,regions);
			Wand wand=new Wand(roiIp);
			wand.autoOutline(assignedRoi[0],assignedRoi[1],0.0,Wand.EIGHT_CONNECTED);
			roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
		}
		
		return roi;
	}

	public void detectPlants_deprecated(int timePt){
		float[] rootRegions=new float[origWidth*origHeight];
		float[] shootRegions=new float[origWidth*origHeight];
		ImageProcessor dbgIp=new ColorProcessor(origWidth,origHeight);
		ImagePlus dbgImg=new ImagePlus("rois",dbgIp);
		dbgImg.show();
		for(int roiNr=0;roiNr<ridgeRois.size();++roiNr){
			RidgeRoi rr=ridgeRois.get(roiNr);
			for(int pixIdx:rr.getRootPixelIndices()){
				rootRegions[pixIdx]=roiNr+1;
				dbgIp.set(pixIdx,Color.white.getRGB());
				
			}
			for(int pixIdx:rr.getShootPixelIndices()){
				shootRegions[pixIdx]=roiNr+1;
				rootRegions[pixIdx]=roiNr+1;
				dbgIp.set(pixIdx,Color.green.getRGB());
			}
			dbgImg.updateAndDraw();
			new WaitForUserDialog("next").show();
			continue;
		}
		ImageProcessor rootIp=new FloatProcessor(origWidth,origHeight,rootRegions);
		ImageProcessor shootIp=new FloatProcessor(origWidth,origHeight,shootRegions);

		for(int row=0;row<plants.size();++row){
			for(int col=0;col<plants.get(row).size();++col){
				Plant plant=plants.get(row).get(col);

				Rectangle searchArea=null;
				Point2D seedCenter=plant.getSeedCenter();
				double searchRange=0;
				searchRange=seedingLayout.getSearchWidth(row,col); //TODO get searchRange from real seed centers
				
				if(seedCenter==null){
					seedCenter=seedingLayout.getSeedPosition(row,col);
				}
				
				double minDistance=Double.MAX_VALUE;
				float assignedRoi=-1f;
				int assignedIdx=-1;
				for(int pIdx=0;pIdx<rootRegions.length;++pIdx){
					if(rootRegions[pIdx]>0){
						int x=pIdx%origWidth;
						int y=pIdx/origWidth;
						double dist=seedCenter.distance(x,y);
						if(dist<searchRange && dist<minDistance){
							minDistance=dist;
							assignedRoi=rootRegions[pIdx];
							assignedIdx=pIdx;
						}
					}
				} // TODO more efficient search algorithm
				
				if(assignedIdx!=-1){
					rootIp.setThreshold(assignedRoi,assignedRoi,ImageProcessor.NO_LUT_UPDATE);
					ThresholdToSelection ths=new ThresholdToSelection();
					try{
					Roi rootRoi=ths.convert(rootIp);
					plant.setRootRoi(timePt,rootRoi);
					}
					catch(Exception e){
						e.printStackTrace();
						new ImageJ();
						new ImagePlus("root regions",rootIp).show();
						new WaitForUserDialog("root roi failure.").show();
					}

					shootIp.setThreshold(assignedRoi,assignedRoi,ImageProcessor.NO_LUT_UPDATE);
					try{
					Roi shootRoi=ths.convert(shootIp);
					plant.setShootRoi(timePt,shootRoi);
					}
					catch(Exception e){
						e.printStackTrace();
						new ImageJ();
						new ImagePlus("shoot regions",shootIp).show();
						new WaitForUserDialog("shoot roi failure.").show();
					}
				}
			}
		}
	}
	
	public void showGrayIp(){
		new ImagePlus("gray scale",origIp.convertToByte(false)).show();
	}

//	public void detectRootParts2(int timePt){
//		int w=origIp.getWidth();
//		int h=origIp.getHeight();
//		ImageProcessor blurredIp=origIp.convertToByte(false);
////		ImageProcessor blurredIp=subtractPlane(origIp,new Rectangle(0,0,w,h));
//		GaussianBlur gb=new GaussianBlur();
//		gb.blurGaussian(blurredIp,5,5,0.001);
//		blurredIp=subtractPlane(blurredIp,new Rectangle(0,0,w,h));
//		FloatProcessor gradMag=(FloatProcessor)blurredIp.convertToFloat();
//		FloatProcessor gradDir=(FloatProcessor)blurredIp.convertToFloat();
//		getGradientMagnitudeAndDirection(gradMag,gradDir);
//
//		ImageProcessor gradEdge=new ByteProcessor(w,h);
//		int mdist=1;
//		for(int y=mdist;y<h-mdist;++y){
//			for(int x=mdist;x<w-mdist;++x){
//				if(gradEdge.get(x,y)>0){
//					continue;
//				}
//					float dir1=gradDir.getf(x+mdist,y);
//					float dir2=gradDir.getf(x-mdist,y);
//					if(dir1*dir2<0){
//						gradEdge.set(x,y,255);
//						continue;
//					}
//
//					dir1=Math.abs(dir1)-(float)Math.PI/2.0f;
//					dir2=Math.abs(dir2)-(float)Math.PI/2.0f;
//					if(dir1*dir2<0){
//						gradEdge.set(x,y,255);
//						continue;
//					}
//			}
//		}
//		new ImagePlus("grad edge",gradEdge).show();
//		new ImagePlus("mag",gradMag).show();
//		new ImagePlus("dir",gradDir).show();
//
//	}
	
	public void detectRootPartsGVF(int timePt,double sigmaBlur,int gvfNIterations){
		ImageProcessor workIp=origIp.duplicate().convertToFloat();
		int w=workIp.getWidth();
		int h=workIp.getHeight();
		
		if (sigmaBlur>0) {
			GaussianBlur gb=new GaussianBlur();
			gb.blurFloat((FloatProcessor)workIp,sigmaBlur,sigmaBlur,1.0e-4);
		}		
		
		double[] pixels=new double[workIp.getPixelCount()];
		for(int i=0;i<pixels.length;++i){
			pixels[i]=workIp.getf(i);
		}
		
		GradientVectorFlow gvfc=new GradientVectorFlow();
		double[][] gvf=gvfc.calculate(pixels,w,h,1.0,0.3,gvfNIterations);
		
		RidgeDetector ridged=new RidgeDetector();
		ridged.calcDerivates(gvf[0],gvf[1],w,h);
		ridged.calcDivergence();
		ridged.calcEigenVectors(ridged.getDivergenceSkeleton());
		
		ImageProcessor binaryIp=ridged.thresholdRidgePixels(origIp,5.0);
		binaryIp.setColor(255);
		for(int row=0;row<plants.size();++row){
			for(int col=0;col<plants.get(row).size();++col){
				Plant plant=plants.get(row).get(col);
				if(plant==null){
					continue;
				}
				Roi shootRoi=plant.getShootRoi(timePt);
				if(shootRoi!=null){
					binaryIp.fill(shootRoi);
				}

				Roi plantRoi=null;
				Point2D seedCenter=plant.getSeedCenter();
				if(binaryIp.get((int)(seedCenter.getX()+0.5),(int)(seedCenter.getY()+0.5))>0){
					Wand wand=new Wand(binaryIp);
					wand.autoOutline((int)(seedCenter.getX()+0.5),(int)(seedCenter.getY()+0.5), 0.0, Wand.EIGHT_CONNECTED);
					plantRoi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
					
				}
				plant.setRootRoi(timePt,plantRoi);

			}
		}
		
	}
	
	public void detectRootParts(int timePt){ //List<List<Point2D>> centerGuesses,List<List<Point2D>> seedPts){
		EDM edm=new EDM();
		for(int row=0;row<plants.size();++row){
			for(int col=0;col<plants.get(row).size();++col){
				Plant plant=plants.get(row).get(col);
				if(plant==null){
					continue;
				}
				Roi shootRoi=plant.getShootRoi(timePt);
				if(shootRoi==null){
					continue;
				}
				shootRoi=(Roi)shootRoi.clone();
				Rectangle shootRect=shootRoi.getBounds();
				
				Rectangle searchArea=null;

				Roi prevPlant=plant.getRootRoi(timePt-1);
				IJ.log("plant "+row+","+col);
				if(prevPlant!=null){
					searchArea=prevPlant.getBounds();
					searchArea.x-=prefs_expert.getInt("shootWidthStep",200)/2;
					searchArea.y-=prefs_expert.getInt("shootHeightStep",200)/2;
					searchArea.width+=prefs_expert.getInt("shootWidthStep",200);
					searchArea.height+=prefs_expert.getInt("shootHeightStep",200);
					IJ.log("prev plant");
				}
				else{
					searchArea=shootRoi.getBounds();
					searchArea.x-=prefs_expert.getInt("shootWidthStep",200)/2;
					searchArea.y-=prefs_expert.getInt("shootHeightStep",200)/2;
					searchArea.width+=prefs_expert.getInt("shootWidthStep",200);
					searchArea.height+=prefs_expert.getInt("shootHeightStep",200);
					IJ.log("shoot roi");
				}

				
				ContrastEnhancer ce=new ContrastEnhancer();
				ImageProcessor diffIp=origIp.duplicate();
				ImageProcessor binaryIp=null;
				Roi binaryRoi=null;
				ThresholdToSelection ts=new ThresholdToSelection();
				
				boolean done=true;
				int loopcnt=0;
				Point trackPt=null;
				do{
					done=true;
					if(searchArea.x<0){
						searchArea.x=0;
					}
					if(searchArea.x+searchArea.width>origIp.getWidth()){
						searchArea.width-=searchArea.x+searchArea.width-origIp.getWidth();
					}
					if(searchArea.y<0){
						searchArea.y=0;
					}
					if(searchArea.y+searchArea.height>origIp.getHeight()){
						searchArea.height-=searchArea.y+searchArea.height-origIp.getHeight();
					}
					IJ.log("searchArea1: "+searchArea.toString());
					
					diffIp=subtractPlane(origIp,searchArea);
					ce.equalize(diffIp);

					shootRoi.setLocation(shootRect.x-searchArea.x,shootRect.y-searchArea.y);

					binaryIp=getBinary(diffIp.duplicate());
					ImageProcessor skeletonIp=getSkeleton((ByteProcessor)binaryIp.duplicate(),shootRoi);
					
					ImageProcessor trackSearchIp=new ByteProcessor(searchArea.width,searchArea.height);
					trackSearchIp.setColor(255);
					trackSearchIp.draw(shootRoi);

					skeletonIp.invert();

					List<Point> possibleTrackPts=new ArrayList<Point>();
					for(int y=0;y<searchArea.height;++y){
						for(int x=0;x<searchArea.width;++x){
							SkeletonNode curNode=new SkeletonNode(x+searchArea.x,y+searchArea.y);
							if(trackSearchIp.get(x,y)==255 && skeletonIp.get(x,y)==255){
								possibleTrackPts.add(new Point(x,y));
								IJ.log("TP: "+x+","+y);
							}
						}
					}

					skeletonIp.setColor(0);
					skeletonIp.fill(shootRoi);
					for(Point pt:possibleTrackPts){
						skeletonIp.set(pt.x,pt.y,255);
					}

					SkeletonGraph trackGraph=new SkeletonGraph();
					trackGraph.create(skeletonIp,255,null,searchArea);

					switch(possibleTrackPts.size()){
					case 0:IJ.log("no track pt!!"); trackPt=null; break;
					case 1:IJ.log("single track pt"); trackPt=possibleTrackPts.get(0); break;
					default:IJ.log("search track pt (one of "+possibleTrackPts.size()+" points)");
						double maxLength=0;
						for(Point pt:possibleTrackPts){
							List<SkeletonNode> lp=trackGraph.getLongestPathFromNode(new SkeletonNode(pt.x+searchArea.x,pt.y+searchArea.y));
							if(lp==null || lp.size()<2)
								continue;
							Double lpLen=trackGraph.getPathLength(lp.get(0),lp.get(lp.size()-1));
							if(lpLen>maxLength){
								maxLength=lpLen;
								trackPt=pt;
							}
						}
					}

					if(trackPt==null){
						done=true;
						continue;
					}
					
					List<Point> skelPixels=identifyRootSkeleton(skeletonIp,255,trackPt,15,3);

					ImageProcessor dmapIp=binaryIp.duplicate();
					edm.toEDM(dmapIp);
					binaryIp=new ByteProcessor(searchArea.width,searchArea.height);
					binaryIp.setColor(255);
					
					for(Point pt:skelPixels){
						int rad=dmapIp.get(pt.x,pt.y);
						if(rad<3)
							rad=3;
						binaryIp.fillOval(pt.x-rad,pt.y-rad,rad+rad,rad+rad);
					}

					binaryIp.setThreshold(1,255,ImageProcessor.NO_LUT_UPDATE);
					binaryRoi=ts.convert(binaryIp);//new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);

					Rectangle binRect=binaryRoi.getBounds();
					
					if(binRect.width>0.3*origIp.getWidth() || binRect.height>0.5*origIp.getHeight()){
						IJ.log("roi size out of bounds! Giving up.");
						binaryRoi=null;
						done=true;
						continue;
					}

					if(binRect.x<=10 && searchArea.x>0){
						searchArea.x-=prefs_expert.getInt("shootWidthStep",200)/2;
						searchArea.width+=prefs_expert.getInt("shootWidthStep",200)/2;
						done=false;
					}
					if(binRect.x+binRect.width>=searchArea.width-10 && searchArea.x+searchArea.width<origIp.getWidth()){
						searchArea.width+=prefs_expert.getInt("shootWidthStep",200)/2;
						done=false;
					}
					if(binRect.y<=10 && searchArea.y>0){
						searchArea.y-=prefs_expert.getInt("shootHeightStep",200)/2;
						searchArea.height+=prefs_expert.getInt("shootHeightStep",200)/2;
						done=false;
					}
					if(binRect.y+binRect.height>=searchArea.height-10 && searchArea.y+searchArea.height<origIp.getHeight()){
						searchArea.height+=prefs_expert.getInt("shootHeightStep",200)/2;
						done=false;
					}
					
					IJ.log("searchArea2: "+searchArea.toString());
					
				}while(!done);
				
				plant.setRootTrackPt(trackPt);
				
				if(binaryRoi!=null){
					if(binaryRoi.getBounds().width>shootRect.width || binaryRoi.getBounds().height>shootRect.height){
						binaryRoi.setLocation(binaryRoi.getBounds().x+searchArea.x,binaryRoi.getBounds().y+searchArea.y);
						plant.setRootRoi(timePt,binaryRoi);
					}
				}
				
			}
		}
	}
	
	private ImageProcessor getBinary(ImageProcessor srcIp){
		AutoThresholder ath=new AutoThresholder();
		int threshold=ath.getThreshold(AutoThresholder.Method.Default,srcIp.getHistogram());
		srcIp.threshold(threshold);

		return new BinaryProcessor((ByteProcessor)srcIp);
	}
	
	private ImageProcessor getSkeleton(ByteProcessor srcIp,Roi shootRoi){
		
		BinaryProcessor binaryIp=new BinaryProcessor(srcIp);
		binaryIp.setColor(255);
		binaryIp.fill(shootRoi);
		
		binaryIp.invert();
		binaryIp.skeletonize();
		
		return binaryIp;
	}
	
	private void cleanupSkeleton(ImageProcessor skelIp){
		int width=skelIp.getWidth();
		int height=skelIp.getHeight();
		
		for(int y=0;y<height;++y){
			for(int x=0;x<width;++x){
				if(skelIp.get(x,y)==0){
					int cnt=0;
					for(int yy=y-1;yy<=y+1;++yy){
						if(yy<0 || yy>height-1)
							continue;
						for(int xx=x-1;xx<=x+1;++xx){
							if(xx<0 || xx>width-1 || xx==yy)
								continue;
							if(skelIp.get(xx,yy)==0){
								cnt++;
							}
						}
					}
					if(cnt==0){
						skelIp.set(x,y,255);
					}
				}
			}
		}
	}
	
	private List<Point> identifyRootSkeleton(ImageProcessor skelIp,int fgColor,Point nearestPt,int maxRange,int segmentMinSize){
		int bgColor=255-fgColor;
		List<TreeSegment> skelSegments=new ArrayList<TreeSegment>();
		List<Point> rootSkel=new ArrayList<Point>();
		Stack<Point> stack=new Stack<Point>();


		int startSegmentIdx=-1;
		for(int y=0;y<skelIp.getHeight();++y){
			for(int x=0;x<skelIp.getWidth();++x){
				if(skelIp.get(x,y)==fgColor){
					stack.push(new Point(x,y));
					skelIp.set(x,y,bgColor);
					
					boolean isStartSegment=false;
					TreeSegment segment=new TreeSegment();
					while(!stack.empty()){
						Point pt=stack.pop();
						segment.add(pt);
						if(pt.equals(nearestPt)){
							startSegmentIdx=skelSegments.size();
							isStartSegment=true;
						}

						for(int yy=pt.y-1;yy<=pt.y+1;++yy){
							for(int xx=pt.x-1;xx<=pt.x+1;++xx){
								if(xx>=0 && xx<skelIp.getWidth() && yy>=0 && yy<skelIp.getHeight()){
									if(skelIp.get(xx,yy)==fgColor){
										stack.push(new Point(xx,yy));
										skelIp.set(xx,yy,bgColor);
									}
								}
							}
						}
					}//while
					if(segment.size()>=segmentMinSize || isStartSegment){
						skelSegments.add(segment);
					}
				}
			}
		}

		Stack<TreeSegment> segStack=new Stack<TreeSegment>();
		segStack.push(skelSegments.get(startSegmentIdx));
		List<Integer> checkedSegments=new ArrayList<Integer>();
		checkedSegments.add(startSegmentIdx);
		while(!segStack.empty()){
			TreeSegment curSeg=segStack.pop();
			rootSkel.addAll(curSeg.getAll());
			for(int j=0;j<skelSegments.size();++j){
				if(checkedSegments.contains(j)){
					continue;
				}
				TreeSegment otherSeg=skelSegments.get(j);
				checkedSegments.add(j);
				double realDist=Double.MAX_VALUE;
				for(int i=0;i<curSeg.size();++i){
					Point curPt=curSeg.get(i);
					double dist=distancePtToSegmentBounds(curPt,otherSeg);
					if(dist<=maxRange && dist<=otherSeg.size()){
						for(int k=0;k<otherSeg.size();++k){
							dist=curSeg.get(i).distance(otherSeg.get(k));
							if(dist<realDist){
								realDist=dist;
							}
						}
					}
				}
				if(realDist<maxRange && realDist<otherSeg.size()){
					segStack.push(otherSeg);
				}
			}
			
		}
		skelIp.setColor(bgColor);
		skelIp.fill();
		for(Point pt:rootSkel){
			skelIp.set(pt.x,pt.y,fgColor);
		}
		
		return rootSkel;
	}
	
//	private ImageProcessor getSkeletonGradientDirection(ImageProcessor diffIp){
//		int width=diffIp.getWidth();
//		int height=diffIp.getHeight();
//
//		BinaryProcessor binaryIp=new BinaryProcessor(new ByteProcessor(width,height));
//		for(int i=0;i<diffIp.getPixelCount();++i){
//			if(diffIp.get(i)>=5){
//				binaryIp.set(i,255);
//			}
//		}
//
//		ImageProcessor dmapIp=binaryIp.duplicate();
//		EDM edm=new EDM();
//		edm.toEDM(dmapIp);
//
//		ImageProcessor blurredIp=diffIp.duplicate();
//		GaussianBlur gb=new GaussianBlur();
//		gb.blurGaussian(blurredIp,5,5,0.001);
//		FloatProcessor gradDirIp=blurredIp.convertToFloatProcessor();
//		getGradientMagnitudeAndDirection(null,gradDirIp);
//
//		ImageProcessor gradEdge=new ByteProcessor(width,height);
//		int mdist=1;
//		for(int y=mdist;y<height-mdist;++y){
//			for(int x=mdist;x<width-mdist;++x){
//				if(gradEdge.get(x,y)>0){
//					continue;
//				}
//				if(dmapIp.get(x,y)>3 && binaryIp.get(x,y)>0){
//					float dir1=gradDirIp.getf(x+mdist,y);
//					float dir2=gradDirIp.getf(x-mdist,y);
//					if(dir1*dir2<0){
//						gradEdge.set(x,y,255);
//						continue;
//					}
//
//					dir1=Math.abs(dir1)-(float)Math.PI/2.0f;
//					dir2=Math.abs(dir2)-(float)Math.PI/2.0f;
//					if(dir1*dir2<0){
//						gradEdge.set(x,y,255);
//						continue;
//					}
//				}
//			}
//		}
//
//		BinaryProcessor gradEdgeSkel=new BinaryProcessor((ByteProcessor)gradEdge);
//		fillHoles(gradEdgeSkel,255,0);
//		gradEdgeSkel.invert();
//		gradEdgeSkel.skeletonize();
//
//		return gradEdgeSkel;
//	}
//
	
	private void connectSkeletonParts(ImageProcessor ip,int fgValue,double maxRange){
		int w=ip.getWidth();
		int h=ip.getHeight();
		List<Point> skeletonPts=new ArrayList<Point>();
		List<Point> skeletonEndPts=new ArrayList<Point>();
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				if(ip.get(x,y)==fgValue){
					int nNeigh=PixelUtils.getNeighbourCnt8(ip,x,y);
					if(nNeigh==1){
						skeletonEndPts.add(new Point(x,y));
					}
					else{
					skeletonPts.add(new Point(x,y));
					}
				}
			}
		}
		
		double minDist=Double.MAX_VALUE;
		double sqrt2=Math.sqrt(2.0);
		ip.setColor(fgValue);
		for(Point ePt:skeletonEndPts){
			Point connectPt=null;
			for(Point pt:skeletonPts){
				double dist=ePt.distance(pt);
				if(dist<=maxRange && dist<minDist && dist>sqrt2){
					minDist=dist;
					connectPt=pt;
				}
			}
			if(connectPt!=null){
				ip.drawLine(ePt.x,ePt.y,connectPt.x,connectPt.y);
			}
		}
	}
	
	private double distancePtToSegmentBounds(Point pt,TreeSegment seg){
		if (pt.x<seg.xMin) { 
            if (pt.y<seg.yMin) {
                return pt.distance(seg.xMin, seg.yMin);
                
            }
            else if (pt.y>seg.yMax) {
                return pt.distance(seg.xMin, seg.yMax);
            }
            else {
                return 0d;
            }
        }
        else if (pt.x>seg.xMax) {
            if (pt.y < seg.yMin) {
                return pt.distance(seg.xMax, seg.yMin);
            }
            else if (pt.y > seg.yMax) {
                return pt.distance(seg.xMax, seg.yMax);
            }
            else {
                return 0d;
            }
        }
        else {
            if(pt.y < seg.yMin){
                return seg.yMin-pt.y;
            }
            else if (pt.y > seg.yMax) {
                return pt.y - seg.yMax;
            }
            else {
                return 0d;
            }
        }
	}
	
//
//	/*
//	 *  code adopted from "Differentials_" package (see below)
//	 */
//	/*=====================================================================
//	| EPFL/STI/IMT/LIB
//	| Philippe Thevenaz
//	| BM-Ecublens 4.137
//	| CH-1015 Lausanne VD
//	| Switzerland
//	|
//	| phone (CET): +41(21)693.51.61
//	| fax: +41(21)693.68.10
//	| RFC-822: philippe.thevenaz@epfl.ch
//	| X-400: /C=ch/A=400net/P=switch/O=epfl/S=thevenaz/G=philippe/
//	| URL: http://bigwww.epfl.ch/
//	\=====================================================================*/
//
//	private void getGradientMagnitudeAndDirection(FloatProcessor magIp,FloatProcessor dirIp){
//		ImageProcessor ip=null;
//		if(magIp!=null)
//			ip=magIp;
//		else
//			ip=dirIp;
//
//		final double FLT_EPSILON =(double)Float.intBitsToFloat((int)0x33FFFFFF);
//		int width=ip.getWidth();
//		int height=ip.getHeight();
//		ImageProcessor h = ip.duplicate();
//		ImageProcessor v = ip.duplicate();
//
//		float[] floatPixelsMag = null;
//		if(magIp!=null){
//			floatPixelsMag=(float[])magIp.getPixels();
//		}
//		float[] floatPixelsDir = null;
//		if(dirIp!=null){
//			floatPixelsDir=(float[])dirIp.getPixels();
//		}
//		float[] floatPixelsH = (float[])h.getPixels();
//		float[] floatPixelsV = (float[])v.getPixels();
//
////		Differentials_ diff=new Differentials_();
////		diff.getHorizontalGradient(h, FLT_EPSILON);
////		diff.getVerticalGradient(v, FLT_EPSILON);
//		for (int y = 0, k = 0; (y < height); y++) {
//			for (int x = 0; (x < width); x++, k++) {
//				if(magIp!=null){
//					floatPixelsMag[k]=(float)Math.sqrt(floatPixelsH[k]*floatPixelsH[k]+floatPixelsV[k]*floatPixelsV[k]);
//				}
//				if(dirIp!=null){
//					floatPixelsDir[k]=(float)Math.atan2(floatPixelsH[k], floatPixelsV[k]);
//				}
//			}
//		}
//
//		if(magIp!=null){
//			magIp.resetMinAndMax();
//		}
//		if(dirIp!=null){
//			dirIp.resetMinAndMax();
//		}
//	}
//
	
    // Binary fill by Gabriel Landini, G.Landini at bham.ac.uk
    // 21/May/2008
    private void fillHoles(ImageProcessor ip, int foreground, int background) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        FloodFiller ff = new FloodFiller(ip);
        ip.setColor(127);
        for (int y=0; y<height; y++) {
            if (ip.getPixel(0,y)==background) ff.fill(0, y);
            if (ip.getPixel(width-1,y)==background) ff.fill(width-1, y);
        }
        for (int x=0; x<width; x++){
            if (ip.getPixel(x,0)==background) ff.fill(x, 0);
            if (ip.getPixel(x,height-1)==background) ff.fill(x, height-1);
        }
        byte[] pixels = (byte[])ip.getPixels();
        int n = width*height;
        for (int i=0; i<n; i++) {
        if (pixels[i]==127)
            pixels[i] = (byte)background;
        else
            pixels[i] = (byte)foreground;
        }
    }
    
    private List<Point> getLinePts(Point center,double radius,double angle){
    	int dx=(int)(Math.round(radius*Math.cos(angle)));
    	int dy=(int)(Math.round(radius*Math.sin(angle)));
		int absdx = dx>=0?dx:-dx;
		int absdy = dy>=0?dy:-dy;
		
		int n = absdy>absdx?absdy:absdx;
		double xinc = (double)dx/n;
		double yinc = (double)dy/n;
		double x1 = center.x-dx;
		double y1 = center.y-dy;
		double x2 = center.x+dx;
		double y2 = center.y+dy;
//		n++;
		if (n>1000000) return null;
		List<Point> linePts=new ArrayList<Point>();
		List<Point> tmpPts=new ArrayList<Point>();
		do {
			linePts.add(new Point((int)Math.round(x1),(int)Math.round(y1)));
			tmpPts.add(new Point((int)Math.round(x2),(int)Math.round(y2)));
			x1 += xinc;
			y1 += yinc;
			x2 -= xinc;
			y2 -= yinc;
		} while (--n>0);
		
		linePts.add(center);
		for(int i=tmpPts.size()-1;i>=0;--i){
			linePts.add(tmpPts.get(i));
		}
		return linePts;
    }
    
    private List<Point> getCirclePts(Point center,double radius){
    	double n=2*Math.PI*radius;
    	double dPhi=1.0/radius;
    	
    	double phi=0;
    	
    	List<Point> circlePts=new ArrayList<Point>();
    	for(int i=0;i<n;++i){
    		Point pt=new Point((int)(radius*Math.cos(phi)+center.getX()+0.5),(int)(radius*Math.sin(phi)+center.getY()+0.5));
    		if(!circlePts.contains(pt)){
    			circlePts.add(pt);
    		}
    		phi+=dPhi;
    	}
    	
    	return circlePts;
    }
    

	private ImageProcessor subtractPlane(ImageProcessor ip,Rectangle searchRect){
		int searchRectArea=searchRect.width*searchRect.height;
		double[] xArr=new double[searchRectArea];
		double[] yArr=new double[searchRectArea];
		double[][] zArr=new double[3][searchRectArea];
		
		for(int y=0;y<searchRect.height;++y){
			if(y+searchRect.y<0 || y+searchRect.y>ip.getHeight()-1)
				continue;
			for(int x=0;x<searchRect.width;++x){
				if(x+searchRect.x<0 || x+searchRect.x>ip.getWidth()-1)
					continue;
				int val=origIp.get(x+searchRect.x,y+searchRect.y);
				int[] rgb={(val>>16)&0xff,(val>>8)&0xff,val&0xff};

				int idx=y*searchRect.width+x;
				xArr[idx]=x;
				yArr[idx]=y;
				for(int i=0;i<3;++i){
					zArr[i][idx]=rgb[i];
				}
			}
		}
		
		ImageProcessor diffIp=new ByteProcessor(searchRect.width,searchRect.height);
		for(int i=0;i<3;++i){
			PlaneFit fit=new PlaneFit(xArr,yArr,zArr[i]);
			fit.calc();
			for(int y=0;y<searchRect.height;++y){
				if(y+searchRect.y<0 || y+searchRect.y>origIp.getHeight())
					continue;
				for(int x=0;x<searchRect.width;++x){
					if(x+searchRect.x<0 || x+searchRect.x>origIp.getWidth())
						continue;
					int idx=y*searchRect.width+x;
					int val=(int)fit.diffAt(x,y,zArr[i][idx]);
					if(diffIp.get(x,y)<val)
						diffIp.set(x,y,val);
				}
			}
		}
		return diffIp;
	}
	
	private List<TreePoint> getNeighbours4(ImageProcessor ip,TreePoint pos,int highThreshold,int lowThreshold,boolean[][] visited){
		List<TreePoint> neighbours=new ArrayList<TreePoint>();
		int level=pos.level;
		if(pos.x>0){
			int neighVal=ip.get(pos.x-1,pos.y);
			if(neighVal<highThreshold && neighVal>lowThreshold && !visited[pos.x-1][pos.y]){
				neighbours.add(new TreePoint(pos.x-1,pos.y,level+1));
			}
		}
		if(pos.x<ip.getWidth()-1){
			int neighVal=ip.get(pos.x+1,pos.y);
			if(neighVal<highThreshold && neighVal>lowThreshold && !visited[pos.x+1][pos.y]){
				neighbours.add(new TreePoint(pos.x+1,pos.y,level+1));
			}
		}
		if(pos.y>0){
			int neighVal=ip.get(pos.x,pos.y-1);
			if(neighVal<highThreshold && neighVal>lowThreshold && !visited[pos.x][pos.y-1]){
				neighbours.add(new TreePoint(pos.x,pos.y-1,level+1));
			}
		}
		if(pos.y<ip.getHeight()-1){
			int neighVal=ip.get(pos.x,pos.y+1);
			if(neighVal<highThreshold && neighVal>lowThreshold && !visited[pos.x][pos.y+1]){
				neighbours.add(new TreePoint(pos.x,pos.y+1,level+1));
			}
		}
		
		return neighbours;
	}
	
	private Map<Point,Integer> getSkeletonThickness(ImageProcessor binaryIp,int minimumEDM){
		int w=binaryIp.getWidth();
		int h=binaryIp.getHeight();
		
		ImageProcessor edmIp=binaryIp.duplicate();
		EDM edm=new EDM();
		edm.toEDM(edmIp);
		
		BinaryProcessor skelIp=new BinaryProcessor((ByteProcessor) binaryIp);
		skelIp.invert();
		skelIp.skeletonize();

		Map<Point,Point> assignedCenters=new HashMap<Point,Point>();
		List<Point> skelPixels=new ArrayList<Point>();
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				if(skelIp.get(x,y)==0){ 
					int edmVal=edmIp.get(x,y);
					if(edmVal<minimumEDM){
						skelIp.set(x,y,255);
					}
					else{
						skelPixels.add(new Point(x,y));
					}
				}
			}
		}
		
		List<Point> usedPixels=new ArrayList<Point>();
		for(Point endPt:skelPixels){
			List<Point> neighbours=PixelUtils.getNeighbours(endPt,skelIp,null);
			Point curPt=null;
			Point tmpPt=neighbours.get(0);
			if(edmIp.get(tmpPt.x,tmpPt.y)>endPt.distance(tmpPt)){
				curPt=neighbours.get(0);
				assignedCenters.put(endPt,curPt);
				usedPixels.add(endPt);
			}

			while(curPt!=null){
				double curDist=endPt.distance(curPt);
				int curEDM=edmIp.get(curPt.x,curPt.y);
				if(curEDM>curDist){
					assignedCenters.put(endPt,curPt);

					List<Point> nextNeighbours=PixelUtils.getNeighbours(curPt,skelIp,0);
					int maxEDM=0;
					curPt=null;
					for(Point p:nextNeighbours){
						if(usedPixels.contains(p))
							continue;
						int edmVal=edmIp.get(p.x,p.y);
						if(edmVal>maxEDM && edmVal>endPt.distance(p)){
							curPt=p;
							maxEDM=edmVal;
						}
						usedPixels.add(p);
					}
				}
			}
		}
		
		return new HashMap<Point,Integer>();
	}
	

	public void calcCieLabDistance(){
		ImageProcessor workIp=origIp.duplicate();
//		new ContrastEnhancer().equalize(workIp);
		ImageStack rgbStack=new ImageStack(origWidth,origHeight);
		for(int i=0;i<3;++i)
			rgbStack.addSlice(new ByteProcessor(origWidth,origHeight));
		for(int y=0;y<origHeight;++y){
			for(int x=0;x<origWidth;++x){
				int val=workIp.get(x,y);
				int[] rgb={(val>>16)&0xff,(val>>8)&0xff,val&0xff};
//				float[] hsb=Color.RGBtoHSB(rgb[0],rgb[1],rgb[2],null);
				for(int i=0;i<3;++i){
					rgbStack.setVoxel(x,y,i,rgb[i]);
				}
			}
		}
		
		int rgbModes[]=new int[3];
		for(int i=0;i<3;++i){
			rgbModes[i]=rgbStack.getProcessor(i+1).getStatistics().mode;
		}
		
		double[] labRef=ColorSpaceConverter.RGBToCieLab(rgbModes);
		ImageProcessor distIp=new ByteProcessor(origWidth,origHeight);
		for(int y=0;y<origHeight;++y){
			for(int x=0;x<origWidth;++x){
				double dist2=0;
				double[] lab=ColorSpaceConverter.RGBToCieLab(new int[]{(int)rgbStack.getVoxel(x,y,0),(int)rgbStack.getVoxel(x,y,1),(int)rgbStack.getVoxel(x,y,2)});
				int dist=(int)ColorSpaceConverter.distanceCie76(lab, labRef);
				if(dist>255)
					dist=255;
				
				distIp.set(x,y,dist);
					
			}
		}
		new ImagePlus("rgb distance",distIp).show();
		
	}
	
	public ImageProcessor calcRgbDistance(){
		ImageProcessor workIp=origIp.duplicate();

		ImageStack rgbStack=new ImageStack(origWidth,origHeight);
		for(int i=0;i<3;++i)
			rgbStack.addSlice(new ByteProcessor(origWidth,origHeight));
		for(int y=0;y<origHeight;++y){
			for(int x=0;x<origWidth;++x){
				int val=workIp.get(x,y);
				int[] rgb={(val>>16)&0xff,(val>>8)&0xff,val&0xff};
//				float[] hsb=Color.RGBtoHSB(rgb[0],rgb[1],rgb[2],null);
				for(int i=0;i<3;++i){
					rgbStack.setVoxel(x,y,i,rgb[i]);
				}
			}
		}
		
		int rgbModes[]=new int[3];
		for(int i=0;i<3;++i){
			rgbModes[i]=ImageStatistics.getStatistics(rgbStack.getProcessor(i+1),ImageStatistics.MODE,null).mode;
		}
		
		ImageProcessor distIp=new ByteProcessor(origWidth,origHeight);
		for(int y=0;y<origHeight;++y){
			for(int x=0;x<origWidth;++x){
				int maxDist=0;
				for(int i=0;i<3;++i){
					double dist=rgbStack.getVoxel(x,y,i)-rgbModes[i];//(rgbModes[i]-rgbStack.getVoxel(x,y,i))*(rgbModes[i]-rgbStack.getVoxel(x,y,i));
					if(dist>maxDist){
						maxDist=(int)dist;
					}
				}

				distIp.set(x,y,maxDist);
					
			}
		}
		new ImagePlus("rgb distance",distIp).show();
		new WaitForUserDialog("rgb dist").show();
		return distIp;
		
	}
	
	public void detectSatEdges(){
		ImageProcessor workIp=new ByteProcessor(origWidth,origHeight);
		
		for(int y=0;y<workIp.getHeight();++y){
			for(int x=0;x<workIp.getWidth();++x){
				int val=origIp.get(x,y);
				int[] rgb={(val>>16)&0xff,(val>>8)&0xff,val&0xff};
				float[] hsb=Color.RGBtoHSB(rgb[0],rgb[1],rgb[2],null);
				workIp.set(x,y,(int)(hsb[1]*255));
			}
		}
		EdgeFilter ef=new EdgeFilter();
		ef.convolve(workIp);
		ef.nonMaxSup();
		new ImagePlus("gradient mag",ef.getGradientMagnitudeProcessor().convertToByte(true)).show();
	}
	
	public void detectEdges(){
		ImageProcessor workIp=origIp.duplicate();
		new ContrastEnhancer().equalize(workIp);
		workIp=workIp.convertToByte(true);
		EdgeFilter ef=new EdgeFilter();
		ef.convolve(workIp);
		ef.nonMaxSup();
		new ImagePlus("gradient mag",ef.getGradientMagnitudeProcessor().convertToByte(true)).show();
		ImageProcessor gradDir=ef.getDirectionProcessor();
		for(int i=0;i<gradDir.getPixelCount();++i){
			gradDir.set(i,gradDir.get(i)*30);
		}
		new ImagePlus("gradient dir",gradDir).show();
	}
	
	public void convertToBaseColors(){
		int w=origIp.getWidth();
		int h=origIp.getHeight();
		
//		new ContrastEnhancer().equalize(origIp);
		ImageProcessor dstIp=new ColorProcessor(w,h);
		for(int i=0;i<origIp.getPixelCount();++i){
			Color dstColor=null;
			int pixVal=origIp.get(i);
			int[] rgb={(pixVal>>16)&0xff,(pixVal>>8)&0xff,pixVal&0xff};
			float[] hsb=Color.RGBtoHSB(rgb[0],rgb[1],rgb[2],null);
			
			if(hsb[1]<0.2){
				if(hsb[2]<0.1){
					dstColor=Color.BLACK;
				}
				else if(hsb[2]>0.9){
					dstColor=Color.WHITE;
				}
				else{
					dstColor=Color.GRAY;
				}
			}
			else{
				int baseHue=(int)(hsb[0]*6);
				switch(baseHue){
				case 0:
				case 6: dstColor=Color.RED; break;
				case 1: dstColor=Color.YELLOW; break;
				case 2: dstColor=Color.GREEN; break;
				case 3: dstColor=Color.CYAN; break;
				case 4: dstColor=Color.BLUE; break;
				case 5: dstColor=Color.MAGENTA; break;
				}
			}
			dstIp.set(i,dstColor.getRGB());
		}
		
		new ImagePlus("base colors",dstIp).show();
		new WaitForUserDialog("base colors").show();
	}
}

class TreePoint extends Point{
	public int level;

	public TreePoint(int x,int y,int level){
		super(x,y);
		this.level=level;
	}
}

class TreeSegment{
	List<Point> pts;
	int xMin;
	int xMax;
	int yMin;
	int yMax;
	
	public TreeSegment(){
		this.pts=new ArrayList<Point>();
		xMin=Integer.MAX_VALUE;
		xMax=Integer.MIN_VALUE;
		yMin=Integer.MAX_VALUE;
		yMax=Integer.MIN_VALUE;
	}
	
	public void add(Point pt){
		pts.add(pt);
		if(pt.x<xMin)
			xMin=pt.x;
		if(pt.x>xMax)
			xMax=pt.x;
		if(pt.y<yMin)
			yMin=pt.y;
		if(pt.y>yMax)
			yMax=pt.y;
	}
	
	public int size(){
		return pts.size();
	}
	
	public Point get(int i){
		return pts.get(i);
	}
	
	public List<Point> getAll(){
		return pts;
	}
}
