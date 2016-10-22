package at.ac.oeaw.gmi.brat.math;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import ij.process.ImageProcessor;
import org.apache.commons.collections15.Transformer;

import java.awt.*;
import java.util.*;
import java.util.List;


public class SkeletonSubGraph {
	private List<Point> skeletonPts;
	private List<MyNode> skeletonEndNodes;
	private Rectangle idOffset;
	private Rectangle skeletonBounds;

	private Graph<MyNode,MyLink> graph;
	private Map<Integer,MyNode> nodeMap;
	private static int edgeCount=0;
	
	private List<Point> longestShortestPath;
	
	public SkeletonSubGraph(List<Point> skeletonPts){
		this.skeletonPts=skeletonPts;
	}
	
	public SkeletonSubGraph(ImageProcessor skeletonIp,int foregroundValue){
		this.skeletonPts=new ArrayList<Point>();
		for(int y=0;y<skeletonIp.getHeight();++y){
			for(int x=0;x<skeletonIp.getWidth();++x){
				if(skeletonIp.get(x,y)==foregroundValue){
					skeletonPts.add(new Point(x,y));
				}
			}
		}
	}
	
	public Rectangle getBounds(){
		return skeletonBounds;
	}
	
	public List<Point> getLongestShortestPath(){
		return longestShortestPath;
	}
	
	public boolean contains(Point pt){
		return nodeMap.containsKey(ptToId(pt));
	}
	
	public void createGraph(){
		graph=new DirectedSparseMultigraph<MyNode,MyLink>();
		nodeMap=new HashMap<Integer,MyNode>();
		edgeCount=0;
		
		int minX=Integer.MAX_VALUE;
		int minY=Integer.MAX_VALUE;
		int maxX=Integer.MIN_VALUE;
		int maxY=Integer.MIN_VALUE;
		skeletonEndNodes=new ArrayList<MyNode>();
		for(int i=0;i<skeletonPts.size();++i){
			Point pt=skeletonPts.get(i);
			if(pt.x<minX){
				minX=pt.x;
			}
			if(pt.x>maxX){
				maxX=pt.x;
			}
			if(pt.y<minY){
				minY=pt.y;
			}
			if(pt.y>maxY){
				maxY=pt.y;
			}
			Integer ptIdx=ptToId(pt);
			MyNode ptNode=null;
			if(!nodeMap.containsKey(ptIdx)){
				ptNode=new MyNode(ptIdx);
				nodeMap.put(ptIdx,ptNode);
			}
			else{
				ptNode=nodeMap.get(ptIdx);
			}
			
			List<Point> neighbours=getNeighbours8(pt,skeletonPts);
//			if(neighbours.size()==0){
//				IJ.log("warn");
//			}
			if(neighbours.size()==1){
				skeletonEndNodes.add(ptNode);
			}
			for(Point nb:neighbours){
				Double dist=pt.distance(nb);
				
				Integer nbIdx=ptToId(nb);
				MyNode nbNode=null; 
				if(!nodeMap.containsKey(nbIdx)){
					nbNode=new MyNode(nbIdx);
					nodeMap.put(nbIdx,nbNode);
				}
				else{
					nbNode=nodeMap.get(nbIdx);
				}

				graph.addEdge(new MyLink(dist),ptNode,nbNode);
			}
		}
		int width=maxX-minX+1;
		int height=maxY-minY+1;
		skeletonBounds=new Rectangle(minX,minY,width,height);
	}
	
