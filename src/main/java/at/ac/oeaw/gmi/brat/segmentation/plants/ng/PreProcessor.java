package at.ac.oeaw.gmi.brat.segmentation.plants.ng;

import at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow.GradientVectorFlow;
import ij.plugin.filter.GaussianBlur;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class PreProcessor {
	final private ImageProcessor origIp;
	final private int N;
	final private int width;
	final private int height;
	
	public PreProcessor(ImageProcessor ip){
		this.origIp=ip;
		this.N=ip.getPixelCount();
		this.width=ip.getWidth();
		this.height=ip.getHeight();
	}
	
	public double[][] process(double sigma,int numIter){
		ImageProcessor workIp=origIp.duplicate().convertToFloat();
		
		if (sigma>0) {
			GaussianBlur gb=new GaussianBlur();
			gb.blurFloat((FloatProcessor)workIp,sigma,sigma,1.0e-4);
		}		
		
		double[] pixels=new double[N];
		for(int i=0;i<N;++i){
			pixels[i]=workIp.getf(i);
		}
		
		GradientVectorFlow gvfc=new GradientVectorFlow();
		return gvfc.calculate(pixels,width,height,1.0,0.05,numIter);
	}
}
