package engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class GrOnto {

	private HtmlOutputFormatter htmlManipulator;
	private OntoVocabulary ov;	
	
	// pre-processing modules 	// Not used yet
	//private Porter po; 
	//private Stopword sw;
	
	//private OntoTreeFilter otf;
	private OntoTree otf;
	
	public GrOnto(String ontologyFile, String stopwordFile, String ontoTermsFile) {
		super();
		htmlManipulator = new HtmlOutputFormatter();
		
		// Not used yet
		//sw = new Stopword(stopwordFile);
		//po = new Porter();
		
		//ov = new OntoVocabulary(ontoTermsFile);
		ov = new OntoVocabulary(""); // Passes everything if ontoTermFile is empty
		
		//otf = new OntoTreeFilter(ontologyFile);
		otf = new OntoTree(ontologyFile);
		
	}

	public Document applyModel(Document d){
		
		otf.initNodes();
		
		// if every result has an ID this should be used as an index, otherwise use an array
		HashMap<Element, HashSet<String> > rawQueryWords = prepareRawResults(d);
		
		// Create the inverted lists of onto terms vs results
		HashMap<OntoTreeNode, LinkedList<Element>> ivt = createInvOntoRes(rawQueryWords);
		
		// get the subtree of the concepts in the ontology (is the cast OK?)
		HashSet<OntoTreeNode> activeNodes = new HashSet<OntoTreeNode>();
		LinkedList<Element> unclDocs = null;
		OntoTreeNode uncl = null;
		for (OntoTreeNode otn : ivt.keySet()){
			if ( otn.getId().equals("unclassified") ){
				unclDocs = ivt.get(otn);
				uncl = otn;
				continue;
			}
			activeNodes.add(otn);
		}
		ivt.remove(uncl);

		LinkedList<OntoTreeNode> parentage = otf.getFullSubtree( activeNodes );

		// finally, prepare an XML document with the hierarchy of concepts and 
		// the results from the original elements
		return generateGrOntoXML(parentage, ivt, unclDocs);
	}
	
	private Document generateGrOntoXML(LinkedList<OntoTreeNode> parentage,
			HashMap<OntoTreeNode, LinkedList<Element>> ivt, LinkedList<Element> unclDocs) {
		
		Document doc = null;	
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
       
        try {
	        DocumentBuilder docBuilder = factory.newDocumentBuilder();
			doc = docBuilder.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

	    Element root = doc.createElement("gronto_results");
	    doc.appendChild(root);

	    // ------------------------------------------
	    //build for each term in the subtree the list of the results
		// get the root of the subtree
	    LinkedList<OntoTreeNode> toVisit = new LinkedList<OntoTreeNode>();
	    
	    OntoTreeNode subTreeRoot = parentage.getFirst();
		for (OntoTreeNode u: parentage)
			if (u.getLevel() < subTreeRoot.getLevel()) 
				subTreeRoot = u;
		toVisit.add(subTreeRoot);

		while (toVisit.size() > 0){
			OntoTreeNode otn = toVisit.pop();
			
			// create the xml element for the node
			LinkedList<Element> postlist;			
			if ( ivt.containsKey(otn) ){
	    		postlist = ivt.get(otn);
	    		//ivt.remove(otn);
			} else {
				postlist = new LinkedList<Element>();
			}
			
    		Element concept = doc.createElement("concept_result_set");
			concept.setAttribute("activation", otn.getActivationCondition());
			concept.setAttribute("cardinality", ""+postlist.size());
			for (Element res : postlist)
				concept.appendChild( doc.importNode( res , true) );

			// organize the node w.r.t. the parent xml
			otn.setXml(concept);
			if ( otn.equals( otn.getParent() )){
				root.appendChild(otn.getXml());
			} else {
				otn.getParent().getXml().appendChild(otn.getXml());
			}
			
			// expand the DFS visit
			for ( OntoTreeNode fronteer : otn.getChildren() )
				if ( parentage.contains(fronteer) )
					toVisit.add(fronteer);
		}
	    
	    // ------------------------------------------
	    //create an XML subtree for unclassified results
	    Element unclConcept = doc.createElement("unclassified_result_set");
		unclConcept.setAttribute("activation", "unclassified");
	    unclConcept.setAttribute("cardinality", ""+unclDocs.size());
		for (Element res : unclDocs)
			unclConcept.appendChild( doc.importNode(res, true) ); 	// see above for notes on import
		
		root.appendChild(unclConcept);
		
		return doc;
		
	}

	private HashMap<Element, HashSet<String> > prepareRawResults(Document d){
		HashMap<Element, HashSet<String> > res = new HashMap<Element, HashSet<String> >();
		Porter pStem = new Porter();
		System.out.println("NOTE: --- Porter Stemmer is active! ---");

		// works only for yahoo xml !!!		
		NodeList resultsetWeb = d.getElementsByTagName("result");
		for(int i=0; i < resultsetWeb.getLength(); i++){
			Element el = (Element)resultsetWeb.item(i);
		
			String corpus = htmlManipulator.decodeUtfText(htmlManipulator.getTextValue(el, "title"))  + 
			" " + htmlManipulator.decodeUtfText(htmlManipulator.getTextValue(el, "abstract"));
		
			corpus.replaceAll("off-dry", "REPoffdry");
			corpus = corpus.toLowerCase().replaceAll("/[.,;:!?-_><)(]", " ");
			corpus.replaceAll("REPoffdry", "off-dry");
				
			HashSet<String> distOntoTerms = ov.handle(corpus);
			
			HashSet<String> distOntoStems = new HashSet<String>();
			for (String s : distOntoTerms){
				String stem = pStem.stripAffixes(s);
				if (stem.length()>0)
					distOntoStems.add(stem);
			}
	
			res.put(el, distOntoStems);		// use stems
			//res.put(el, distOntoTerms);	// use flat terms
		}
		
		return res;
	}
		
	private HashMap<OntoTreeNode, LinkedList<Element>> createInvOntoRes(HashMap<Element,HashSet<String>> resSet){
		HashMap<OntoTreeNode, LinkedList<Element> > ivt = new HashMap<OntoTreeNode, LinkedList<Element>>();
		LinkedList<Element> unclassifiedRes = new LinkedList<Element>();
		
		for (Element resId : resSet.keySet() ){
			LinkedList<OntoTreeNode> ontoNodes = otf.fireNodesWithTerms( resSet.get(resId) );
			
			if (ontoNodes.size()==0){
				// the element is unclassified
				unclassifiedRes.add(resId);
				continue;
			}
			
			LinkedList<Element> posting;
			for (OntoTreeNode node : ontoNodes){
				if ( ivt.containsKey(node) ){
					posting = ivt.get(node);
					if (!posting.contains(resId))
						posting.add(resId);
				} else {
					posting = new LinkedList<Element>();
					posting.add(resId);
					ivt.put(node, posting);
				}
			}
		}
		
		//create a node for unclassified documents
		OntoTreeNode unclassifiedNode = new OntoTreeNode("unclassified", "unclassified");
		unclassifiedNode.setParent(unclassifiedNode);
		
		ivt.put(unclassifiedNode, new LinkedList<Element>(unclassifiedRes) );
		return ivt;		
	}
	
	
}
