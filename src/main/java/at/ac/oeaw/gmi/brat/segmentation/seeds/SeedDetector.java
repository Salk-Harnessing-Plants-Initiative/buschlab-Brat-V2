package at.ac.oeaw.gmi.brat.segmentation.seeds;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

import at.ac.oeaw.gmi.brat.math.HistogramCorrelation;
import at.ac.oeaw.gmi.brat.math.KMeans1d;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.ColorSpaceConverter;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.ColorSrm;
import at.ac.oeaw.gmi.brat.segmentation.parameters.Parameters;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.WaitForUserDialog;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.EllipseFitter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

public class SeedDetector {
	ImageProcessor ip;
	SeedingLayout seedLayout;
	List<List<Roi>> assignedRois;
	
	public SeedDetector(ImageProcessor srcIp){
		this.ip=srcIp;
		seedLayout=new SeedingLayout();
		seedLayout.setMarcoLayout(); //TODO alexandro seed layout
	}
	
	public List<List<Roi>> getAssignedRois(){
		return assignedRois;
	}
	public List<List<Point2D>> getAssignedRoiCenters(){
		List<List<Point2D>> roiCenters=new ArrayList<List<Point2D>>();
		for(List<Roi> rowRois:assignedRois){
			List<Point2D> rowCenters=new ArrayList<Point2D>();
			for(Roi roi:rowRois){
				if(roi==null){
					rowCenters.add(null);
				}
				else{
					ip.setRoi(roi);
					ImageStatistics roiStats=ImageStatistics.getStatistics(ip,ImageStatistics.CENTROID,null);
					rowCenters.add(new Point2D.Double(roiStats.xCentroid,roiStats.yCentroid));
				}
			}
			roiCenters.add(rowCenters);
		}
		return roiCenters;
	}
	
	public ImageProcessor blueRemoval(){
		for(int i=0;i<ip.getPixelCount();++i){
			int pixVal=ip.get(i);
			int[] rgb={(pixVal>>16)&0xff,(pixVal>>8)&0xff,pixVal&0xff};
			if(rgb[0] < (rgb[1]+rgb[2])/2){
				ip.set(i,0);
			}
		}
		return ip;
	}
	
	public List<List<Double>> calcCorrelationMatrix(List<Roi> rois){
		List<int[]> roiHistos=new ArrayList<int[]>();
		for(Roi roi:rois){
			ip.setRoi(roi);
			roiHistos.add(ip.getHistogram());
		}
		
		HistogramCorrelation hCorr=new HistogramCorrelation();
		List<List<Double>> correlations=new ArrayList<List<Double>>();
		for(int i=0;i<roiHistos.size();++i){
			correlations.add(new ArrayList<Double>());
			for(int j=0;j<roiHistos.size();++j){
				double corr=hCorr.intersection(roiHistos.get(i),roiHistos.get(j));
				correlations.get(i).add(corr);
			}
		}
		
		return correlations;
	}
	
