package at.ac.oeaw.gmi.brat.segmentation.plants;

import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonGraph;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.graph.SkeletonNode;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

public class Plant {
	String plantID;
	
	Roi seedRoi;
	Point2D seedCenter;
	
//	List<Roi> shootRoi;
//	List<Roi> plantRoi;
	Map<Integer,Phenotype> phenotype;
	Point rootTrackPt;
	Point shootCoM;
	
	public Plant(int plantNr){
		this.plantID=String.format("%02d",plantNr+1);
		this.phenotype=new TreeMap<Integer,Phenotype>();
//		this.shootRoi=new ArrayList<Roi>();
//		this.plantRoi=new ArrayList<Roi>();
	}
	
	public void setSeedRoi(Roi seedRoi){
		this.seedRoi=seedRoi;
	}
	public Point2D getSeedCenter() {
		return seedCenter;
	}
	public void setSeedCenter(Point2D seedCenter) {
		this.seedCenter = seedCenter;
	}

	
	public String getPlantID(){
		return plantID;
	}
	
	public Roi getShootRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.shootRoi;
		}
		return null;
	}
	public void setShootRoi(Integer time,Roi shootRoi) {
		if(!phenotype.containsKey(time))
			phenotype.put(time,new Phenotype());
		phenotype.get(time).topology.shootRoi=shootRoi;
	}

	public void setShootCoM(Point shootCoM){
		this.shootCoM=shootCoM;
	}
	public Point getShootCoM(){
		return shootCoM;
	}
	
	public Point getRootTrackPt() {
		return rootTrackPt;
	}
	public void setRootTrackPt(Point rootTrackPt) {
		this.rootTrackPt = rootTrackPt;
	}

	
	public Roi getRootRoi(int time) {
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootRoi;
		}
		return null;
	}
	public void setRootRoi(Integer time,Roi rootRoi) {
		if(phenotype.containsKey(time)){
			phenotype.get(time).topology.rootRoi=rootRoi;
		}
	}
	
	public void createTopology(Integer time,SkeletonNode stNodeGuess){
		if(phenotype.containsKey(time)){
			phenotype.get(time).topology.determineTopology(stNodeGuess);
		}
	}
	public void createTopologies(SkeletonNode stNodeGuess){
		for(Integer time:phenotype.keySet()){
			phenotype.get(time).topology.determineTopology(stNodeGuess);
		}
	}
	
	public List<SkeletonNode> getRootMainPath(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootMainPath; 
		}
		return null;
	}
	
	public List<Point> getRootMainPathPoints(int time){
		List<Point> pathPts=null;
		if(phenotype.containsKey(time)){
			List<SkeletonNode> mainPath=phenotype.get(time).topology.rootMainPath;
			if(mainPath!=null){
				pathPts=new ArrayList<Point>();
				for(SkeletonNode node:mainPath){
					pathPts.add(new Point(node.getX(),node.getY()));
				}
			}
		}
		return pathPts;
	}
	
	public SkeletonNode getStartNode(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootStartNode; 
		}
		return null;
	}
	
	public SkeletonNode getEndNode(int time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).topology.rootEndNode; 
		}
		return null;
		
	}
	public void calcTraits(Integer time){
		if(phenotype.containsKey(time)){
			phenotype.get(time).calcTraits();
		}
	}
	
