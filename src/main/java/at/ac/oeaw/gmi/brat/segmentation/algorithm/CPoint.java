package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import java.awt.Point;

public class CPoint extends Point implements Comparable<CPoint>{
	
	public CPoint(int x,int y){
		super(x,y);
//		this.x=x;
//		this.y=y;
	}
	
	
	@Override
	public int compareTo(CPoint o) {
		if(getX()<o.getX() || (getX()==o.getX() && getY()<o.getY())){
			return -1;
		}
		else if(getX()==o.getX() && getY()==o.getY()){
			return 0;
		}
		else{
			return 1;
		}
	}
	
	static double cross(CPoint o,CPoint a,CPoint b){
		return (a.getX()-o.getX())*(b.getY()-o.getY())-(a.getY()-o.getY())*(b.getX()-o.getX());
	}	
}