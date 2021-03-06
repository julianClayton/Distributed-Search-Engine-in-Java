package edu.carleton.comp4601.SDA.db;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.carleton.comp4601.graph.*;
import edu.carleton.comp4601.networking.Marshaller;
import edu.carleton.comp4601.pagerank.PageRank;

import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import Jama.Matrix;
import edu.carleton.comp4601.crawler.Controller;
import edu.carleton.comp4601.crawler.PageParser;
import edu.carleton.comp4601.dao.Document;
import edu.carleton.comp4601.dao.DocumentCollection;

public class DatabaseManager {
	
	private final String GRAPH_COL = "graph";
	private final String DOC_COL = "documents";
	private final String DOC_NUM_COL = "docnum";

	
	private MongoClient	m;
	private DBCollection col;
	private DB db;
	private static DatabaseManager instance;
	
	public DatabaseManager() {
		instance = this;
		initConnection();

	}

	public boolean graphExists() {
		switchCollection(GRAPH_COL);
		DBCursor cur = col.find().limit(1);
		if (cur.hasNext()) {
			return true;
		}
		return false;
	}
	private int getDocNum() {
		switchCollection(DOC_NUM_COL);
		DBCursor cur = col.find().limit(1);
		int num = 1000;
		if (cur.hasNext()) {
			DBObject obj = cur.next();
			num = (int) obj.get("docnum");
		}
		return num;
	}
	
	public Document findDoc(int docId){
		switchCollection(DOC_COL);
		DBCursor cur = col.find(new BasicDBObject("id", docId));
		DBObject searchObj = null;
		if (cur.hasNext()) {
			searchObj = cur.next();
		}
		DBObject obj = searchObj;
		
		if (obj != null){
			Document document = new Document();
			document.setId((Integer) obj.get("id"));
			document.setUrl((String) obj.get("url"));
			document.setLinks((ArrayList<String>) obj.get("links"));
			document.setTags((ArrayList<String>) obj.get("tags"));
			document.setText((String) obj.get("text"));
			document.setName((String) obj.get("name"));
			return document;
		}
		return null;
	}
	
	public void incrementDocNum() {
		switchCollection(DOC_NUM_COL);
		DBCursor cur = col.find().limit(1);
		int num = 0;
		DBObject obj;
		if (cur.hasNext()) {
			obj = cur.next();
			num = (int) obj.get("docnum");
		}
		col.remove(new BasicDBObject("name","docid"));
		num++;
		DBObject newDocId = BasicDBObjectBuilder.start("name", "docid").add("docnum", num).get();
		col.insert(newDocId);
	}
	
	public void addDocToDb(Document document) {
		incrementDocNum();
		int id = getDocNum();
		document.setId(id);
		DBObject obj = BasicDBObjectBuilder
				.start("name", document.getName())
				.add("id", document.getId())
				.add("url", document.getUrl())
				.add("text", document.getText())
				.add("tags",document.getTags())
				.add("links", document.getLinks())
				.get();
		
		byte[] bytes = getGraphData();
		PageGraph pg = null;
		try {
			pg = (PageGraph) Marshaller.deserializeObject(bytes);
			Vertex v = new Vertex(id,document.getUrl());
			for (String link : document.getLinks()) {
				if (pg.hasVertex(link)){
					pg.connectNewVertexToExistingVertex(v, link);
				}
				else {
					pg.addVertex(v);
				}
			}
			bytes = Marshaller.serializeObject(pg);
		} catch (IOException e) {
			e.printStackTrace();
		}
		addNewGraph(bytes);
		switchCollection(DOC_COL);
		col.save(obj);
		getAllDocuments();
	}
	
	public void addPageToDb(PageParser p) {
		incrementDocNum();
		Document document = p.getDoc();
		int id = p.getID();
		switchCollection(DOC_COL);
		document.setId(id);
		DBObject obj = BasicDBObjectBuilder
				.start("name", document.getName())
				.add("id", document.getId())
				.add("url", document.getUrl())
				.add("text", document.getText())
				.add("tags",document.getTags())
				.add("links", document.getLinks())
				.add("metadata",p.getType())
				.add("timestamp",p.getTime())
				.get();

		col.save(obj);
		getAllDocuments();
	}
	

	public void updateDocLinks(int id, ArrayList<String> links) {
		switchCollection(DOC_COL);
		DBCursor cur = col.find(new BasicDBObject("id", id));
		DBObject obj = null;
		if (cur.hasNext()) {
			obj = cur.next();
		}
		DBObject newObject = obj;
		newObject.put("links", links);
		col.remove(new BasicDBObject("id",id));
		col.save(newObject);
		getAllDocuments();
	}
	public void updateDocTags(int id, ArrayList<String> tags) {
		switchCollection(DOC_COL);
		DBCursor cur = col.find(new BasicDBObject("id", id));
		DBObject obj = null;
		if (cur.hasNext()) {
			obj = cur.next();
		}
		DBObject newObject = obj;
		newObject.put("tags", tags);
		col.remove(new BasicDBObject("id",id));
		col.save(newObject);
		getAllDocuments();
	}
	
