package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

import ij.ImagePlus;
import ij.plugin.filter.EDM;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.*;

public class RidgeDetector {
	private int m;
	private int n;
//	private double[] ux; // first partial derivatives or gradient vector flow in x-direction
//	private double[] uy; // first partial derivatives or gradient vector flow in y-direction

	private double[] uxx; // second partial derivatives d^2/dx^2 
	private double[] uxy; // second partial derivatives d^2/dxdy
//	private double[] uyx; // second partial derivatives d^2/dydx //TODO not needed?
	private double[] uyy; // second partial derivatives d^2/dy^2
	private double[] divergence;
	
//	private byte[] divMask;
	private Map<Integer,EigenvalueDecomposition2D> eigenValueDecompositions;
	private List<RidgeRoi> ridgeRois;

	//	double[] F;
//	double[][] eValues;
//	Map<Integer,Matrix> eVectors;
	
//	public double[] getWeightedRigdePixels(){
//		return F;
//	}
//	
//	public Map<Integer,Matrix> getEigenVectors(){
//		return eVectors;
//	}
//	
//	public double[][] getEigenValues(){
//		return eValues;
//	}
	
	public Map<Integer, EigenvalueDecomposition2D> getEigenValueDecompositions() {
		return eigenValueDecompositions;
	}
	
	public List<RidgeRoi> getRidgeRois(){
		return ridgeRois;
	}
	
	public double[] getDivergence(){
		return divergence;
	}
	
	public void calcDerivates(double[] ux, double[] uy,final int m,final int n){
		this.m=m;
		this.n=n;
		
		uxx=new double[ux.length];
		uxy=new double[ux.length];
		Gradient.calc(ux,uxx,uxy,m,n);
		
//		uyx=new double[uy.length];
		uyy=new double[uy.length];
		Gradient.calc(uy,null,uyy,m,n);
	}
	
	public void setDerivates(final double[] uxx,final double[] uxy,final double[] uyy,final int m, final int n){
		this.m=m;
		this.n=n;
		this.uxx=uxx;
		this.uxy=uxy;
		this.uyy=uyy;
	}
	
//	public void calcDivergence(){
//		this.divergence=new double[m*n];
//		for (int i = 0; i < divergence.length; i++) {
//			divergence[i]=uxx[i]+uyy[i];
//		}
////		
////		new ImagePlus("divergence",new FloatProcessor(m,n,divergence)).show();
//	}
	
	public List<Integer> getDivergenceSkeleton(){
		byte[] divMask=new byte[m*n];
		for(int i=0;i<divMask.length;++i){
			double divergence=uxx[i]+uyy[i];
			if(divergence>=0){
				divMask[i]=(byte)255;
			}
		}
		
		ByteProcessor maskIp=new ByteProcessor(m,n,Arrays.copyOf(divMask,divMask.length));
//		new ImagePlus("bin mask skeleton",maskIp.duplicate()).show();
		BinaryProcessor binIp=new BinaryProcessor(maskIp);
		binIp.skeletonize();
		
		List<Integer> skelPixIndices=new ArrayList<Integer>();
		for(int i=0;i<maskIp.getPixelCount();++i){
			if(maskIp.get(i)==0){
				skelPixIndices.add(i);
			}
		}
//		new ImagePlus("div mask",new ByteProcessor(m,n,divMask)).show();
		
		return skelPixIndices;
	}
	
	public void calcDivergence(){
		divergence=new double[uxx.length];
		for(int i=0;i<uxx.length;++i){
			divergence[i]=uxx[i]+uyy[i];
		}
	}
	
