package at.ac.oeaw.gmi.brat.segmentation.seeds;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import at.ac.oeaw.gmi.brat.math.KMeans1d;
import at.ac.oeaw.gmi.brat.utility.FileUtils;

public class SeedingLayout {
	private final static Logger log=Logger.getLogger(SeedingLayout.class.getName());
	private final Preferences prefs_simple = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	private final Preferences prefs_expert = prefs_simple.node("expert");
	private final double mmPerPixel = 25.4/prefs_expert.getDouble("resolution",1200);

	private double scaleFactor;
	private Shape plateShape;

	private int expectedRows;
	private int expectedColumns;
	private List<List<Point2D>> seedPositions;
	private List<Double> rowYPositions;
	
	public SeedingLayout(){
		this.scaleFactor=0.5;
		this.expectedRows=2;
		this.expectedColumns=12;
		
		float w=(float)(116.0f/mmPerPixel);
		this.plateShape=new RoundRectangle2D.Float(0f,0f,w,w,1200f,1200f);
		
		seedPositions=new ArrayList<List<Point2D>>();
		rowYPositions=new ArrayList<Double>();
		List<Point2D> tmpPositions=new ArrayList<Point2D>();
		tmpPositions.add(new Point2D.Double(360,932));
		tmpPositions.add(new Point2D.Double(748,932));
		tmpPositions.add(new Point2D.Double(1148,932));
		tmpPositions.add(new Point2D.Double(1725,932));
		tmpPositions.add(new Point2D.Double(2127,932));
		tmpPositions.add(new Point2D.Double(2534,932));
		tmpPositions.add(new Point2D.Double(3091,932));
		tmpPositions.add(new Point2D.Double(3510,932));
		tmpPositions.add(new Point2D.Double(3927,932));
		tmpPositions.add(new Point2D.Double(4487,932));
		tmpPositions.add(new Point2D.Double(4886,932));
		tmpPositions.add(new Point2D.Double(5293,932));
		seedPositions.add(tmpPositions);
		rowYPositions.add(932.0);
		
		tmpPositions=new ArrayList<Point2D>();
		tmpPositions.add(new Point2D.Double(358,3260));
		tmpPositions.add(new Point2D.Double(750,3260));
		tmpPositions.add(new Point2D.Double(1142,3260));
		tmpPositions.add(new Point2D.Double(1725,3260));
		tmpPositions.add(new Point2D.Double(2121,3260));
		tmpPositions.add(new Point2D.Double(2530,3260));
		tmpPositions.add(new Point2D.Double(3090,3260));
		tmpPositions.add(new Point2D.Double(3505,3260));
		tmpPositions.add(new Point2D.Double(3926,3260));
		tmpPositions.add(new Point2D.Double(4480,3260));
		tmpPositions.add(new Point2D.Double(4889,3260));
		tmpPositions.add(new Point2D.Double(5293,3260));
		seedPositions.add(tmpPositions);
		rowYPositions.add(3260.0);
	}
	
	public void setAlexandroLayout(){
		this.scaleFactor=0.5;
		this.expectedRows=1;
		this.expectedColumns=12;
		
		seedPositions=new ArrayList<List<Point2D>>();
		rowYPositions=new ArrayList<Double>();
		List<Point2D> tmpPositions=new ArrayList<Point2D>();
		tmpPositions.add(new Point2D.Double(360,2300));
		tmpPositions.add(new Point2D.Double(748,2300));
		tmpPositions.add(new Point2D.Double(1148,2300));
		tmpPositions.add(new Point2D.Double(1725,2300));
		tmpPositions.add(new Point2D.Double(2127,2300));
		tmpPositions.add(new Point2D.Double(2534,2300));
		tmpPositions.add(new Point2D.Double(3091,2300));
		tmpPositions.add(new Point2D.Double(3510,2300));
		tmpPositions.add(new Point2D.Double(3927,2300));
		tmpPositions.add(new Point2D.Double(4487,2300));
		tmpPositions.add(new Point2D.Double(4886,2300));
		tmpPositions.add(new Point2D.Double(5293,2300));
		seedPositions.add(tmpPositions);
		rowYPositions.add(2300.0);
		
	}
	
