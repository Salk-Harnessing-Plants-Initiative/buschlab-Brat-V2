package at.ac.oeaw.gmi.brat.math;

public class HistogramCorrelation {
	
	
	public double pearsonCoeff(int[] hist1,int[] hist2){
		int maxH1=0;
		int maxH2=0;
		
		for(int i=0;i<hist1.length;++i){
			if(hist1[i]>maxH1){
				maxH1=hist1[i];
			}
			if(hist2[i]>maxH2){
				maxH2=hist2[i];
			}
		}
		double meanH1=0;
		double meanH2=0;
		double[] h1=new double[hist1.length];
		double[] h2=new double[hist1.length];
		for(int i=0;i<hist1.length;++i){
			h1[i]=(double)hist1[i]/maxH1;
			h2[i]=(double)hist2[i]/maxH2;
			meanH1+=h1[i];
			meanH2+=h2[i];
		}
		meanH1/=hist1.length;
		meanH2/=hist1.length;
		
		double sumH1mH1d2=0;
		double sumH2mH2d2=0;
		double covariance=0;
		for(int i=0;i<h1.length;++i){
			double tmp1=(h1[i]-meanH1);
			sumH1mH1d2+=tmp1*tmp1;
			
			double tmp2=(h2[i]-meanH2);
			sumH2mH2d2+=tmp2*tmp2;
			
			covariance+=tmp1*tmp2;
		}
		
		return covariance/Math.sqrt(sumH1mH1d2*sumH2mH2d2);
	}
	
	public double intersection(int[] hist1,int[] hist2){
		double sumIs=0;
		for(int i=0;i<hist1.length;++i){
			if(hist1[i]<hist2[i]){
				sumIs+=hist1[i];
			}
			else{
				sumIs+=hist2[i];
			}
		}
		return sumIs;
	}
}
