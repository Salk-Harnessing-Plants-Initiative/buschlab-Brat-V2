package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

import java.util.ArrayList;
import java.util.List;

public class RidgeRoi {
	private List<Integer> shootPixelIndices;
	private List<Integer> rootPixelIndices;
	
	public RidgeRoi(){
		shootPixelIndices=new ArrayList<Integer>();
		rootPixelIndices=new ArrayList<Integer>();
	}
	
	public RidgeRoi(List<Integer> rootPixelIndices){
		this.shootPixelIndices=new ArrayList<Integer>();
		this.rootPixelIndices=rootPixelIndices;
		
	}
	
//	public void addShootPixel(int index){
//		shootPixelIndices.add(index);
//	}

	public void setShootPixelIndices(List<Integer> shootPixelsIndices){
		this.shootPixelIndices=shootPixelsIndices;
	}
	
	public void setRootPixelIndices(List<Integer> rootPixelsIndices){
		this.rootPixelIndices=rootPixelsIndices;
	}
	
	public int getTotalArea(){
		return shootPixelIndices.size()+rootPixelIndices.size();
	}

	public List<Integer> getAllPixelIndices() {
		List<Integer> allPixels=new ArrayList<Integer>(shootPixelIndices.size()+rootPixelIndices.size());
		allPixels.addAll(shootPixelIndices);
		allPixels.addAll(rootPixelIndices);
		return allPixels;
	}

	public List<Integer> getShootPixelIndices() {
		return shootPixelIndices;
	}
	
	public List<Integer> getRootPixelIndices(){
		return rootPixelIndices;
	}
}