	public void setMarcoLayout(){
		this.scaleFactor=0.5;
		this.expectedRows=1;
		this.expectedColumns=8;
		
		seedPositions=new ArrayList<List<Point2D>>();
		rowYPositions=new ArrayList<Double>();
		List<Point2D> tmpPositions=new ArrayList<Point2D>();
		tmpPositions.add(new Point2D.Double(336,1020));
		tmpPositions.add(new Point2D.Double(930,1020));
		tmpPositions.add(new Point2D.Double(1632,1020));
		tmpPositions.add(new Point2D.Double(2328,1020));
		tmpPositions.add(new Point2D.Double(3156,1020));
		tmpPositions.add(new Point2D.Double(3840,1020));
		tmpPositions.add(new Point2D.Double(4452,1020));
		tmpPositions.add(new Point2D.Double(5094,1020));
//		tmpPositions.add(new Point2D.Double(,1020));
//		tmpPositions.add(new Point2D.Double(,1020));
//		tmpPositions.add(new Point2D.Double(,1020));
//		tmpPositions.add(new Point2D.Double(,1020));
		seedPositions.add(tmpPositions);
		rowYPositions.add(1020.0);
		
	}
	
	public int getExpectedRows(){
		return expectedRows;
	}
	
	public int getExpectedColumns(){
		return expectedColumns;
	}
	
	public Shape getPlateShape(){
		return plateShape;
	}
	
	public List<Double> getRowYPos(){
		return rowYPositions;
	}
	
	public List<List<Point2D>> getSeedPositions(){
		return seedPositions;
	}
	
	public Point2D getSeedPosition(int row,int col){
		return seedPositions.get(row).get(col);
	}
	
	public List<List<Point2D>> getPlateCenteredPositions(int plateWidth,int plateHeight){
		List<List<Point2D>> newPositions=new ArrayList<List<Point2D>>();
		for(List<Point2D> rowPts:seedPositions){
			double[] xExtrema={Double.MAX_VALUE,-Double.MAX_VALUE};
			for(Point2D pt:rowPts){
				if(pt.getX()<xExtrema[0])
					xExtrema[0]=pt.getX();
				if(pt.getX()>xExtrema[1])
					xExtrema[1]=pt.getX();
			}
			
			double correction=(plateWidth-(xExtrema[1]-xExtrema[0]))/2.0-xExtrema[0];
			List<Point2D> tmpPositions=new ArrayList<Point2D>();
			for(Point2D pt:rowPts){
				tmpPositions.add(new Point2D.Double(pt.getX()+correction,pt.getY()));
			}
			newPositions.add(tmpPositions);
		}
		
		return newPositions;
	}
	
	public List<List<Point2D>> getTransformedPositions(double rotation,Point2D referencePt){
		List<List<Point2D>> correctedPos=new ArrayList<List<Point2D>>();
		
		double si=Math.sin(-rotation);
		double co=Math.cos(-rotation);
		
		for(List<Point2D> rowPts:seedPositions){
			List<Point2D> tmpPositions=new ArrayList<Point2D>();
			for(Point2D pt:rowPts){
				double x=(pt.getX()-referencePt.getX())*co-(pt.getY()-referencePt.getY())*si;//+referencePt.getX();
				double y=(pt.getX()-referencePt.getX())*si+(pt.getY()-referencePt.getY())*co;//+referencePt.getY();
				tmpPositions.add(new Point2D.Double(x,y));
			}
			correctedPos.add(tmpPositions);
		}
		
		return correctedPos;
	}

