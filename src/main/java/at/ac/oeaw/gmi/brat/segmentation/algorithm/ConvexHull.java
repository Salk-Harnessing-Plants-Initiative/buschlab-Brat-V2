package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConvexHull {
	List<CPoint> pts;
	List<CPoint> hull;
	
	public ConvexHull(List<CPoint> pts){
		this.pts=pts;
	}
	
	public ConvexHull(int[] xpoints,int[] ypoints,int npoints){
		this.pts=new ArrayList<CPoint>(npoints);
		for(int i=0;i<npoints;++i){
			pts.add(new CPoint(xpoints[i],ypoints[i]));
		}
	}
	
	public Polygon getConvexHullPolygon(){
		createConvexHull();

		int npoints=hull.size();
		int[] xpoints=new int[npoints];
		int[] ypoints=new int[npoints];
		for(int i=0;i<npoints;++i){
			xpoints[i]=hull.get(i).x;
			ypoints[i]=hull.get(i).y;
		}
		
		return new Polygon(xpoints,ypoints,npoints);
	}
	
	public Roi getConvexHullRoi(){
		createConvexHull();
		
		int npoints=hull.size();
		int[] xpoints=new int[npoints];
		int[] ypoints=new int[npoints];
		for(int i=0;i<npoints;++i){
			xpoints[i]=hull.get(i).x;
			ypoints[i]=hull.get(i).y;
		}

		return new PolygonRoi(xpoints,ypoints,npoints,Roi.POLYGON);
	}
	
	private void createConvexHull(){
		int n=pts.size();
		int k=0;
		
		Collections.sort(pts);

		hull=new ArrayList<CPoint>(2*n);
		while(hull.size()<2*n) hull.add(null);
		
		for(int i=0;i<n;++i){
			while(k>=2 && CPoint.cross(hull.get(k-2),hull.get(k-1),pts.get(i))<=0)
				k--;
			hull.set(k,pts.get(i));
			k++;
		}

		for(int i=n-2,t=k+1;i>=0;--i) {
            while(k>=t && CPoint.cross(hull.get(k-2),hull.get(k-1),pts.get(i))<=0)
            	k--;
            hull.set(k,pts.get(i));
            k++;
    	}
		
		hull=hull.subList(0,k);
	}
}

