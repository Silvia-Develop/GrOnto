package engine;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RawQueryExecutor {
	/** original **/
	//private static final String urlHead = "http://boss.yahooapis.com/ysearch/web/v1/";	
	
	private static final String urlHead = "http://yboss.yahooapis.com/ysearch/web?web.q=";
	private static final String urlTail = "&format=xml&abstract=long&style=raw";
	/** original **/
	//private static final String urlTail = 
	//	"?appid=EbcedETV34FzgjZlKM1BHCEorTTB6TkjgvdgMEG9HFGr_WGiMNM4fKiYXEuiPYzNw3QBCo4-&format=xml&count=20&abstract=long&style=raw";
	
	public RawQueryExecutor() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Document yahooBossQuery(String query){
    	
		String wellFormedQuery = query.replaceAll("query=", "");
       	String bossQuery = urlHead+wellFormedQuery+urlTail;
       	System.out.println(bossQuery);
    	
    	// Parse the retrieved docs from xml format
       
       	Document d = null;
       	 
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			d = factory.newDocumentBuilder().parse(bossQuery);
		     //System.out.println("DBG : Fake query executor");
		     //d = factory.newDocumentBuilder().parse(new File("queryquery=sauvignon.xml"));
		     //System.out.println("DBG : XML loaded");
		    //
		} catch (ParserConfigurationException e) {
			System.out.println(e);
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println(e);
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();				
		}

		//return d;
		return addIdsToResults(d);
		
	}
	
	Document addIdsToResults(Document d){
		
		NodeList resultsetWeb = d.getElementsByTagName("result");
		for(int i=0; i < resultsetWeb.getLength(); i++){
			Element el = (Element)resultsetWeb.item(i);
			el.setAttribute("grontoresultid", ""+(i+1));
		}
		
		return d;
	}
	
	
	
}
