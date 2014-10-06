package engine;

import java.io.*;

public class HomePageProvider {
	
	private String content;
	
	public HomePageProvider() throws IOException{
		//System.out.println("Read File");
		
	    content = "";
	    System.out.println("Read File......");
	    
	    try {
	      // Here BufferedInputStream is added for fast reading.
		  BufferedReader bis  = new BufferedReader(
				  new FileReader(
						  (new File(".")).getCanonicalPath()+File.separator+"welcome.htm"
						  )
				  );
	      String str;
		  while ((str = bis.readLine()) != null) {
	            content += str;
	        }
	      bis.close();

	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	}

	public String read(){
		return content;
	}

 }
