package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

public class Gradient {
	public static void calc(final double[] in,final double[] fx,final double[] fy,final int m,final int n){
		for(int j=0,cIdx=0;j<n;++j){
			for(int i=0;i<m;++i,++cIdx){
				
				if(fx!=null){
					if(i==0){
						fx[cIdx]=in[cIdx+1]-in[cIdx];
					}
					else if(i==m-1){
						fx[cIdx]=in[cIdx]-in[cIdx-1];
					}
					else{
						fx[cIdx]=0.5*(in[cIdx+1]-in[cIdx-1]);
					}
				}
				
				if(fy!=null){
					if(j==0){
						fy[cIdx]=in[cIdx+m]-in[cIdx];
					}
					else if(j==n-1){
						fy[cIdx]=in[cIdx]-in[cIdx-m];
					}
					else{
						fy[cIdx]=0.5*(in[cIdx+m]-in[cIdx-m]);
					}
				}
				
			}
		}
	}
	
//	public static void calc2(final double[] f, final double[] fx, final double[] fy, final int m, final int n, final int delta){
//		for(int cy=0cy<m;++cy){
//			int y1=(cy-delta) < 0 ? delta-cy : cy-delta;
//			int y2=(cy+delta) >= m ? cy
//		}
//	}
}
