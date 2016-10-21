package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

public class MirrorBoundary {
	public static double[] expand(final double[] in,final int m,final int n){
		final double[] out=new double[(m+2)*(n+2)];
		out[0]=in[m+1];
		out[m+1]=in[2*m-2];
		out[(n+1)*(m+2)]=in[1+(n-2)*m];
		out[(m+1)+(n+1)*(m+2)]=in[(m-2)+(n-2)*m];
		
		for(int i=1;i<n+1;++i){
			out[i*(m+2)]=in[1+(i-1)*m];
			out[(m+1)+i*(m+2)]=in[(m-2)+(i-1)*m];
		}
		
		for(int i=1;i<m+1;++i){
			out[i]=in[(i-1)+m];
			out[i+(n+1)*(m+2)]=in[(i-1)+(n-2)*m];
		}
		
		for(int i=1;i<m+1;++i){
			for(int j=1;j<n+1;++j){
				out[i+j*(m+2)]=in[i-1+(j-1)*m];
			}
		}
		
		return out;
	}

/**
 * unfinished method for arbitray delta distance
 */
	public static double[] expand2(final double[] in,final int m,final int n,final int delta){
		final int mOut=m+2*delta;
		final int nOut=n+2*delta;
		final double[] out=new double[mOut*nOut];
		
		for(int y=n-delta,iOut=0;y<n+delta;++y){
			if(y<0){
				y=Math.abs(y);
			}
			if(y>=n){
				y=2*n-y-1;
			}
			for(int x=m-delta;x<m+delta;++x,++iOut){
				if(x<0){
					x=Math.abs(x);
				}
				if(x>=m){
					x=2*m-x-1;
				}
				out[iOut]=in[y*m+x];
			}
		}
		return out;
	}
	
	public static double[] reduce(final double[] in,final int m,final int n){
		final double[] out=new double[(m-2)*(n-2)];
		for(int i=0;i<m-2;++i){
			for(int j=0;j<n-2;++j){
				out[i+j*(m-2)]=in[(i+1)+(j+1)*m];
			}
		}
		
		return out;
	}
	
	public static double[] reduce2(final double[] in,final int m,final int n,final int delta){
		final double[] out=new double[(m-delta)*(n-delta)];
		final int mOut=m-delta;
		final int nOut=n-delta;
		for(int y=delta;y<nOut;++y){
			for(int x=delta;x<mOut;++x){
				out[y*mOut+x]=in[y*m+x];
			}
		}
		return out;
	}
	
	public static void update(final double[] in,final int m,final int n){
	    in[0]=in[2+2*m];
	    in[m-1]=in[m-3+2*m];
	    in[(n-1)*m]=in[2+(n-3)*m];
	    in[m-1+(n-1)*m]=in[(m-3)+(n-3)*m];
	    
	    for(int i=1;i<n-1;i++){
	        in[i*m]=in[2+i*m];
	        in[m-1+i*m]=in[m-3+i*m];
	    }
	    for(int i=1;i<m-1;i++){
	        in[i]=in[i+2*m];
	        in[i+(n-1)*m]=in[i+(n-3)*m];
	    }
	}
	
//	public static void update2(final double[] in,final int m,final int n){
//		for(int y=0;y<delta;++y){
//			for(int x=delta;x<m;++x){
//				
//			}
//		}
//	}
}