	public void identifySeeds(double seedMinAxis,double seedMaxAxis){
		blueRemoval();
		
		ImageProcessor tmpIp=ip.convertToByte(false);
		tmpIp.setThreshold(1,255,ImageProcessor.NO_LUT_UPDATE);
		ThresholdToSelection ts=new ThresholdToSelection();
		Roi combinedRoi=ts.convert(tmpIp);
//		new ImagePlus("combined roi mask",combinedRoi.getMask()).show();
		ShapeRoi sRoi=new ShapeRoi(combinedRoi);
		List<Roi> separatedRois=new ArrayList<Roi>();
		List<Point2D> roiCenters=new ArrayList<Point2D>();
//		ContrastEnhancer ce=new ContrastEnhancer();
//		IJ.log("roi size: "+sRoi.getRois().length);
		for(Roi roi:sRoi.getRois()){
			Rectangle rr=roi.getBounds();
			if(rr.width<seedMinAxis/Parameters.mmPerPixel || rr.height<seedMinAxis/Parameters.mmPerPixel ||
					rr.width>5*seedMaxAxis/Parameters.mmPerPixel || rr.height>5*seedMaxAxis/Parameters.mmPerPixel){
				continue;
			}
			tmpIp.setRoi(roi);
			if(tmpIp.getStatistics().mean==0){
				continue;
			}

			Point2D roiCenter=new Point2D.Double(rr.x+rr.width/2.0,rr.y+rr.height/2.0);

			roiCenters.add(roiCenter);
			separatedRois.add(roi);
		}
		IJ.log("separatedRois size: "+separatedRois.size());
		
		List<List<Point2D>> seedPts=seedLayout.getPlateCenteredPositions(tmpIp.getWidth(),tmpIp.getHeight());
		List<Double> seedRowY=seedLayout.getRowYPos();
		List<List<Integer>> assignedRoiIdx=new ArrayList<List<Integer>>();
		for(int i=0;i<seedPts.size();++i){
			assignedRoiIdx.add(new ArrayList<Integer>(seedPts.get(i).size()));
			for(int j=0;j<seedPts.get(i).size();++j){
				assignedRoiIdx.get(i).add(-1);
			}
		}
		
		int maxIterations=10;
		int changed=0;
		int iterations=0;
		while(changed!=-1 && iterations<=maxIterations){
			changed=-1;
			for(int row=0;row<seedPts.size();++row){
				for(int col=0;col<seedPts.get(row).size();++col){
					Point2D pt=seedPts.get(row).get(col);
					double minDist=Double.MAX_VALUE;
					int nearestRoiIdx=-1;
					for(int roiIdx=0;roiIdx<roiCenters.size();++roiIdx){
						Point2D roiCenter=roiCenters.get(roiIdx);
						double distance=pt.distance(roiCenter);
						if(distance<minDist){
							minDist=distance;
							nearestRoiIdx=roiIdx;
						}
					}
					if(assignedRoiIdx.get(row).get(col)!=nearestRoiIdx){
						assignedRoiIdx.get(row).set(col,nearestRoiIdx);
						changed=row;
					}
				}
				// calculate row median y
				if(changed==row){
					List<Double> rowXVals=new ArrayList<Double>();
					List<Double> rowYVals=new ArrayList<Double>();
					for(int col=0;col<assignedRoiIdx.get(row).size();++col){
						int idx=assignedRoiIdx.get(row).get(col);
						rowXVals.add(roiCenters.get(idx).getX()-seedPts.get(row).get(col).getX());
						rowYVals.add(roiCenters.get(idx).getY());
					}
					
					Collections.sort(rowXVals);
					Collections.sort(rowYVals);
					int half=rowYVals.size()/2;
					double rowXMedian=rowXVals.size()%2==0 ? (rowXVals.get(half)+rowXVals.get(half+1))/2.0 : rowXVals.get(half);
					double rowYMedian=rowYVals.size()%2==0 ? (rowYVals.get(half)+rowYVals.get(half+1))/2.0 : rowYVals.get(half);
					//double corrY=rowMedian-seedRowY.get(row);
					for(int col=0;col<seedPts.get(row).size();++col){
						Point2D pt=seedPts.get(row).get(col);
						seedPts.get(row).get(col).setLocation(pt.getX()+rowXMedian,rowYMedian);
					}
					seedRowY.set(row,rowYMedian);
				}
			}
			iterations++;
		}
		IJ.log("iterations: "+iterations);
		
		
		// get near rois for each seed pt
		List<List<Integer>> possibleRois=new ArrayList<List<Integer>>();
		for(int row=0;row<seedPts.size();++row){
			for(int col1=0;col1<seedPts.get(row).size();++col1){
				double curMinDist=Double.MAX_VALUE;
				Point2D curSeedPt=seedPts.get(row).get(col1);
				for(int col2=0;col2<seedPts.get(row).size();++col2){
					if(col1==col2)
						continue;
					double curDist=curSeedPt.distance(seedPts.get(row).get(col2));
					if(curDist<curMinDist){
						curMinDist=curDist;
					}
				}
				List<Integer> curPossibleRois=new ArrayList<Integer>();
				for(int roiIdx=0;roiIdx<roiCenters.size();++roiIdx){
					if(curSeedPt.distance(roiCenters.get(roiIdx))<=curMinDist/2.0){
						curPossibleRois.add(roiIdx);
					}
				}
				possibleRois.add(curPossibleRois);
			}
		}
		
		int seedPtIdx=0;
		List<Roi> correlateRois=new ArrayList<Roi>();
		for(List<Integer> pRois:possibleRois){
//			IJ.log("seed pt "+seedPtIdx+": "+pRois.size());
			seedPtIdx++;
			for(int roiIdx:pRois){
				correlateRois.add(separatedRois.get(roiIdx));
			}
		}
		List<List<Double>> correlations=calcCorrelationMatrix(correlateRois);
		
		//find roi with highest correlation to others
		seedPtIdx=0;
		int correlationIdx=0;
		for(int row=0;row<seedPts.size();++row){
			for(int col=0;col<seedPts.get(row).size();++col){
				if(possibleRois.get(seedPtIdx).size()<1){
					assignedRoiIdx.get(row).set(col,null);
				}
				else if(possibleRois.get(seedPtIdx).size()==1){
					assignedRoiIdx.get(row).set(col,possibleRois.get(seedPtIdx).get(0));
					correlationIdx++;
				}
				else{
					double maxSumCorr=-Double.MAX_VALUE;
					for(int i=0;i<possibleRois.get(seedPtIdx).size();++i){
						int roi1Idx=correlationIdx;
						double sumCorrelation=0;

						for(int roi2Idx=0;roi2Idx<correlateRois.size();++roi2Idx){
							if(roi2Idx==roi1Idx)
								continue;
							sumCorrelation+=correlations.get(roi1Idx).get(roi2Idx);
						}
						if(sumCorrelation>maxSumCorr){
							maxSumCorr=sumCorrelation;
							assignedRoiIdx.get(row).set(col,possibleRois.get(seedPtIdx).get(i));
						}
						correlationIdx++;
					}
//					correlationIdx+=possibleRois.get(seedPtIdx).size();
				}
				seedPtIdx++;
			}
		}
		
//		ip.setColor(Color.red);
//		Color nextDrawColor;
		assignedRois=new ArrayList<List<Roi>>();
		for(int i=0;i<assignedRoiIdx.size();++i){
			List<Roi> assignedRoisRow=new ArrayList<Roi>();
			for(int j=0;j<assignedRoiIdx.get(i).size();++j){
				Point2D seedPt=seedPts.get(i).get(j);
				if(assignedRoiIdx.get(i).get(j)!=null){
					assignedRoisRow.add(separatedRois.get(assignedRoiIdx.get(i).get(j)));
//					ip.drawRoi(separatedRois.get(assignedRoiIdx.get(i).get(j)));
//					nextDrawColor=Color.CYAN;
				}
				else{
					assignedRoisRow.add(null);
//					nextDrawColor=Color.RED;
				}
//				ip.setColor(nextDrawColor);
//				ip.fillOval((int)seedPt.getX()-5,(int)seedPt.getY()-5,10,10);
			}
			assignedRois.add(assignedRoisRow);
		}
		
		
//		drawSeedingLayout();
//		new ImagePlus("select rois",ip.duplicate()).show();
		
	}
	
