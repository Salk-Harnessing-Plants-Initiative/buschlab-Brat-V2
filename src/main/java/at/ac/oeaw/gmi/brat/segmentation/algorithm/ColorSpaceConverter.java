package at.ac.oeaw.gmi.brat.segmentation.algorithm;

import java.awt.Color;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

public class ColorSpaceConverter {
	final static double[] XYZr={0.95047,1.0,1.08883};            	// reference illuminant D65
	final static double eps=0.008856;                            	// actual CIE standard
	final static double kappa=903.3;								// actual CIE standard
	
	final static double[][] M={{0.4124564,0.3575761,0.1804375},
							   {0.2126729,0.7151522,0.0721750},
							   {0.0193339,0.1191920,0.9503041}}; // sRGB D65
	
//	final double[][] M={{0.4360747f,0.3850649f,0.1430804f},  
//	 					{0.2225045f,0.7168786f,0.0606169f},
//	 					{0.0139322f,0.0971045f,0.7141733f}}; // sRGB D50
	
	
	public static double[] RGBToXYZ(double[] rgb){
		for(int i=0;i<3;++i){
			if(rgb[i]>0.04045){
				rgb[i]=Math.pow((rgb[i]+0.055)/1.055,2.4);
			}
			else{
				rgb[i]/=12.92;
			}
		}
		
		double[] xyz=new double[3];
		for(int j=0;j<3;++j){
			for(int i=0;i<3;++i){
				xyz[j]+=M[j][i]*rgb[i];
			}
		}
		
		return xyz;
	}
	
	public static double[] XYZToCieLab(double[] xyz){
		double[] fxyz=new double[3];
		for(int i=0;i<3;++i){
			xyz[i]/=XYZr[i];
			
			if(xyz[i]>eps){
				fxyz[i]=Math.pow(xyz[i],1.0/3.0);
			}
			else{
				  fxyz[i]=(kappa*xyz[i]+16.0)/116.0;
			}
		}
		
		double[] Lab=new double[3];
		Lab[0]=116.0*fxyz[1]-16.0;
		Lab[1]=500.0*(fxyz[0]-fxyz[1]);
		Lab[2]=200.0*(fxyz[1]-fxyz[2]);

		return Lab;
	}

	public static double[] RGBToCieLab(double[] rgb){
		return XYZToCieLab(RGBToXYZ(rgb));
	}
	
	public static double[] RGBToCieLab(int[] rgb){
		double[] dRGB=new double[rgb.length];
		for(int i=0;i<rgb.length;++i){
			dRGB[i]=(double)rgb[i]/255.0;
		}
		
		return RGBToCieLab(dRGB);
	}
	
	public static double[] CieLabToCieLCh(double[] lab){
		double[] lch=new double[3];
		lch[0]=lab[0];
		lch[1]=Math.sqrt(lab[1]*lab[1]+lab[2]*lab[2]);
		double tmpAngle=Math.atan2(lab[2],lab[1]);
		if(tmpAngle>=0){
			lch[2]=Math.toDegrees(tmpAngle);
		}
		else{
			lch[2]=Math.toDegrees(2*Math.PI+tmpAngle);
		}
//		if(lch[2]>7)
//			IJ.log("warning");
		return lch;
	}
	
	public static double[] RGBToCieLCh(double[] rgb){
		double[] lab=RGBToCieLab(rgb);
		double[] lch=new double[3];
		lch[0]=lab[0];
		lch[1]=Math.sqrt(lab[1]*lab[1]+lab[2]*lab[2]);
		lch[2]=Math.PI+Math.atan2(lab[2],lab[1]);
		
		return lch;
	}
	
	public static double[] RGBToCieLCh(int[] rgb){
		double[] dRGB=new double[rgb.length];
		for(int i=0;i<rgb.length;++i){
			dRGB[i]=(double)rgb[i]/255.0;
		}
		
		return RGBToCieLCh(dRGB);
		
	}
	
	public static double distanceCie76(double[] lab1,double[] lab2){
		double dist=0d;
		for(int i=0;i<lab1.length;++i){
			dist+=(lab2[i]-lab1[i])*(lab2[i]-lab1[i]);
		}
		
		return Math.sqrt(dist);
	}
	
