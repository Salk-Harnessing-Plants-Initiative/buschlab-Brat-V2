package at.ac.oeaw.gmi.brat.segmentation.seeds;

import at.ac.oeaw.gmi.brat.math.HistogramCorrelation;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class SeedDetector {
	private final static Logger log=Logger.getLogger(SeedDetector.class.getName());
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private final double mmPerPixel = 25.4/prefs_expert.getDouble("resolution",1200);
	private ImageProcessor ip;
	private SeedingLayout seedLayout;
	private List<List<Roi>> assignedRois;
	
	public SeedDetector(ImageProcessor srcIp){
		this.ip=srcIp;
		seedLayout=new SeedingLayout();
//		seedLayout.setAlexandroLayout(); //TODO alexandro seed layout
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
		ShapeRoi sRoi=new ShapeRoi(combinedRoi);
		List<Roi> separatedRois=new ArrayList<Roi>();
		List<Point2D> roiCenters=new ArrayList<Point2D>();
		for(Roi roi:sRoi.getRois()){
			Rectangle rr=roi.getBounds();
			if(rr.width<seedMinAxis/mmPerPixel || rr.height<seedMinAxis/mmPerPixel ||
					rr.width>5*seedMaxAxis/mmPerPixel || rr.height>5*seedMaxAxis/mmPerPixel){
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
		log.finer("separatedRois size: "+separatedRois.size());
		
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
		log.finer("iterations: "+iterations);
		
		
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
				}
				seedPtIdx++;
			}
		}
		
		assignedRois=new ArrayList<List<Roi>>();
		for(int i=0;i<assignedRoiIdx.size();++i){
			List<Roi> assignedRoisRow=new ArrayList<Roi>();
			for(int j=0;j<assignedRoiIdx.get(i).size();++j){
				Point2D seedPt=seedPts.get(i).get(j);
				if(assignedRoiIdx.get(i).get(j)!=null){
					assignedRoisRow.add(separatedRois.get(assignedRoiIdx.get(i).get(j)));
				}
				else{
					assignedRoisRow.add(null);
				}
			}
			assignedRois.add(assignedRoisRow);
		}
	}
	
	public void drawSeedingLayout(){
		List<List<Point2D>> seedPos=seedLayout.getPlateCenteredPositions(ip.getWidth(),ip.getHeight());
		ip.setColor(Color.blue);
		for(List<Point2D> rowPts:seedPos){
			for(Point2D pt:rowPts){
				ip.fillOval((int)pt.getX()-5,(int)pt.getY()-5,10,10);
			}
		}
		new ImagePlus("seed layout",ip).show();
	}
}
