package at.ac.oeaw.gmi.brat.segmentation.algorithm.graph;

public class SkeletonLink {
	private double length;
	
	public SkeletonLink(double length){
		this.length=length;
	}
	
	public double getLength(){
		return length;
	}
	
	public String toString(){
		return "E("+length+")";
	}
}
