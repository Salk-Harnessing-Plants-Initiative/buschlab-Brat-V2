package at.ac.oeaw.gmi.brat.segmentation.plate;

import at.ac.oeaw.gmi.brat.segmentation.algorithm.ConvexHull;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.EdgeFilter;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.LinearHT;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.LinearHT.HoughLine;
import ij.IJ;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.plugin.ContrastEnhancer;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class PlateDetector {
	private final static Logger log=Logger.getLogger(PlateDetector.class.getName());
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private final double mmPerPixel = 25.4/prefs_expert.getInt("resolution",1200);
	private double scaleFactor;
	private Shape plateShape;
	private ImageProcessor origIp;
	private int width;  //orig width
	private int height; //orig height
	
	private List<HoughLine> houghlines;
	private Point2D htCenter;
	private Map<Double,List<Integer>> parallelLines;
	
	private double rotAngle;
	private Polygon convexOutline;
	private Point2D referencePt;
	

	public PlateDetector(ImageProcessor origIp,double scaleFactor,Shape plateShape){
		this.scaleFactor=scaleFactor;
		this.plateShape=plateShape;
		this.origIp=origIp;
		this.width=origIp.getWidth();
		this.height=origIp.getHeight();
	}
	
	public double getRotation(){
		return rotAngle;
	}
	
	public Point2D getReferencePt(){
		return referencePt;
	}
	
	public Shape getPlateShape(){
		return plateShape;
	}
	
	public double getScalefactor(){
		return scaleFactor;
	}
	
	
	public void detect(){
		ImageProcessor tmpIp=preProcess();
		getHoughLines(tmpIp);
		searchBestRoiFit(tmpIp);
	}
	
	public void detectInsideArea(){
		int maxColorRise=3;
		ImageProcessor binaryIp=new ByteProcessor(width,height);
		ImageProcessor workIp=origIp; //ef.getGradientMagnitudeProcessor().convertToByte(true);

		byte[][] pixelStatus=new byte[width][height];
//		int[][] outlineCols=new int[width][2];
//		for(int i=0;i<width;++i){
//			outlineCols[i]=new int[]{Integer.MAX_VALUE,Integer.MIN_VALUE};
//		}
//		int[][] outlineRows=new int[height][2];
//		for(int i=0;i<height;++i){
//			outlineRows[i]=new int[]{Integer.MAX_VALUE,Integer.MIN_VALUE};
//		}
		Stack<int[]> pStack=new Stack<int[]>();
		pStack.push(new int[]{width/2,height/2});
		pixelStatus[width/2][height/2]=1;
		while(!pStack.empty()){
			int[] curCoords=pStack.pop();
			binaryIp.set(curCoords[0],curCoords[1],255);

			int[] nCoords=new int[2];
			int nVal=0;
			int[] nRgbDiff;
			
			for(int i=0;i<4;++i){
				switch(i){
				case 0:	nCoords[0]=curCoords[0]-1;
						nCoords[1]=curCoords[1];
						break;
				case 1: nCoords[0]=curCoords[0]+1;
						nCoords[1]=curCoords[1];
						break;
				case 2: nCoords[0]=curCoords[0];
						nCoords[1]=curCoords[1]-1;
						break;
				case 3: nCoords[0]=curCoords[0];
						nCoords[1]=curCoords[1]+1;
						break;
				}
				
				if(nCoords[0]<0 || nCoords[0]>width-1 || nCoords[1]<0 || nCoords[1]>height-1)
					continue;
				
				int curVal=workIp.get(curCoords[0],curCoords[1]);

				if(pixelStatus[nCoords[0]][nCoords[1]]==0){
					nVal=workIp.get(nCoords[0],nCoords[1]);
					if(addPixel(curVal,nVal,maxColorRise)){
						pixelStatus[nCoords[0]][nCoords[1]]=1;
						pStack.push(nCoords.clone());
					}
					else{
						pixelStatus[nCoords[0]][nCoords[1]]=2;
					}
				}
			}
		}
		
//		new ImagePlus("detectInsideArea",binaryIp).show();
		int xWand=width/2;
//		int yWand=height/2;
		boolean done=false;
		int loopCnt=0;
		while (!done && loopCnt<3){
			Wand wand=new Wand(binaryIp);
			wand.autoOutline(width/2,height/2,0.0,Wand.FOUR_CONNECTED);

			ConvexHull ch=new ConvexHull(wand.xpoints,wand.ypoints,wand.npoints);
			convexOutline=ch.getConvexHullPolygon();

			//		double totalLength=0;

			Map<Double,List<Integer>> sortedAngles=new HashMap<Double,List<Integer>>();
			List<Double> lineAngles=new ArrayList<Double>();
			List<Double> lineLengths=new ArrayList<Double>();
			int lineIdx=0;
			for(int i=1;i<convexOutline.npoints;++i){
				Point2D pt1=new Point2D.Double(convexOutline.xpoints[i-1],convexOutline.ypoints[i-1]);
				Point2D pt2=new Point2D.Double(convexOutline.xpoints[i],convexOutline.ypoints[i]);

				double dist=pt1.distance(pt2);

				if(dist*mmPerPixel<10)
					continue;

				lineLengths.add(dist);
				double dx=pt2.getX()-pt1.getX();
				double dy=pt2.getY()-pt1.getY();
				double angle=Math.atan(dy/dx);

				if(Math.abs(angle)>Math.PI/4.0){
					if(angle>0){
						angle-=Math.PI/2.0;
					}
					else{
						angle+=Math.PI/2.0;
					}
				}
				lineAngles.add(angle);
				angle=roundToDec(angle,2);
				if(!sortedAngles.containsKey(angle)){
					sortedAngles.put(angle,new ArrayList<Integer>());
				}
				sortedAngles.get(angle).add(lineIdx);
				lineIdx++;
			}

			SortedMap<Double,Double> sortedAngles2=new TreeMap<Double,Double>();
			for(List<Integer> lineIndices:sortedAngles.values()){
				double totalLength=0;
				double meanAngle=0;
				for(int i:lineIndices){
					double length=lineLengths.get(i);
					totalLength+=length;
					meanAngle+=lineAngles.get(i)*length;
				}
				sortedAngles2.put(totalLength,meanAngle/totalLength);
			}

			if(sortedAngles2.size()!=0){
				rotAngle=sortedAngles2.get(sortedAngles2.lastKey());
				done=true;
			}
			xWand+=width/7;
			loopCnt++;
		}
		IJ.log("rotation="+rotAngle);
	}
	
	private boolean addPixel(int refVal,int pixVal,int maxColorRise){
		int[] refRgb={(refVal>>16)&0xff,(refVal>>8)&0xff,refVal&0xff};
		int[] pixRgb={(pixVal>>16)&0xff,(pixVal>>8)&0xff,pixVal&0xff};
		
		double dist2=0;
		for(int i=0;i<3;++i){
			dist2+=(refRgb[i]-pixRgb[i])*(refRgb[i]-pixRgb[i]);
		}
		
		return (dist2<3*maxColorRise);
		}
	
	private double getNormalAngle(double angle){
		double normalAngle=angle+Math.PI/2.0;
		if(normalAngle>Math.PI){
			normalAngle-=Math.PI;
		}
		return normalAngle;
	}
	
	private double roundToDec(double val,int nDecimals){
		double factor=Math.pow(10,nDecimals);
		return (double)Math.round(val*factor)/factor;
	}
	
	private ImageProcessor preProcess(){
		EdgeFilter edged=new EdgeFilter();
		ContrastEnhancer ce=new ContrastEnhancer();

		ImageProcessor tmpIp=origIp.resize((int)(origIp.getWidth()*scaleFactor),(int)(origIp.getHeight()*scaleFactor),true);
		ce.stretchHistogram(tmpIp,1.0);
		
		edged.convolve(tmpIp.convertToByte(true));
//		edged.nonMaxSup();
		ByteProcessor edgeIp=(ByteProcessor)edged.getGradientMagnitudeProcessor().convertToByte(true);
		edgeIp.autoThreshold();
		
		return edgeIp;
	}
	
	private void getHoughLines(ImageProcessor ip){
		ByteProcessor tmpIp=null;
		if(ip instanceof ByteProcessor){
			tmpIp=(ByteProcessor)ip;
		}
		else{
			tmpIp=(ByteProcessor)(ip.convertToByte(true));
		}
		LinearHT houghTransform=new LinearHT(tmpIp,720,tmpIp.getWidth());
		houghlines=houghTransform.getMaxLines(100,0);
		
		htCenter=new Point2D.Double(houghTransform.getXc(),houghTransform.getYc());
		
		parallelLines=new HashMap<Double,List<Integer>>();

		
		for(int i=0;i<houghlines.size();++i){
			double angle=roundToDec(houghlines.get(i).getAngle(),3);
			if(!parallelLines.containsKey(angle)){
				parallelLines.put(angle,new ArrayList<Integer>());
			}

			parallelLines.get(angle).add(i);
		}
		
		return;
	}
	
	private void searchBestRoiFit(ImageProcessor ip){
		double roiFittness=Double.MAX_VALUE;
		
		int width=ip.getWidth();
		byte[] ipPixels=(byte[])ip.convertToByte(false).getPixels();
		
		for(double angle:parallelLines.keySet()){
			if(angle<Math.PI/4.0 || angle>3.0*Math.PI/4.0){
				continue;
			}
			
			double normalAngle=roundToDec(getNormalAngle(angle),3);
			if(!parallelLines.containsKey(normalAngle)){  
				continue;
			}
			
			List<Integer> indices1=parallelLines.get(angle);
			List<Integer> indices2=parallelLines.get(normalAngle);
			
			for(int idx1:indices1){
				for(int idx2:indices2){
					Point2D is=lineIntersection(houghlines.get(idx1),houghlines.get(idx2));
					
					Shape tShape=transformShape2(scaleFactor,angle-Math.PI/2.0,new double[]{is.getX()+htCenter.getX(),is.getY()+htCenter.getY()});
					Rectangle shapeB=tShape.getBounds();
					if(shapeB.x<0 || shapeB.x+shapeB.width>ip.getWidth() || shapeB.y<0 || shapeB.y+shapeB.height>ip.getHeight()){
						continue;
					}
					
					Roi sRoi=new ShapeRoi(tShape);
					byte[] sRoiMPixels=(byte[])sRoi.getMask().getPixels();

					int pixSum=0;
					for(int y=shapeB.y, my=0; y<(shapeB.y+shapeB.height); y++, my++){
						int i = y*width + shapeB.x;
						int mi = my*shapeB.width;
						for (int x=shapeB.x; x<(shapeB.x+shapeB.width); x++) {
							if (sRoiMPixels[mi++]!=0) {
								pixSum+=ipPixels[i] & 0xff;
							}
							i++;
						}
					}
					double rfit=(double)pixSum/ipPixels.length;
					
					if(rfit<roiFittness){
						roiFittness=rfit;
						rotAngle=angle-Math.PI/2.0;
						referencePt=new Point2D.Double(sRoi.getBounds().x/scaleFactor,sRoi.getBounds().y/scaleFactor);
					}
				}
				
			}
		}
		IJ.log("best fittness="+roiFittness);
		
		IJ.log("ref pt: "+referencePt.getX()+","+referencePt.getY());
		
	}

	public ImageProcessor getCorrectedIp(){
		double si=Math.sin(rotAngle);
		double co=Math.cos(rotAngle);
//
		Shape tShape=transformShape(scaleFactor,-rotAngle,new double[]{0,0});
		Rectangle shapeBounds=tShape.getBounds();
		ImageProcessor shapeMask=new ShapeRoi(tShape).getMask();
		ImageProcessor dstIp=origIp.createProcessor(shapeBounds.width,shapeBounds.height);
		Rectangle outlineBounds=convexOutline.getBounds();
		int xOffset=outlineBounds.width-shapeBounds.width;
		int yOffset=outlineBounds.height-shapeBounds.height;
		referencePt=new Point2D.Double(outlineBounds.x+xOffset,outlineBounds.y+yOffset); //TODO: should be done when calculating rotation
		for(int y=0;y<dstIp.getHeight();++y){
			for(int x=0;x<dstIp.getWidth();++x){
				if(shapeMask.get(x,y)>0){
					int rx=(int)(x*co-y*si+0.5)+outlineBounds.x+xOffset;
					int ry=(int)(x*si+y*co+0.5)+outlineBounds.y+yOffset;
					dstIp.set(x,y,origIp.get(rx,ry));
				}
			}
		}
		return dstIp;
	}

	public ImageProcessor getCorrectedIp_old(){
//		ImageProcessor workIp=origIp.duplicate();
//		workIp.setColor(0);
//		workIp.fillOutside(convexRoi);
//		workIp.setRoi(convexRoi);
//		workIp=workIp.crop();
//		workIp.rotate(Math.toDegrees(-rotAngle));
//		return workIp;
		
		double si=Math.sin(rotAngle);
		double co=Math.cos(rotAngle);
//		
//		long startTime=System.nanoTime();
		Shape tShape=transformShape(scaleFactor,-rotAngle,new double[]{0,0});
		Rectangle shapeBounds=tShape.getBounds();
		ImageProcessor shapeMask=new ShapeRoi(tShape).getMask();
		ImageProcessor dstIp=origIp.createProcessor(shapeBounds.width,shapeBounds.height);
		Rectangle outlineBounds=convexOutline.getBounds();
		int xOffset=outlineBounds.width-shapeBounds.width;
		int yOffset=outlineBounds.height-shapeBounds.height;
		for(int y=0;y<dstIp.getHeight();++y){
			for(int x=0;x<dstIp.getWidth();++x){
				if(shapeMask.get(x,y)>0){
//					int nx=x+outlineBounds.x;
//					int ny=y+outlineBounds.y;
					
					int rx=(int)(x*co-y*si+0.5)+outlineBounds.x+xOffset;
					int ry=(int)(x*si+y*co+0.5)+outlineBounds.y+yOffset;
					dstIp.set(x,y,origIp.get(rx,ry));
				}
			}
		}
//		long elapsedTime1=System.nanoTime()-startTime;
//		long mins1=TimeUnit.NANOSECONDS.toMinutes(elapsedTime1);
//		long secs1=TimeUnit.NANOSECONDS.toSeconds(elapsedTime1)-mins1*60;
//		double ms1=(elapsedTime1-mins1*60.0*1.0e9-secs1*1.0e9)/1.0e6;
//		IJ.log(String.format("roi transform: %02dm %02ds %03.3fms",mins1,secs1,ms1));
		return dstIp;
	}
	
	public ImageProcessor getCorrectedIp2(){
		Rectangle plateBounds=plateShape.getBounds();
		ImageProcessor dstIp=origIp.createProcessor(plateBounds.width,plateBounds.height);
		
		int xOffset=(int)(referencePt.getX());
		int yOffset=(int)(referencePt.getY());
		
		Roi fitRoi=new ShapeRoi(plateShape);
		double si=Math.sin(rotAngle);
		double co=Math.cos(rotAngle);
		ImageProcessor bestRoiMask=fitRoi.getMask();
		for(int y=0;y<dstIp.getHeight();++y){
			for(int x=0;x<dstIp.getWidth();++x){
				if(bestRoiMask.get(x,y)>0){
					int nx=(int)(x*co-y*si+0.5)+xOffset;
					int ny=(int)(x*si+y*co+0.5)+yOffset;
					if(nx>=0 && nx<origIp.getWidth() && ny>=0 && ny<origIp.getHeight()){
						dstIp.set(x,y,origIp.get(nx,ny));
					}
				}
			}
		}
//		new ImagePlus("best fit",dstIp).show();
		return dstIp;
		
	}
	
	private Point2D lineIntersection(HoughLine line1,HoughLine line2){
		double[] is=new double[2];

		double radius1=line1.getRadius();
		double radius2=line2.getRadius();
		double angle1=line1.getAngle();
		double angle2=line2.getAngle();
		
		double sin1=Math.sin(angle1);
		double sin2=Math.sin(angle2);
		double cos1=Math.cos(angle1);
		
		is[0]=(radius2*sin1-radius1*sin2)/Math.sin(angle1-angle2);
		is[1]=(radius1-is[0]*cos1)/sin1;
		
//		is[0]+=line1.getXc();
//		is[1]+=line1.getYc();
		
		return new Point2D.Double(is[0],is[1]);
	}
	
	private Shape transformShape(double scaleFactor,double rotationAngle,double[] positionVector){
		Path2D shapePath=new GeneralPath(convexOutline);
		if(scaleFactor!=1d){
			AffineTransform at=new AffineTransform();
			at.scale(scaleFactor,scaleFactor);
			shapePath.transform(at);
		}

		if(rotationAngle!=0d){
			AffineTransform at=new AffineTransform();
			at.rotate(rotationAngle);
			shapePath.transform(at);
		}

		Rectangle shapeBounds=shapePath.getBounds();
		double[] translationVector=new double[]{positionVector[0]-shapeBounds.x,positionVector[1]-shapeBounds.y};
		AffineTransform at=new AffineTransform();
		at.translate(translationVector[0],translationVector[1]);
		shapePath.transform(at);

		Shape dstShape=shapePath.createTransformedShape(null);

//		Roi dstRoi=new ShapeRoi(dstShape);
//		Roi srcRoi=new ShapeRoi(plateShape);
//		ImageProcessor roiIp=new ByteProcessor(srcRoi.getBounds().width,srcRoi.getBounds().height);
//		roiIp.setColor(255);
//		roiIp.draw(dstRoi);
//		roiIp.draw(srcRoi);
//		new ImagePlus("roi outline",roiIp).show();

		return dstShape;
	}
	
	private Shape transformShape2(double scaleFactor,double rotationAngle,double[] translationVector){
		Path2D shapePath=new GeneralPath(plateShape);
		if(scaleFactor!=1d){
			AffineTransform at=new AffineTransform();
			at.scale(scaleFactor,scaleFactor);
			shapePath.transform(at);
		}

		if(rotationAngle!=0d){
			AffineTransform at=new AffineTransform();
			at.rotate(rotationAngle);
			shapePath.transform(at);
		}

		if(translationVector[0]!=0d || translationVector[1]!=0d){
			Rectangle shapeBounds=shapePath.getBounds();
			if(translationVector[0]>0){
				translationVector[0]-=shapeBounds.getWidth();
			}
			if(translationVector[1]>0){
				translationVector[1]-=shapeBounds.getHeight();
			}
			AffineTransform at=new AffineTransform();
			at.translate(translationVector[0],translationVector[1]);
			shapePath.transform(at);
		}

		Shape dstShape=shapePath.createTransformedShape(null);

//		Roi dstRoi=new ShapeRoi(dstShape);
//		Roi srcRoi=new ShapeRoi(plateShape);
//		ImageProcessor roiIp=new ByteProcessor(srcRoi.getBounds().width,srcRoi.getBounds().height);
//		roiIp.setColor(255);
//		roiIp.draw(dstRoi);
//		roiIp.draw(srcRoi);
//		new ImagePlus("roi outline",roiIp).show();

		return dstShape;
	}
}
