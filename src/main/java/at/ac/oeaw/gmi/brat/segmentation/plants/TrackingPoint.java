package at.ac.oeaw.gmi.brat.segmentation.plants;

import java.awt.Point;

public class TrackingPoint extends Point{
	double radius;
	double angle;
	
	public TrackingPoint(Point position,double radius,double angle){
		super(position);
		this.radius=radius;
		this.angle=angle;
	}

	public TrackingPoint(int x,int y,double radius,double angle){
		super(x,y);
		this.radius=radius;
		this.angle=angle;

	}
	
	public double getRadius() {
		return radius;
	}
	public void setRadius(double radius) {
		this.radius = radius;
	}

	public double getAngle() {
		return angle;
	}
	public void setAngle(double angle) {
		this.angle = angle;
	}
}
