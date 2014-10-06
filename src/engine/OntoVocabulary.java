package engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.StringTokenizer;

public class OntoVocabulary {
	private HashSet<String> ontoSet;

	public OntoVocabulary(String ontoFilepath) {
		ontoSet = new HashSet<String>();
		
		if (ontoFilepath!=null && (!ontoFilepath.equals(""))){			
		    try {
			      // Here BufferedInputStream is added for fast reading.
				  BufferedReader bis  = new BufferedReader(
						  new FileReader(
								  (new File(".")).getCanonicalPath()+File.separator+ontoFilepath
								  )
						  );
			      String str;
				  while ((str = bis.readLine()) != null) {
					  	ontoSet.add(str.trim().toLowerCase());
			        }
			      bis.close();
	
			    } catch (IOException e) {
			      e.printStackTrace();
			    }
		}
	}
	
	public HashSet<String> handle(String t){
		HashSet<String> textSet = new HashSet<String>();
		
		StringTokenizer st = new StringTokenizer(t.toLowerCase());
		while (st.hasMoreTokens())
			textSet.add( st.nextToken() );
		
		if ( ontoSet.size() > 0 )
			textSet.retainAll(ontoSet);
		
		return textSet;
	}

	
}
