/*******************************************************************************
 * Trombone is a flexible text processing and analysis library used
 * primarily by Voyant Tools (voyant-tools.org).
 * 
 * Copyright (©) 2007-2012 Stéfan Sinclair & Geoffrey Rockwell
 * 
 * This file is part of Trombone.
 * 
 * Trombone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Trombone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Trombone.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.voyanttools.trombone.input.extract;

//import it.svario.xpathapi.jaxp.XPathAPI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.detect.XmlRootExtractor;
import org.voyanttools.trombone.input.source.InputSource;
import org.voyanttools.trombone.model.DocumentFormat;
import org.voyanttools.trombone.model.DocumentMetadata;
import org.voyanttools.trombone.model.StoredDocumentSource;
import org.voyanttools.trombone.storage.StoredDocumentSourceStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.LangDetector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.sf.saxon.lib.NamespaceConstant;
import net.sf.saxon.xpath.XPathFactoryImpl;

/**
 * @author sgs
 *
 */
public class XmlExtractor implements Extractor, Serializable {
	
	
	private static final long serialVersionUID = -8659873836740839314L;
	private StoredDocumentSourceStorage storedDocumentSourceStorage;
	private FlexibleParameters parameters;
	
	/**
	 * the Transformer used to produce XML output from nodes
	 */
	private Transformer transformer;
	
	private XPathFactory xpathFactory;

	public XmlExtractor(
			StoredDocumentSourceStorage storedDocumentSourceStorage,
			FlexibleParameters parameters) {
		this.storedDocumentSourceStorage = storedDocumentSourceStorage;
		this.parameters = parameters;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new IllegalStateException(
					"Unable to create XML transformer.", e);
		}
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
		xpathFactory = new XPathFactoryImpl();
		
