package at.ac.oeaw.gmi.brat.math;

import java.util.List;

import Jama.Matrix;

public class QuadricFit {
	private final int n;
	private Matrix P;
    private double SSE;
//    private double SST;
    private double stdErr;
	
    private double[] xArr;
    private double[] yArr;
    private double[] zArr;
    
	public QuadricFit(double[] xArr,double[] yArr,double[] zArr){
		n=xArr.length;
		this.xArr=xArr;
		this.yArr=yArr;
		this.zArr=zArr;
//		calc(xArr,yArr,zArr);
	}
	
	public QuadricFit(List<Double> xList,List<Double> yList,List<Double> zList){
		n=xList.size();
		xArr=new double[n];
		yArr=new double[n];
		zArr=new double[n];
		for(int i=0;i<n;++i){
			xArr[i]=xList.get(i);
			yArr[i]=yList.get(i);
			zArr[i]=zList.get(i);
		}
//		calc(xArr,yArr,zArr);
	}
	
	public void calc(){
		double[][] a=new double[6][6];
		double[] b=new double[6];
		
		for(int i=0;i<n;++i){
			double x=xArr[i];
			double x2=x*x;
			double x3=x2*x;
			double x4=x3*x;
			double y=yArr[i];
			double y2=y*y;
			double y3=y2*y;
			double y4=y3*y;
			
			a[0][0]+=x4;
			a[0][1]+=x3*y;
			a[0][2]+=x2*y2;
			a[0][3]+=x3;
			a[0][4]+=x2*y;
			a[0][5]+=x2;

			a[1][0]+=x3*y;
			a[1][1]+=x2*y2;
			a[1][2]+=x*y3;
			a[1][3]+=x2*y;
			a[1][4]+=x*y2;
			a[1][5]+=x*y;

			a[2][0]+=x2*y2;
			a[2][1]+=x*y3;
			a[2][2]+=y4;
			a[2][3]+=x*y2;
			a[2][4]+=y3;
			a[2][5]+=y2;

			a[3][0]+=x3;
			a[3][1]+=x2*y;
			a[3][2]+=x*y2;
			a[3][3]+=x2;
			a[3][4]+=x*y;
			a[3][5]+=x;

			a[4][0]+=x2*y;
			a[4][1]+=x*y2;
			a[4][2]+=y3;
			a[4][3]+=x*y;
			a[4][4]+=y2;
			a[4][5]+=y;

			a[5][0]+=x2;
			a[5][1]+=x*y;
			a[5][2]+=y2;
			a[5][3]+=x;
			a[5][4]+=y;
			a[5][5]+=1;

			double z=zArr[i];
			b[0]+=z*x2;
			b[1]+=z*x*y;
			b[2]+=z*y2;
			b[3]+=z*x;
			b[4]+=z*y;
			b[5]+=z;
		}

		Matrix A=new Matrix(a);
		Matrix B=new Matrix(b,6);
		P=A.solve(B);
	}
	
	public double getStdError(){
		SSE=0.0;
		for(int i=0;i<n;++i){
			double x=xArr[i];
			double y=yArr[i];
			double predictedValue=p(0)*x*x+p(1)*x*y+p(2)*y*y+p(3)*x+p(4)*y+p(5);
			double err=zArr[i]-predictedValue;
			SSE+=err*err;
		}
		stdErr=Math.sqrt(SSE/(n-6));
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
		return p(0)*x*x+p(1)*x*y+p(2)*y*y+p(3)*x+p(4)*y+p(5);
	}
	
	public double diffAt(double x,double y,double z){
		return z-valueAt(x,y);
	}
	
	public String toString(){
		String str="";
		str+=String.format("%.3e*X^2 + ",p(0));
		str+=String.format("%.3e*XY + ",p(1));
		str+=String.format("%.3e*Y^2 + ",p(2));
		str+=String.format("%.3e*X + ",p(3));
		str+=String.format("%.3e*Y + ",p(4));
		str+=String.format("%.3e",p(5));
		
		return str;
	}
}
