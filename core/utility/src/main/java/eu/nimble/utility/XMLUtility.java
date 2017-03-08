package eu.nimble.utility;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@SuppressWarnings("restriction")
public class XMLUtility {

	public static Logger log = LoggerFactory.getLogger(XMLUtility.class);

	private static String getNodeValue(Node node) {
		if(node == null || node.getFirstChild() == null)
			return "";
		
		return node.getFirstChild().getNodeValue();
	}

	private static String getAttributeValue(Node node) {
		return node.getNodeValue();
	}

	private static List<String> processNodeListAndReturnStringList(NodeList nodeList, boolean isNode) {
		List<String> resultList = new ArrayList<String>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (isNode) {
				resultList.add(getNodeValue(nodeList.item(i)));
			} else {
				resultList.add(getAttributeValue(nodeList.item(i)));
			}
		}

		return resultList;
	}

	public static NodeList evaluateXPath(Node document, String xpathExpression) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		NamespaceContext namespaceContext = new NamespaceResolver();
		xPath.setNamespaceContext(namespaceContext);
		NodeList result = null;
		try {
			XPathExpression expr = xPath.compile(xpathExpression);
			result = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
		} catch (XPathExpressionException ex) {
			log.error("", ex);
		}
		return result;
	}

	public static NodeList evaluateXPath(String xmlString, String xpathExpression) {
		Document document = parseXMLFromString(xmlString);
		return evaluateXPath(document, xpathExpression);
	}

	public static String evaluateXPathAndGetNodeValue(String xmlString, String xpathExpression) {
		Document document = parseXMLFromString(xmlString);
		NodeList nodeList = evaluateXPath(document, xpathExpression);

		return getNodeValue(nodeList.item(0));
	}

	public static String evaluateXPathAndGetNodeValue(Node node, String xpathExpression) {
		NodeList nodeList = evaluateXPath(node, xpathExpression);

		return getNodeValue(nodeList.item(0));
	}

	public static List<String> evaluateXPathAndGetNodeValues(String xmlString, String xpathExpression) {
		Document document = parseXMLFromString(xmlString);
		NodeList nodeList = evaluateXPath(document, xpathExpression);
		return processNodeListAndReturnStringList(nodeList, true);
	}

	public static List<String> evaluateXPathAndGetNodeValues(Node node, String xpathExpression) {
		NodeList nodeList = evaluateXPath(node, xpathExpression);
		return processNodeListAndReturnStringList(nodeList, true);
	}

	public static String evaluateXPathAndGetAttributeValue(String xmlString, String xpathExpression) {
		Document document = parseXMLFromString(xmlString);
		return evaluateXPathAndGetAttributeValue(document, xpathExpression);
	}

	public static String evaluateXPathAndGetAttributeValue(Node node, String xpathExpression) {
		NodeList nodeList = evaluateXPath(node, xpathExpression);
		return getAttributeValue(nodeList.item(0));
	}

	public static List<String> evaluateXPathAndGetAttributeValues(String xmlString, String xpathExpression) {
		Document document = parseXMLFromString(xmlString);
		NodeList nodeList = evaluateXPath(document, xpathExpression);
		return processNodeListAndReturnStringList(nodeList, false);
	}

	public static List<String> evaluateXPathAndGetAttributeValues(Node node, String xpathExpression) {
		NodeList nodeList = evaluateXPath(node, xpathExpression);
		return processNodeListAndReturnStringList(nodeList, false);
	}

	public static Document parseXMLFromString(String xml) {
		return parseXML(new InputSource(new StringReader(xml)));
	}

	public static Document parseXML(InputSource is) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(is);
			return document;
		} catch (SAXException | IOException | ParserConfigurationException ex) {
			log.error("", ex);
		}
		return null;
	}

	public static Document parseXMLFromFile(String filePath) {
		try {
			return parseXML(new InputSource(new FileReader(filePath)));
		} catch (FileNotFoundException ex) {
			log.error("", ex);
		}
		return null;
	}

	public static String nodeToString(Node node) {
		StringWriter sw = new StringWriter();
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
			log.error("", te);
		}
		return sw.toString();
	}

	public static void main(String argv[]) {
		/*Document doc = parseXMLFromFile("src/test/resources/ExampleSensorML.xml");
		 log.info("XML: {}", nodeToString(doc));
		 NodeList results = evaluateXPath(doc, "//sml:identification");
		 log.info("Result: {}", nodeToString(results.item(0)));*/

		//log.info(XMLUtility.convert("2012-11-19T14:00:00+01:00").toString());
		//log.info(XMLUtility.convert("2014-06-01T03:02:13.552+00:00").toString());
		//log.info(getCurrentTimeStamp());
		String sampleXML = "<cricketers>\n"
			+ "    <cricketer type=\"righty\">\n"
			+ "        <name>MS Dhoni</name>\n"
			+ "        <role>Captain</role>\n"
			+ "        <position>Wicket-Keeper</position>\n"
			+ "    </cricketer>\n"
			+ "    <cricketer type=\"lefty\">\n"
			+ "        <name>Shikhar Dhawan</name>\n"
			+ "        <role>Batsman</role>\n"
			+ "        <position>Point</position>\n"
			+ "    </cricketer>\n"
			+ "    <cricketer type=\"righty\">\n"
			+ "        <name>Virat Kohli</name>\n"
			+ "        <role>Batsman</role>\n"
			+ "        <position>cover</position>\n"
			+ "    </cricketer>\n"
			+ "    <cricketer type=\"righty\">\n"
			+ "        <name>Shami</name>\n"
			+ "        <role>Bowler</role>\n"
			+ "        <position>SquareLeg</position>\n"
			+ "    </cricketer>\n"
			+ "    <cricketer type=\"lefty\">\n"
			+ "        <name>Zaheer Khan</name>\n"
			+ "        <position>FineLeg</position>\n"
			+ "    </cricketer>\n"
			+ "</cricketers>\n";
		//System.out.println(evaluateXPathAndGetAttributeValue(sampleXML, "//cricketer/@type"));
		//System.out.println(evaluateXPathAndGetAttributeValues(sampleXML, "//cricketer/@type").toString());
		//System.out.println(evaluateXPathAndGetNodeValue(sampleXML, "//cricketer/name"));
		//System.out.println(evaluateXPathAndGetNodeValues(sampleXML, "//cricketer/name").toString());

		String om = "<om:OM_Observation gml:id=\"observation\" xmlns:om=\"http://www.opengis.net/om/2.0\" xmlns:gml=\"http://www.opengis.net/gml/3.2\">\n"
			+ "<om:type xlink:href=\"http://www.opengis.net/def/observationType/OGC-OM/2.0/OM_Measurement\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n"
			+ "<om:phenomenonTime>\n"
			+ "<gml:TimeInstant gml:id=\"phenomenonTime\">\n"
			+ "<gml:timePosition>2015-12-15T15:13:13</gml:timePosition>\n"
			+ "</gml:TimeInstant>\n"
			+ "</om:phenomenonTime>\n"
			+ "<om:resultTime>\n"
			+ "<gml:TimeInstant gml:id=\"resultTime\">\n"
			+ "<gml:timePosition>2015-12-15T15:13:13</gml:timePosition>\n"
			+ "</gml:TimeInstant>\n"
			+ "</om:resultTime>\n"
			+ "<om:procedure xlink:href=\"urn:ogc:object:feature:sensor:nanobiz:1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n"
			+ "<om:observedProperty xlink:href=\"http://sensorml.com/ont/swe/property/Temperature\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n"
			+ "<om:featureOfInterest xlink:href=\"urn:turkey:karadeniz:eregli:liman\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"/>\n"
			+ "<om:result uom=\"cel\" xsi:type=\"gml:MeasureType\" xmlns:gml=\"http://www.opengis.net/gml/3.2\" xmlns:om=\"http://www.opengis.net/om/2.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">23</om:result>\n"
			+ "</om:OM_Observation>\n";

		String notify = "<wsnt:Notify>\n"
			+ "	<wsnt:NotificationMessage>\n"
			+ "		<wsnt:Topic Dialect=\"http://docs.oasis-open.org/wsn/t-1/TopicExpression/Simple\"> \n"
			+ "			Measurements\n"
			+ "		</wsnt:Topic>\n"
			+ "		<wsnt:Message>\n"
			+ om
			+ "		</wsnt:Message>\n"
			+ "	</wsnt:NotificationMessage>\n"
			+ "</wsnt:Notify>";

		String soapRequest = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "   <soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://www.w3.org/2005/08/addressing\" xmlns:wsnt=\"http://docs.oasis-open.org/wsn/b-2\">\n"
			+ "      <soap:Header>\n"
			+ "         <wsa:To>http://localhost</wsa:To>\n"
			+ "         <wsa:Action>http://docs.oasis-open.org/wsn/bw-2/NotificationConsumer/Notify</wsa:Action>\n"
			+ "         <wsa:MessageID>uuid:4e595160-185a-9b3c-3eb6-592c7c5b0c7a</wsa:MessageID>\n"
			+ "         <wsa:From>\n"
			+ "            <wsa:Address>http://www.w3.org/2005/08/addressing/role/anonymous</wsa:Address>\n"
			+ "         </wsa:From>\n"
			+ "      </soap:Header>"
			+ "<soap:Body>";
		soapRequest += notify;
		soapRequest += "</soap:Body>\n"
			+ "</soap:Envelope>";

		NodeList observationNodes = XMLUtility.evaluateXPath(soapRequest, "//om:OM_Observation");
		System.out.println(" Number of observations {}" + observationNodes.getLength());

		for (int i = 0; i < observationNodes.getLength(); i++) {

			String omString = XMLUtility.nodeToString(observationNodes.item(i));
			System.out.println(omString);
			Node node = observationNodes.item(i);

			String id = XMLUtility.evaluateXPathAndGetAttributeValue(node, "@gml:id");
			String sensorID = XMLUtility.evaluateXPathAndGetAttributeValue(node, "om:procedure/@xlink:href");
			String time = XMLUtility.evaluateXPathAndGetNodeValue(node, "om:phenomenonTime/gml:TimeInstant/gml:timePosition");
			String featureOfInterestID = XMLUtility.evaluateXPathAndGetAttributeValue(node, "om:featureOfInterest/@xlink:href");
			String observedProperty = XMLUtility.evaluateXPathAndGetAttributeValue(node, "om:observedProperty/@xlink:href");
			String value = XMLUtility.evaluateXPathAndGetNodeValue(node, "om:result");
			String unitOfMeasure = XMLUtility.evaluateXPathAndGetAttributeValue(node, "om:result/@uom");
			String mime = XMLUtility.evaluateXPathAndGetAttributeValue(node, "om:result/@xsi:type");

			System.out.println(id + " " + sensorID + " " + time + " " + featureOfInterestID + " " + observedProperty + " " + value + " " + unitOfMeasure + " " + mime);
		}

	}
}
