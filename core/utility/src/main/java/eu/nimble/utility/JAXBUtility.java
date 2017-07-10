/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.utility;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 *
 * @author yildiray
 */
public class JAXBUtility {

	public static Logger log = LoggerFactory.getLogger(JAXBUtility.class);

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static String serialize(Object object, String rootElementName) {
		try {
			StringWriter writer = new StringWriter();
			String packageName = object.getClass().getPackage().getName();
			JAXBContext jc = JAXBContext.newInstance(packageName);
			Marshaller marsh = jc.createMarshaller();
			marsh.setProperty("jaxb.formatted.output", true);
			JAXBElement element = new JAXBElement(
				new QName("", rootElementName), object.getClass(), object);

			marsh.marshal(element, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("", e);
		}
		return null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static String serialize(Object object, JAXBElement element) {
		try {
			StringWriter writer = new StringWriter();
			String packageName = object.getClass().getPackage().getName();
			JAXBContext jc = JAXBContext.newInstance(packageName);
			Marshaller marsh = jc.createMarshaller();
			marsh.setProperty("jaxb.formatted.output", true);
			marsh.marshal(element, writer);
			return writer.toString();
		} catch (JAXBException e) {
			log.error("", e);
		}
		return null;
	}

	public static Object deserialize(String xmlContent, String packageName) {
		javax.xml.bind.JAXBElement result = null;
		try {
			InputStream is = new ByteArrayInputStream(
				xmlContent.getBytes("UTF-8"));
			JAXBContext context = JAXBContext.newInstance(packageName);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			result = (javax.xml.bind.JAXBElement) unmarshaller.unmarshal(is);
			log.debug(" $$$ Deserialization complete and the class name is: " + result.getClass().getName());
		} catch (UnsupportedEncodingException | JAXBException e) {
			log.error("", e);
		}

		return result.getValue();
	}

	public static Object deserialize(Node node, String packageName) {
		javax.xml.bind.JAXBElement result = null;
		try {
			JAXBContext context = JAXBContext.newInstance(packageName);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			result = (javax.xml.bind.JAXBElement) unmarshaller.unmarshal(node);
			log.debug(" $$$ Deserialization complete and the class name is: " + result.getClass().getName());
		} catch (JAXBException e) {
			log.error("", e);
		}

		return result.getValue();
	}
}
