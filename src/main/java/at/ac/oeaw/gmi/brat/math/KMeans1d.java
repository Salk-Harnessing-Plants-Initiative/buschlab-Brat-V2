package at.ac.oeaw.gmi.brat.math;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class KMeans1d {
	List<Double> data;
	Integer k;
	
	List<Double> centers;
	List<List<Double>> clusteredData;
	public KMeans1d(){
		
	}
	public Integer getK() {
		return k;
	}
	public void setK(Integer k) {
		this.k = k;
	}
	public void setData(List<Double> data) {
		this.data = data;
	}
	
	
	public List<Double> getCenters() {
		if(this.centers!=null){
			Collections.sort(centers);
		}
		return centers;
	}
	public void setCenters(List<Double> centers) {
		this.centers = centers;
	}
	public List<List<Double>> getclusteredData() {
		return clusteredData;
	}
	private void randomCenters(){
		if(this.data.size()<k){
			return;
		}
		Random random=new Random();
		centers=new ArrayList<Double>();
		
		//for(int i=0;i<k;++i){
		while(centers.size()<k){
			int rnd=random.nextInt(this.data.size());
			if(!centers.contains(data.get(rnd)));
				centers.add(new Double(data.get(rnd)));
		}
	}

	private void assignmentStep(){
		clusteredData=new ArrayList<List<Double>>();
		for(int i=0;i<k;++i){
			clusteredData.add(new ArrayList<Double>());
		}
		
		for(int dIdx=0;dIdx<data.size();++dIdx){
			Double pt=data.get(dIdx);
			int assignIdx=0;

			double minDist=Double.MAX_VALUE;
			for(int cIdx=0;cIdx<centers.size();++cIdx){
				Double ct=centers.get(cIdx);
				double dist=Math.abs(pt-ct);
				if(dist<minDist){
					minDist=dist;
					assignIdx=cIdx;
				}
			}
			clusteredData.get(assignIdx).add(pt);
		}
	}

	private boolean updateMeanStep(){
		List<Double> newCenters=new ArrayList<Double>();
		for(int i=0;i<centers.size();++i){
			newCenters.add(new Double(0));
		}
		
		boolean changed=false;

		for(int i=0;i<clusteredData.size();++i){
			List<Double> ci=clusteredData.get(i);
			double sum=0;
			for(int j=0;j<ci.size();++j){
				sum+=ci.get(j);
			}
			newCenters.set(i,sum/ci.size());
			
			if(newCenters.get(i)!=centers.get(i))
				changed=true;
			
			centers.set(i,newCenters.get(i));
		}

		return changed;
	}
	
	public boolean updateMedianStep(){
		List<Double> newCenters=new ArrayList<Double>();
		for(int i=0;i<centers.size();++i){
			newCenters.add(new Double(0));
		}
		
		boolean changed=false;

		for(int i=0;i<clusteredData.size();++i){
			List<Double> ci=clusteredData.get(i);
			double tmp;
			Collections.sort(ci);
			newCenters.set(i,ci.get(ci.size()/2));
			
			if(newCenters.get(i)!=centers.get(i))
				changed=true;
			
			centers.set(i,newCenters.get(i));
		}

		return changed;
		
		
	}
	
	public void cluster(){
		if(centers==null)
			randomCenters();
		if(centers==null){
			return; // could not assign centers
		}
		
		boolean changed=true;
		while(changed){
			assignmentStep();
			boolean proceed=false;
			while(!proceed){
				proceed=true;
				for(int i=0;i<clusteredData.size();++i){
					List<Double> ci=clusteredData.get(i);
					if(ci.size()==0){
						proceed=false;
						Random random=new Random();
						int rnd=random.nextInt(this.data.size());
						centers.set(i,new Double(data.get(rnd)));
						assignmentStep();
					}
				}
			}
			changed=updateMedianStep();
		}
	}
}
