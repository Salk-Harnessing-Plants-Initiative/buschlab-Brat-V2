package at.ac.oeaw.gmi.brat.segmentation.algorithm;

public class HoughLine implements Comparable<HoughLine> {
	double angle;
	double radius;
	double xc;
	double yc;
	int count;

	// no public constructor
	public HoughLine(double angle, double radius, double xc,double yc,int count){
		this.angle  = angle;	
		this.radius = radius;	
		this.xc=xc;
		this.yc=yc;
		this.count  = count;	
	}
	
	public double getAngle() {
		return angle;
	}
	
	public double getRadius() {
		return radius;
	}
	
	public int getCount() {
		return count;
	}
	
	public double getXc() {
		return xc;
	}
	
	public double getYc() {
		return yc;
	}
	
	
	/**
	 * Returns the perpendicular distance between this line and the point (x, y).
	 * The result may be positive or negative, depending on which side of
	 * the line (x, y) is located.
	 */
	public double getDistance(double x, double y) {
		final double xs = x - xc;
		final double ys = y - yc;
		return Math.cos(angle) * xs + Math.sin(angle) * ys - radius;
	}
	
	public int compareTo (HoughLine hl){
		HoughLine hl1 = this;
		HoughLine hl2 = hl;
		if (hl1.count > hl2.count)
			return -1;
		else if (hl1.count < hl2.count)
			return 1;
		else
			return 0;
	}
	
	public String toString() {
		return String.format("%s <angle=%.3f, radius=%.3f, count=%d>", 
				HoughLine.class.getSimpleName(), angle, radius, count);
	}


}
