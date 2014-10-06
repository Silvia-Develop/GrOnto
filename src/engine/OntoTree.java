package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class OntoTree {
	
	// ontology graph structures
	private OntoTreeNode root;
	private ArrayList<OntoTreeNode> nodeList;

	public OntoTree(String ontoTreeFilepath) {
		int mode = 0;
		BufferedReader bis = null;
		
		nodeList = new ArrayList<OntoTreeNode>();
		HashMap<String,OntoTreeNode> parentAndOrphans = new HashMap<String,OntoTreeNode>();
		
		// Build the ontology view tree
	    try {
	    	// Here BufferedInputStream is added for fast reading.
			bis = new BufferedReader(
				  new FileReader(
						  (new File(".")).getCanonicalPath()+File.separator+ontoTreeFilepath
						  )
				  );
			  
		    String str;
		    while ((str = bis.readLine()) != null) {
		    	if (str.charAt(0)=='#'){
			  		continue;
			  	}
				  	
			  	if (str.equals("[nodes]")){
			  		// parse the nodes
			  		mode = 0;
			  		continue;
			  	}
				  	
			  	if (str.equals("[leaves]")){
			  		// parse the instances
			  		mode = 1;
			  		continue;
			  	}

				String attribs = str.split("@")[1].trim().toLowerCase();
				String parentage = str.split("@")[0].trim().toLowerCase();
				StringTokenizer stp = new StringTokenizer(parentage);
				
			  	if (mode==0){
			  		// add an inner node to the tree
			  		String node = stp.nextToken();
					String parent = stp.nextToken();
					
					OntoTreeNode ontoNode = new OntoTreeNode(node, attribs);

					for (OntoTreeNode o : nodeList)
						if ( o.getId().equals(parent) )
							ontoNode.setParent(o);
					
					if (node.equals(parent)){
						root = ontoNode;
						root.setParent(root);
					}
					
					if (ontoNode.getParent()==null)
						parentAndOrphans.put(parent, ontoNode);
					
					//System.out.println("DBG "+ontoNode);
					nodeList.add(ontoNode);
					
				} else if (mode==1){
					// add instance to the tree
				  	String instance = stp.nextToken();
				  	
					while (stp.hasMoreTokens()){
						String parent = stp.nextToken();
						OntoTreeNode ontoInstance = new OntoTreeNode(instance, attribs);
						ontoInstance.setAsLeafe();
						
						for (OntoTreeNode o : nodeList)
							if ( o.getId().equals(parent) )
								ontoInstance.setParent(o);
						
						if (ontoInstance.getParent()==null)
							parentAndOrphans.put(parent, ontoInstance);
						
						nodeList.add(ontoInstance);
					}
			  	}
				  	
		    }
			
		    bis.close();
		} catch (IOException e) {
	      e.printStackTrace();
		}
		
		// resolve orphan nodes
		LinkedList<String> parentsMiss = new LinkedList<String>(parentAndOrphans.keySet());
		while (parentsMiss.size()>0){
			String miss = parentsMiss.pop();
			for (OntoTreeNode o : nodeList)
				if ( o.getId().equals(miss) )
					parentAndOrphans.get(miss).setParent(o);
		}
			
	}
	
	public LinkedList<OntoTreeNode> getFullSubtree(HashSet<OntoTreeNode> activeNodes){
		
		LinkedList<OntoTreeNode> toVisit = new LinkedList<OntoTreeNode>(activeNodes);
		
		// probably could be optimized with an hashSet, but for now i'm more confident with overloaded equals from Strings
		HashMap<String, OntoTreeNode> subTree = new HashMap<String, OntoTreeNode>();
		
		// iterate on the tree visit
		while (toVisit.size() > 0) {
			OntoTreeNode v = toVisit.pop();
			while (true){ 
				if (subTree.containsKey(v.getId()))
					break;
				subTree.put(v.getId(), v);
				v = v.getParent();
			}
		}
		
		return new LinkedList<OntoTreeNode>( subTree.values() );
	}

	public void initNodes(){
		for (OntoTreeNode node : nodeList){
			node.setXml(null);
		}
	}
	
	public LinkedList<OntoTreeNode> fireNodesWithTerms(HashSet<String> terms) {
		LinkedList<OntoTreeNode> activeNodes = new LinkedList<OntoTreeNode>();		
				
		for(OntoTreeNode node: nodeList ){
			// should I consider the node?
			if ( node.activate(terms) ){
				activeNodes.add(node);
				/**
				if ( !node.isLeafe() )
					activeNodes.add(node);
				else
					activeNodes.add(node.getParent());
				**/
			}	
		}
		return activeNodes;
	}
	

	public LinkedList<OntoTreeNode> getPrunedSubtree(HashSet<OntoTreeNode> activeNodes) {
		// TODO re-factor this 
		
		LinkedList<OntoTreeNode> toVisit = new LinkedList<OntoTreeNode>(activeNodes);
		
		// probably could be optimized with an hashSet, but for now i'm more confident with overloaded equals from Strings
		HashMap<String, OntoTreeNode> subTree = new HashMap<String, OntoTreeNode>();
		
		// iterate on the tree visit
		while (toVisit.size() > 0) {
			OntoTreeNode v = toVisit.pop();
			while (true){
				if (subTree.containsKey(v.getId()))
					break;
				subTree.put(v.getId(), v);
				v = v.getParent();
			}
		}
		
		return new LinkedList<OntoTreeNode>( subTree.values() );
	}

}
