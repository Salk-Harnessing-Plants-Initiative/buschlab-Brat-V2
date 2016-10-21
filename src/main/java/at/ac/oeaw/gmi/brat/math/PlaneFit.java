package at.ac.oeaw.gmi.brat.math;

import java.util.List;

import Jama.Matrix;

public class PlaneFit {
		private final int n;
		private Matrix P;
	    private double SSE;
//	    private double SST;
	    private double stdErr;
		
	    private double[] xArr;
	    private double[] yArr;
	    private double[] zArr;
	    
		public PlaneFit(double[] xArr,double[] yArr,double[] zArr){
			n=xArr.length;
			this.xArr=xArr;
			this.yArr=yArr;
			this.zArr=zArr;
//			calc(xArr,yArr,zArr);
		}
		
		public PlaneFit(List<Double> xList,List<Double> yList,List<Double> zList){
			n=xList.size();
			xArr=new double[n];
			yArr=new double[n];
			zArr=new double[n];
			for(int i=0;i<n;++i){
				xArr[i]=xList.get(i);
				yArr[i]=yList.get(i);
				zArr[i]=zList.get(i);
			}
//			calc(xArr,yArr,zArr);
		}
		
		public void calc(){
			double[][] a=new double[3][3];
			double[] b=new double[3];
			
			for(int i=0;i<n;++i){
				double x=xArr[i];
				double y=yArr[i];
				double z=zArr[i];

				double x2=x*x;
				double xy=x*y;
				double y2=y*y;
				double xz=x*z;
				double yz=y*z;
				
				a[0][0]+=x2;
				a[0][1]+=xy;
				a[0][2]+=x;

				a[1][0]+=xy;
				a[1][1]+=y2;
				a[1][2]+=y;

				a[2][0]+=x;
				a[2][1]+=y;
				a[2][2]+=1.0;

				b[0]+=xz;
				b[1]+=yz;
				b[2]+=z;
			}

			Matrix A=new Matrix(a);
			Matrix B=new Matrix(b,3);
			P=A.solve(B);
		}
		
		public double getStdError(){
			SSE=0.0;
			for(int i=0;i<n;++i){
				double x=xArr[i];
				double y=yArr[i];
				double predictedValue=p(0)*x+p(1)*y+p(2);
				double err=zArr[i]-predictedValue;
				SSE+=err*err;
			}
			stdErr=Math.sqrt(SSE/(n-3));
			return stdErr;
		}
		
		public double p(int j){
			return P.get(j,0);
		}
		
		public double[] getCoefficients(){
	    	double[] coeffs=new double[P.getRowDimension()];
	    	for(int i=0;i<P.getRowDimension();++i){
	    		coeffs[i]=p(i);
	    	}
	    	return coeffs;
		}
		
		public double valueAt(double x,double y){
			return p(0)*x+p(1)*y+p(2);
		}
		
		public double diffAt(double x,double y,double z){
			return z-valueAt(x,y);
		}
		
		public String toString(){
			String str="";
			str+=String.format("%.3e*X + ",p(0));
			str+=String.format("%.3e*Y + ",p(1));
			str+=String.format("%.3e",p(2));
			
			return str;
		}
	

}
