package edu.carleton.comp4601.searching;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.carleton.comp4601.SDA.db.DatabaseManager;

public class MyLucene {

	private static final String INDEX_DIR = FileSystems.getDefault().getPath("").toAbsolutePath().toString() + "/Lucene";
	private static FSDirectory dir;
	private static IndexWriter	writer;
	
	private static final String URL = "url";
	private static final String DOC_ID = "docId";
	private static final String DATE = "date";
	private static final String CONTENT = "content";
	private static final String METADATA = "metadata";
	

	public static void indexLucene(DBCursor cursor){

	try	{	
		dir	=	FSDirectory.open(new File(INDEX_DIR));	
		Analyzer	analyzer	=	new	StandardAnalyzer(Version.LUCENE_45);	
		IndexWriterConfig iwc	=	new	IndexWriterConfig(Version.LUCENE_45, analyzer);	
		iwc.setOpenMode(OpenMode.CREATE);	
		writer = new IndexWriter(dir, iwc);	
		
		while(cursor.hasNext()){	
			indexADoc(cursor.next());	
		}
		
	} catch	(Exception	e)	{	
		e.printStackTrace();	
		}	finally	{	
			try	{	
			 	if	(writer	!=	null)	{	
					writer.close();	
			 	}
			 	if	(dir	!=	null)	{
					dir.close();	
			 	}
			 } catch (IOException	e)	{	
					e.printStackTrace();	
			 	
			 }
		}	
	}
	
	private static void indexADoc(DBObject object) throws IOException	{	
		
		try{
			Document lucDoc	=	new	Document();	
			
			
			String docId = object.get("id").toString();
			String url = (String)object.get("url");
			String text = (String)object.get("text");
			String type = (String)object.get("metadata");
			String ts = (String)object.get("timestamp");
		
			//System.out.println("Id" + docId + "\nurl: " + url + "\ntext " + text + "\ntype:'" + type + "\ndate:" + ts);
			lucDoc.add(new	StringField(DOC_ID, docId, Field.Store.YES));	
			lucDoc.add(new	StringField(URL, url, Field.Store.YES));
			lucDoc.add(new TextField(CONTENT, (String)object.get("text"), Field.Store.YES));
			lucDoc.add(new	StringField(DATE, ts, Field.Store.YES));
			lucDoc.add(new	StringField(METADATA, type, Field.Store.YES));
		
	
			writer.addDocument(lucDoc);
			
		}catch(Exception e){
			System.out.println("-------Error:  "+e);	
		}
	}
	
	
	public static void reindexLucene(DBCursor cursor, HashMap<Integer, Float> hm){
		
		try	{	
			dir	=	FSDirectory.open(new File(INDEX_DIR));	
			Analyzer	analyzer	=	new	StandardAnalyzer(Version.LUCENE_45);	
			IndexWriterConfig iwc	=	new	IndexWriterConfig(Version.LUCENE_45, analyzer);	
			iwc.setOpenMode(OpenMode.CREATE);	
			writer = new IndexWriter(dir, iwc);	
			
			while(cursor.hasNext()){	
				DBObject ob = cursor.next();
			    int id = (int) ob.get("id");
				float score =  hm.get(id);
				boostADoc(ob, score);	
			}
			
		} catch	(Exception	e)	{	
			e.printStackTrace();	
			}	finally	{	
				try	{	
				 	if	(writer	!=	null)	{	
						writer.close();	
				 	}
				 	if	(dir	!=	null)	{
						dir.close();	
				 	}
				 } catch (IOException	e)	{	
						e.printStackTrace();	
				 	
				 }
			}	
		}
		
		
	