	public static ImageStack RGBToLabStack(ImageProcessor rgbIp){
		int w=rgbIp.getWidth();
		int h=rgbIp.getHeight();
		int size=w*h;
		
		ImageStack labStack=new ImageStack(w,h);
		labStack.addSlice("L",new FloatProcessor(w,h));
		labStack.addSlice("a",new FloatProcessor(w,h));
		labStack.addSlice("b",new FloatProcessor(w,h));
		
//		double[] minValues={Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
		double[] maxValues={Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE};
		double[] cieLab;
		int[] rgb=new int[3];
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				rgb=rgbIp.getPixel(x,y,rgb);
				cieLab=ColorSpaceConverter.RGBToCieLab(rgb);
				for(int i=0;i<3;++i){
					labStack.setVoxel(x,y,i,cieLab[i]);
//					if(Math.abs(cieLab[i])>maxValues[i]){
//						maxValues[i]=Math.abs(cieLab[i]);
//					}
				}
			}
		}
//		double[] scales={255d/maxValues[0],127/maxValues[1],127/maxValues[2]};
//		
//		for(int i=0;i<3;++i){
//			for(int y=0;y<h;++y){
//				for(int x=0;x<w;++x){
//					double value=labStack.getVoxel(x,y,i);
//					if(i==0){
//						labStack.setVoxel(x,y,i,(int)((value*scales[i])+0.5));
//					}
//					else{
//						if(value<0){
//							labStack.setVoxel(x,y,i,127-(int)((value*scales[i])+0.5));
//						}
//						else{
//							labStack.setVoxel(x,y,i,128+(int)((value*scales[i])+0.5));
//						}
//					}
//				}
//			}
//		}
		
		return labStack;
	}
	
	public static ImageStack RGBToLChStack(ImageProcessor rgbIp){
		int w=rgbIp.getWidth();
		int h=rgbIp.getHeight();
		int size=w*h;
		
		ImageStack labStack=new ImageStack(w,h);
		labStack.addSlice("L",new FloatProcessor(w,h));
		labStack.addSlice("a",new FloatProcessor(w,h));
		labStack.addSlice("b",new FloatProcessor(w,h));
		ImageStack lchStack=new ImageStack(w,h);
		lchStack.addSlice("L",new FloatProcessor(w,h));
		lchStack.addSlice("C",new FloatProcessor(w,h));
		lchStack.addSlice("h",new FloatProcessor(w,h));
		
//		double[] minValues={Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE};
		double[] maxValues={Double.MIN_VALUE,Double.MIN_VALUE,Double.MIN_VALUE};
		double[] cieLab;
		double[] cieLCh;
		int[] rgb=new int[3];
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				rgb=rgbIp.getPixel(x,y,rgb);
				cieLab=ColorSpaceConverter.RGBToCieLab(rgb);
				for(int i=0;i<3;++i){
					labStack.setVoxel(x,y,i,cieLab[i]);
				}
				cieLCh=ColorSpaceConverter.CieLabToCieLCh(cieLab);
				for(int i=0;i<3;++i){
					lchStack.setVoxel(x,y,i,cieLCh[i]);
					
//					if(Math.abs(cieLCh[i])>maxValues[i]){
//						maxValues[i]=Math.abs(cieLCh[i]);
//					}
				}
			}
		}
//		double[] scales={255d/maxValues[0],255d/maxValues[1],255d/maxValues[2]};
//		
//		for(int i=0;i<3;++i){
//			for(int y=0;y<h;++y){
//				for(int x=0;x<w;++x){
//					double value=lchStack.getVoxel(x,y,i);
//					lchStack.setVoxel(x,y,i,(int)((value*scales[i])+0.5));
//				}
//			}
//		}
//		new ImagePlus("Lab",labStack).show();
//		new ImagePlus("LCh",lchStack).show();
		
		return lchStack;
	}

	public static ImageStack RgbToHsbStack(ImageProcessor rgbIp/*,Level logLevel*/){
//		log.setLevel(logLevel);
//		log.fine("convert to HSB started.");
		int width=rgbIp.getWidth();
		int height=rgbIp.getHeight();

		ImageStack hsbStack=new ImageStack(width,height);
		ImageProcessor[] hsbProcessors=new ByteProcessor[3];
		for(int i=0;i<3;++i){
			hsbProcessors[i]=new ByteProcessor(width,height);
			hsbStack.addSlice(hsbProcessors[i]);
		}
		for(int y=0;y<height;++y){
			for(int x=0;x<width;++x){
				int[] pixRgb=rgbIp.getPixel(x, y, new int[3]);
				float[] pixHsb=Color.RGBtoHSB(pixRgb[0], pixRgb[1], pixRgb[2], new float[3]);
				for(int i=0;i<3;++i){
					hsbProcessors[i].set(x,y,(int)(pixHsb[i]*255));
				}
			}
		}
//		log.fine("convert to HSB finished");
		return hsbStack;
	}
}
