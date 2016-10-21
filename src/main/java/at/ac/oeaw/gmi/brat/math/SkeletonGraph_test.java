package at.ac.oeaw.gmi.brat.math;

import ij.process.ImageProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class SkeletonGraph_test {
	ImageProcessor skeletonIp;
	Rectangle idOffset;
	Integer fgColor;
	int width;
	int height;
	
	List<SkeletonSubGraph> subGraphs;
	
	public SkeletonGraph_test(ImageProcessor skeletonIp,Integer fgColor,Rectangle idOffset){
		this.skeletonIp=skeletonIp;
		this.fgColor=fgColor;
		this.idOffset=idOffset;
		width=skeletonIp.getWidth();
		height=skeletonIp.getHeight();
	}
	
	public void createGraph(){
		
		int bgColor=255-fgColor;
		for(int y=0;y<height;++y){
			for(int x=0;x<width;++x){
				if(skeletonIp.get(x,y)==fgColor){
					Stack<Point> stack=new Stack<Point>();
					stack.push(new Point(x,y));
					
					List<Point> graphPixels=new ArrayList<Point>();
					while(!stack.empty()){
						Point pt=stack.pop();
						skeletonIp.set(x,y,bgColor);
						graphPixels.add(pt);
						
						List<Point> neighbors=getNeighbors8(pt);
						for(Point nPt:neighbors){
							stack.push(nPt);
						}
					}
//					SkeletonSubGraph subSkel=new SkeletonSubGraph(graphPixels,idOffset);
//					subSkel.createGraph();
//					subGraphs.add(subSkel);
				}
			}
		}
	}
	
//	public void connectSubGraphs(int maxDistance,Point containedPt){
//		if(containedPt!=null){
//			for(SkeletonSubGraph sub:subGraphs){
//				if(sub.contains(containedPt)){
//					
//				}
//			}
//		}
//	}
	
	private List<Point> getNeighbors8(Point pt){
		List<Point> neighbors=new ArrayList<Point>();
		
		for(int y=pt.y-1;y<=pt.y+1;++y){
			if(y<0 || y>height-1){
				continue;
			}
			for(int x=pt.x-1;x<=pt.x+1;++x){
				if(x<0 || x>width-1){
					continue;
				}
				if(skeletonIp.get(x,y)==fgColor){
					neighbors.add(new Point(x,y));
				}
			}
		}
		
		return neighbors;
	}
}