//	public Topology getTopology(int time){
//		if(phenotype.containsKey(time)){
//			return phenotype.get(time).topology; 
//		}
//		return null;
//	}
	
	public List<Object> getTraitsAsList(Integer time){
		if(phenotype.containsKey(time)){
			List<Object> tl=new ArrayList<Object>();
			tl.add(phenotype.get(time).traits.rootEuclidianLength); 
			tl.add(phenotype.get(time).traits.rootTotalLength);
			tl.add(phenotype.get(time).traits.rootDirectionalEquivalent);
			for(int i=0;i<phenotype.get(time).traits.rootStdDevs.length;++i){
				tl.add(phenotype.get(time).traits.rootStdDevs[i]);
			}
			for(int i=0;i<phenotype.get(time).traits.rootAverageWidths.length;++i){
				tl.add(phenotype.get(time).traits.rootAverageWidths[i]);
			}
			tl.add(phenotype.get(time).traits.shootArea);
			tl.add(phenotype.get(time).traits.rootAngle);
			tl.add("unused");
			tl.add("unused");
			tl.add(phenotype.get(time).traits.rootGravitropicScore);

			return tl;
		}
		return null;
	}
	
	public Double getRootEuclidianLength(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootEuclidianLength;
		}
		return null;
	}
	public Double getRootTotalLength(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootTotalLength;
		}
		return null;
	}
	public Double getRootDirectionalEquivalent(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootDirectionalEquivalent;
		}
		return null;
	}
	public Double[] getRootStdDevs(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootStdDevs;
		}
		return null;
	}
	public Double getRootAngle(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootAngle;
		}
		return null;
	}
	public Double getRootGravitropicScore(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootGravitropicScore;
		}
		return null;
	}
	public Double[] getRootAverageWidths(Integer time){
		if(phenotype.containsKey(time)){
			return phenotype.get(time).traits.rootAverageWidths;
		}
		return null;
	}
	
	public Collection<SkeletonNode> getRootSkeleton(Integer time){
		return phenotype.get(time).topology.graph.getNodes();
	}
}

class Phenotype{
	Topology topology;
	Traits traits;
	int[] gravityVec={0,1};
	
	protected Phenotype(){
		this.topology=new Topology();
		this.traits=new Traits();
	}

	protected void calcTraits(){
		calcRootLengths();
		calcRootWidths();
		calcRootAngle();
		calcRootStdDevs();
		calcRootDirectionalEquivalent();
		calcShootArea();
	}
	
	
	private void calcRootLengths(){
		List<SkeletonNode> mainPath=topology.rootMainPath;
		if(mainPath!=null){
			SkeletonNode startNode=mainPath.get(0);
			SkeletonNode endNode=mainPath.get(mainPath.size()-1);
			
			traits.rootEuclidianLength=startNode.distance(endNode);
			traits.rootTotalLength=topology.graph.getPathLength(startNode,endNode);
		}
	}
	
	private void calcRootWidths(){
		List<SkeletonNode>mainPath=topology.rootMainPath;
		if(mainPath!=null){
			double[] avgWidth = new double[traits.percentiles.length+1];
			int[] pixCnts = new int[avgWidth.length];
			int numPix=mainPath.size();
			int[] percPix=new int[traits.percentiles.length];

			for(int i=0;i<traits.percentiles.length;++i){
				percPix[i]=(int)Math.round(numPix/100.0*traits.percentiles[i]);
			}

			int avgId=0;
			for(int pixId=0;pixId<numPix;++pixId){
				avgWidth[0]+=2.0*mainPath.get(pixId).getDMapValue();
				pixCnts[0]++;
				for(int i=0;i<percPix.length;++i)
					if(pixId==percPix[i]){
						avgId++;
					}
				if(avgId>0){
					avgWidth[avgId]+=2.0*mainPath.get(pixId).getDMapValue();
					pixCnts[avgId]++;
				}			
			}
			for(int i=0;i<avgWidth.length;++i){
				avgWidth[i]/=pixCnts[i];
			}
			for(int i=0;i<avgWidth.length;++i){
				traits.rootAverageWidths[i]=(Double)avgWidth[i];
			}
		}
	}

