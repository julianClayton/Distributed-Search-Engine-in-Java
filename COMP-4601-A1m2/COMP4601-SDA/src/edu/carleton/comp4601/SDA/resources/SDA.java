package edu.carleton.comp4601.SDA.resources;

import java.awt.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.mongodb.MongoException;

import edu.carleton.comp4601.SDA.db.DatabaseManager;
import edu.carleton.comp4601.dao.Document;
import edu.carleton.comp4601.dao.DocumentCollection;
import edu.carleton.comp4601.searching.MyLucene;
import edu.carleton.comp4601.utility.ServiceRegistrar;


@Path("sda")
public class SDA {
	@Context
	UriInfo uriInfo;
	@Context
	Request request;
	private String name;
	
	public SDA() {
		name = "COMP4601 Searchable Document Archive V2.1: Julian and Laura";
	}
	
	@GET
	@Produces(MediaType.TEXT_HTML)
	public String sda2() {
		return "<html><head><title>COMP 4601</title></head><body><h1>"+ name +"</h1></body></html>";
	}

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response newDocument(
			@FormParam("name") String name,
			@FormParam("text") String text,
			@FormParam("tags") String tags,
			@FormParam("links") String links,
			@Context HttpServletResponse servletResponse) throws IOException {
		
		
		ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split("\\s*,\\s*")));		
		ArrayList<String> linksList = new ArrayList<String>(Arrays.asList(links.split("\\s*,\\s*")));
		
		if (tagsList.size() == 0) {
			return Response.status(204).build();
		}
		Document document = new Document();
		document.setName(name);
		document.setText(text);
		document.setLinks(linksList);
		document.setTags(tagsList);
		
		
		try {
			DatabaseManager.getInstance().addDocToDb(document);
			MyLucene.addDocument(document);
		}catch (Exception e) {
			return Response.status(204).build();
		}
		return Response.ok().build();
	}
	

	
	@POST
	@Path("{DOC_ID}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response updateDocumentLinks(
			@PathParam("DOC_ID") String id,
			@FormParam("links") String links,
			@FormParam("tags") String tags) {
		
		DatabaseManager dbm = DatabaseManager.getInstance();
		
		ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split("\\s*,\\s*")));		
		ArrayList<String> linksList = new ArrayList<String>(Arrays.asList(links.split("\\s*,\\s*")));
		
		
		try {
			if (tagsList.size() > 0) {
				dbm.updateDocTags(Integer.parseInt(id), tagsList);
			}
			if (linksList.size() > 0) {
				dbm.updateDocLinks(Integer.parseInt(id), linksList);
			}
			else if (linksList.size() == 0 && tagsList.size() == 0) {
				return Response.status(204).build();
			}
		} catch (Exception e) {
			return Response.status(204).build();
		}
		return Response.ok().build();
	}

	@GET
	@Path("/reset")
	@Produces(MediaType.TEXT_HTML)
	public String reset() {
		boolean reset = DatabaseManager.getInstance().deleteAllDocuments();
		if (reset){
			return "All documents reset";
		}
		return "ERROR: could not remove docs";
	}
	
	@GET
	@Path("{DOC_ID}")	
	@Produces(MediaType.TEXT_HTML)
	public String getDoc(@PathParam("DOC_ID") String id) {
		String regex = "\\d+";
		if (!id.matches(regex)) {
			return resetDocuments(id);
		}
		Document doc = DatabaseManager.getInstance().getDocument(Integer.parseInt(id));
		return "Name: " + doc.getName() + "\n Text: " + doc.getText() + "\n Links: " + doc.getLinks() + "\n Tags: " + doc.getTags();		
	}
	

	
	@DELETE
	@Path("{DOC_ID}")
	public Response deleteDoc(@PathParam("DOC_ID") String id) {
		DatabaseManager dbm = DatabaseManager.getInstance();
		if (dbm.deleteDocument(Integer.parseInt(id))) {
			return Response.ok().build();
		}
		return Response.status(204).build();
	}
	
	@GET 
	@Path("delete/{TAGS}")
	@Produces(MediaType.TEXT_XML)
	public Response deleteDocumentWithTags(@PathParam("TAGS") String tags) {
		DatabaseManager dbm = DatabaseManager.getInstance();
		ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split("\\s*,\\s*")));		
		if (dbm.deleteDocumentsWithTags(tagsList)) {
			return Response.ok().build();

		}
		return Response.status(204).build();
	}
	@GET 
	@Path("search/{TAGS}")
	@Produces(MediaType.TEXT_HTML)
	public String searchDocumentWithTags(@PathParam("TAGS") String tags) {
		DatabaseManager dbm = DatabaseManager.getInstance();
		ArrayList<String> tagsList = new ArrayList<String>(Arrays.asList(tags.split("\\s*,\\s*")));	
		String titleString = "";
		for (String tag : tagsList) {
			titleString = titleString + tag + ", ";
		}
		ArrayList<Document> docs = dbm.getDocumentsWithTags(tagsList);
		String htmlList = "<ul>";
		for (Document doc : docs) {
			String link = "<a href=\"http://localhost:8080/COMP4601-SDA/rest/sda/"+doc.getId() + "\">" + doc.getName() +" </a>";
			htmlList = htmlList +  "<li>" + link + "</li>";
		}
		htmlList = htmlList + "</ul>";
		return "<html><head><title>Document List</title></head><body><h1>Documents with tag(s) " + titleString + "</h1>" + htmlList +"</body></html>";
	}

	@GET 
	@Path("query/{TERMS}")
	@Produces(MediaType.TEXT_HTML)
	public String queryDocsWithTerms(@PathParam("TERMS") String terms) {
	    ArrayList<Document> queryDocs = MyLucene.query(terms);
	    DocumentCollection docs = new DocumentCollection();
	    docs.setDocuments(queryDocs);
	    String htmlList = "<ul>";
		for (Document doc : queryDocs) {
			String link = "<a href=\"http://localhost:8080/COMP4601-SDA/rest/sda/"+doc.getId() + "\">" + doc.getName() +" </a>";
			htmlList = htmlList +  "<li>" + link + "</li>";
		}
		htmlList = htmlList + "</ul>";
		return "<html><head><title>Document List</title></head><body><h1>Documents that match terms(s) " + terms + "</h1>" + htmlList +"</body></html>";
	}
	
	@GET 
	@Path("documents")
	@Produces(MediaType.TEXT_HTML)
	public String getAllDocuments() {
		DatabaseManager dbm = DatabaseManager.getInstance();

		ArrayList<Document> docs = dbm.getAllDocuments();
		String htmlList = "<ul>";
		for (Document doc : docs) {
			String link = "<a href=\"http://localhost:8080/COMP4601-SDA/rest/sda/"+doc.getId() + "\">" + doc.getName() +" </a>";
			htmlList = htmlList +  "<li>" + link + "</li>";
		}
		htmlList = htmlList + "</ul>";
		return "<html><head><title>Document List</title></head><body><h1>All Documents</h1>" + htmlList +"</body></html>";
	}
	
	@GET
	@Path("list")
	@Produces(MediaType.TEXT_HTML)
	public String listDiscoveredServices() {
		String sr = ServiceRegistrar.list();
		return sr;
	}
	private String resetDocuments(String path) {
		if (!path.toLowerCase().equals("reset")) {
			return Response.status(404).build().toString();
		}
		DatabaseManager dbm = DatabaseManager.getInstance();
		try {
			dbm.dropDocuments();
		} catch (MongoException e) {
			return "<html><head><title>Document Reset Failed!</title></head></html>";
		}
		return "<html><head><title>Documents Dropped!</title></head></html>";
	}
	
	@GET
	@Path("pagerank")
	@Produces(MediaType.TEXT_HTML)
	public String getDocPageRanks() {
		DatabaseManager dbm = DatabaseManager.getInstance();
		ArrayList<HashMap<String, Float>> documents = dbm.getAllPageRanks();
		for (HashMap doc : documents) {
			System.out.println(doc.keySet());
			System.out.println(doc.values());
		}
		
		return "";
	}
	
	
	public String sayXML() {
		return "<?xml version=\"1.0\"?>" + "<bank> " + name + " </bank>";
	}
}


