package at.ac.oeaw.gmi.brat.segmentation.plants;

import ij.IJ;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.Wand;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;

import at.ac.oeaw.gmi.brat.math.Rgb2Hsb;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.ColorSpaceConverter;
import at.ac.oeaw.gmi.brat.segmentation.algorithm.ColorSrm;

public class LotusSegmentation {
//	final static Logger log=Logger.getLogger(LotusSegmentation.class.getName());
	final private ImageProcessor plantIp;
	
	private List<Roi> plantRois;
	private List<Roi> shootRois;
	
	public LotusSegmentation(final ImageProcessor ip,Level logLevel){
		this.plantIp=ip;
	}
	
	public List<Roi> getPlantRois() {
		return plantRois;
	}

	public List<Roi> getShootRois() {
		return shootRois;
	}

	
	public void segment(){
		new ContrastEnhancer().equalize(plantIp);
		ImageStack hsbStack=ColorSpaceConverter.RgbToHsbStack(plantIp);
		
		ImageProcessor hueIp=hsbStack.getProcessor(1);
		ImageProcessor satIp=hsbStack.getProcessor(2);
		ImageProcessor briIp=hsbStack.getProcessor(3);
		
		int hueMode=hueIp.getStatistics().mode;
		int satMode=satIp.getStatistics().mode;
		int briMode=briIp.getStatistics().mode;
		
		
		for(int i=0;i<briIp.getPixelCount();++i){
			int color=hueIp.get(i)-hueMode;
			if(color<0)
				color+=255;
			int saturation=satIp.get(i)-satMode;
			if(saturation<0)
				saturation+=255;
			
			int brightness=briIp.get(i)-briMode;
			if(brightness<0)
				brightness+=255;
			
			if((color<42 || color>255-42) && (saturation<42 || saturation>255-42)/* && (brightness<42 || brightness>255-42)*/){
				hueIp.set(i,0);
				satIp.set(i,0);
				briIp.set(i,0);
			}
			
		}
		
		ImageProcessor rgbIp=new ColorProcessor(plantIp.getWidth(),plantIp.getHeight());
		for(int i=0;i<rgbIp.getPixelCount();++i){
			rgbIp.set(i,Color.HSBtoRGB((float)hueIp.get(i)/255f,(float)satIp.get(i)/255f,(float)briIp.get(i)/255f));
		}
//		new ImagePlus("rgb",rgbIp).show();
//		new WaitForUserDialog("reduce wait").show();
		ImageProcessor greyIp=rgbIp.convertToByte(false);
		int w=greyIp.getWidth();
		int h=greyIp.getHeight();
		greyIp.threshold(1);
		//new ImagePlus("mask1",greyIp).show();
		int wandX=0;
		int wandY=0;
		for(int y=h/2;y<h;++y){
			if(wandX!=0 && wandY!=0)
				break;
			for(int x=3*w/4;x<w;++x){
//				IJ.log(x+","+y+": "+greyIp.get(x,y));
				if(greyIp.get(x,y)==0){
					wandX=x;
					wandY=y;
					break;
				}
			}
		}
		Wand wand=new Wand(greyIp);
		wand.autoOutline(wandX,wandY,0.0d,Wand.EIGHT_CONNECTED);
		
		Roi outsideRoi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
//		IJ.log(outsideRoi.getBounds().toString());
		greyIp.setColor(0);
		greyIp.fillOutside(outsideRoi);
		//new ImagePlus("removed outside",greyIp).show();
		
		plantRois=new ArrayList<Roi>();
//		SRM srm=new SRM();
//		srm.setQ(25f);
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				if(greyIp.get(x,y)!=0/* && hueIp.get(x,y)>40 && hueIp.get(x,y)<128*/){
					wand.autoOutline(x,y,0.0d,Wand.EIGHT_CONNECTED);
					Roi roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
					int roiSize=roi.getMask().getHistogram()[255];
					Rectangle roiRect=roi.getBounds();
					if(roiSize>500 && roiSize<1000000){ //TODO: hardcoded size
						greyIp.setRoi(roi);
						ImageProcessor ipMask=greyIp.crop();
						ImageProcessor roiMask=roi.getMask().duplicate();
						ipMask.copyBits(roiMask,0,0,Blitter.XOR);

						ImageProcessor edmIp=ipMask.duplicate();
						EDM edm=new EDM();
						edm.toEDM(edmIp);

						Wand subWand=new Wand(ipMask);
						ipMask.setColor(0);
						roiMask.setColor(0);
						for(int y2=0;y2<roiRect.height;++y2){
							for(int x2=0;x2<roiRect.width;++x2){
								if(roiMask.get(x2,y2)>0 && ipMask.get(x2,y2)>0){
									subWand.autoOutline(x2,y2,0d,Wand.EIGHT_CONNECTED);
									Roi subRoi=new PolygonRoi(subWand.xpoints,subWand.ypoints,subWand.npoints,Roi.POLYGON);
									edmIp.setRoi(subRoi);
									double subMaxWidth=edmIp.getStatistics().max;
									if(subMaxWidth>10){
										roiMask.fill(subRoi);
									}
									ipMask.fill(subRoi);
								}
							}
						}
						
						roiMask.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);
						ThresholdToSelection ts=new ThresholdToSelection();
						Roi realRoi=ts.convert(roiMask);
						
						Rectangle rr=realRoi.getBounds();
						realRoi.setLocation(rr.x+roiRect.x,rr.y+roiRect.y);
						plantRois.add(realRoi);
					}
					greyIp.fill(roi);
				}
			}
		}
		