	private void calcRootDirectionalEquivalent(){
		List<SkeletonNode> mainRoot=topology.rootMainPath;
		if(mainRoot==null) return;
		int sumVecEqui=0;
		for(int pixNr=0;pixNr<mainRoot.size()-1;++pixNr){
			SkeletonNode pix1=mainRoot.get(pixNr);
			SkeletonNode pix2=mainRoot.get(pixNr+1);

			
			int[] pixVec={pix2.getX()-pix1.getX(),pix2.getY()-pix1.getY()};//pix1.vectorTo(pix2);
			int[] deltaVec=new int[2];
			deltaVec[0]=pixVec[0]-gravityVec[0];
			deltaVec[1]=pixVec[1]-gravityVec[1];
			if(Math.abs(deltaVec[0])>1 || Math.abs(deltaVec[1])>1){
				continue;
			}		

			int vecEquiv=Math.abs(deltaVec[0])+Math.abs(deltaVec[1]);
			if(vecEquiv==2 && deltaVec[1]==-2)
				vecEquiv=4;

			sumVecEqui+=vecEquiv;
		}

		traits.rootDirectionalEquivalent=new Double((double)sumVecEqui/(double)(mainRoot.size()));
	}

	private void calcRootStdDevs(){
		List<SkeletonNode> mainRoot=topology.rootMainPath;
		if(mainRoot==null) return;
		double[] mean={0.0,0.0};
		int n=mainRoot.size();
		for(int pixNr=0;pixNr<n;++pixNr){
			mean[0]+=(double)mainRoot.get(pixNr).getX();
			mean[1]+=(double)mainRoot.get(pixNr).getY();
		}
		mean[0]/=(double)n;
		mean[1]/=(double)n;

		double s2[]={0.0,0.0,0.0};
		for(int pixNr=0;pixNr<n;++pixNr)
		{
			double tx,ty;
			tx=(double)mainRoot.get(pixNr).getX()-mean[0];
			ty=(double)mainRoot.get(pixNr).getY()-mean[1];	
			s2[0]+=tx*tx;
			s2[1]+=ty*ty;
			s2[2]+=tx*ty;
		}
		s2[0]/=n; //Sxx
		s2[1]/=n; //Syy
		s2[2]/=n; //Sxy


		double[] s={0.0,0.0,0.0};
		s[0]=Math.sqrt(s2[0]); //Sx (stdDev_x)
		s[1]=Math.sqrt(s2[1]); //Sy

		double rxy=s2[2]/(s[0]*s[1]);

		s[2]=rxy*rxy;//syy/s2[1]; //R^2 value
		for(int i=0;i<3;++i)
			traits.rootStdDevs[i]=s[i];
	}

	private void calcRootAngle(){
		
		if(topology.rootStartNode==null || topology.rootEndNode==null) return;

		double dx=(double)(topology.rootEndNode.getX()-topology.rootStartNode.getX());
		double dy=(double)(topology.rootEndNode.getY()-topology.rootStartNode.getY());

		double phi=Math.atan2(dy,dx);
		//gravitational direction is 0 deg

		double grav_angle=Math.atan2(gravityVec[1],gravityVec[0]);
		double phiDeg=Math.toDegrees(grav_angle)-Math.toDegrees(phi);

		traits.rootAngle=new Double(phiDeg);

		double gravitropicScore;
		if(Math.abs(phiDeg)<15)
			gravitropicScore=0;
		else if(Math.abs(phiDeg)<45)
			gravitropicScore=0.5;
		else
			gravitropicScore=1.0;

		traits.rootGravitropicScore=new Double(gravitropicScore);					
	}

	private void calcShootArea(){
		if(topology.shootRoi==null) return;
		ImageProcessor roiMask=topology.shootRoi.getMask();
		Roi tmpRoi=(Roi)topology.shootRoi.clone();
		tmpRoi.setLocation(0,0);
		roiMask.setRoi(tmpRoi);
		traits.shootArea=roiMask.getStatistics().area;
	}
}

class Topology{
	Roi shootRoi;
	Roi rootRoi;

	Rectangle combinedBounds;
	
	SkeletonNode rootStartNode;
	SkeletonNode rootEndNode;
	List<SkeletonNode> rootMainPath;
	
	SkeletonGraph graph;
	
	protected void determineTopology(SkeletonNode stNodeGuess){
		createGraph();
		detectRootStartPoint(stNodeGuess);
		detectRootMainPath();
		setRootEndPoint();
	}
	