	public void connectNotConnectedParts(double maxDist){
        Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>() {
            public Double transform(MyLink link) {
                return link.weight;
            }
        };
        DijkstraShortestPath<MyNode,MyLink> alg = new DijkstraShortestPath<MyNode,MyLink>(graph,wtTransformer);
		for(int i=0;i<skeletonEndNodes.size();++i){
//			IJ.log("check end node: "+idToPt(skeletonEndNodes.get(i).getId()));
			for(int j=i+1;j<skeletonEndNodes.size();++j){
				if(alg.getDistance(skeletonEndNodes.get(i),skeletonEndNodes.get(j))==null){
					List<MyNode> nodes1=getConnectedGraph(skeletonEndNodes.get(i));
					List<MyNode> nodes2=getConnectedGraph(skeletonEndNodes.get(j));
					double minDist=maxDist;
					MyNode closestNode1=null;
					MyNode closestNode2=null;
					for(MyNode node1:nodes1){
						for(MyNode node2:nodes2){
							double dist=euclidianNodeDistance(node1,node2);
							if(dist<=minDist){
								minDist=dist;
								closestNode1=node1;
								closestNode2=node2;
							}
						}
					}
					if(closestNode1!=null){
//						IJ.log("added link: "+idToPt(closestNode1.getId())+" -> "+idToPt(closestNode2.getId()));
						graph.addEdge(new MyLink(minDist),closestNode1,closestNode2);
						graph.addEdge(new MyLink(minDist),closestNode2,closestNode1);
					}
				}
			}
		}
	}
	
	public void cleanupGraph(int minGraphNodes){
		for(int i=0;i<skeletonEndNodes.size();++i){
			List<MyNode> graphNodes=getConnectedGraph(skeletonEndNodes.get(i));

			if(graphNodes.size()<minGraphNodes){
				for(MyNode gNode:graphNodes){
					Collection<MyLink> nodeLinks=graph.getIncidentEdges(gNode);
					if(nodeLinks!=null){
						for(MyLink link:nodeLinks){
							graph.removeEdge(link);
						}
					}
					graph.removeVertex(gNode);
				}
			}
		}
	}
	
	public List<Point> getLongestPathContainingPt(Point pt){
		int ptNodeIdx=ptToId(pt);
		MyNode ptNode=null;
		if(nodeMap.containsKey(ptNodeIdx)){
			ptNode=nodeMap.get(ptNodeIdx);
		}
		
		List<MyNode> correspondingEndNodes=new ArrayList<MyNode>();
        Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>() {
            public Double transform(MyLink link) {
                return link.weight;
            }
        };
        DijkstraShortestPath<MyNode,MyLink> alg = new DijkstraShortestPath<MyNode,MyLink>(graph,wtTransformer);
		for(int i=0;i<skeletonEndNodes.size();++i){
			Double dist=(Double)alg.getDistance(ptNode,skeletonEndNodes.get(i));
			if(dist!=null){
				correspondingEndNodes.add(skeletonEndNodes.get(i));
			}
		}

		Double maxLength=0.0;
        MyNode lpStartNode=null;
        MyNode lpEndNode=null;
        for(int i=0;i<correspondingEndNodes.size();++i){
    		MyNode sNode=correspondingEndNodes.get(i);
        	for(int j=i+1;j<correspondingEndNodes.size();++j){
        		MyNode eNode=correspondingEndNodes.get(j);
        		Number dist=alg.getDistance(sNode,eNode);

        		if(dist.doubleValue()>maxLength){
        			maxLength=dist.doubleValue();
        			lpStartNode=sNode;
        			lpEndNode=eNode;
        		}
        	}
        }

        List<Point> longestPath=new ArrayList<Point>();
        if(lpStartNode!=null && lpEndNode!=null){
        	List<MyLink> lpLinks = alg.getPath(lpStartNode,lpEndNode);

        	Collection<MyNode> edgeVerticesColl=graph.getIncidentVertices(lpLinks.get(0));
        	List<MyNode> edgeVertices=new ArrayList<MyNode>(edgeVerticesColl);

        	longestPath.add(idToPt(edgeVertices.get(0).getId()));
        	for(MyLink edge:lpLinks){
        		edgeVerticesColl=graph.getIncidentVertices(edge);
        		edgeVertices=new ArrayList<MyNode>(edgeVerticesColl);

        		longestPath.add(idToPt(edgeVertices.get(1).getId()));
        	}
        }
        else{
        	longestPath=null;
        }

		return longestPath;
	}
	
	
	public List<Point> getShortestPath(Point startPt,Point endPt){
		int startNodeIdx=ptToId(startPt);
		if(!nodeMap.containsKey(startNodeIdx)){
			return null;
		}
		int endNodeIdx=ptToId(endPt);
		if(!nodeMap.containsKey(endNodeIdx)){
			return null;
		}
		
        Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>() {
            public Double transform(MyLink link) {
                return link.weight;
            }
        };
        DijkstraShortestPath<MyNode,MyLink> alg = new DijkstraShortestPath<MyNode,MyLink>(graph,wtTransformer);
		MyNode startNode=nodeMap.get(startNodeIdx);
		MyNode endNode=nodeMap.get(endNodeIdx);
		List<MyLink> pathLinks=alg.getPath(startNode,endNode);
		
		List<Point> path=new ArrayList<Point>();
		List<MyNode> edgeVertices=new ArrayList<MyNode>(graph.getIncidentVertices(pathLinks.get(0)));
		path.add(idToPt(edgeVertices.get(0).getId()));
		for(MyLink edge:pathLinks){
			edgeVertices=new ArrayList<MyNode>(graph.getIncidentVertices(edge));			
			path.add(idToPt(edgeVertices.get(1).getId()));
		}
		
		return path;
	}
	
