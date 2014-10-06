package engine;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

// EbcedETV34FzgjZlKM1BHCEorTTB6TkjgvdgMEG9HFGr_WGiMNM4fKiYXEuiPYzNw3QBCo4-

public class RunEngine {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		InetSocketAddress addr = new InetSocketAddress(8080);
	    HttpServer server;
	    
		try {
			server = HttpServer.create(addr, 0);
	     	server.createContext("/gronto", new GrontoHandler());
	     	server.setExecutor(Executors.newCachedThreadPool()); // creates a default executor
			server.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Server is listening on port 8080" );
		System.out.println ("Current dir : " + (new File(".")).getCanonicalPath());

	}

}
