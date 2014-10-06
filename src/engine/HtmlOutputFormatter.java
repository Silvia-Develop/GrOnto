package engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class HtmlOutputFormatter {
	private int menuId;
	
	// buffer used during the evaluation phase
	private String measureQueryContent;
	private String measureGrontoContent;
	private HashMap<String, HashSet<String> > measureGrontoHierarchy;
	
	public String getMeasureQueryContent() {
		return measureQueryContent;
	}

	public String getMeasureGrontoContent() {
		return measureGrontoContent;
	}

	public String getMeasureGrontoHierarchy() {
		String txt = "";
		String linesep = System.getProperty("line.separator");

		for ( String cat : measureGrontoHierarchy.keySet() )
			for ( String resId : measureGrontoHierarchy.get(cat) )
				txt += resId + ":" + cat.replaceAll("\"", "") + linesep;
		
		return txt;
	}

	public HtmlOutputFormatter() {
		super();
		menuId = 0;
		measureGrontoHierarchy = new HashMap<String, HashSet<String> >();
	}

	public String getGrontoHtmlResults(Document d){
		
		HashMap<Element, String> ancorRes = new HashMap<Element, String>();
		NodeList resultsetWeb = d.getElementsByTagName("result");
		
		String txt = ""; // for raw text file filling used during evaluation phase
		String linesep = System.getProperty("line.separator");
		HashMap<Element, LinkedList<String> > ancorIdList = new HashMap<Element, LinkedList<String> >();
		HashSet<String> anchorSet = new HashSet<String>();
		
		for(int i=0; i < resultsetWeb.getLength(); i++){
			String body = "";		

			Element el = (Element)resultsetWeb.item(i);
			Element parentEl = (Element)el.getParentNode();
			String intResults = el.getAttribute("grontoresultid");
			
			body += "<hr color=\"#0099CC\" size=\"1px\" width=\"400px\" align=\"left\">"+" - &nbsp;"+intResults+"&nbsp; - ";			
			body += contentHtml("title",el);
			body += "<textarea class=\"result_field\" style=\"width:800px; height:50px;\">"+contentHtml("abstract",el)+"</textarea><br>";
			String url = getTextValue(el, "url");
			
			if (url.length() >= 100) {
				body += "<a href=\""+url+"\">link to the website</a>";
			}else{
				body += "<a href=\""+url+"\">"+url+"</a>";
			}
			
			if (ancorRes.containsKey(parentEl)) {
				ancorRes.put(parentEl, ancorRes.get(parentEl) + body);
			} else {
				ancorRes.put(parentEl, new String(body) );
				ancorIdList.put(parentEl, new LinkedList<String>());
			}
			ancorIdList.get(parentEl).add(intResults);

		}
		
		String bodyResults = "";
		for (Element ancorEl : ancorRes.keySet()){
			String anchor = ancorEl.getAttributes().getNamedItem("activation").toString().split("activation=")[1];			
			bodyResults += "<a name="+anchor+"></a>";
			bodyResults += "<hr color=\"#0099CC\" size=\"0.5px\" width=\"400px\" align=\"left\"><br><b>-"+ anchor +"-</b> &nbsp;-> "+
			"<a href=\"#top\">Top</a>&nbsp;<-<br><hr color=\"#0099CC\" size=\"0.5px\" width=\"400px\" align=\"left\">";
			bodyResults += ancorRes.get(ancorEl);
			
			for ( String resId : ancorIdList.get(ancorEl) ){
				String entry = resId + ":" + anchor.replaceAll("\"", "") + linesep; 
				if (! anchorSet.contains(entry)){
					txt += entry;
					anchorSet.add(entry);
				}
				
			}
		}
		
		// for validation measures
		measureGrontoContent = txt;
		return bodyResults;
	}
	
	public String getGrontoHtmlMenu(Document d){
		Element root = (Element)d.getElementsByTagName("concept_result_set").item(0);
		HashSet<String> distinctDocs = new HashSet<String>();
		String menu = menuHtmlRecur(root, distinctDocs);
		
		/*** unclassified ***/
		Element unclassified = (Element)d.getElementsByTagName("unclassified_result_set").item(0);		
		String card = unclassified.getAttribute("cardinality");
		menu += "<p><a href=\"#unclassified\"><font class=\"result_field\">Unclassified</font>("+card+")</p>"; 
		return menu;
	}
	
	@SuppressWarnings("unchecked")
	private String menuHtmlRecur(Element el, HashSet<String> parentDistinct){
		String body = "";
		NodeList nl = el.getChildNodes();

		if (nl.getLength()>0){
			String subMenu = "";

			for (int i=0; i<nl.getLength(); i++){
				Element elChild = (Element)nl.item(i);
				if (elChild.getNodeName().equals("result")) {
					//String uid = contentHtml("title", elChild ) + contentHtml("abstract", elChild );
					String uid = elChild.getAttribute("grontoresultid");
					parentDistinct.add(uid);
				}
			}

			HashSet<String> distinct = new HashSet<String>();
			HashSet<String> childDistinct = new HashSet<String>();
			
			for (int i=0; i<nl.getLength(); i++){
				Element elChild = (Element)nl.item(i);
				if (elChild.getNodeName().equals("concept_result_set")){
					childDistinct.clear();
					subMenu += menuHtmlRecur(elChild, childDistinct);
					distinct.addAll(childDistinct);
				}
			}

			menuId++;
			body += "\n<table cellpadding=\"1\" cellspacing=\"1\" width=\"288px\">";
			body += "<tr><td width=\"1px\" bgcolor=\"#66CCFF\"></td><td>";
			
			String activeCond = el.getAttribute("activation");
			if (subMenu.length()>0){
				body += "<a id=\"menu"+menuId+"\" href=\"javascript:aprichiudi('"+menuId+"');\">"+"+"+"</a>";
				body += "<a href=\"#"+activeCond+"\"><font class=\"result_field\">"+activeCond+"</font></a>";
				//body += "("+ el.getAttribute("cardinality") +"/"+ distinct.size() +")";
				body += "("+ distinct.size() +")";
				body += "</td></tr></table>\n";
				body += "\n<div id=\""+menuId+"\" style=\"display: none; margin-left: 2em;\">"+subMenu+"</div>";
			} else {
				body += "<a href=\"#"+el.getAttribute("activation")+"\"><font class=\"result_field\">"+el.getAttribute("activation")+"</font></a>";
				if (distinct.size()==0)
					body += "("+ el.getAttribute("cardinality") +")";
				else
					//body += "("+ el.getAttribute("cardinality") +")";
					body += "("+ distinct.size() +"/"+ el.getAttribute("cardinality") +")";
					
				body += "</td></tr></table>";
			}
						
			parentDistinct.addAll(distinct);
			measureGrontoHierarchy.put(activeCond, new HashSet<String>(parentDistinct) );

		}
		
		return body;
	}
	
	public String saveToFile(Document d) {
		// For debug purpose

        TransformerFactory transFactory = TransformerFactory.newInstance(); 
	    Transformer trans = null;
		try {
			trans = transFactory.newTransformer();
        	trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        	trans.setOutputProperty(OutputKeys.INDENT, "yes");
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		}
		
		//create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);

        DOMSource source = new DOMSource(d);
        try {
        	trans.transform(source, result);
        } catch (TransformerException e){
          e.printStackTrace();
        }
        
        // TODO call here the conversion of the XML to an HTML segment
		try {
			File xmlfile= new File("test_results.xml");
			BufferedWriter writer = new BufferedWriter(new FileWriter(xmlfile));
		    writer.write(sw.toString());
		    writer.close();
		} catch (IOException e) {
		    e.printStackTrace();
		} 
	
		return sw.toString();
	}

	public String getQueryOutput(Document d){
		
		String txt = ""; // for raw text file filling used during evaluation phase
		String linesep = System.getProperty("line.separator");
		String table="";
		String head = "<table align=\"center\" cellpadding=\"2px\" cellspacing=\"2px\" border=\"0\" width=\"70%\">"+
		"<tr><td bgcolor=\"#CC0000\">&nbsp;</td><td>";
		
		NodeList resultsetWeb = d.getElementsByTagName("result");
		String body="";
		for(int i=0; i < resultsetWeb.getLength(); i++){
			Element el = (Element)resultsetWeb.item(i);
			String intResults = el.getAttribute("grontoresultid");
			String title = contentHtml("title",el);
			String content = contentHtml("abstract",el);
			
			body += "<hr color=\"#0099CC\" size=\"1px\" width=\"400px\" align=\"left\">"+" -&nbsp;"+intResults+"&nbsp;- ";
			body += title;
			body += content;
			body += contentHtml("date",el);
			body += contentHtml("url",el);
			
			txt += linesep+linesep;
			txt += "# per ogni categoria scrivi il docid ed una categoria separandoli da :" + linesep;
			txt += "#"+ intResults + ":<categoria>"+linesep;
			
			txt += "#title: " + title + linesep + "#abstract: " + linesep + "# ";
			
			String[] lcont = content.split(" ");
			for (int j=0; j<lcont.length; j++){
				txt += lcont[j] + " ";
				if ( j == (lcont.length/2) )
					txt += linesep + "# ";				
			}
			
		}
		table= head+body+"</td><td bgcolor=\"#CC0000\">&nbsp;</td></tr></table>";
		measureQueryContent = txt;
		return table;
	}
	
	private String contentHtml(String field, Element el){
		String content = "";
		String body = "";
		
		if (field.equalsIgnoreCase("date")){
			Date d = getDateValue(el,field);
			if(d!=null){
			body = d+"<br>";}
		}else{
			content = getTextValue(el, field);
			if(!content.equalsIgnoreCase(null)){				
				String c = decodeUtfText(content);				
				if (field.equalsIgnoreCase("title")){
					body = "<font class=\"result_title\">"+ c +"</font><br>";
				}else{
					if (field.equalsIgnoreCase("abstract")){
						body = c;
						//body += "<label>"+c+"</label><br>";
					}
				}
			}
		}
		
		return body;
	}
	
	public String decodeUtfText(String content) {
		String c = "";
		try {
			c = content.replaceAll("</*b>", "");
			c = URLEncoder.encode(c, "UTF-8").replaceAll("%27", "'").replaceAll("%2C", ",").replaceAll("%3B", ";");
			c = c.replaceAll("\\+", " ").replaceAll("%3A", ":").replaceAll("%7C", "|").replaceAll("%25", "%");
			c = c.replaceAll("%2F", "/").replaceAll("%E2%80%94", "-").replaceAll("%28", "(").replaceAll("%29", ")");
			c = c.replaceAll("%26amp%3B*&amp;","&").replaceAll("%C3%AA", "ê").replaceAll("%C3%89", "É").replaceAll("%C3%A9", "é").replaceAll("%C3%A8", "è");
			c = c.replaceAll("%23", "#").replaceAll("%EF%AC%81", "?").replaceAll("%3F", "?").replaceAll("%26amp;", "&");
			c = c.replaceAll("%22", "\"").replaceAll("%E2%80%93", "-").replaceAll("&gt;", ">").replaceAll("%24", "USD");
			c = c.replaceAll("%C3%A7", "ç").replaceAll("%26gt;", ">").replaceAll("%E2%84%A2", "™").replaceAll("%21", "!");
			c = c.replaceAll("%C2%B7", "-");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return c;
	}
	
	public String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getTextContent().toString();
			//textVal = el.getFirstChild().getNodeValue();
		}

		return textVal;
	}
	
	public int getIntValue(Element ele, String tagName) {
		return Integer.parseInt(getTextValue(ele,tagName));
	}
	
	public Date getDateValue(Element ele, String tagName) {
		Date date = null;
		try {
	        date = DateFormat.getDateInstance(DateFormat.DEFAULT).parse(getTextValue(ele,tagName));
	    } catch (ParseException e) {
	    	date = null;
	    }
	    return date;
	}

}
