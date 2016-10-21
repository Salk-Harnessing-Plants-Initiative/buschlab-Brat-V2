package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class LineDetector {
	ImageProcessor detectIp;
	
	int[][] kernels={{-1,-1,-1,2,2,2,-1,-1,-1},{-1,-1,2,-1,2,-1,2,-1,-1},{-1,2,-1,-1,2,-1,-1,2,-1},{2,-1,-1,-1,2,-1,-1,-1,2}};
//	int[] kernelp45={-1,-1,2,-1,2,-1,2,-1,-1};
//	int[] kernely={-1,2,-1,-1,2,-1,-1,2,-1};
//	int[] kernelm45={2,-1,-1,-1,2,-1,-1,-1,2};
	
	public void detect(ImageProcessor ip){
		detectIp=new FloatProcessor(ip.getWidth(),ip.getHeight());
		ImageProcessor tmpIp;
		for(int i=0;i<4;++i){
			tmpIp=ip.duplicate();
			tmpIp.convolve3x3(kernels[i]);
			detectIp.copyBits(tmpIp,0,0,Blitter.MAX);
		}
	}
	
	public ImageProcessor getResultIp(){
		return detectIp;
	}
}