	private void createGraph(){
		ShapeRoi sRoiShoot=new ShapeRoi(shootRoi);
		ShapeRoi sRoiRoot=new ShapeRoi(rootRoi);
		ShapeRoi combinedRoi=sRoiRoot.or(sRoiShoot);
		graph=new SkeletonGraph();
		graph.create(combinedRoi);
	}
	
	private void detectRootStartPoint(SkeletonNode stNodeGuess){
		ShapeRoi sRoiShoot=new ShapeRoi(shootRoi);
		ShapeRoi sRoiRoot=new ShapeRoi(rootRoi);
		ShapeRoi combinedRoi=sRoiRoot.and(sRoiShoot);
		if(stNodeGuess==null){
			System.err.println("start point guess: null");
			if(combinedRoi.getBounds().width!=0 && combinedRoi.getBounds().height!=0){
				ImageProcessor combinedMask=combinedRoi.getMask();
				//		new ImagePlus("combined roi",combinedMask).show();
				double stX=0;
				double stY=0;
				int stCnt=0;
				for(int y=0;y<combinedMask.getHeight();++y){
					for(int x=0;x<combinedMask.getWidth();++x){
						if(combinedMask.get(x,y)>0){
							stX+=x;
							stY+=y;
							stCnt++;
						}
					}
				}
				stX/=stCnt;
				stY/=stCnt;

				stX+=combinedRoi.getBounds().x;
				stY+=combinedRoi.getBounds().y;
				double minDist=Double.MAX_VALUE;
				for(SkeletonNode node:graph.getNodes()){
					double dist=node.distanceSq(stX,stY);
					if(dist<minDist){
						minDist=dist;
						rootStartNode=node;
					}
				}
			} //if
			else{
				Rectangle shootBounds=shootRoi.getBounds();
				ImageProcessor shootMask=shootRoi.getMask();
				List<SkeletonNode> shootNodes=new ArrayList<SkeletonNode>();
				List<SkeletonNode> rootNodes=new ArrayList<SkeletonNode>();
				for(SkeletonNode node:graph.getNodes()){
					int x=node.getX()-shootBounds.x;
					int y=node.getY()-shootBounds.y;
					if(x>0 && x<shootBounds.width && y>0 && y<shootBounds.height){
						if(shootMask.get(x,y)>0){
							shootNodes.add(node);
							continue;
						}
					}
					rootNodes.add(node);
				}

				double minDist=Double.MAX_VALUE;
				for(SkeletonNode rNode:rootNodes){
					for(SkeletonNode sNode:shootNodes){
						double dist=rNode.distanceSq(sNode);
						if(dist<minDist){
							minDist=dist;
							rootStartNode=rNode;
						}
					}
				}

			}
		} // if(stNodeGuess==null)
		else{
			double stX=stNodeGuess.getX();
			double stY=stNodeGuess.getY();
			System.err.println("start point guess: "+stX+","+stY);
			double minDist=Double.MAX_VALUE;
			for(SkeletonNode node:graph.getNodes()){
				double dist=node.distanceSq(stX,stY);
				if(dist<minDist){
					minDist=dist;
					rootStartNode=node;
				}
			}
			
		}
	}
	
	private void detectRootMainPath(){
		rootMainPath=graph.getLongestPathFromNode(rootStartNode);
	}
	
	private void setRootEndPoint(){
		if(rootMainPath!=null){
			if(rootMainPath.size()>0){
				rootEndNode=rootMainPath.get(rootMainPath.size()-1);
			}
		}
	}
	
}

class Traits{
	final double[] percentiles={0.0,20.0,40.0,60.0,80.0};
	Double rootEuclidianLength;
	Double rootTotalLength;
	Double rootDirectionalEquivalent;
	Double[] rootStdDevs=new Double[3];
	Double rootAngle;
	Double rootGravitropicScore;
	Double[] rootAverageWidths=new Double[percentiles.length+1];
;
	Double shootArea;
//	Double endPtScore;
//	Double[] avgShootColor;
//	Double[] medianShootColor;
	
	
}