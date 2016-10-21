package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import java.awt.Point;
import java.util.ArrayList;

import ij.process.ImageProcessor;

public class PixelUtils {
	public static Integer getNeighbourCnt8(final ImageProcessor ip,final Integer x,final Integer y){
		Integer neighCnt;
		int ownVal=ip.get(x,y);
		neighCnt=0;
		for(int j=y-1;j<=y+1;++j){
			for(int i=x-1;i<=x+1;++i){
				if(i>=0 && i<ip.getWidth() && j>=0 && j<ip.getHeight() && !(i==x && j==y)){
					if(ip.get(i,j)==ownVal)
						neighCnt++;
				}
			}
		}
		return neighCnt;
	}

	public static Integer getNeighbourCnt4(ImageProcessor ip,Integer x,Integer y){
		Integer neighCnt=0;
		int ownVal=ip.get(x,y);
		if(x-1>=0)
			if(ip.get(x-1,y)==ownVal)
				neighCnt++;
		
		if(x+1<ip.getWidth())
			if(ip.get(x+1,y)==ownVal)
				neighCnt++;
		
		if(y-1>=0)
			if(ip.get(x,y-1)==ownVal)
				neighCnt++;
		
		if(y+1<ip.getHeight())
			if(ip.get(x,y+1)==ownVal)
				neighCnt++;
		
		return neighCnt;
	}

	public static ArrayList<Point> getNeighbours(final Point myPix,final ImageProcessor ip,Integer checkVal){
		ArrayList<Point> neighbours=new ArrayList<Point>();
		if(checkVal==null){
			checkVal=ip.get(myPix.x,myPix.y);
		}
		
		for(int j=myPix.y-1;j<=myPix.y+1;++j){
			for(int i=myPix.x-1;i<=myPix.x+1;++i){
				if(i==myPix.x && j==myPix.y)
					continue;
				if(i<0 || i>ip.getWidth()-1 || j<0 || j>ip.getHeight()-1)
					continue;
				if(ip.get(i,j)==checkVal){
					neighbours.add(new Point(i,j));
				}
			}
		}
		return neighbours;
	}
}
