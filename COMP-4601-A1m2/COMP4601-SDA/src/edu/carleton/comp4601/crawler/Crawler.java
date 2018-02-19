package edu.carleton.comp4601.crawler;

import edu.carleton.comp4601.graph.PageGraph;
import edu.carleton.comp4601.graph.Vertex;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

import edu.carleton.comp4601.SDA.db.DatabaseManager;


public class Crawler extends WebCrawler{

	PageGraph pageGraph = new PageGraph();
	
     public boolean shouldVisit(Page referringPage, WebURL url) {
    	//prevent off-site visits
         String href = url.getURL().toLowerCase();
         return  true;
     }


     @Override
     public void visit(Page page) {
    	 int docID = page.getWebURL().getDocid();
    	 String url = page.getWebURL().getURL();
         String parentUrl = page.getWebURL().getParentUrl();
         
         System.out.println("URL : " + url);
         System.out.println("DocID : " + docID);
         
         Vertex v = new Vertex (url, page);
         
         DatabaseManager dm = DatabaseManager.getInstance();
         dm.addDocToDb(v.getDoc());
         if (Controller.pageGraph.hasVertex(parentUrl)) {
        	 System.out.println("Has parent url: " + parentUrl);
        	 System.out.println("Current url: " + url);
        	 Controller.pageGraph.connectToExistingVertex(v, parentUrl);
         }
         else {
        	 Controller.pageGraph.addVertex(v);
         }      
     }
}