	public Double getPathLength(Point startPt,Point endPt){
		int startNodeIdx=ptToId(startPt);
		if(!nodeMap.containsKey(startNodeIdx)){
			return null;
		}
		int endNodeIdx=ptToId(endPt);
		if(!nodeMap.containsKey(endNodeIdx)){
			return null;
		}
		
		
        Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>() {
            public Double transform(MyLink link) {
                return link.weight;
            }
        };
        DijkstraShortestPath<MyNode,MyLink> alg = new DijkstraShortestPath<MyNode,MyLink>(graph,wtTransformer);
		MyNode startNode=nodeMap.get(startNodeIdx);
		MyNode endNode=nodeMap.get(endNodeIdx);
		
		return (Double)alg.getDistance(startNode,endNode);
	}
	
	public void findLongestShortestPath(){
        Transformer<MyLink, Double> wtTransformer = new Transformer<MyLink,Double>() {
            public Double transform(MyLink link) {
                return link.weight;
            }
        };
        
        
        DijkstraShortestPath<MyNode,MyLink> alg = new DijkstraShortestPath<MyNode,MyLink>(graph,wtTransformer);
        Double maxLength=0.0;
        MyNode lpStartNode=null;
        MyNode lpEndNode=null;
        for(int i=0;i<skeletonEndNodes.size();++i){
    		MyNode sNode=skeletonEndNodes.get(i);
        	for(int j=i+1;j<skeletonEndNodes.size();++j){
        		MyNode eNode=skeletonEndNodes.get(j);
        		Number dist=alg.getDistance(sNode,eNode);
        		if(dist==null){
        			continue;
        		}
        		if(dist.doubleValue()>maxLength){
        			maxLength=dist.doubleValue();
        			lpStartNode=sNode;
        			lpEndNode=eNode;
        		}
        	}
        }
        
        if(lpStartNode!=null && lpEndNode!=null){
        List<MyLink> lpLinks = alg.getPath(lpStartNode,lpEndNode);

        longestShortestPath=new ArrayList<Point>();
//        ArrayList<Integer> lpPixIds=new ArrayList<Integer>();
        Collection<MyNode> edgeVerticesColl=graph.getIncidentVertices(lpLinks.get(0));
        List<MyNode> edgeVertices=new ArrayList<MyNode>(edgeVerticesColl);
//        lpPixIds.add(edgeVertices.get(0).getId());
        longestShortestPath.add(idToPt(edgeVertices.get(0).getId()));
        for(MyLink edge:lpLinks){
        	edgeVerticesColl=graph.getIncidentVertices(edge);
        	edgeVertices=new ArrayList<MyNode>(edgeVerticesColl);
        	
//        	lpPixIds.add(edgeVertices.get(1).getId());
        	longestShortestPath.add(idToPt(edgeVertices.get(1).getId()));
        }
        }
        else{
        	longestShortestPath=null;
        }
        
	}
	
	
	private List<MyNode> getConnectedGraph(MyNode node){
		List<MyNode> connectedGraph=new ArrayList<MyNode>();
		List<Integer> usedVertices=new ArrayList<Integer>();
		Stack<MyNode> stack=new Stack<MyNode>();
		stack.push(node);
		while(!stack.empty()){
			MyNode curNode=stack.pop();
			usedVertices.add(curNode.getId());
			connectedGraph.add(curNode);
			Collection<MyNode> neighbourNodes=graph.getSuccessors(curNode);
			if(neighbourNodes==null){
				continue;
			}
			for(MyNode sNode:neighbourNodes){
				if(!usedVertices.contains(sNode.getId())){
					stack.push(sNode);
				}
			}
		}
		return connectedGraph;
		
	}
	
