package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;

public class Stopword {
	private HashSet<String> stopSet;
	
	public Stopword(String stopwordFilepath) {
		
		stopSet = new HashSet<String>();
		
	    try {
		      // Here BufferedInputStream is added for fast reading.
			  BufferedReader bis  = new BufferedReader(
					  new FileReader(
							  (new File(".")).getCanonicalPath()+File.separator+stopwordFilepath
							  )
					  );
		      String str;
			  while ((str = bis.readLine()) != null) {
				  	stopSet.add(str.trim());
		        }
		      bis.close();

		    } catch (IOException e) {
		      e.printStackTrace();
		    }
	}
	
	public String handleWord(String w){
		if (stopSet.contains(w)){
			return w;
		}
		return null;
	}
	
	public String handleText(String t){
		String stoppedT="";
		
		StringTokenizer st = new StringTokenizer(t);
		while (st.hasMoreTokens()){
			String s = handleWord( st.nextToken() );
			if (s != null){
				stoppedT += " "+s+" ";
			}
		}
		
		return stoppedT.substring(1);
	}

}
