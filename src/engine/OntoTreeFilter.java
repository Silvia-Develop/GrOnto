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

public class OntoTreeFilter {
	// ontology graph structures
	private String root;
	private HashMap<String, String> nodeTree;
	private HashMap<String, ArrayList<String> >leavesLevel;
	private HashMap<String, String> nodeActivationMap;

	// additional support datastruct
	private HashMap<String, Integer> nodeLevelTree; //is it needed?
	private HashMap<String, Integer> nodeVisitCount; //is it needed?
	
	public OntoTreeFilter(String ontoTreeFilepath) {
		BufferedReader bis = null;
		
		nodeTree = new HashMap<String, String>();
		nodeLevelTree = new HashMap<String, Integer>();
		leavesLevel = new HashMap<String, ArrayList<String> >();
		nodeVisitCount = new HashMap<String, Integer>();
		
		nodeActivationMap = new HashMap<String, String>();
		
		int mode = 0;
		
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
			  		// parse the nodes/granules
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
			  		String node = stp.nextToken();
					String parent = stp.nextToken();
						
					// node in the tree
					nodeTree.put(node, parent);
					if (nodeLevelTree.containsKey(parent)){
						nodeLevelTree.put(node, new Integer( nodeLevelTree.get(parent).intValue() + 1) );
					} else {
						nodeLevelTree.put(node, new Integer(0) );
					}
					if (node.equals(parent)){
						root = node;						
					}
					
					// add the node activation attributes
					nodeActivationMap.put(node, attribs);
					
				} else if (mode==1){
					// add instance to the tree
			  		ArrayList<String> parents = new ArrayList<String>();	  		
				  	String instance = stp.nextToken();
					while (stp.hasMoreTokens()){
						// add the parent list
						parents.add( stp.nextToken() );
					}
					leavesLevel.put(instance, parents);
					
					// add node activation pattern
					nodeActivationMap.put(instance, attribs);
					
			  	}
				  	
		    }
			
		    bis.close();
		} catch (IOException e) {
	      e.printStackTrace();
		}
		
	}
	
	public HashMap<String, String> getFullSubtree(HashSet<String> ontoItems){
		
		LinkedList<String> toVisit = initVisitNodes(ontoItems);
		HashMap<String, String> subTree = new HashMap<String, String>();
		
		// init visit count structure
		nodeVisitCount.clear();
		for (String k : nodeTree.keySet() ){
			nodeVisitCount.put( k, new Integer(0) );
		}
			
		// iterate on the tree visit
		while (toVisit.size() > 0) {
			String v = toVisit.pop();
			String parentV = nodeTree.get(v);

			int c = nodeVisitCount.get(v).intValue();
			nodeVisitCount.put(v, c+1);

			// add v and it's parent to the trace
			if (!subTree.containsKey(v)){
				subTree.put(v, nodeTree.get(v));
			}
			
			if (!v.equals(parentV)){
				toVisit.add(parentV);
			}
		}
		
		return subTree;
	}

	public LinkedList<String> initVisitNodes(HashSet<String> ontoItems) {
		LinkedList<String> toVisit = new LinkedList<String>();		

		for(String node: nodeActivationMap.keySet() ){
			// should I consider the node?
			if ( hasProperAttributes(ontoItems, nodeActivationMap.get(node)) ){
				
				// if it is an internal node
				if ( nodeTree.containsKey(node) ) {
					toVisit.add(node);
					continue; 
				}
				
				// is leaf, add all the parents
				if (leavesLevel.containsKey(node)) {
					for ( String parent : leavesLevel.get(node)){
						toVisit.add(parent);
					}
				}	
			}
		}
		
		// removes duplicate nodes
		HashSet<String> singlesToVisit = new HashSet<String>(toVisit);
		toVisit.clear();
		toVisit.addAll(singlesToVisit);
		
		return toVisit;
	}
	
	private boolean hasProperAttributes(HashSet<String> items, String attribs){
		String[] andConditions = attribs.split(" ");
		
		for (String c : andConditions){
			if (c.charAt(0)=='('){
				//this is an OR condition
				boolean satisfied = false;
				c = c.replace('(', ' ').replace(')', ' ').trim();				
				for (String orCondition : c.split(",")){
					if (items.contains(orCondition) == true){
						satisfied = true;
						break;
					}
				}
				
				if (satisfied==false){
					return false;
				}
				
			} else {
				// the condition c is not contained in items
				if (items.contains(c)==false){
					return false;
				}
			}
			
		}
		
		return true;
	}

	@SuppressWarnings("unchecked")
	public HashMap<String, String> getPrunedSubtree(HashSet<String> activeNodes) {
		HashMap<String, String> subTree = new HashMap<String, String>();
		
		for (String v: activeNodes){
			
			HashSet<String> nodesExceptV = (HashSet<String>) activeNodes.clone();
			nodesExceptV.remove(v);
			
			for (String w: nodesExceptV ){
				String parentW = nodeTree.get(w);
				// check if v is an ancestor of w
				// if true add the relation to the subTree
				while (!w.equals(parentW)){
					if (parentW.equals(v)){
						subTree.put(w, v);
						break;
					}
					if (w.equals(root)){
						subTree.put(v, v);
					}
					
					
				}
			}
		}
		return subTree;
	}

}