	public void drawSeedingLayout(){
		List<List<Point2D>> seedPos=seedLayout.getPlateCenteredPositions(ip.getWidth(),ip.getHeight());
		ip.setColor(Color.blue);
		for(List<Point2D> rowPts:seedPos){
			for(Point2D pt:rowPts){
				ip.fillOval((int)pt.getX()-5,(int)pt.getY()-5,10,10);
			}
		}
		
		ip.setColor(Color.green);
		for(List<Point2D> assignedRowPts:getAssignedRoiCenters()){
			for(Point2D pt:assignedRowPts){
				if(pt!=null)
					ip.fillOval((int)pt.getX()-5,(int)pt.getY()-5,10,10);
			}
		}
		new ImagePlus("seed layout",ip).show();
	}
	
//	public void detectSeeds(double seedMinAxis,double seedMaxAxis){
//		ImageProcessor tmpIp=ip.convertToByte(false);
//		ImageProcessor seedsIp=new ColorProcessor(ip.getWidth(),ip.getHeight());
//		tmpIp.setThreshold(1,255,ImageProcessor.NO_LUT_UPDATE);
//		ThresholdToSelection ts=new ThresholdToSelection();
//		ShapeRoi sRoi=new ShapeRoi(ts.convert(tmpIp));
//		Roi[] rois=sRoi.getRois();
//		ColorSrm srm=new ColorSrm();
//		ContrastEnhancer ce=new ContrastEnhancer();
//		for(Roi roi:rois){
//			ip.setRoi(roi);
//			
//			Rectangle rr=roi.getBounds();
//			if(roi.getBounds().width<seedMinAxis/Parameters.MM_PER_PIXEL || roi.getBounds().height<seedMinAxis/Parameters.MM_PER_PIXEL ||
//					roi.getBounds().width>3*seedMaxAxis/Parameters.MM_PER_PIXEL || roi.getBounds().height>3*seedMaxAxis/Parameters.MM_PER_PIXEL){
//				continue;
//			}
//			ImageProcessor roiMask=roi.getMask();
////			tmpIp.setRoi(roi);
//			ImageStatistics roiStats=ip.getStatistics();
//			
//			int rcX=(int)roiStats.xCentroid;
//			int rcY=(int)roiStats.yCentroid;
//			IJ.log("checking roi at: "+rcX+","+rcY);
//			if(ip.get(rcX,rcY)==0)
//				continue;
//			
//			rcX-=rr.x;
//			rcY-=rr.y;
//			
//			srm.setQ((float)roiStats.area);
//			tmpIp=ip.crop();
//			ce.equalize(tmpIp);
//			tmpIp=srm.srm2D(tmpIp,true).convertToByte(false);
//			
//			List<Point> seedPixels=new ArrayList<Point>();
//			Stack<Point> stack=new Stack<Point>();
//			stack.push(new Point(rcX,rcY));
//			seedPixels.add(new Point(rcX,rcY));
//			while(!stack.isEmpty()){
//				Point curPt=stack.pop();
//				roiMask.set(curPt.x,curPt.y,0);
//				
//				for(int y=curPt.y-1;y<=curPt.y+1;++y){
//					if(y<0 || y>=roiMask.getHeight()){
//						continue;
//					}
//					for(int x=curPt.x-1;x<=curPt.x+1;++x){
//						if(x<0 || x>=roiMask.getWidth()){
//							continue;
//						}
//						if(x==curPt.x && y==curPt.y){
//							continue;
//						}
//						int curVal=tmpIp.get(curPt.x,curPt.y);
//						int neighVal=tmpIp.get(x,y);
//						if(neighVal<=curVal && roiMask.get(x,y)!=0){
//							Point nPt=new Point(x,y);
//							roiMask.set(x,y,0);
//							seedPixels.add(nPt);
//							stack.push(nPt);
//						}
//					}
//				}
//			}
//			
//			for(Point seedPix:seedPixels){
//				int x=seedPix.x+rr.x;
//				int y=seedPix.y+rr.y;
//				seedsIp.set(x,y,ip.get(x,y));
//			}
//			seedsIp.drawRoi(roi);
////			ImageProcessor roiIp=tmpIp.crop();
////			MaximumFinder mf=new MaximumFinder();
////			
////			Polygon maxima=mf.getMaxima(roiIp,1,true);
////			for(int i=0;i<maxima.npoints;++i){
////				roiIp.drawRoi(new PointRoi(maxima.xpoints[i],maxima.ypoints[i]));
////			}
//
//		}
//		ImagePlus rImg=new ImagePlus("seed rois",seedsIp);
//		rImg.show();
//		
//	}
//	
//	public void segmentSeeds(double seedMinAxis,double seedMaxAxis){
//		ImageProcessor tmpIp=ip.convertToByte(false);
//		tmpIp.setThreshold(1,255,ImageProcessor.NO_LUT_UPDATE);
//		ThresholdToSelection ts=new ThresholdToSelection();
//		ShapeRoi roi2=new ShapeRoi(ts.convert(tmpIp));
//		Roi[] rois=roi2.getRois();
//		IJ.log("rois.length="+rois.length);
//		//dstIp.drawRoi(roi2);
////		int seedRoiCnt=0;
//		List<EllipseFitter> detectedSeeds=new ArrayList<EllipseFitter>();
//		for(Roi r:rois){
//			ip.setRoi(r);
//			if(r.getBounds().width<seedMinAxis/Parameters.MM_PER_PIXEL || r.getBounds().height<seedMinAxis/Parameters.MM_PER_PIXEL || r.getBounds().width>3*seedMaxAxis/Parameters.MM_PER_PIXEL || r.getBounds().height>3*seedMaxAxis/Parameters.MM_PER_PIXEL)
//				continue;
//			double area=ip.getStatistics().area;
//			if(r.getBounds().width>2 && r.getBounds().height>2 && r.getBounds().width<roi2.getBounds().width/6.0 &&  r.getBounds().height<roi2.getBounds().height/6.0){ 
//				ImageProcessor rIp=ip.crop();
//				ContrastEnhancer ce2=new ContrastEnhancer();
//				ce2.equalize(rIp);
//				
//				ColorSrm srm=new ColorSrm();
//				srm.setQ((float)(area));
//				rIp=srm.srm2D(rIp, true);
//				
//				ImagePlus rImg=new ImagePlus("seed roi",rIp);
//				rImg.show();
//				new WaitForUserDialog("seed roi").show();
//				rImg.close();
//				
//				ImageStack cielabStack=ColorSpaceConverter.RGBToLabStack(rIp);
//				ImageStack cielchStack=ColorSpaceConverter.RGBToLChStack(rIp);
////				IJ.log("cielab conversion done.");
//				double maxb=-Double.MAX_VALUE;
//				double maxC=-Double.MAX_VALUE;
//				for(int y=0;y<rIp.getHeight();++y){
//					for(int x=0;x<rIp.getWidth();++x){
//						double b=cielabStack.getVoxel(x,y,2);
//						double c=cielchStack.getVoxel(x,y,1);
////						if(b/c>0.98){
////							rIp.setColor(Color.RED);
////							rIp.drawPixel(x, y);
////						}
//						if(b>maxb){
//							maxb=b;
//						}
//						if(c>maxC){
//							maxC=c;
//						}
//					}
//				}
//				if(maxb<=0){
//					continue;
//				}
//				ImageProcessor maskIp=new ByteProcessor(rIp.getWidth(),rIp.getHeight());
//				int maskPixelCnt=0;
//				for(int y=0;y<rIp.getHeight();++y){
//					for(int x=0;x<rIp.getWidth();++x){
//						double b=cielabStack.getVoxel(x,y,2);
//						double c=cielchStack.getVoxel(x,y,1);
//						if(b/maxb>0.70 && c/maxC>0.70){
//							maskIp.set(x,y,255);
//							maskPixelCnt++;
//						}
//					}
//				}
//				if(maskPixelCnt==0){
//					continue;
//				}
//				
//				maskIp.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);
//				ts=new ThresholdToSelection();
//				Roi seedRoi=ts.convert(maskIp);
//				maskIp.setRoi(seedRoi);
//				EllipseFitter ef=new EllipseFitter();
//				ef.fit(maskIp,null);
//
//				if(ef.minor*Parameters.MM_PER_PIXEL<seedMinAxis || ef.major*Parameters.MM_PER_PIXEL>seedMaxAxis){
//					continue;
//				}
//				ef.xCenter+=r.getBounds().x;
//				ef.yCenter+=r.getBounds().y;
//				ip.setColor(Color.red);
//				ef.drawEllipse(ip);
//				detectedSeeds.add(ef);
//			}
//		}
//		
//		IJ.log("identified "+detectedSeeds.size()+" seeds.......................................");
//		
////		ImageProcessor edgeIp=dstIp.duplicate().convertToByte(true);
////		edgeIp.findEdges();
////		new ImagePlus("edges",edgeIp).show();
////		
////		for(int i=0;i<np;++i){
////			if(edgeIp.get(i)!=0){
////				int pixVal=dstIp.get(i);
////				int[] pixRGB={(pixVal&0xff0000)>>16,(pixVal&0xff00)>>8,(pixVal&0xff)};
////				float[] pixHSB=Color.RGBtoHSB(pixRGB[0],pixRGB[1],pixRGB[2],new float[3]);
////
////				if((pixHSB[0]<0.04 && pixHSB[0]>0.2) || pixHSB[1]<0.2){
////					dstIp.set(i,0);
////				}
////			}
////		}
//		
//		SeedingLayout sl=new SeedingLayout();
//		List<List<Point2D>> seedPos=sl.getSeedPositions();
//		ip.setColor(Color.blue);
//		for(List<Point2D> rowPts:seedPos){
//			for(Point2D pt:rowPts){
//				ip.fillOval((int)pt.getX()-5,(int)pt.getY()-5,10,10);
//			}
//		}
//		new ImagePlus("segmented seeds",ip).show();
//		
//		
//	}
//
}
