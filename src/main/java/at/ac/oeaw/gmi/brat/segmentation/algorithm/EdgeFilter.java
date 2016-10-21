package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class EdgeFilter {
	ImageProcessor gradMag;
	ImageProcessor gradDir;
	
	float[] xkernel={-1.0f,0.0f,1.0f,-2.0f,0.0f,2.0f,-1.0f,0.0f,1.0f};
	float[] ykernel={-1.0f,-2.0f,-1.0f,0.0f,0.0f,0.0f,1.0f,2.0f,1.0f}; //sobel
	
	float[] cmpAngles={0.0f,(float)(Math.PI/4.0),(float)(Math.PI/2.0),(float)(3*Math.PI/4.0),(float)(Math.PI)};
	
	public void convolve(ImageProcessor ip){
		ip.filter(ImageProcessor.MEDIAN_FILTER);
		ImageProcessor gx=ip.convertToFloat();
		gx.convolve(xkernel,3,3);
		ImageProcessor gy=ip.convertToFloat();
		gy.convolve(ykernel,3,3);
		
		gradMag=new FloatProcessor(ip.getWidth(),ip.getHeight());
		gradDir=new ByteProcessor(ip.getWidth(),ip.getHeight());
		for(int i=0;i<ip.getPixelCount();++i){
			gradMag.setf(i,(float)Math.sqrt(gx.getf(i)*gx.getf(i)+gy.getf(i)*gy.getf(i)));
			
			float dir=(float)(Math.atan2(gx.getf(i),gy.getf(i)));
			if(dir<0)
				dir+=(float)Math.PI;
			float minDev=Float.MAX_VALUE;
			float angle=0;
			for(int j=0;j<cmpAngles.length;++j){
				float dev=Math.abs(dir-cmpAngles[j]);
				if(dev<minDev){
					minDev=dev;
					if(j!=4)
						angle=j; //cmpAngles[j];
					else
						angle=0; //cmpAngles[0];
				}
			}
			gradDir.setf(i,angle);
		}

//		new ImagePlus("gradient magnitude",gradMag).show();
//		new ImagePlus("gradient direction",direction).show();
		
	}
	
	public void nonMaxSup(){
		int w=gradDir.getWidth();
		int h=gradDir.getHeight();
		
		int x1=0,y1=0;
		int x2=0,y2=0;
		for(int y=0;y<w;++y){
			for(int x=0;x<h;++x){
				int dir=gradDir.get(x,y);
				switch(dir){
				case 0:	x1=x;
						y1=y-1;
						x2=x;
						y2=y+1;
						break;
				case 1:	x1=x-1;
						y1=y-1;
						x2=x+1;
						y2=y+1;
						break;
				case 2:	x1=x-1;
						y1=y;
						x2=x+1;
						y2=y;
						break;
				case 3: x1=x+1;
						y1=y-1;
						x2=x-1;
						y2=y+1;
						break;
				}
				
				float val1=0f;
				if(x1>0 && x1<w && y1>0 && y1<h){
					val1=gradMag.getf(x1,y1);
				}
				float val2=0f;
				if(x2>0 && x2<w && y2>0 && y2<h){
					val2=gradMag.getf(x2,y2);
				}
				float val=gradMag.getf(x,y);
				if(val<val1 || val<val2){
					gradMag.setf(x,y,0f);
				}
				
			}
		}
	}
	
	public ImageProcessor getGradientMagnitudeProcessor(){
		return gradMag;
	}
	
	public ImageProcessor getDirectionProcessor(){
		return gradDir;
	}
}