	public void calcEigenVectors(List<Integer> posIndices){
		if(posIndices==null){
			posIndices=new ArrayList<Integer>(divergence.length);
			for(int i=0;i<divergence.length;++i){
				if(divergence[i]<0){
					posIndices.add(i);
				}
			}
		}
		ImageProcessor evDirIp=new FloatProcessor(m,n);
		for(int i=0;i<evDirIp.getPixelCount();++i){
			evDirIp.setf(i,Float.NaN);
		}

		eigenValueDecompositions=new HashMap<Integer,EigenvalueDecomposition2D>(posIndices.size());
		for (Integer idx:posIndices) {
			double tmp1=uxx[idx]-uyy[idx];
			double root=Math.sqrt(tmp1*tmp1+4.0*uxy[idx]*uxy[idx]);
			tmp1=uxx[idx]+uyy[idx];
			double[] lambda=new double[] {(tmp1+root)/2.0,(tmp1-root)/2.0};
			if (Math.abs(lambda[0])<Math.abs(lambda[1])) {
				double tmp=lambda[0];
				lambda[0]=lambda[1];
				lambda[1]=tmp;
			}

			tmp1=uxy[idx]*uxy[idx];
			double tmp2=lambda[0]-uxx[idx];
			root=1.0/Math.sqrt(tmp1+tmp2*tmp2);
			double[] ev=new double[2];
			ev[0]=uxy[idx]*root;
			ev[1]=tmp2*root;

//			evDirIp.setf(idx,Float.NaN);

			// TODO second eigenvector not needed because we know it is normal to the first one.
			// tmp2=lambda[1]-uxx[idx];
			// root=1.0/Math.sqrt(tmp1+tmp2*tmp2);
			// ev[1][0]=uxy[idx]*root;
			// ev[1][1]=tmp2*root;

//					evDirIp.setf(idx,(float)(Math.atan2(tmp2,uxy[idx])*180.0/Math.PI));

			EigenvalueDecomposition2D ed=new EigenvalueDecomposition2D();
			ed.setEigenValues(lambda);
			ed.setEigenVector(ev);

			eigenValueDecompositions.put(idx,ed);
			evDirIp.setf(idx,(float)(Math.atan2(-ev[0],ev[1])*180.0/Math.PI));

//					val=Math.abs(lambda[0])*greyIp.getf(idx);
//					if(val>maxVal){
//						maxVal=val;
//					}
		}
//		new ImagePlus("evDir",evDirIp).show();
	}
	
	public ImageProcessor thresholdRidgePixels(ImageProcessor ip,double minDifference){
		ImageProcessor greyIp=ip.duplicate().convertToFloat();
		ImageProcessor binaryIp=new ByteProcessor(m,n);
		
		binaryIp.setColor(255);
		Map<Integer,EigenvalueDecomposition2D> tmpEvDecomp=new HashMap<Integer,EigenvalueDecomposition2D>();
		for(int idx:eigenValueDecompositions.keySet()){
			float centerVal=greyIp.getf(idx);
			int xc=idx%m;
			int yc=idx/m;
			double[] ev=eigenValueDecompositions.get(idx).getFirstEigenVector();
			
			float edgeVal1=centerVal;
			float edgeVal2=centerVal;
			boolean t1fixed=false;
			boolean t2fixed=false;
			int t=1;
			
			int t1=1;
			int t2=1;
			while(!t1fixed && !t2fixed){
				double dx=t*ev[0];
				double dy=t*ev[1];
				double xt=xc+dx;
				double yt=yc+dy;
				
				double ipVal=interpolateBilinear(divergence,m,n,xt,yt);
				if(!t1fixed){
					if(Double.isNaN(ipVal) || ipVal>0){
						t1fixed=true;
						edgeVal1=greyIp.getf((int)xt,(int)yt);
						t1=t;
					}
//					else if(greyIp.getf((int)xt1,(int)yt1)>maxEdge1){
//						maxEdge1=greyIp.getf((int)xt1,(int)yt1);
//					t1=t;
//					}
				}

				xt=xc-dx;
				yt=yc-dy;
				ipVal=interpolateBilinear(divergence,m,n,xt,yt);
				if(!t2fixed){
					if(Double.isNaN(ipVal) || ipVal>0){
						t2fixed=true;
						edgeVal2=greyIp.getf((int)xt,(int)yt);
						t2=t;
					}
//					else if(greyIp.getf((int)xt2,(int)yt2)>maxEdge2){
//						maxEdge2=greyIp.getf((int)xt2,(int)yt2);
//						t2=t;
//					}
				}
				t++;
			}
			
			double diff=Math.max(centerVal-edgeVal1,centerVal-edgeVal2);
			if(diff>=minDifference){
				tmpEvDecomp.put(idx,eigenValueDecompositions.get(idx));
				
				double threshold=centerVal-diff*0.5;
				int rad=Math.max(t1,t2);
				for(int y=yc-rad;y<=yc+rad;++y){
					if(y<0 || y>=n){
						continue;
					}
					
					for(int x=xc-rad;x<=xc+rad;++x){
						if(xc<0 || xc>=m){
							continue;						
						}
						
						if(greyIp.getf(x,y)>threshold){
							binaryIp.set(x,y,255);
						}
					}
					
				}
			}
		}
		
//		binaryIp=binaryIp.convertToColorProcessor();
//		binaryIp.setColor(Color.red);
//		for(int idx:tmpEvDecomp.keySet()){
//			int x=idx%m;
//			int y=idx/m;
//			binaryIp.drawPixel(x, y);
//		}
//		
//		new ImagePlus("outline ip",binaryIp).show();
		eigenValueDecompositions=tmpEvDecomp;
		return binaryIp;
	}
	
//	public void thresholdRidgePixels(ImageProcessor ip,double minDifference){
//		double[] grey=ip.duplicate().convertToFloat().getPixels();
//		for(int i=0;i<divergence.length;++i){
//			greyIp.setf(i,(float)(-divergence[i]*greyIp.getf(i)));
//		}
//		ImageProcessor outlineIp=new ByteProcessor(m,n);
//		outlineIp.setColor(255);
//		Map<Integer,EigenvalueDecomposition2D> tmpEvDecomp=new HashMap<Integer,EigenvalueDecomposition2D>();
//		for(int idx:eigenValueDecompositions.keySet()){
//			float centerVal=greyIp.getf(idx);
//			int xc=idx%m;
//			int yc=idx/m;
//			double[] ev=eigenValueDecompositions.get(idx).getFirstEigenVector();
//			
//			float edgeVal1=centerVal;
//			float edgeVal2=centerVal;
//			boolean t1fixed=false;
//			boolean t2fixed=false;
//			int t=1;
//			int t1=1;
//			int t2=1;
//			while(!t1fixed && !t2fixed){
//				double dx=t*ev[0];
//				double dy=t*ev[1];
//				double xt=xc+dx;
//				double yt=yc+dy;
//				
//				double ipVal=interpolateBilinear(divergence,m,n,xt,yt);
//				if(!t1fixed){
//					if(Double.isNaN(ipVal) || ipVal>0){
//						t1fixed=true;
//						edgeVal1=greyIp.getf((int)xt,(int)yt);
//						t1=t;
//					}
//				}
//
//				xt=xc-dx;
//				yt=yc-dy;
//				ipVal=interpolateBilinear(divergence,m,n,xt,yt);
//				if(!t2fixed){
//					if(Double.isNaN(ipVal) || ipVal>0){
//						t2fixed=true;
//						edgeVal2=greyIp.getf((int)xt,(int)yt);
//						t2=t;
//					}
//				}
//				t++;
//			}
//			
//			int rad=Math.min(t1,t2);
//			double diff=Math.max(centerVal-edgeVal1,centerVal-edgeVal2);
//			if(diff>=minDifference){
//				tmpEvDecomp.put(idx,eigenValueDecompositions.get(idx));
//				outlineIp.set(idx,255);
//			}
//		}
//		
//		outlineIp=outlineIp.convertToColorProcessor();
//		outlineIp.setColor(Color.red);
//		for(int idx:tmpEvDecomp.keySet()){
//			int x=idx%m;
//			int y=idx/m;
//			outlineIp.drawPixel(x, y);
//		}
//		
//		new ImagePlus("outline ip",outlineIp).show();
//		eigenValueDecompositions=tmpEvDecomp;
//	}
	
