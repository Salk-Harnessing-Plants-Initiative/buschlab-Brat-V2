package at.ac.oeaw.gmi.brat.math;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;

import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class Rgb2Hsb {
	private final static Logger log=Logger.getLogger(Rgb2Hsb.class.getName());
	public static enum Channel{
		hue,
		sat,
		bri;
	}
	
	public static ImageProcessor getChannel(ImageProcessor rgbIp,Channel channel,Level logLevel){
		log.setLevel(logLevel);
		log.fine("convert to HSB started. Converting to "+channel.toString());
		int width=rgbIp.getWidth();
		int height=rgbIp.getHeight();
		
		ImageProcessor retIp=new ByteProcessor(width,height);
		for(int y=0;y<height;++y){
			for(int x=0;x<width;++x){
				int[] pixRgb=rgbIp.getPixel(x, y, new int[3]);
				float[] pixHsb=Color.RGBtoHSB(pixRgb[0], pixRgb[1], pixRgb[2], new float[3]);
				retIp.putPixel(x, y, (int)(pixHsb[channel.ordinal()]*255));
			}
		}
		log.fine("convert to HSB finished");
	return retIp;
	}

	
	public static ImageStack getHSBStack(ImageProcessor rgbIp,Level logLevel){
		log.setLevel(logLevel);
		log.fine("convert to HSB started.");
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
		log.fine("convert to HSB finished");
		return hsbStack;
	}
}