	public List<Point> getConnectedGraph(Point pt){
		int ptNodeIdx=ptToId(pt);
		if(!nodeMap.containsKey(ptNodeIdx)){
			return null;
		}
		MyNode ptNode=nodeMap.get(ptNodeIdx);
		List<MyNode> connectedGraphNodes=getConnectedGraph(ptNode);
		List<Point> connectedGraphPts=new ArrayList<Point>();
		
		for(MyNode node:connectedGraphNodes){
			connectedGraphPts.add(idToPt(node.getId()));
		}
		return connectedGraphPts;
		
//		List<Point> connectedGraph=new ArrayList<Point>();
//		List<Integer> usedVertices=new ArrayList<Integer>();
//		Stack<MyNode> stack=new Stack<MyNode>();
//		stack.push(nodeMap.get(ptNodeIdx));
//		while(!stack.empty()){
//			MyNode curNode=stack.pop();
//			usedVertices.add(curNode.getId());
//			connectedGraph.add(idToPt(curNode.getId()));
//			Collection<MyNode> neighbourNodes=graph.getSuccessors(curNode);
//			if(neighbourNodes==null){
//				continue;
//			}
//			for(MyNode node:neighbourNodes){
//				if(!usedVertices.contains(node.getId())){
//					stack.push(node);
//				}
//			}
//		}
//		return connectedGraph;
	}
	
	private Integer ptToId(Point pt){
		return (pt.y+idOffset.y)*idOffset.width+(pt.x+idOffset.x);
	}
	
	private Rectangle getDimensions(List<Point> pixels){
		int minX=Integer.MAX_VALUE;
		int minY=Integer.MAX_VALUE;
		int maxX=Integer.MIN_VALUE;
		int maxY=Integer.MIN_VALUE;
		for(Point pt:pixels){
			if(pt.x<minX){
				minX=pt.x;
			}
			if(pt.x>maxX){
				maxX=pt.x;
			}
			if(pt.y<minY){
				minY=pt.y;
			}
			if(pt.y>maxY){
				maxY=pt.y;
			}
		}
		int width=maxX-minX+1;
		int height=maxY-minY+1;

		return new Rectangle(minX,minY,width,height);
	}

	private List<Point> getNeighbours8(Point pt,List<Point> others){
		List<Point> neighbours=new ArrayList<Point>();
		int curRange=1;
		for(Point other:others){
			if((other.y==pt.y-curRange || other.y==pt.y+curRange) && (other.x==pt.x-curRange || other.x==pt.x || other.x==pt.x+curRange)){
				neighbours.add(other);
			}
			else if(other.y==pt.y && (other.x==pt.x-curRange || other.x==pt.x+curRange)){
				neighbours.add(other);
			}
		}
		return neighbours;
	}

	private Point idToPt(int id){
		int y=id/idOffset.width;
		int x=id%idOffset.width;
		return new Point(x,y);
	}
	
	private Double euclidianNodeDistance(MyNode node1,MyNode node2){
		int y1=node1.getId()/idOffset.width;
		int x1=node1.getId()%idOffset.width;
		int y2=node2.getId()/idOffset.width;
		int x2=node2.getId()%idOffset.width;
		
		return Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
	}
	
	class MyNode {
		private int id; // good coding practice would have this as private
		public MyNode(int id) {
			this.id = id;
		}
		public String toString() { // Always a good idea for debuging
			return "V"+id;
			// JUNG2 makes good use of these.
		}
		public int getId(){
			return id;
		}
	}

	class MyLink {
		//double capacity; // should be private
		private double weight; // should be private for good practice
		int id;
		public MyLink(double weight) {
			this.id = edgeCount++; // This is defined in the outer class.
			this.weight = weight;
			//this.capacity = capacity;
		}
		public String toString() { // Always good for debugging
			return "E"+id;
		}
	}

}
