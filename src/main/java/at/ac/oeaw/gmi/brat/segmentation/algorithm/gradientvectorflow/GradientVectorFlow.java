package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

import ij.ImagePlus;
import ij.process.FloatProcessor;

import java.util.Arrays;

public class GradientVectorFlow {
	public double[][] calculate(final double[] f,final int m,final int n,final double alpha,final double mu,final int iter){
		//normalization
		double max=f[0];
		double min=f[0];
		for(int i=1;i<f.length;++i){
			if(f[i]>max) max=f[i];
			if(f[i]<min) min=f[i];
		}
		if(max==min){
			return null;
		}
		for(int i=0;i<f.length;++i){
			f[i]=(f[i]-min)/(max-min);
		}
		
		//gradient
		double[] f2=MirrorBoundary.expand(f,m,n);
		double[] fx=new double[f2.length];
		double[] fy=new double[f2.length];
		Gradient.calc(f2,fx,fy,m+2,n+2);
		
		
		normalizeGradient(fx, fy);
//		new ImagePlus("f2",new FloatProcessor(m+2,n+2,f2)).show();
//		new ImagePlus("fx",new FloatProcessor(m+2,n+2,fx)).show();
//		new ImagePlus("fy",new FloatProcessor(m+2,n+2,fy)).show();
		
		//initialization
		double[] u=Arrays.copyOf(fx,fx.length);
		double[] v=Arrays.copyOf(fy,fy.length);
		
		//square of magnitude
		double[] sqrMagf=new double[f2.length];
		for(int i=0;i<f2.length;++i){
			sqrMagf[i]=fx[i]*fx[i]+fy[i]*fy[i];
		}
//		new ImagePlus("mag",new FloatProcessor(m+2,n+2,sqrMagf)).show();
//		new ImagePlus("v",new FloatProcessor(m+2,n+2,v)).show();
		
		for(int it=0;it<iter;++it){
			double[] del2u=del2(u,m+2,n+2);
			double[] del2v=del2(v,m+2,n+2);
			
			for(int i=0;i<f2.length;++i){
				u[i]=u[i]+alpha*(mu*del2u[i]-sqrMagf[i]*(u[i]-fx[i]));
				v[i]=v[i]+alpha*(mu*del2v[i]-sqrMagf[i]*(v[i]-fy[i]));
			}
			MirrorBoundary.update(u,m+2,n+2);
			MirrorBoundary.update(v,m+2,n+2);
		}
		
		u=MirrorBoundary.reduce(u,m+2,n+2);
		v=MirrorBoundary.reduce(v,m+2,n+2);

		// absolute values?
//		for(int i=0;i<u.length;++i){
//			if(u[i]<0){
//				u[i]=-u[i];
//			}
//			if(v[i]<0){
//				v[i]=-v[i];
//			}
//		}
		
		double[] mag=new double[u.length];
		double[] dir=new double[u.length];
		for(int i=0;i<u.length;++i){
			mag[i]=Math.sqrt(u[i]*u[i]+v[i]*v[i]);
			dir[i]=Math.atan2(v[i],u[i])*180/Math.PI;
		}
		
//		new ImagePlus("u",new FloatProcessor(m,n,u)).show();
//		new ImagePlus("v",new FloatProcessor(m,n,v)).show();
//		new ImagePlus("mag",new FloatProcessor(m,n,mag)).show();
//		new ImagePlus("dir",new FloatProcessor(m,n,dir)).show();
		
		return new double[][]{u,v};
	}
	
	public void recreateImage(double[] u,double[] v, int m,int n){
		double[] out=new double[u.length];
		
		for(int y=0,i=0;y<n;++y){
			double lineSum=0;
			for(int x=0;x<m;++x,++i){
				if(Math.abs(u[i])>0.1){
					lineSum+=u[i];
				}
				out[i]=lineSum;
			}
		}
		
		for(int x=0;x<m;++x){
			double colSum=0;
			for(int y=0;y<n;++y){
				int i=x+y*m;
				if(Math.abs(v[i])>0.1){
					colSum+=v[i];
				}
				out[i]+=colSum;
			}
		}
		
//		new ImagePlus("recreated",new FloatProcessor(m,n,out)).show();
	}
	
	private void normalizeGradient(double[] fx,double[] fy){
		double[] fabs=new double[fx.length];
		double fabsmax=-Double.MAX_VALUE;
		for (int i = 0; i < fx.length; i++) {
			fabs[i]=Math.sqrt(fx[i]*fx[i]+fy[i]*fy[i]);
			if(fabs[i]>fabsmax){
				fabsmax=fabs[i];
			}
		}
		for (int i = 0; i < fabs.length; i++) {
			fx[i]=fx[i]/fabsmax;
			fy[i]=fy[i]/fabsmax;
		}
	}
	
	private double[] del2(final double[] in,final int m,final int n){
		double[] out=new double[in.length];
		for(int i=1;i<m-1;++i){
//			if(i==0 || i==m-1){
//				continue;
//			}
			for(int j=1;j<n-1;++j){
//				if(j==0 || j==n-1){
//					continue;
//				}
				
				int cIdx=i+j*m;
				out[cIdx]=(in[cIdx+1]+in[cIdx-1]+in[cIdx+m]+in[cIdx-m])-4*in[cIdx];
			}
		}
		
		return out;
	}

	public static void main(String[] args){
		System.out.println("gradient vector flow test.");
	}


}
