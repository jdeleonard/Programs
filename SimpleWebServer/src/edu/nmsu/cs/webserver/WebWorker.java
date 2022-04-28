package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.TimeZone;
import java.util.*;
import java.io.File;

public class WebWorker implements Runnable
{

	private Socket socket;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			String filename = readHTTPRequest(is);
			String contentType = getContentType(filename);
			boolean issueFound = writeHTTPHeader(os, contentType, filename);
			System.out.println(getContentType(filename));
			writeContent(os, issueFound, filename,contentType);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is)
	{
		String line;
      ArrayList<String> request = new ArrayList<String>();
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();
            request.add(line);
				System.err.println("Request line: (" + line + ")");
				if (line.length() == 0)
					break;
			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
      String filename = request.get(0);
      int index = filename.indexOf("/");
      filename = filename.substring(index);
      int index2 = filename.indexOf(" ");
      filename = filename.substring(1, index2);
      
		return filename;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private boolean writeHTTPHeader(OutputStream os, String contentType, String filename) throws Exception
	{  
      boolean issueFound = true;
      	File requestFile = new File(filename);
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		if(filename.equals("") || requestFile.exists()){
			os.write("HTTP/1.1 200 OK\n".getBytes());
			issueFound = false;
		}
		else{
			os.write("HTTP/1.1 404 NOT FOUND \n".getBytes());
			issueFound = true;
		}
         
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return issueFound;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, boolean issueFound, String filename, String contentType) throws Exception
	{
		LocalDate date = LocalDate.now();
		if(issueFound){
			 os.write("<html><head></head><body>\n".getBytes());
			 os.write("<h3>404 NOT FOUND</h3>\n".getBytes());
			 os.write("</body></html>\n".getBytes());
		}
		else if (contentType.equals("image/gif")) {
			System.out.print("++++++IN+++++++");
			File img = new File(filename);
			byte[] imgData = Files.readAllBytes(img.toPath());
			os.write(imgData);
		}
		else if(1 == 1){
	         if(filename.equals("")){
	      		os.write("<html><head></head><body>\n".getBytes());
	      		os.write("<h3>My web server works!</h3>\n".getBytes());
	      		os.write("</body></html>\n".getBytes());
         }
         
     else {
     BufferedReader reader = new BufferedReader(new FileReader(filename));
      String line = "";
      
      while((line = reader.readLine()) != null){
    	 if(line.indexOf("<cs371date>") != -1) {
    		 line = line.replaceAll("<cs371date>", LocalDate.now().toString());
    	 }
    	 if(line.indexOf("<cs371server>") != -1){
    		 line = line.replaceAll("<cs371server>", "James's Server");
    	 }
         os.write(line.getBytes());
      }
      }
   }  
}


	public String getContentType(String filename){

		if(filename.equals("") || filename.indexOf(".") == -1) {
			return "text/html";
		}
		
		File f = new File(filename);
		
		if(f.exists()) {
			filename = filename.substring(filename.indexOf("."));
		
			if (filename.equals(".gif")){
				return "image/gif";
			}
			if (filename.equals(".png")){
				return "image/png";
			}
			if (filename.equals(".jpg")){
				return "image/jpg";
			}
		
		}
		
		return "text/html";

	}



} // end class