	public Document getDocument(int id) {
		switchCollection(DOC_COL);
		DBCursor cur = col.find(new BasicDBObject("id", id));
		DBObject obj = null;
		if (cur.hasNext()) {
			obj = cur.next();
		}
		if (obj == null) {
			return null;
		}
		Document document = new Document();
		document.setId((Integer) obj.get("id"));
		document.setUrl( (String) obj.get("url"));
		document.setLinks((ArrayList<String>) obj.get("links"));
		document.setTags((ArrayList<String>) obj.get("tags"));
		document.setText((String) obj.get("text"));
		document.setName((String) obj.get("name"));
		return document;
		
	}
	public boolean deleteDocument(int id) {
		switchCollection(DOC_COL);
		DBCursor cur = col.find(new BasicDBObject("id", id));
		boolean isFound = false;
		DBObject obj = null;
		if (cur.hasNext()) {
			obj = cur.next();
			isFound = true;
			col.remove(obj);
		}
		getAllDocuments();
		return isFound;
	}
	public boolean deleteDocumentsWithTags(ArrayList<String> tags) {
		switchCollection(DOC_COL);
		BasicDBObject inQuery = new BasicDBObject();
		inQuery.put("tags", new BasicDBObject("$in", tags));
		DBCursor cursor = col.find(inQuery);
		DBObject obj = null;
		boolean docsDeleted = false;
		
		while(cursor.hasNext()) {
			obj = cursor.next();
			col.remove(obj);
			docsDeleted = true;
		}
		getAllDocuments();
		return docsDeleted;
	}

	public boolean deleteAllDocuments() {
		switchCollection(DOC_COL);
		DBCursor cursor = col.find();
		boolean docsDeleted = false;

		while(cursor.hasNext()) {
			col.remove(cursor.next());
			docsDeleted = true;
		}
		getAllDocuments();
		return docsDeleted;
	}

	public DocumentCollection getDocumentsWithTags(ArrayList<String> tags) {
		switchCollection(DOC_COL);
		BasicDBObject inQuery = new BasicDBObject();
		inQuery.put("tags", new BasicDBObject("$in", tags));
		DBCursor cursor = col.find(inQuery);
		DBObject obj = null;
		ArrayList<Document> docs = new ArrayList<Document>();
		DocumentCollection dc = new DocumentCollection();
		while(cursor.hasNext()) {
			obj = cursor.next();
			Document document = new Document();
			document.setId((Integer) obj.get("id"));
			document.setUrl((String) obj.get("url"));
			document.setLinks((ArrayList<String>) obj.get("links"));
			document.setTags((ArrayList<String>) obj.get("tags"));
			document.setText((String) obj.get("text"));
			document.setName((String) obj.get("name"));
			docs.add(document);
		}
		dc.setDocuments(docs);
		return dc;
	}
	
	public ArrayList<Document> getAllDocuments() {
		switchCollection(DOC_COL);
		DBCursor cursor = col.find();
		
		ArrayList<Document> docs = new ArrayList<Document>();
		DBObject obj = null;
		while(cursor.hasNext()) {
			obj = cursor.next();
			Document document = new Document();
			document.setId((Integer) obj.get("id"));
			document.setUrl((String) obj.get("url"));
			document.setLinks((ArrayList<String>) obj.get("links"));
			document.setTags((ArrayList<String>) obj.get("tags"));
			document.setText((String) obj.get("text"));
			document.setName((String) obj.get("name"));
			docs.add(document);
		}
		
		DocumentCollectionWrapper.getInstance().getDocumentCollection().setDocuments(docs); 
		return docs;
	}
	
	public DBCursor getAllDocCursor(){
		switchCollection(DOC_COL);
		BasicDBObject query	= new BasicDBObject();	
		DBCursor cursor = col.find();	
		return cursor;
	}

	
	public static DatabaseManager getInstance() {
		if (instance == null)
			instance = new DatabaseManager();
		return instance;
	}
	
	public static void setInstance(DatabaseManager instance) {
		DatabaseManager.instance = instance;	
	}
	
	
	private void initConnection() {
		try {
			m = new	MongoClient("localhost", 27017);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}	
		db = m.getDB("sda");	
		switchCollection(GRAPH_COL);
	}
	
	private void switchCollection(String collection) {
		col = db.getCollection(collection);
	}
	public void removeOldGraph() {
		if (!graphExists()) {
			return;
		}
		switchCollection(GRAPH_COL);
		DBCursor cur = col.find().limit(1);
		DBObject o = null;
		while(cur.hasNext()) {
			System.out.println("loading graph");
		    o = cur.next();
		}
		col.remove(o);
	}
	
	public synchronized byte[] getGraphData() {

		try {
			BasicDBObject query = new BasicDBObject("name", "test");
			switchCollection(GRAPH_COL);
			DBObject result = col.findOne(query);

			if(result != null) {
				Map<?, ?> graphMap = result.toMap();
				byte[] bytes = (byte[]) graphMap.get("bytes");	
				return bytes;
			}

			return null;
		} catch (MongoException e) {
			System.out.println("MongoException: " + e.getLocalizedMessage());
			return null;
		}
	}
	public boolean dropDocuments() {
		switchCollection(DOC_COL);
		BasicDBObject document = new BasicDBObject();
		col.remove(document);
		DBCursor cursor = col.find();
		boolean success = false;
		while (cursor.hasNext()) {
		    col.remove(cursor.next());
		    success = true;
		}
		getAllDocuments();
		return success;
	}
	public ArrayList<HashMap<Integer, Float>> getAllPageRanks() {
		
		ArrayList<HashMap<Integer, Float>> docsWithRank = PageRank.getInstance().computePageRank();
		
		
		return docsWithRank;
	}
	public synchronized boolean addNewGraph(byte[] graph) {
		removeOldGraph(); 
		try {
			switchCollection(GRAPH_COL);
			BasicDBObject obj = new BasicDBObject();
			obj.put("name", "test");
			obj.put("bytes", graph);
			col.insert(obj);
		} catch (MongoException e) {
			System.out.println("MongoException: " + e.getLocalizedMessage());
			return false;
		}
		
		return true;
}
}