	public void createOutlines(/*ImageProcessor ip*/){
//		ImageProcessor thIp=ip.convertToFloat();
		ImageProcessor distanceIp=new ByteProcessor(m,n);
//		binaryIp.setColor(255);
//		binaryIp.fill();
		
		for(int i=0;i<divergence.length;++i){
			if(divergence[i]<0){
				distanceIp.set(i,255);
			}
		}
		
		EDM edm=new EDM();
		edm.toEDM(distanceIp);
		
		ImageProcessor binaryIp=new ByteProcessor(m,n);
		binaryIp.setColor(255);
		for(int evIdx:eigenValueDecompositions.keySet()){
			int x=evIdx%m;
			int y=evIdx/m;
			int rad=(int)(distanceIp.get(evIdx)/1.4);
			binaryIp.drawOval(x-rad,y-rad,2*rad,2*rad);
		}
		
//		new ImagePlus("create outline dbg",binaryIp).show();
	}
	
//	public void detectRidgeRois(ImageProcessor valIp){
//		ridgeRois=new ArrayList<RidgeRoi>();
//		boolean[] visited=new boolean[divergence.length];
//		
////		ImageProcessor dbgIp=new FloatProcessor(m,n);
//		Stack<Integer> stack=new Stack<Integer>();
//		for (int i = 0; i < visited.length; i++) {
//			if(!visited[i] && divergence[i]<0){
//				stack.push(i);
//				visited[i]=true;
//				
//				List<Integer> regionIndices=new ArrayList<Integer>();
//				double regionValue=0.0;
//				List<Integer> ambientIndices=new ArrayList<Integer>();
//				double ambientValue=0.0;
//				while(!stack.isEmpty()){
//					int idx=stack.pop();
//					
//					regionIndices.add(idx);
//					regionValue+=valIp.getf(idx);
//					
//					int upRow=idx-m;
//					int downRow=idx+m;
//					int[] neighCandidates=new int[]{upRow-1,upRow,upRow+1,idx-1,idx+1,downRow-1,downRow,downRow+1};
//					for (int ni : neighCandidates) {
//						if(ni<0 || ni>=visited.length){
//							continue;
//						}
//						if(!visited[ni]){
//							if(divergence[ni]<0){
//								stack.push(ni);
//								visited[ni]=true;
//							}
//							else{
//								ambientIndices.add(ni); //TODO replace with simple cnt?
//								ambientValue+=valIp.getf(ni);
//							}
//						}
//					}
//				}
//				
//				regionValue/=regionIndices.size();
//				ambientValue/=ambientIndices.size();
//				if(regionValue>ambientValue+5){ //TODO hardcoded threshold for debugging
//					ridgeRois.add(new RidgeRoi(regionIndices));
////					
////					for (Integer idx : regionIndices) {
////						dbgIp.setf(idx,(float) regionValue);
////					}
//				}
//			}
//		}
////		new ImagePlus("region mean",dbgIp).show();
//	}
	
//	public void detect(double[] ux,double[] uy,final int m,final int n,ImageProcessor greyIp/*List<Integer> greenPixels*/){
////		double[] uxx=new double[ux.length];
////		double[] uxy=new double[ux.length];
////		Gradient.calc(ux,uxx,uxy,m,n);
////		
////		double[] uyx=new double[uy.length];
////		double[] uyy=new double[uy.length];
////		Gradient.calc(uy,uyx,uyy,m,n);
//		
////		ImageProcessor gradIp=new FloatProcessor(m,n);
////		for (int i = 0; i < ux.length; i++) {
////			gradIp.setf(i,(float)Math.sqrt(ux[i]*ux[i]+uy[i]*uy[i]));
////		}
////		new ImagePlus("gradient",gradIp).show();
//		
////		double minDivergence=0;
//		ImageProcessor divMask=new ByteProcessor(m,n);
//		ImageProcessor divIp=new FloatProcessor(m,n);
//		for (int i = 0; i < divIp.getPixelCount(); i++) {
//			double divergence=uxx[i]+uyy[i];
//			if(divergence<0){
//				divMask.setf(i, 255);
////				minDivergence=divergence;
//			}
//			divIp.setf(i, (float)(divergence));
//		}
//		
//		new ImagePlus("div",divIp).show();
//		new ImagePlus("div mask",divMask).show();
//		
//		ImageProcessor divZeroC=new ColorProcessor(m,n);
//		divZeroC.setColor(Color.green);
//		for (int y = 0; y < n; y++) {
//			for (int x = 0; x < m; x++) {
//				if(divIp.get(x,y)<0){
//					boolean zeroc=false;
//					for(int ny=y-1;ny<=y+1;++ny){
//						if(ny<0 || ny>=n){
//							continue;
//						}
//						if(zeroc){
//							break;
//						}
//						for (int nx = x-1; nx <= x+1; nx++) {
//							if(nx<0 || nx>=m){
//								continue;
//							}
//							if(divIp.get(nx,ny)>0){
//								zeroc=true;
//								divZeroC.drawPixel(x, y);;
//								break;
//							}
//						}
//					}
//				}
//			}
//		}
//		divZeroC.setColor(Color.red);
////		
////		ImageProcessor mask=new ByteProcessor(m,n);
////		mask.setColor(255);
////		ImagePlus dbgImp=new ImagePlus("mask",mask);
////		dbgImp.show();
////		Wand wand=new Wand(divZeroC);
////		double numThreshold=m*n*0.10;
////		for (int idx : greenPixels) {
////			if(divZeroC.get(idx)==0 && mask.get(idx)==0){
////				wand.autoOutline(idx%m, idx/m, 0.0, Wand.EIGHT_CONNECTED);
////				Roi roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
////				int boundArea=roi.getBounds().x*roi.getBounds().y;
////				if(boundArea<numThreshold){
////					mask.fill(roi);
////				}
////				mask.draw(roi);
////				dbgImp.updateAndDraw();
//////				new WaitForUserDialog("next").show();
////			}
////		}
////		new ImagePlus("mask",mask).show();
//////		ContrastEnhancer ce=new ContrastEnhancer();
//////		ce.setNormalize(true);
//////		ce.stretchHistogram(divIp, 0.0);
//		
////		divIp.setHistogramSize(256);
////		divIp.setHistogramRange(-0.0002,0.0);
////		long[] histo=divIp.getStatistics().getHistogram();
////		double histoMin=divIp.getHistogramMin();
////		double histoStep=(divIp.getHistogramMax()-histoMin)/divIp.getHistogramSize();
////		double[] yHisto=new double[histo.length];
////		double[] xHisto=new double[histo.length];
////		double[] dHisto=new double[histo.length];
////		yHisto[0]=histo[0];
////		for (int i = 1; i < histo.length; i++) {
////			yHisto[i]+=yHisto[i-1]+histo[i];
////			xHisto[i]=histoMin+i*histoStep;
////		}
////		double yHistoMax=yHisto[yHisto.length-1];
////		for (int i = 1; i < yHisto.length; i++) {
////			dHisto[i]=(yHisto[i]-yHisto[i-1])/(xHisto[i]-xHisto[i-1])*0.0002/yHistoMax;
////		}
////		Plot histoplot=new Plot("histo","v","cnt",xHisto,dHisto);
////		histoplot.show();
//		
////		ImageProcessor dirIp=new FloatProcessor(m,n);
//		eigenValueDecompositions=new HashMap<Integer,EigenvalueDecomposition2D>();
////		List<Integer> indices=new ArrayList<Integer>();
////		List<EigenvalueDecomposition2D> eigDecomp=new ArrayList<EigenvalueDecomposition2D>();
//
////		ImageProcessor evMagIp=new FloatProcessor(m,n);
////		ImageProcessor evDirIp=new FloatProcessor(m,n);
//		double maxVal=-Double.MAX_VALUE;
//		for (int y = 0, i = 0; y < divIp.getHeight(); ++y) {
//			for (int x = 0; x < divIp.getWidth(); ++x,++i) {
//				if(divIp.getf(x,y)<0.0) {
//					double val=divIp.getf(x,y);
//					
//					double tmp1=uxx[i]-uyy[i];
//					double root=Math.sqrt(tmp1*tmp1+4.0*uxy[i]*uxy[i]);
//					tmp1=uxx[i]+uyy[i];
//					double[] lambda=new double[] {(tmp1+root)/2.0,(tmp1-root)/2.0};
//					if (Math.abs(lambda[0])<Math.abs(lambda[1])) {
//						double tmp=lambda[0];
//						lambda[0]=lambda[1];
//						lambda[1]=tmp;
//					}
//					
////					// TODO: Hardcoded threshold
////					if(Math.abs(lambda[1]/lambda[0])>0.5){
////						continue;
////					}
//					tmp1=uxy[i]*uxy[i];
//					double tmp2=lambda[0]-uxx[i];
//					root=1.0/Math.sqrt(tmp1+tmp2*tmp2);
//					double[][] ev=new double[2][2];
//					ev[0][0]=uxy[i]*root;
//					ev[0][1]=tmp2*root;
//
////					evMagIp.setf(i,(float)Math.sqrt(tmp1+tmp2*tmp2));
////					evDirIp.setf(i,(float)(Math.atan2(tmp2,uxy[i])*180.0/Math.PI));
////					dirIp.setf(i, (float)(Math.atan2(ny,nx)*180/Math.PI));
//					double xx=x-ev[0][0];
//					double yy=y-ev[0][1];
//					if(val>divIp.getInterpolatedPixel(xx, yy)){
//						continue;
//					}
//					
//					xx=x+ev[0][0];
//					yy=y+ev[0][1];
//					if(val>divIp.getInterpolatedPixel(xx, yy)){
//						continue;
//					}
//					
//					tmp2=lambda[1]-uxx[i];
//					root=1.0/Math.sqrt(tmp1+tmp2*tmp2);
//					ev[1][0]=uxy[i]*root;
//					ev[1][1]=tmp2*root;
//					
////					evDirIp.setf(i,(float)(Math.atan2(tmp2,uxy[i])*180.0/Math.PI));
//					
//					EigenvalueDecomposition2D ed=new EigenvalueDecomposition2D();
//					ed.setEigenValues(lambda);
//					ed.setEigenVectors(ev);
//					
//					eigenValueDecompositions.put(i,ed);
//
//					val=Math.abs(lambda[0])*greyIp.getf(i);
//					if(val>maxVal){
//						maxVal=val;
//					}
//				} // div<0
//			}
//		}
//
////		new ImagePlus("evMag",evMagIp).show();
////		new ImagePlus("evDir",evDirIp).show();
//		
////		System.out.println("EigenvalueDecomposition finished.");
////		System.out.println("identified "+eigenValueDecompositions.size()+" ridge pixels.");
//////		ImageProcessor dbgIp2=new FloatProcessor(m,n);
////		double vThres=maxVal*0.10;
////		int cnt=0;
//		ImageProcessor dbgIp=new FloatProcessor(m,n);
//		for (Integer idx:eigenValueDecompositions.keySet()) {
//			double[] lambda=eigenValueDecompositions.get(idx).getEigenValues();
//			dbgIp.setf(idx,(float)((1.0-Math.abs(lambda[1]/lambda[0]))*Math.abs(divIp.getf(idx))));
//			divZeroC.drawPixel(idx%m, idx/m);
//		}
//		new ImagePlus("ridge zero cross",divZeroC).show();
////			cnt++;
////			if(cnt%1000 == 0){
////				System.out.print(".");
////			}
////			double[] ev=eigenValueDecompositions.get(idx).getEigenVector(0);
////			
////			boolean done=false;
////			int r=0;
////			int x=idx%m;
////			int y=idx/m;
////			double dDiv=0;
////			while(!done && r<30){ //TODO hardcoded search range
////				int cx1=(int)Math.round(x+r*ev[0]);
////				if(cx1<0 || cx1>=m){
////					done=true;
////					continue;
////				}
////				int cy1=(int)Math.round(y+r*ev[1]);
////				if(cy1<0 || cy1>=m){
////					done=true;
////					continue;
////				}
////				double val1=divIp.getf(cx1,cy1);
////				
////				int cx2=(int)Math.round(cx1+ev[0]);
////				if(cx2<0 || cx2>=m){
////					done=true;
////					continue;
////				}
////				int cy2=(int)Math.round(cx2+ev[1]);
////				if(cy2<0 || cy2>=m){
////					done=true;
////					continue;
////				}
////				double val2=divIp.getf(cx2,cy2);
////				
////				if(val1*val2<=0){
////					dDiv=val2-val1;
////					done=true;
////					continue;
////				}
////				
////				cx1=(int)Math.round(x-r*ev[0]);
////				if(cx1<0 || cx1>=m){
////					done=true;
////					continue;
////				}
////				cy1=(int)Math.round(y-r*ev[1]);
////				if(cy1<0 || cy1>=m){
////					done=true;
////					continue;
////				}
////				val1=divIp.getf(cx1,cy1);
////				
////				
////				cx2=(int)Math.round(cx1-ev[0]);
////				if(cx2<0 || cx2>=m){
////					done=true;
////					continue;
////				}
////				cy2=(int)Math.round(cx2-ev[1]);
////				if(cy2<0 || cy2>=m){
////					done=true;
////					continue;
////				}
////				val2=divIp.getf(cx2,cy2);
////				
////				if(val1*val2<=0){
////					dDiv=val2-val1;
////					done=true;
////					continue;
////				}
////				
////				r++;
////			}
////			
//////			int idx=indices.get(j);{
////			
//////			double[] lambda=eigenValueDecompositions.get(idx).getEigenValues();
//////			double val=Math.abs(lambda[0])*greyIp.getf(idx);
////			
//////			double[][] ev=eigenValueDecompositions.get(idx).getEigenVectors();
//////			int x=idx%m;
//////			int y=idx/m;
//////			int r=1;
////////			int dx=(int)(r*ev[0][0]);
////////			int dy=(int)(r*ev[0][1]);
////////			int xm=x-dx;
////////			int xp=x+dx;
////////			int ym=y-dy;
////////			int yp=y+dy;
//////			while(r<50) {
//////				r++;
//////				int dx=(int)(r*ev[0][0]);
//////				int dy=(int)(r*ev[0][1]);
//////				int xm=x-dx;
//////				if(xm<0 || xm>=m){
//////					break;
//////				}
//////				int xp=x+dx;
//////				if(xp<0 || xp>=m){
//////					break;
//////				}
//////				int ym=y-dy;
//////				if(ym<0 || ym>=n){
//////					break;
//////				}
//////				int yp=y+dy;
//////				if(yp<0 || yp>=n){
//////					break;
//////				}
//////				try{
//////				if(divIp.getf(xm,ym)>=0){
//////					break;
//////				}
//////				if(divIp.getf(xp,yp)>=0){
//////					break;
//////				}
//////				}
//////				catch(Exception e){
//////					System.out.println("error -> xm: "+xm+", xp: "+xp+", ym: "+ym+", yp: "+yp);
//////				}
//////			}
////
////			
//////			if(val>vThres){
////
//////				double angle1=Math.atan(eigenValueDecompositions.get(idx).getEigenVector(1)[1]/eigenValueDecompositions.get(idx).getEigenVector(1)[0]);
//////				double angle2=Math.atan(eigenValueDecompositions.get(idx).getEigenVector(0)[1]/eigenValueDecompositions.get(idx).getEigenVector(0)[0]);
//////				if(angle<0){
//////					angle+=Math.PI;
//////				}
//////				if(greenPixels.contains(idx)){
//////					dbgIp.setColor(Color.green);
//////				}
//////				else{
//////					dbgIp.setColor(Color.white);
//////				}
//////				dbgIp.drawPixel(idx%m, idx/m);
//////				dbgIp.setf(idx,(float)val);//(float)Math.abs(lambda[0]*(divIp.getf(idx)*1.0e6)));//(float)(angle*180/Math.PI));
////				dbgIp.setf(idx,(float)Math.abs(dDiv));//(float)Math.abs(lambda[0]*(divIp.getf(idx)*1.0e6)));//(float)(angle*180/Math.PI));
//////				dbgIp2.setf(idx,(float)angle2);//(float)Math.abs(lambda[1]));
//////			}
////		}
////		
//////		int green=0xff00;
//////		for(int idx:greenPixels){
//////			if(dbgIp.get(idx)==255){
//////				dbgIp.set(idx,green);
//////			}
//////		}
//		new ImagePlus("ridges",dbgIp).show();
////		new ImagePlus("ridges2",dbgIp2).show();
//		new WaitForUserDialog("next").show();
//	}
	
//	public void detect_old(final double[] ux,final double[] uy,final int m,final int n){
//		double[] uxx=new double[ux.length];
//		double[] uxy=new double[ux.length];
//		Gradient.calc(ux,uxx,uxy,m,n);
//		
//		double[] uyx=new double[uy.length];
//		double[] uyy=new double[uy.length];
//		Gradient.calc(uy,uyx,uyy,m,n);
//		
//		F=new double[ux.length];
//		eVectors=new HashMap<Integer,Matrix>();
//		eValues=new double[ux.length][2];
//		
//		double[] div=new double[ux.length];
//		for(int i=0;i<ux.length;++i){
//			div[i]=uxx[i]+uyy[i];
//			if(div[i]>0){
//				div[i]=0;
//			}
//		}
//		
//		new ImagePlus("raw divergence",new FloatProcessor(m,n,div)).show();
//		new WaitForUserDialog("raw div").show();
////		double[] junctions=new double[u.length];
//		for(int i=0;i<ux.length;++i){
////			junctions[i]=Double.NaN;
//			F[i]=0;
//			if(div[i]<0){
//
//				double[][] J=new double[][]{{uxx[i],uyx[i]},{uxy[i],uyy[i]}};
//				Matrix matJ=new Matrix(J);
//				EigenvalueDecomposition e=matJ.eig();
//
//				double[] iLambda=e.getImagEigenvalues();
//				boolean isReal=true;
//				for(int j=0;j<iLambda.length;++j){
//					if(iLambda[j]!=0){
//						isReal=false;
//						break;
//					}
//				}
//				
//				if(isReal){
//					double[] rLambda=e.getRealEigenvalues();
//					Matrix eVec=e.getV(); //new double[][]{{e.getV().get(0,0),e.getV().get(1,0)},{e.getV().get(0,1),e.getV().get(1,1)}};
//					double mag1=Math.sqrt(eVec.get(0,0)*eVec.get(0,0)+eVec.get(1,0)*eVec.get(1,0));
//					double mag2=Math.sqrt(eVec.get(0,1)*eVec.get(0,1)+eVec.get(1,1)*eVec.get(1,1));
//					if(Math.abs(rLambda[0])<Math.abs(rLambda[1])){
//						double tmp=rLambda[0];
//						rLambda[0]=rLambda[1];
//						rLambda[1]=tmp;
//						
//						double[] vTmp={eVec.get(0,0),eVec.get(1,0)};
//						eVec.set(0,0,eVec.get(0,1)/mag2);
//						eVec.set(1,0,eVec.get(1,1)/mag2);
//						eVec.set(0,1,vTmp[0]/mag1);
//						eVec.set(1,1,vTmp[1]/mag1);
//					}
//					else{
//						eVec.set(0,0,eVec.get(0,0)/mag1);
//						eVec.set(1,0,eVec.get(1,0)/mag1);
//						eVec.set(0,1,eVec.get(0,1)/mag2);
//						eVec.set(1,1,eVec.get(1,1)/mag2);
//					}
//
//					F[i]=Math.abs(div[i])*Math.sqrt((rLambda[0]-rLambda[1])*(rLambda[0]-rLambda[1])/(rLambda[0]*rLambda[0]+rLambda[1]*rLambda[1]));
//					eValues[i]=rLambda;
//					
////					if(Math.abs(1-rLambda[1]/rLambda[0])<0.3)
////					junctions[i]=-div[i]*rLambda[1]/rLambda[0];
//					
//					//check if local minimum
//					double x=i%m;
//					double y=i/m;
//					double x1=x+eVec.get(0,0);
//					double y1=y+eVec.get(1,0);
//
//					if(div[i]<interpolateBilinear(div,m,n,x1,y1)){
//						double x2=x-eVec.get(0,0);
//						double y2=y-eVec.get(1,0);
//						if(div[i]<interpolateBilinear(div,m,n,x2,y2)){
//							eVectors.put(i,eVec);
////							eValues[i]=rLambda;
//						}
//					}
//				}
//			}
//		}
//		
////		new ImagePlus("junctions",new FloatProcessor(m,n,junctions)).show();
//		
////		// cleaning inside of cells
////		FloatProcessor divIp=new FloatProcessor(m,n,div);
////		
////		divIp.setColor(-1.0);
////		ImageProcessor mask=new ByteProcessor(m,n);
////		Wand wand=new Wand(divIp);
////		for(int y=0;y<divIp.getHeight();++y){
////			for(int x=0;x<divIp.getWidth();++x){
////				if(divIp.getf(x,y)==0){
////					wand.autoOutline(x,y,0.0,0.0,Wand.EIGHT_CONNECTED);
////					Roi roi=new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
////					divIp.fill(roi);
////					mask.copyBits(roi.getMask(),roi.getBounds().x,roi.getBounds().y,Blitter.COPY_ZERO_TRANSPARENT);
////				}
////			}
////		}
////		wand=null;
////		divIp=null;
////		
////		Iterator<Entry<Integer, Matrix>> it=eVectors.entrySet().iterator();
////		while(it.hasNext()){
////			Entry<Integer,Matrix> e=it.next();
////			if(mask.get(e.getKey())==255){
////				it.remove();
////			}
////		}
//		
//		
////		for(Integer i:eVectors.keySet()){
////			if(mask.get(i)==255){
////				eVectors.remove(i);
////			}
////		}
////		for(int i=0;i<F.length;++i){
////			if(mask.get(i)==255 && F[i]>0){
////				F[i]=0;
////			}
////		}
////		new ImagePlus("mask",mask).show();
////		new ImagePlus("divergence",new FloatProcessor(m,n,div)).show();
//	}
	
//	public void junctions(int w,int h){
//		FloatProcessor ip=new FloatProcessor(w,h);
//		FloatProcessor ip2=new FloatProcessor(w,h);
//		for(Integer idx:eVectors.keySet()){
//			Matrix m=eVectors.get(idx);
////			double mag1=m.get(0,0)*m.get(0,0)+m.get(1,0)*m.get(1,0);
////			double mag2=m.get(0,1)*m.get(0,1)+m.get(1,1)*m.get(1,1);
//			ip.setf(idx,(float)(Math.abs(m.get(0,0)*m.get(0,1)+m.get(1,0)*m.get(1,1))));
//			if(eValues[idx]!=null)
//				ip2.setf(idx,(float)(Math.abs(eValues[idx][1]/eValues[idx][0])));
//		}
//		
//		new ImagePlus("v2/v1",ip).show();
//		for(Integer idx:eVectors.keySet()){
//			if(eValues[idx]!=null)
//				ip2.setf(idx,(float)(Math.abs(eValues[idx][1]/eValues[idx][0])));
//		}
//		
//		new ImagePlus("l2/l1",ip2).show();
//	}
	
	
	private double interpolateBilinear(double[] f,int m,int n, double x,double y){
		if(x<0 || x>m-1 || y<0 || y>n-1){
			return Double.NaN;
		}
		
		double x1=Math.floor(x);
		double x2=Math.ceil(x);
		double y1=Math.floor(y);
		double y2=Math.ceil(y);
		
		int[] idx=new int[]{(int)(x1+y1*m),(int)(x2+y1*m),(int)(x1+y2*m),(int)(x2+y2*m)};

		double dx=x2-x1;
		double dx1=x-x1;
		double dx2=x2-x;
		
		double dy=y2-y1;
		double dy1=y-y1;
		double dy2=y2-y;
		
		return (f[idx[0]]*dx2*dy2+f[idx[1]]*dx1*dy2+f[idx[2]]*dx2*dy1+f[idx[3]]*dx1*dy1)/(dx*dy);
	}
	
//	private double interpolateBilinear(double[] f, double x,double y){
//		if(x<0 || x>width-1 || y<0 || y>height-1){
//			return Double.NaN;
//		}
//		
//		double x1=Math.floor(x);
//		double x2=Math.ceil(x);
//		double y1=Math.floor(y);
//		double y1s=y1*width;
//		double y2=Math.ceil(y);
//		double y2s=y2*width;
//		
//		
//		int[] idx=new int[]{(int)(x1+y1s),(int)(x2+y1s),(int)(x1+y2s),(int)(x2+y2s)};
//
//		double dx=x2-x1;
//		double dx1=x-x1;
//		double dx2=x2-x;
//		
//		double dy=y2-y1;
//		double dy1=y-y1;
//		double dy2=y2-y;
//		
//		return (f[idx[0]]*dx2*dy2+f[idx[1]]*dx1*dy2+f[idx[2]]*dx2*dy1+f[idx[3]]*dx1*dy1)/(dx*dy);
//	}
	
//	public double[] getDivergence(){
//		return divergence;
//	}
	
}