	public double getSearchWidth(int row,int col){
		double width1=Double.MAX_VALUE;
		if(col>0){
			width1=seedPositions.get(row).get(col).getX()-seedPositions.get(row).get(col-1).getX();
//			if(width<searchWidth){
//				searchWidth=(int)width;
//			}
		}
		
		double width2=Double.MAX_VALUE;
		if(col<seedPositions.get(row).size()-1){
			width2=seedPositions.get(row).get(col+1).getX()-seedPositions.get(row).get(col).getX();
//			if(width<searchWidth){
//				searchWidth=(int)width;
//			}
		}
		return width1<width2 ? width1 : width2;
	}
	
	private void assignPtsToRows(List<Point2D> pts){
		ArrayList<Double> gridPtY=new ArrayList<Double>();
		
		List<Double> yExtrema=new ArrayList<Double>();
		yExtrema.add(Double.MAX_VALUE);
		yExtrema.add(-Double.MAX_VALUE);
		for(Point2D pt:pts){
			gridPtY.add(pt.getY());
			if(pt.getY()<yExtrema.get(0))
				yExtrema.set(0,pt.getY());
			if(pt.getY()>yExtrema.get(1))
				yExtrema.set(1,pt.getY());
		}
	
		KMeans1d clusterer=new KMeans1d();
		clusterer.setK(expectedRows);
		clusterer.setData(gridPtY);
		clusterer.setCenters(yExtrema);
		clusterer.cluster();
		rowYPositions=clusterer.getCenters();

		List<List<Point2D>> seedPositions=new ArrayList<List<Point2D>>();
		for(int i=0;i<this.expectedRows;++i){
			seedPositions.add(new ArrayList<Point2D>());
		}
		for(Point2D pt:pts){
			double minDist=Double.MAX_VALUE;
			int lineIdx=0;
			for(int i=0;i<rowYPositions.size();++i){
				double dist=Math.abs(rowYPositions.get(i)-pt.getY());
				if(dist<minDist){
					minDist=dist;
					lineIdx=i;
				}
			}
			seedPositions.get(lineIdx).add(pt);
		}
	}

	public void readStartPoints(String baseDirectory, String imgFileName) {
		String stptFilename="StartPoints_"+ FileUtils.removeExtension(imgFileName)+".txt";
		File stPtFile=new File(baseDirectory,stptFilename);

		BufferedReader br=null;
		seedPositions=new ArrayList<List<Point2D>>();
		rowYPositions=new ArrayList<Double>();
		List<Point2D> newPositions=new ArrayList<Point2D>();
		try{
			br=new BufferedReader(new FileReader(stPtFile));
			String line=null;
			int lineCnt=0;
			double rowY=0;
			if((line = br.readLine()).equals("StartPointsV1.2")){
				lineCnt++;
				log.config(String.format("Found start points file version %s", line));
				String[] parts = br.readLine().split("\\s+");
				lineCnt++;
				String[] layoutstr = parts[1].split("x");
				log.config(String.format("Layout: %s rows, %s columns", layoutstr[0], layoutstr[1]));
				this.expectedRows = Integer.parseInt(layoutstr[0]);
				this.expectedColumns = Integer.parseInt(layoutstr[1]);
			}
			else{
				String[] cols=line.split("\\s+");
				Point2D.Double pt=new Point2D.Double(Double.parseDouble(cols[1]),Double.parseDouble(cols[2]));
				newPositions.add(pt);
				rowY+=pt.getY();
				lineCnt++;
			}

			while((line=br.readLine())!=null){
				String[] cols=line.split("\\s+");
				Point2D.Double pt=new Point2D.Double(Double.parseDouble(cols[1]),Double.parseDouble(cols[2]));
				newPositions.add(pt);
				rowY+=pt.getY();
				lineCnt++;
			}

			seedPositions.add(newPositions);
			rowY/=lineCnt;
			rowYPositions.add(rowY);
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
			log.warning(String.format("Could not find start point file: %s",stPtFile.getAbsolutePath()));
		} catch (IOException e) {
//			e.printStackTrace();
			log.warning(String.format("ERROR: Could not read start point file: %s",stPtFile.getAbsolutePath()));
		}
		finally{
			if(br!=null){
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	
}
