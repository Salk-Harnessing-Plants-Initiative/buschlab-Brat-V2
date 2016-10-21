package at.ac.oeaw.gmi.brat.segmentation.algorithm.gradientvectorflow;

public class EigenvalueDecomposition2D {
	double[] eigenValues;
	double[] eigenVector;
	
	public double[] getEigenValues() {
		return eigenValues;
	}
	public void setEigenValues(double[] eigenValues) {
		this.eigenValues = eigenValues;
	}
	public void setEigenValues(double ev1,double ev2) {
		this.eigenValues=new double[] {ev1,ev2};
	}
	
	public double[] getFirstEigenVector() {
		return eigenVector;
	}
	public double[] getSecondEigenVector(){
		return new double[]{-eigenVector[1],eigenVector[0]};
	}
	public void setEigenVector(double[] eigenVector) {
		this.eigenVector = eigenVector;
	}
	public void setEigenVectors(double vx,double vy) {
		this.eigenVector = new double[] {vx,vy};
	}

}