//		greyIp=new ByteProcessor(w,h);
//		greyIp.setColor(255);
//		for(Roi roi:plantRois){
//			greyIp.fill(roi);
//		}
		shootRootSeparation(plantIp);
		
	}
	
	
	public void segment2(){
//		ImageProcessor origIp=plantIp.duplicate();
		new ContrastEnhancer().equalize(plantIp);

		ImageStack hsbStack=Rgb2Hsb.getHSBStack(plantIp,Level.OFF);
//		new ImagePlus("hsb",hsbStack).show();
		
		ImageProcessor hueIp=hsbStack.getProcessor(1);
		ImageProcessor satIp=hsbStack.getProcessor(2);
		ImageProcessor briIp=hsbStack.getProcessor(3);
		
		int hueMode=hueIp.getStatistics().mode;
		int satMode=satIp.getStatistics().mode;
//		int briMode=briIp.getStatistics().mode;
		
		
		for(int i=0;i<briIp.getPixelCount();++i){
			int color=hueIp.get(i)-hueMode;
			if(color<0)
				color+=255;
			int saturation=satIp.get(i)-satMode;
			if(saturation<0)
				saturation+=255;
			
//			int brightness=briIp.get(i)-briMode;
//			if(brightness<0)
//				brightness+=255;
			
			if((color<42 || color>255-42) && (saturation<42 || saturation>255-42)){
				hueIp.set(i,0);
				satIp.set(i,0);
				briIp.set(i,0);
			}
		}
		
		ImageProcessor rgbIp=new ColorProcessor(plantIp.getWidth(),plantIp.getHeight());
		for(int i=0;i<rgbIp.getPixelCount();++i){
			rgbIp.set(i,Color.HSBtoRGB((float)hueIp.get(i)/255f,(float)satIp.get(i)/255f,(float)briIp.get(i)/255f));
		}
//		new ImagePlus("rgb",rgbIp).show();
		ImageProcessor greyIp=rgbIp.convertToByte(false);
		int w=greyIp.getWidth();
		int h=greyIp.getHeight();
		greyIp.threshold(1);
//		new ImagePlus("mask1",greyIp).show();
		int wandX=0;
		int wandY=0;
		for(int y=h/2;y<h;++y){
			if(wandX!=0 && wandY!=0)
				break;
			for(int x=3*w/4;x<w;++x){
				//IJ.log(x+","+y+": "+greyIp.get(x,y));
				if(greyIp.get(x,y)==0){
					wandX=x;
					wandY=y;
					break;
				}
			}
		}
		Wand wand=new Wand(greyIp);
		wand.autoOutline(wandX,wandY,0.0d,Wand.EIGHT_CONNECTED);
		
		Roi outsideRoi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
//		IJ.log(outsideRoi.getBounds().toString());
		greyIp.setColor(0);
		greyIp.fillOutside(outsideRoi);
//		new ImagePlus("mask2",greyIp).show();
		
//		List<Roi> plantRois=new ArrayList<Roi>();
		double roiMaxArea=0;
		plantRois=new ArrayList<Roi>();
		for(int y=0;y<h;++y){
			for(int x=0;x<w;++x){
				if(greyIp.get(x,y)!=0/* && hueIp.get(x,y)>40 && hueIp.get(x,y)<128*/){
					wand.autoOutline(x,y,0.0d,Wand.EIGHT_CONNECTED);
					Roi roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
					int roiSize=roi.getMask().getHistogram()[255];
					Rectangle roiRect=roi.getBounds();
					if(roiSize>500 && roiSize<1000000){ //TODO: hardcoded size
						greyIp.setRoi(roi);
						double roiArea=greyIp.getStatistics().area;
						if(roiArea>roiMaxArea){
							roiMaxArea=roiArea;
						}
						ImageProcessor ipMask=greyIp.crop();
						ImageProcessor roiMask=roi.getMask();
						roiMask.copyBits(ipMask,0,0,Blitter.AND);
						roiMask.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);
						ThresholdToSelection ts=new ThresholdToSelection();
						Roi realRoi=ts.convert(roiMask);
						realRoi.setLocation(roiRect.x,roiRect.y);
						plantRois.add(realRoi);
					}
					greyIp.fill(roi);
				}
			}
		}
		
