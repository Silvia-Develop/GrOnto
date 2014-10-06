package engine;

import java.util.HashSet;

import org.w3c.dom.Element;

public class OntoTreeNode {
	private String id;
	private String activationCondition;
	private OntoTreeNode parent;
	private HashSet<OntoTreeNode> children;
	private int level;
	private boolean isLeafe;
	private Element xml;
	
	public OntoTreeNode(String id, String activationCondition) {
		this.id = id;
		this.activationCondition = activationCondition;
		level = 0;
		isLeafe = false;
		parent = null;
		children = new HashSet<OntoTreeNode>();
		xml = null;
	}
	
	public OntoTreeNode clone(){
		OntoTreeNode c = new OntoTreeNode(this.id, this.activationCondition);
		c.parent = parent;
		c.children.addAll(children);
		c.level = level;
		c.isLeafe = isLeafe;
		c.setXml(xml);
		return c;
	}

	public String getId() {
		return id;
	}

	public String getActivationCondition(){
		return activationCondition;
	}

	public OntoTreeNode getParent() {
		return parent;
	}

	public int getLevel(){
		return level;
	}
	
	public void setAsLeafe(){
		isLeafe = true;
	}
	
	public boolean isLeafe(){
		return isLeafe;
	}
	public void setParent(OntoTreeNode parent) {
		this.parent = parent;
		this.level = parent.level + 1;
		// avoid navigation loops with the root
		if (!parent.getId().equals(id))
			parent.getChildren().add(this);
	}

	public HashSet<OntoTreeNode> getChildren() {
		return children;
	}

	public boolean activate(HashSet<String> attributes){
		String[] andConditions = activationCondition.split(" ");
		
		for (String c : andConditions){
			if (c.charAt(0)=='('){
				//this is an OR condition
				boolean satisfied = false;
				c = c.replace('(', ' ').replace(')', ' ').trim();				
				for (String orCondition : c.split(",")){
					if (attributes.contains(orCondition) == true){
						satisfied = true;
						break;
					}
				}
				
				if (satisfied==false){
					return false;
				}
				
			} else {
				// the condition c is not contained in items
				if (attributes.contains(c)==false){
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean equals(OntoTreeNode otn){
		return otn.getId().equals(this.id);
	}
	
	public String toString(){
		String out = "id:"+id;
		out += ", parent:"+parent.getId();
		out += ", activation:"+activationCondition;
		
		out += ", children:[";
		for (OntoTreeNode n : getChildren() )
			out += " "+n.getId();
		out += " ]";
		
		return out;
	}

	public Element getXml() {
		return xml;
	}

	public void setXml(Element xml) {
		this.xml = xml;
	}

}