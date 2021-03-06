package edu.carleton.comp4601.graph;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;


public class PageGraph implements Serializable{

	private static final long serialVersionUID = 8620029720345685784L;

	private final String BASE_VERTEX_KEY = "BASE";
	private  String baseUrl;
	private  Graph<Vertex, DefaultEdge> directedGraph;
	private  ConcurrentHashMap<String, Vertex> vertexMap;
	private String name; 
	
	public PageGraph(String baseUrl, String name) {
		directedGraph = new DefaultDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);
		vertexMap = new ConcurrentHashMap<String, Vertex>();
		//vertexMap.put(baseUrl, new Vertex(-1, BASE_VERTEX_KEY));

		this.baseUrl = baseUrl;
	}
	public PageGraph() {
		directedGraph = new DefaultDirectedGraph<Vertex, DefaultEdge>(DefaultEdge.class);
		vertexMap = new ConcurrentHashMap<String, Vertex>();

	}
	
	public synchronized void addVertex(Vertex vertex) {
		vertexMap.put(vertex.getUrl(), vertex);
		directedGraph.addVertex(vertex);
	}
	
	public synchronized void connectVertex(Vertex vertex1, Vertex vertex2) {
		directedGraph.addVertex(vertex1);
		directedGraph.addVertex(vertex2);
		
		directedGraph.addEdge(vertex1, vertex2);
		
	}
	public synchronized void connectExisitingVertexToNewVertex(Vertex vertex1, String vertex2url) {
		directedGraph.addVertex(vertex1);
		vertexMap.put(vertex1.getUrl(), vertex1);
		Vertex vertex2;
		vertex2 = vertexMap.get(vertex2url);
		directedGraph.addVertex(vertex2);
		directedGraph.addEdge(vertex2, vertex1);
		
	}
	public synchronized void connectNewVertexToExistingVertex(Vertex vertex1, String vertex2url) {
		directedGraph.addVertex(vertex1);
		vertexMap.put(vertex1.getUrl(), vertex1);
		Vertex vertex2;
		if (vertex2url == null) {
			vertex2 = vertexMap.get(BASE_VERTEX_KEY);		
		} else {
			vertex2 = vertexMap.get(vertex2url);
		}
		directedGraph.addVertex(vertex2);
		directedGraph.addEdge(vertex1, vertex2);
		
	}

	public synchronized Vertex getVertex(String url) {
		Vertex vertex = vertexMap.get(url);
		return vertex;
	}
	public String getName() {
		return name;
	}
	public synchronized boolean hasVertex(String url) {
		if (vertexMap.containsKey(url)) {
			return true;
		}
		return false;
	}
	public String getGraphUrl() {
		return baseUrl;
	}
	public synchronized Graph<Vertex, DefaultEdge> getGraph() {
		return directedGraph;
	}
	public synchronized ConcurrentHashMap getMap() {
		return vertexMap;
	}

}
