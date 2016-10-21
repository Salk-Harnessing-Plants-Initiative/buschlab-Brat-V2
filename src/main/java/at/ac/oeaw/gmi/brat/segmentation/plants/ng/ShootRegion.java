package at.ac.oeaw.gmi.brat.segmentation.plants.ng;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.*;

public class ShootRegion {
	private byte[] pixels;
	private Rectangle bounds;
	private int area;
	private Point com; // center of mass
	
	Roi roi;
	
	public ShootRegion(byte[] pixelIndices,Rectangle bounds){
		this.pixels=pixelIndices;
		this.bounds=bounds;
	}
	
	public ShootRegion(byte[] pixelIndices,Rectangle bounds,Point com,int area){
		this.pixels=pixelIndices;
		this.bounds=bounds;
		this.com=com;
		this.area=area;
	}
	
	public void setCenterOfMass(Point com){
		this.com=com;
	}
	
	public ImageProcessor getMask(){
		return new ByteProcessor(bounds.width,bounds.height,pixels);
	}

	public Rectangle getBounds() {
		return bounds;
	}
	
	public Point getCoM(){
		return com;
	}

	public double area() {
		return area;
	}
	
	public Roi getRoiOutline(){
		ImageProcessor mask=getMask();
		if(roi!=null){
			return roi;
		}
		else{
			Wand wand=new Wand(mask);
			for(int y=0;y<bounds.height;++y){
				if(roi!=null){
					break;
				}
				for(int x=0;x<bounds.width;++x){
					if(mask.get(x,y)>0){
						wand.autoOutline(x,y,0.0,Wand.EIGHT_CONNECTED);
						roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
						roi.setLocation(bounds.x,bounds.y);
						break;
					}
				}
			}
		}
		return roi;
	}
	
	public double getDistance(double x,double y){
		double minDist=Double.MAX_VALUE;
		for(int i=0;i<pixels.length;++i){
			if((pixels[i]&0xff)>(byte)0){
				int px=i%bounds.width+bounds.x;
				int py=i/bounds.width+bounds.y;
				double dist=Math.sqrt((px-x)*(px-x)+(py-y)*(py-y));
				if(dist<minDist){
					minDist=dist;
				}
			}
		}
		return minDist;
	}
	
	public String toString(){
		return bounds.toString();
	}
}