//		greyIp=new ByteProcessor(w,h);
//		greyIp.setColor(255);
//		for(Roi roi:plantRois){
//			greyIp.fill(roi);
//		}
		
//		new ImagePlus("mask3",greyIp.duplicate()).show();
		shootRootSeparation(rgbIp);
	}
	
	
	
	public void shootRootSeparation(ImageProcessor ip){
		shootRois=new ArrayList<Roi>();
		Iterator<Roi> roiIt=plantRois.iterator();
//		for(Roi plantRoi:plantRois){
		while(roiIt.hasNext()){
			Roi plantRoi=roiIt.next();
			if(plantRoi.getBounds().width==0 || plantRoi.getBounds().height==0)
				continue;
			
			Rectangle roiBounds=plantRoi.getBounds();
			ImageProcessor roiMask=plantRoi.getMask().duplicate();
			int plantRoiArea=roiMask.getHistogram()[255];
//			IJ.log("roi area: "+roiArea);

			ImageStack roiStack=new ImageStack(roiBounds.width,roiBounds.height);
			for(int i=0;i<3;++i){
				roiStack.addSlice(new ByteProcessor(roiBounds.width,roiBounds.height));
			}
			
			for(int y=0;y<roiBounds.height;++y){
				int linePixCnt=0;
				for(int x=0;x<roiBounds.width;++x){
					if(roiMask.get(x,y)>0){
						int[] rgbVals=ip.getPixel(x+roiBounds.x,y+roiBounds.y,new int[3]);
						float[] hsbVals=Color.RGBtoHSB(rgbVals[0],rgbVals[1],rgbVals[2],new float[3]);
						roiStack.setVoxel(x,y,0,rgbVals[0]);
						roiStack.setVoxel(x,y,1,rgbVals[1]);
						roiStack.setVoxel(x,y,2,rgbVals[2]);
					}
				}
			}
			
			ColorSrm srm=new ColorSrm();
			srm.setQ(10f);
			ImageStack srmStack=new ImageStack(roiBounds.width,roiBounds.height);
			for(int i=0;i<3;++i){
				ImageProcessor srmIp=srm.srm2D(roiStack.getProcessor(i+1),true);
				srmStack.addSlice(srmIp);
			}
			
			ImageProcessor rgbIp=new ColorProcessor(roiBounds.width,roiBounds.height);
			ImageProcessor shootIp=new ByteProcessor(roiBounds.width,roiBounds.height);
			int shootCnt=0;
			for(int y=0;y<roiBounds.height;++y){
				for(int x=0;x<roiBounds.width;++x){
					if(roiMask.get(x,y)>0){
						float[] hsbVals=Color.RGBtoHSB((int)srmStack.getVoxel(x,y,0),(int)srmStack.getVoxel(x,y,1),(int)srmStack.getVoxel(x,y,2),new float[3]);
						
						if(hsbVals[1]>0.3){
							if(Math.abs(hsbVals[0]-0.33)<0.17){
								shootIp.set(x,y,255);
								shootCnt++;
							}
						}
						int pixColor=(int)srmStack.getVoxel(x,y,0);
						pixColor=(pixColor<<8)+(int)srmStack.getVoxel(x,y,1);
						pixColor=(pixColor<<8)+(int)srmStack.getVoxel(x,y,2);
						rgbIp.set(x,y,pixColor);
					}
				}
			}
			
			if(shootCnt<200 || shootCnt>plantRoiArea*0.9){
				roiIt.remove();
				IJ.log("artefact plant detected. Removed Roi at: "+roiBounds.toString());
			}
			else{
				shootIp.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);
				ThresholdToSelection ts=new ThresholdToSelection();
				Roi shootRoi=ts.convert(shootIp);
				if(shootRoi instanceof ShapeRoi){
					double shootArea=shootRoi.getMask().getHistogram()[255];
					Roi[] rois=((ShapeRoi)shootRoi).getRois();
					SortedMap<Integer,Roi> sortedRois=new TreeMap<Integer,Roi>(Collections.reverseOrder());
					for(int i=0;i<rois.length;++i){
						sortedRois.put(rois[i].getMask().getHistogram()[255],rois[i]);
					}
					Roi mainRoi=sortedRois.get(sortedRois.firstKey());
					shootIp.setRoi(mainRoi);
					ImageStatistics mainStats=shootIp.getStatistics();
					Point mainCoM=new Point((int)mainStats.xCenterOfMass,(int)mainStats.yCenterOfMass);
					shootIp.setColor(0);
//					new ImagePlus("hue ip",shootIp.duplicate()).show();
					for(Roi roi:rois){
						int roiArea=roi.getMask().getHistogram()[255];
						if((double)roiArea/(double)shootArea<0.5){
							shootIp.setRoi(roi);
							ImageStatistics subStats=shootIp.getStatistics();
 							Point subCoM=new Point((int)subStats.xCenterOfMass,(int)subStats.yCenterOfMass);
							double dist=mainCoM.distance(subCoM);
							if((double)roiArea/dist<5 || roiArea<10){
								ImageProcessor subRoiMask=roi.getMask();
								for(int suby=0;suby<subRoiMask.getHeight();++suby){
									for(int subx=0;subx<subRoiMask.getWidth();++subx){
										if(subRoiMask.get(subx,suby)>0){
											shootIp.set(subx+roi.getBounds().x,suby+roi.getBounds().y,0);
										}
									}
								}
								//shootIp.fill(sortedRois.get(roiArea));
							}
//							if(dist>0.3*roiBounds.width && dist>0.3*roiBounds.height){
//								shootIp.fill(sortedRois.get(roiArea));
//							}
						}
					}
					
					shootIp.setThreshold(255,255,ImageProcessor.NO_LUT_UPDATE);
					ts=new ThresholdToSelection();
					shootRoi=ts.convert(shootIp);
				}
//				rgbIp.setColor(Color.red);
//				rgbIp.draw(shootRoi);
//				
//				new ImagePlus("shoot segmentation rgb ip",rgbIp.duplicate()).show();
				//new ImagePlus("hue ip",shootIp.duplicate()).show();
			
				//new WaitForUserDialog("wait").show();
				shootRoi.setLocation(shootRoi.getBounds().x+roiBounds.x,shootRoi.getBounds().y+roiBounds.y);
				shootRois.add(shootRoi);
			}
		}
		
	}

}