		public static void resetBoostLucene(DBCursor cursor, Float score){
		
			try	{	
				dir	=	FSDirectory.open(new File(INDEX_DIR));	
				Analyzer	analyzer	=	new	StandardAnalyzer(Version.LUCENE_45);	
				IndexWriterConfig iwc	=	new	IndexWriterConfig(Version.LUCENE_45, analyzer);	
				iwc.setOpenMode(OpenMode.CREATE);	
				writer = new IndexWriter(dir, iwc);	
				
				while(cursor.hasNext()){				
					boostADoc(cursor.next(), score);	
				}
				
			} catch	(Exception	e)	{	
				e.printStackTrace();	
				}	finally	{	
					try	{	
					 	if	(writer	!=	null)	{	
							writer.close();	
					 	}
					 	if	(dir	!=	null)	{
							dir.close();	
					 	}
					 } catch (IOException	e)	{	
							e.printStackTrace();	
					 	
					 }
				}	
		}

		
		private static void boostADoc(DBObject object, Float score) throws IOException	{	
			
			try{
				Document lucDoc	=	new	Document();	
				
				int id = (int) object.get("id");
				//float score =  hm.get(id);
				System.out.println("applying boost " + score + " to doc " + id);
				
				FieldType myStringType = new FieldType(StringField.TYPE_STORED);
				myStringType.setOmitNorms(false);

				Field docId = new Field(DOC_ID, object.get("id").toString(), myStringType);
				Field url = new Field(URL, object.get("url").toString(),  myStringType);
				TextField content = new TextField(CONTENT, (String)object.get("text"), Field.Store.YES);
				Field metadata = new Field(METADATA, object.get("metadata").toString(),  myStringType);
				Field ts = new Field(DATE, object.get("timestamp").toString(),  myStringType);
				
				//System.out.println("Id" + docId + "\nurl: " + url + "\ntext " + content + "\ntype:'" + metadata + "\ndate:" + ts);
				
				docId.setBoost(score);
				url.setBoost(score);
				content.setBoost(score);
				url.setBoost(score);
				ts.setBoost(score);
				
				System.out.println("boost is: " + url.boost());
				
				lucDoc.add(docId);	
				lucDoc.add(url);
				lucDoc.add(content);
				lucDoc.add(metadata);
				lucDoc.add(ts);
		
			    Term term = new Term("id", (object.get("id").toString()));
				writer.updateDocument(term, lucDoc);
				
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("-------Error:  " + e);	
			}
		}
	
	
	public	static ArrayList<edu.carleton.comp4601.dao.Document> query(String searchStr)	{	
		try	{	
		    IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(INDEX_DIR)));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_45);
			QueryParser parser = new QueryParser(Version.LUCENE_45, CONTENT, analyzer);
			Query q = parser.parse(searchStr);
			TopDocs results = searcher.search(q, 100);
		 	
		 	ScoreDoc[]	hits =	results.scoreDocs;
		 	
		 	ArrayList<edu.carleton.comp4601.dao.Document> docs = new ArrayList<edu.carleton.comp4601.dao.Document>();	
			
		 	for	(ScoreDoc hit :	hits)	{	
			 	Document indexDoc = searcher.doc(hit.doc);	
			 	String id = indexDoc.get(DOC_ID);
			 	
			 	if	(id	!=	null) {	
				 	edu.carleton.comp4601.dao.Document d = DatabaseManager.getInstance().findDoc(Integer.valueOf(id));	
					if	(d	!=	null)	{	
						System.out.print(d.getId());
						d.setScore(hit.score);	
						docs.add(d);	
					}	
				 } 		
		 	}
		 	reader.close();
		 	return	docs;
		} catch (Exception e)	{	
			e.printStackTrace();
		}	
		return	null;	
	}
	
	public static void updateDocument(edu.carleton.comp4601.dao.Document newDoc){
		try {
			dir	= FSDirectory.open(new	File(INDEX_DIR));
			Analyzer analyzer = new	StandardAnalyzer(Version.LUCENE_45);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_45, analyzer);
			writer = new IndexWriter(dir, iwc);
			Document doc = new Document();
			
			String tagString = "";
			
			for (String t : newDoc.getTags()){
				tagString += " " + t;
			}
			
			doc.add(new	StringField(URL, "Url undefined: user created doc", Field.Store.YES));
			doc.add(new	StringField(DOC_ID, newDoc.getId().toString(), Field.Store.YES));
			doc.add(new	StringField(DATE, new Date().toString(), Field.Store.YES));
			doc.add(new	TextField(CONTENT, newDoc.getText() + tagString, Field.Store.YES));
			doc.add(new	TextField(METADATA, "User created doc", Field.Store.YES));

			System.out.println("updating document " + newDoc.getId());
			TextField content = new TextField(CONTENT, newDoc.getText() + tagString, Field.Store.YES);
			content.setBoost(2);
			writer.addDocument(doc);

			if	(writer != null)
				writer.close();
		 	if	(dir != null)
				dir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	public static void addDocument(edu.carleton.comp4601.dao.Document updateDoc){
		try {
			dir	= FSDirectory.open(new	File(INDEX_DIR));
			Analyzer analyzer = new	StandardAnalyzer(Version.LUCENE_45);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_45, analyzer);
			writer = new IndexWriter(dir, iwc);
			Document doc = new Document();
			
			String tagString = "";
			
			for (String t : updateDoc.getTags()){
				tagString += " " + t;
			}
			
			doc.add(new	StringField(URL, "Url undefined: user created doc", Field.Store.YES));
			doc.add(new	StringField(DOC_ID, updateDoc.getId().toString(), Field.Store.YES));
			doc.add(new	StringField(DATE, new Date().toString(), Field.Store.YES));
			doc.add(new	TextField(CONTENT, updateDoc.getText() + tagString, Field.Store.YES));
			doc.add(new	TextField(METADATA, "User created doc", Field.Store.YES));

			System.out.println("adding document " + updateDoc.getId());
			TextField content = new TextField(CONTENT, updateDoc.getText() + tagString, Field.Store.YES);
			content.setBoost(2);
			Term term = new Term("id", updateDoc.getId().toString());
			writer.updateDocument(term, doc);

			if	(writer != null)
				writer.close();
		 	if	(dir != null)
				dir.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
