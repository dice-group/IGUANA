package org.aksw.iguana.utils.parser;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Der ConfigParser dient dazu eine XMLDatei einfacher durchzuparsen
 * Hierzu wird ein Parser mit dem Pfad zur XML Datei generiert.
 * Der Parser steht nun noch auf keinem Knoten, sondern über jedem. 
 * Um in einen Knoten zu wechseln kann dies mittles setNode geschehen oder 
 * automatisch bei dem Aufruf getElementAt.
 * 
 * Wird getElementAt aufgerufen gibt es einerseits das gesuchte Element zurück, zeigt aber 
 * nun auch auf diesen. D.h. alle weiteren Aufrufe des Parsers geschehen nun nur auf diesem Element. 
 * D.h. er kann keine Elemente, die nicht innerhalb des momentanen Elements sind, finden.
 * Will man nun aber genau andere Elemente. Kann mann mittles der Funktion resetNode
 * Den Knoten auf den Initialstatus setzen
 * 
 * @author Felix Conrads
 *
 */
public class ConfigParser {
	
	private Document xmlFile;
	private Element current;

	private ConfigParser(){
		
	}
	
	/**
	 * Generiert den Parser mit dem angegeben XML Dokument
	 * 
	 * 
	 * @param pathToXMLFile Pfad zur XML Datei
	 * @return Den generierten Parser
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static ConfigParser getParser(String pathToXMLFile) throws SAXException, IOException, ParserConfigurationException{
		ConfigParser cp = new ConfigParser();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// Erstellt das XML Document
		DocumentBuilder db = dbf.newDocumentBuilder();
		cp.xmlFile = db.parse(pathToXMLFile);
		return cp;
	}
	
	
	/**
	 * Generiert den Parser mit dem angegeben Knoten
	 * 
	 * 
	 * @param pathToXMLFile Pfad zur XML Datei
	 * @return Den generierten Parser
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static ConfigParser getParser(Node node) throws SAXException, IOException, ParserConfigurationException{
		ConfigParser cp = new ConfigParser();
		cp.current = (Element) node;
		cp.xmlFile = node.getOwnerDocument();
		return cp;
	}
	
	
	/**
	 * Sucht alle Knoten mit dem gegebene Tagname innerhalb des momentanen Elements
	 * Setzt NICHT das neue Element, sondern verbleibt!
	 * 
	 * 
	 * @param tagName Der Tagname der Elemente
	 * @return Liste mit Knoten die den tagName haben
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public NodeList getNodeList(String tagName) throws ParserConfigurationException, SAXException, IOException{
		
		// Sucht nach die tagName tags
		if(current==null){
			return xmlFile.getElementsByTagName(tagName);
		}
		return current.getElementsByTagName(tagName);
	
	}
	
	/**
	 * Setzt den Knoten auf Initalstatus zurück
	 * 
	 */
	public void resetNode(){
		this.current = null;
	}
	
	
	/**
	 * Setzt den Knoten auf den momentan gezeigt werden soll
	 * 
	 * @param node Zu setzender Knoten
	 */
	public void setNode(Element node){
		this.current = node;
	}
	
	/**
	 * Sucht Ein Element, innerhalb des momentanen Elements, mit gegebenen Tagname und an gefundener Stelle
	 * 
	 * @param tagName Der Tagname der Elemente
	 * @param index Stelle an der das Element auftritt (0 := Erster Auftritt des Elements)
	 * @return Das gefunden Element
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public Element getElementAt(String tagName, Integer index) throws ParserConfigurationException, SAXException, IOException{
		Element node = (Element) getNodeList(tagName).item(index);
		setNode(node);
		return node;
	}
	
	/**
	 * Sucht nach dem ersten auftreten eines Elements, innerhalb des momentan Elements, 
	 * mit gegebenen Tagname und der gegebenen Attribute:Value Kombination 
	 * 
	 * @param attribute Der Name des Attribut, welches das Element besitzen soll
	 * @param value Der Wert des Attribut, welches das Element besitzen soll
	 * @param tagName Der tagName des Elements
	 * @return Das gefunden Element
	 */
	public Element getElementWithAttribute(String attribute, String value, String tagName){
		NodeList dbs=null;
		if(this.current==null){
			dbs = xmlFile.getElementsByTagName(tagName);
		}
		else{
			dbs  = current.getElementsByTagName(tagName);
		}
		Element element = null;
		for(int i=0; i<dbs.getLength(); i++){
			element=(Element)dbs.item(i);
			if(element.getAttribute(attribute).equals(value)){
				break;
			}
			else if(i==dbs.getLength()-1){
				return null;
			}
		}
		return element;
		
	}
	
}