		// for some reason XPathAPI doesn't work properly with the default
		// XPathFactory, so we'll use Saxon
		System.setProperty("javax.xml.xpath.XPathFactory:"
				+ NamespaceConstant.OBJECT_MODEL_SAXON,
				"net.sf.saxon.xpath.XPathFactoryImpl");
	}

	/* (non-Javadoc)
	 * @see org.voyanttools.trombone.input.extract.Extractor#getInputSource(org.voyanttools.trombone.document.StoredDocumentSource)
	 */
	@Override
	public InputSource getExtractableInputSource(StoredDocumentSource storedDocumentSource)
			throws IOException {
		
		FlexibleParameters localParameters = parameters.clone();
		
		// no format specified, so let's have a peek at the contents to see if we can determine a sub-format
		DocumentFormat guessedFormat = DocumentFormat.UNKNOWN;
		if (localParameters.getParameterValue("inputFormat","").isEmpty()) {
			DocumentFormat df = storedDocumentSource.getMetadata().getDocumentFormat();
			if (df.isXml() && df!=DocumentFormat.XML) {guessedFormat=df;}
			else {
				InputStream is = null;
				try {
					is = storedDocumentSourceStorage.getStoredDocumentSourceInputStream(storedDocumentSource.getId());
					XmlRootExtractor xmlRootExtractor = new XmlRootExtractor();
					QName qname = xmlRootExtractor.extractRootElement(is);
					if (qname!=null) {
						String name = qname.getLocalPart();
						if (name.equals("feed") && qname.getNamespaceURI().toLowerCase().contains("atom")) guessedFormat = DocumentFormat.ATOM;
						else if (name.equals("TEI")) guessedFormat = DocumentFormat.TEI;
						else if (name.equals("teiCorpus")) guessedFormat = DocumentFormat.TEICORPUS;
						else if (name.equals("rss")) guessedFormat = DocumentFormat.RSS;
						else if (name.equals("EEBO")) guessedFormat = DocumentFormat.EEBODREAM;
						else if (name.equals("FictionBook")) guessedFormat = DocumentFormat.FICTIONBOOK;
						else if (name.equals("html")) guessedFormat = DocumentFormat.HTML; // TODO throw error if actually HTML?
						if (guessedFormat.isXml()) {
							storedDocumentSource.getMetadata().setDocumentFormat(guessedFormat);
						}
					}
				}
				finally {
					if (is!=null) is.close();
				}
			}
		}
		
		
		if (localParameters.getParameterValue("inputFormat","").isEmpty()==false || guessedFormat!=DocumentFormat.UNKNOWN) {
			
			String guessedFormatString = guessedFormat==DocumentFormat.UNKNOWN ? localParameters.getParameterValue("inputFormat","") : guessedFormat.name();			
			Properties properties = new Properties();
			
			String resourcePath = "/org/voyanttools/trombone/input-formats/"+guessedFormatString.toLowerCase()+".xml";
			try (InputStream is = this.getClass().getResourceAsStream(resourcePath)) {
				properties.loadFromXML(is);
				if (localParameters.getParameterBooleanValue("splitDocuments")) {
					for (String key : properties.stringPropertyNames()) {
						if (key.contains(".splitDocuments")) {
							localParameters.setParameter(key.split("\\.")[0], properties.getProperty(key)); // overwrite prefix key
						}
					}
				}
				for (String key : properties.stringPropertyNames()) {
					if (localParameters.getParameterValue(key,"").isEmpty()==true) {
						localParameters.setParameter(key, properties.getProperty(key));
					}
				}
			} catch (IOException|NullPointerException e) {
				// silently catch exceptions for unknown input formats
			}
		}
		
		String[] relevantParameters = new String[]{"xmlContentXpath","xmlTitleXpath","xmlAuthorXpath","xmlPubPlaceXpath","xmlPublisherXpath","xmlPubDateXpath","xmlKeywordXpath","xmlCollectionXpath","xmlExtraMetadataXpath"};
		StringBuilder parametersBuilder = new StringBuilder();
		for (String p : relevantParameters) {
			if (localParameters.getParameterValue(p, "").isEmpty()==false) {
				parametersBuilder.append(p);
				for (String s : localParameters.getParameterValues(p)) {
					parametersBuilder.append(s);
				}
			}
		}
		
		/* This was skipped, but we probably need to extract anyway to strip XML comments, detect language, etc.
		 * 
		// no special parameters and nothing to extract from XML, so just return the original stored document
		if (parametersBuilder.length()==0) {
			return new StoredDocumentSourceInputSource(storedDocumentSourceStorage, storedDocumentSource);
		}
		*/
		
		return new ExtractableXmlInputSource(DigestUtils.md5Hex(storedDocumentSource.getId()+relevantParameters+String.valueOf(serialVersionUID)), storedDocumentSource, localParameters);
	}

	private class ExtractableXmlInputSource implements InputSource {
		
		
		private String id;
		
		private String storedDocumentSourceId;

		private StoredDocumentSource storedDocumentSource;
		
		private DocumentMetadata metadata;
		
		private boolean isProcessed = false;
		
		private FlexibleParameters localParameters;
		
		private ExtractableXmlInputSource(String id, StoredDocumentSource storedDocumentSource, FlexibleParameters localParameters) {
			this.id = id;
			this.storedDocumentSourceId = storedDocumentSource.getId();
			this.storedDocumentSource = storedDocumentSource;
			this.metadata = storedDocumentSource.getMetadata().asParent(storedDocumentSourceId, DocumentMetadata.ParentType.EXTRACTION);
			this.metadata.setLocation(storedDocumentSource.getMetadata().getLocation());
			this.metadata.setEncoding(Charset.forName(storedDocumentSource.getMetadata().getEncoding()));
			try {
				if (this.metadata.getDocumentFormat().isXml()==false) {
					this.metadata.setDocumentFormat(DocumentFormat.XML);
				}
			} catch (IOException e) { // fail silently
				this.metadata.setDocumentFormat(DocumentFormat.XML);
			}
			this.localParameters = localParameters;
			transformer.setOutputProperty(OutputKeys.ENCODING, this.metadata.getEncoding());
		}

		@Override
		public InputStream getInputStream() throws IOException {

			InputStream inputStream = null;
			Document doc;
			try {

				inputStream = storedDocumentSourceStorage
						.getStoredDocumentSourceInputStream(storedDocumentSourceId);
				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				factory.setFeature("http://xml.org/sax/features/validation", false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
				factory.setIgnoringComments(true);
				DocumentBuilder builder = factory.newDocumentBuilder();
				doc = builder.parse(inputStream);

			} catch (ParserConfigurationException e) {
				throw new IOException("Error with XML parser configuration for "
						+ storedDocumentSource, e);
			} catch (SAXException e) {
				throw new IOException("Error with XML parsing for "
						+ storedDocumentSource, e);
			} finally {
				if (inputStream != null)
					inputStream.close();
			}
			
			if (localParameters.containsKey("xmlExtractorTemplate")) {

				Source source = null;
				String xmlExtractorTemplate = localParameters.getParameterValue("xmlExtractorTemplate");
			
				URI templateUrl;
				try {
					templateUrl = this.getClass().getResource("/org/voyanttools/trombone/templates/"+xmlExtractorTemplate).toURI();
				} catch (URISyntaxException e1) {
					throw new IOException("Unable to find local template directory", e1);
				}
				File file = new File(templateUrl);
				if (file.exists()) {
					source = new StreamSource(file);
				}
				
				if (source!=null) {
					DOMResult result = new DOMResult();
					try {
						Transformer extractorTransformer = TransformerFactory.newInstance().newTransformer(source);
						extractorTransformer.transform(new DOMSource(doc), result);
					} catch (TransformerException e) {
						throw new IOException("Unable to transform document during expansion "+metadata, e);
					}
					try {
						doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					} catch (ParserConfigurationException e) {
						throw new IllegalStateException("Unable to create new XML document during templated extraction.", e);
					}
					doc = (Document) result.getNode();
//					   DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
//					    LSSerializer lsSerializer = domImplementation.createLSSerializer();
//					    System.out.println(lsSerializer.writeToString(doc).substring(0, 3000));
				}
				else {
					throw new IOException("Unable to find extractor template "+xmlExtractorTemplate);
				}
			}
			
			
			// try to find title if needed
			String[] titles = getNodesAsStringsFromParametersValue(doc, "xmlTitleXpath");
			if (titles.length>0) {
				metadata.setTitles(titles);
			}
			
			// try to find author if needed
			String[] authors = getNodesAsStringsFromParametersValue(doc, "xmlAuthorXpath");
			if (authors.length>0) {
				metadata.setAuthors(authors);
			}

			// try to find publplace if needed
			String[] pubPlaces = getNodesAsStringsFromParametersValue(doc, "xmlPubPlaceXpath");
			if (pubPlaces.length>0) {
				metadata.setPubPlaces(pubPlaces);
			}

			// try to find title if needed
			String[] publishers = getNodesAsStringsFromParametersValue(doc, "xmlPublisherXpath");
			if (publishers.length>0) {
				metadata.setPublishers(publishers);
			}

			// try to find pubDates if needed
			String[] pubDates = getNodesAsStringsFromParametersValue(doc, "xmlPubDateXpath");
			if (pubDates.length>0) {
				metadata.setPubDates(pubDates);
			}
			
			// try to find pubDates if needed
			String[] collections = getNodesAsStringsFromParametersValue(doc, "xmlCollectionXpath");
			if (collections.length>0) {
				metadata.setCollections(collections);
			}
			
			// try to find pubDates if needed
			String[] keywords = getNodesAsStringsFromParametersValue(doc, "xmlKeywordXpath");
			if (keywords.length>0) {
				metadata.setKeywords(keywords);
			}
			
			for (String extra : localParameters.getParameterValues("xmlExtraMetadataXpath")) {
				for (String x :extra.split("(\r\n|\r|\n)+")) {
					x = x.trim();
					String[] parts = x.split("=");
					if (parts.length>1) {
						String key = parts[0].trim();
						String xpath = StringUtils.join(Arrays.copyOfRange(parts, 1, parts.length), "=").trim();
						String[] values = getNodesAsStringsFromXpath(doc, xpath);
						if (values.length>0) {
							metadata.setExtras(key, values);
						}
					}
				}
			}
			
			// if no XPath is defined, consider the whole source XML (but allow for additional metadata ot be identified
			String xmlContentXpath = localParameters.getParameterValue("xmlContentXpath","/");
			
			NodeList nodeList;
			XPath xpath = xpathFactory.newXPath();
			try {
				nodeList = (NodeList) xpath.evaluate(xmlContentXpath, doc.getDocumentElement(), XPathConstants.NODESET);
				if (nodeList.getLength()==0) {
					throw new XPathExpressionException(xmlContentXpath);
				}
			} catch (XPathExpressionException e) {
				throw new IllegalArgumentException(
						"A problem was encountered proccesing this XPath query: " + xmlContentXpath+" (for document: "+this.metadata.getLocation()+")", e);
			}
			
			Node newParentNode;
			// just use the single node as root
			if (nodeList.getLength()==1) {
				newParentNode = nodeList.item(0);
			}
			
			// encapsulate child nodes in document root
			else {
				newParentNode = doc.getDocumentElement().cloneNode(false);
				for (int i=0, len=nodeList.getLength(); i<len; i++) {
					newParentNode.appendChild(nodeList.item(i));
				}
			}
			

			StringWriter sw = new StringWriter(); // no need to close
			Result streamResult = new StreamResult(sw);
			try {
				DOMSource domSource = new DOMSource(newParentNode);
				transformer.transform(domSource, streamResult);
			} catch (TransformerException e) {
				throw new IOException(
						"Unable to transform node during XML extraction: "+storedDocumentSource);
			} catch (Exception e) {
				throw new IOException(
						"Unable to transform node during XML extraction: "+storedDocumentSource, e);
			}
	
			String string = sw.toString();
			
			// try to determine language
			metadata.setLanguageCode(LangDetector.detect(string, parameters));

	        isProcessed = true;

	        return new ByteArrayInputStream(string.getBytes(metadata.getEncoding()));
			
		}

		private String[] getNodesAsStringsFromParametersValue(Document doc, String parameterKey) {
			String xpathString = localParameters.getParameterValue(parameterKey,"");
			return getNodesAsStringsFromXpath(doc, xpathString);
		}

		private String[] getNodesAsStringsFromXpath(Document doc, String xpathString) {
			String[] strings = new String[0];
			if (xpathString.isEmpty()==false) {
				Set<String> values = new HashSet<String>();
				XPath xpath = xpathFactory.newXPath();
				NodeList nodeList;
				try {
					// this is awful to have to specify the return type, this should probably use a different library
					if (xpathString.startsWith("string") || xpathString.startsWith("concat(") || xpathString.startsWith("replace(")) {
						 String s = (String) xpath.evaluate(xpathString, doc.getDocumentElement(), XPathConstants.STRING);
						 values.add(s);
					}
					else {
						nodeList = (NodeList) xpath.evaluate(xpathString, doc.getDocumentElement(), XPathConstants.NODESET);
						for (int i=0, len=nodeList.getLength(); i<len; i++) {
							values.add(nodeList.item(i).getTextContent());
						}
					}
				}
				catch (XPathExpressionException e) {
					throw new IllegalArgumentException(
							"A problem was encountered proccesing this XPath query: " + xpathString, e);
				}
				return values.toArray(strings);
			}
			return strings;
		}
		
		@Override
		public DocumentMetadata getMetadata() throws IOException {
			return isProcessed ? this.metadata : storedDocumentSourceStorage.getStoredDocumentSourceMetadata(id);
		}

		@Override
		public String getUniqueId() {
			return this.id;
		}
	}
	

}
