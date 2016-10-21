package at.ac.oeaw.gmi.brat.segmentation.plants.ng;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class ColorAnalyzer {
	private float[] weights;
	private int width;
	private int nRegions;
	List<Double> coms;
	
	public ColorAnalyzer(int width){
		this.width=width;
	}
	
	public float[] getGreenPixelWeights(){
		return weights;
	}
	
	public void calcWeights(int[] values){
		weights=new float[values.length];
		for(int i=0;i<values.length;++i){
			double[] rgb=new double[]{
					((values[i]>>16 & 0xff)/255.0),
					((values[i]>>8  & 0xff)/255.0),
					((values[i]     & 0xff)/255.0)
			};

//			double max=Math.max(rgb[0],Math.max(rgb[1],rgb[2]));
//			double min=Math.min(rgb[0],Math.min(rgb[1],rgb[2]));
//			double sum=rgb[0]+rgb[1]+rgb[2];
			boolean blueMax=(rgb[2]>rgb[0] && rgb[2]>rgb[1]);
			
			if(!blueMax){
				weights[i]=1.0f/*-(float)(max/sum)*/;
			}
		}
//		new ImagePlus("weights",new FloatProcessor(width,weights.length/width,weights)).show();
	}
	
	public void calcWeights2(int[] values){
		weights=new float[values.length];
		for(int i=0;i<values.length;++i){
			double[] rgb=new double[]{
					((values[i]>>16 & 0xff)/255.0),
					((values[i]>>8  & 0xff)/255.0),
					((values[i]     & 0xff)/255.0)
			};

			double max=Math.max(rgb[0],Math.max(rgb[1],rgb[2]));
			double min=Math.min(rgb[0],Math.min(rgb[1],rgb[2]));
			double sum=rgb[0]+rgb[1]+rgb[2];
			boolean greenMax=(rgb[1]>rgb[0] && rgb[1]>rgb[2]);
			
			if(greenMax){
				weights[i]=1.0f-(float)((rgb[2]/sum)/**(max-min)*/);
			}
		}
//		minFilter(weights,width,3);
	}
	
	public List<ShootRegion> getShootRegions(int minSize){
//		ImageProcessor dbgIp=new ByteProcessor(width,weights.length/width);
//		for(int i=0;i<weights.length;++i){
//			if(weights[i]>0){
//				dbgIp.set(i,255);
//			}
//		}
//		new ImageJ();
//		new ImagePlus("shoot mask dbg",dbgIp).show();
		
		coms=new ArrayList<Double>();
		List<ShootRegion> shootRegions=new ArrayList<ShootRegion>();
//		int[] shootRegionIndices=new int[weights.length];
		boolean[] visited=new boolean[weights.length];
//		int regionNr=1;
		for(int i=0;i<weights.length;++i){
			if(visited[i] || weights[i]==0.0f){
				continue;
			}

			Stack<Integer> stack=new Stack<Integer>();
			stack.push(i);
			
			int regionCnt=0;
			Point com=new Point(0,0);
			Point upLeft=new Point(Integer.MAX_VALUE,Integer.MAX_VALUE);
			Point downRight=new Point(0,0);
			List<Integer> curRegIndices=new ArrayList<Integer>();
			while(!stack.isEmpty()){
				int cIdx=stack.pop();
				
				// update region properties
				curRegIndices.add(cIdx);
				regionCnt++;
				int x=cIdx%width;
				int y=cIdx/width;
				com.setLocation(com.getX()+x,com.getY()+y);
				upLeft.setLocation(upLeft.getX()>x ? x : upLeft.getX(),upLeft.getY()>y ? y : upLeft.getY());
				downRight.setLocation(downRight.getX()<x ? x : downRight.getX(),downRight.getY()<y ? y : downRight.getY());
				
				// neighbour search
				int up=cIdx-width;
				int down=cIdx+width;
				int[] nIndices=new int[]{up-1,up,up+1,
										 cIdx-1,cIdx+1,
										 down-1,down,down+1
				};
				for(int nIdx:nIndices){
					if(nIdx<0 || nIdx>=weights.length){
						continue;
					}
					if(weights[nIdx]>0.0f && !visited[nIdx]){
						stack.push(nIdx);
						visited[nIdx]=true;
					}
				}
			}
//			if(regionCnt>=minSize){
//				for(int idx:curRegIndices){
//					shootRegionIndices[idx]=regionNr;
//				}
//			}
			if(regionCnt>=minSize){
				com.setLocation(com.x/regionCnt,com.y/regionCnt);
				Rectangle shBounds=new Rectangle(upLeft.x,upLeft.y,downRight.x-upLeft.x+1,downRight.y-upLeft.y+1);
				byte[] roiMask=new byte[shBounds.width*shBounds.height];
				for(int idx:curRegIndices){
					int mx=(idx%width)-shBounds.x;
					int my=(idx/width)-shBounds.y;
					int mi=my*shBounds.width+mx;
					roiMask[mi]=(byte)255;
				}
				ShootRegion shRegion=new ShootRegion(roiMask,shBounds,com,regionCnt);
				shootRegions.add(shRegion);
			}
//			nRegions=regionNr;
//			regionNr++;
		}
		
		return shootRegions;
	}
	
	private static void minFilter(float[] values,int width,int radius){
		float[] vals=Arrays.copyOf(values,values.length);
		for(int i=0;i<vals.length;++i){
			if(vals[i]==0){
				continue;
			}
			float minValue=Float.MAX_VALUE;

			int up=i-width;
			for(int idx=up-radius;idx<=up+radius;++idx){
				if(idx<0 || idx>=vals.length){
					continue;
				}
				if(vals[idx]<minValue){
					minValue=vals[idx];
				}
			}
			
			for(int idx=i-radius;idx<=i+radius;++idx){
				if(idx<0 || idx>=vals.length){
					continue;
				}
				if(vals[idx]<minValue){
					minValue=vals[idx];
				}
			}
			
			int down=i+width;
			for(int idx=down-radius;idx<=down+radius;++idx){
				if(idx<0 || idx>=vals.length){
					continue;
				}
				if(vals[idx]<minValue){
					minValue=vals[idx];
				}
			}
			values[i]=minValue;
		}
	}

	public int getNRegions() {
		return nRegions;
	}
}
