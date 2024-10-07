package com.mnet.framework.utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;

import com.mnet.framework.reporting.FrameworkLog;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.PropertyException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Utilities for file read / write / parsing operations.
 * @version Spring 2023
 * @author Arya Biswas
 */
public class FileUtilities {
	
	private FrameworkLog log;
	
	public FileUtilities(FrameworkLog frameworkLog) {
		log = frameworkLog;
	}
	
	/**
	 * Writes a string to a file at the designated path.
	 * Creates a new file or overwrites if it already exists.
	 * @param filePath Absolute path where file is located. Includes desired filename and extension.
	 * @param filename Name of file (with extension) to be created or updated at path.
	 * @param text Text to be written to file.
	 */
	public void writeToFile(String filePath, String text) {
		File target;
		FileWriter writer;
		
		target = new File(filePath);

		if (!target.exists()) {
			createFileAtPath(target);
		}

		writer = getFileWriterForTarget(target);
		
		outputTextToFile(writer, text);
	}
	
	/**
	 * Creates a directory at the target path, if it does not already exist.
	 * @param path Absolute path where directory is to be created.
	 */
	public void createDirectory(String path) {
		try {
			Files.createDirectories(Paths.get(path));
		} catch (IOException ioe) {
			String err = "Failed to create directory at path: " + path;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	/**
	 * Copies directory and its contents to target path.
	 * @param source Directory being copied.
	 * @param target New parent directory of folder being copied.
	 */
	public void copyDirectoryToPath(String source, String target) {
		String folder = getRelativePath(Paths.get(source));
		
		try {
			FileUtils.copyDirectory(new File(source), new File(target + File.separator + folder));
		} catch (IOException ioe) {
			String err = "Failed to copy directory from " + source + " to " + target;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Copies directory and its contents to target path. Optional parameter to delete original directory after copy.
	 * @param source Directory being copied.
	 * @param target New parent directory of folder being copied.
	 * @param deleteSource If true, deletes source directory after being copied.
	 */
	public void copyDirectoryToPath(String source, String target, boolean deleteSource) {
		copyDirectoryToPath(source, target);
		
		if (!deleteSource) { 
			return; 
		}
		
		try {
			FileUtils.deleteDirectory(new File(source));
		} catch (IOException ioe) {
			String err = "Failed to delete directory at path: " + source;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Copies file from a source directory to a target directory.
	 * @param sourceFilePath Fully qualified path (with extension) of file to copy.
	 * @param targetPath Destination directory for designated file.
	 */
	public void copyFileToPath(String sourceFilePath, String targetPath) {
		try {
			FileUtils.copyFileToDirectory(new File(sourceFilePath), new File(targetPath));
		} catch (IOException ioe) {
			String err = "Failed to copy file from " + sourceFilePath + " to " + targetPath;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * Creates XML at logging path (LOG_DIR\testName_timestamp) with the designated attributes.
	 * @param data JAXB data class with XML representation.
	 * @param fileName Local XML file name (exclude path).
	 */
	public void generateXML(XMLData data, String fileName) {
		JAXBContext context = getXMLContext(data);
		Marshaller xmlBuilder = defineXMLFormat(context);
		StringWriter output = buildXMLFromData(xmlBuilder, data);
		
		writeStreamToXML(output, fileName);
	}
	
	/**
	 * @return true if the file exists at the designated absolute path on the local file system. Otherwise, returns false. 
	 **/
	public boolean fileExists(String filePath) {
		File file = new File(filePath);
		
		return (file.exists() && !file.isDirectory());
	}
	
	/**
	 * Extracts substring between starting and ending delimiters.
	 * @param body String containing desired content between bounds.
	 * @param startDelimiter Substring designating starting point of parsing.
	 * @param endDelimiter Substring designating end point of parsing.
	 */
	public String getContentBetweenBounds(String body, String startDelimiter, String endDelimiter) {
		try {
			return body.substring(body.indexOf(startDelimiter) + startDelimiter.length(), body.lastIndexOf(endDelimiter));
			
		} catch (StringIndexOutOfBoundsException oob) {
			String err = "Missing content between bounds - start: " + startDelimiter + " end: " + endDelimiter + " for content: \n" + body;
			log.error(err);
			log.printStackTrace(oob);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @param filePath Fully qualified local file path.
	 * @return Contents of file located at designated path.
	 */
	public String getFileContent(String filePath) {
		try {
			return Files.lines(Paths.get(filePath)).collect(Collectors.joining(System.lineSeparator()));
		} catch (IOException ioe) {
			String err = "Error when reading target file at path: " + filePath;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @param filePath Fully qualified local file path.
	 * @return File contents as a byte array.
	 */
	public byte[] getFileBytes(String filePath) {
		try {
			return Files.readAllBytes(Paths.get(filePath));
		} catch (IOException ioe) {
			String err = "Failed to read file at path: " + filePath;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	// TODO: Accept File and return XMLData with raw / parsed fileName
	// TODO: Refactor relevant functions to XMLParser
	
	/**
	 * Takes a input text file which has XML content as a substring and writes the content to an XML file.
	 * Returns XML data representation of context written to XML file. 
	 * @param sourceFile Fully qualified path of file (with extension) which contains XML content.
	 * @param targetFile Fully qualified path of target XML file.
	 * @param startDelimiter Substring designating starting point of XML parsing.
	 * @param endDelimiter Substring designating end point of XML parsing.
	 * @param responseXMLData Data class representing XML response object
	 */
	public XMLData extractXMLFromFile(String sourceFile, String targetFile, String startDelimiter, String endDelimiter, Class<? extends XMLData> responseXMLData) {
		return extractXMLFromContent(getFileContent(sourceFile), targetFile, startDelimiter, endDelimiter, responseXMLData);
	}
	
	/**
	 * Takes a string which contains XML content as a substring and writes the content to an XML file.
	 * Returns XML data representation of context written to XML file. 
	 * @param content String block containing XML content as a substring.
	 * @param targetFile Fully qualified path of target XML file.
	 * @param startDelimiter Substring designating starting point of XML parsing.
	 * @param endDelimiter Substring designating end point of XML parsing.
	 * @param responseXMLData Data class representing XML response object
	 */
	public XMLData extractXMLFromContent(String content, String targetFile, String startDelimiter, String endDelimiter, Class<? extends XMLData> responseXMLData) {
		String xmlSubContent = getContentBetweenBounds(content, startDelimiter, endDelimiter);
		String xmlContent = xmlSubContent.substring(xmlSubContent.indexOf("<"), xmlSubContent.lastIndexOf(">") + 1);
		
		writeTextToXML(xmlContent, targetFile);
		
		return getXMLDataFromFile(targetFile, responseXMLData);
	}
	
	/**
	 * Takes a string which represents XML content and writes the content to an XML file.
	 * Returns XML data representation of context written to XML file.
	 * If input string contains non-XML data, use extractXMLContent(String, String, String , String, Class<? extends XMLData>) instead.
	 * @param content String block representing XML content.
	 * @param targetFile Fully qualified path of target XML file.
	 * @param startDelimiter Substring designating starting point of XML parsing.
	 * @param endDelimiter Substring designating end point of XML parsing.
	 * @param responseXMLData Data class representing XML response object
	 */
	public XMLData extractXMLFromContent(String content, String targetFile, Class<? extends XMLData> responseXMLData) {
		return extractXMLFromContent(content, targetFile, "", "", responseXMLData);
	}

	/**
	 * Clean and delete content of folder mentioned
	 */
	public void cleanFolder(String folderPath) {
		ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", "rmdir " + folderPath + " /S /Q");
		builder.redirectErrorStream(true);
		try {
			builder.start();
		} catch (IOException e) {
			log.error("Failed to delete folder");
			log.printStackTrace(e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get last modified file from the mentioned directory
	 */
	public String getLastModified(String directoryPath) {
		File directory = new File(directoryPath);
		File[] files = directory.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		Path path = chosenFile.toPath();
		Path fileName = path.getFileName();
		return fileName.toString();
	}

	/**
	 * Generates properties file from argument properties
	 * @param fileName Fully qualified path of target properties file.
	 */
	public void createPropertiesFile(String fileName, Properties properties) {
		try {
			properties.store(new FileWriter(fileName), null);
		} catch (IOException ioe) {
			String err = "Failed to create properties file at path: " + fileName;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * To delete the existing file
	 */
	public void deleteFileIfExists(String filePath)
	{
		try {
            Files.deleteIfExists(
                Paths.get(filePath));
        }
        catch (FileNotFoundException ffe) {
        	String err = "No such file/directory exists";
			log.error(err);
			log.printStackTrace(ffe);
			throw new RuntimeException(err);
        }
        catch (IOException ie) {
        	String err = "No information found in file";
			log.error(err);
			log.printStackTrace(ie);
			throw new RuntimeException(err);
        }
	}
	
	/**Returns the latest file based on fileName passed**/
	public String getLatestFile(String localPath, String jsonFileName) {
		
		File directory = new File(localPath);
		File[] files = directory.listFiles(File::isFile);
		long lastModifiedTime = Long.MIN_VALUE;
		File chosenFile = null;

		if (files != null) {
			for (File file : files) {
				if (file.getName().contains(jsonFileName) && file.lastModified() > lastModifiedTime) {
					chosenFile = file;
					lastModifiedTime = file.lastModified();
				}
			}
		}
		Path path = chosenFile.toPath();
		Path fileName = path.getFileName();
		return fileName.toString();
		
	}
	
	/*
	 * Helper functions
	 */
	
	private void createFileAtPath(File target) {
		try {
			target.createNewFile();
		} catch (IOException ioe) {
			String err = "Could not create file at path: " + target.getAbsolutePath();
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	private void outputTextToFile(FileWriter writer, String text) {
		try {
			writer.write(text);
			writer.close();
		} catch (IOException ioe) {
			String err = "Failed to write text to file using FileWriter: " + writer.toString();
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	/**
	 * @param filePath Fully qualified file path of destination XML.
	 * @param fileContent XML-formatted contents to write to file.
	 */
	private void writeTextToXML(String fileContent, String filePath) {
		filePath = getFileNameAsXML(filePath);
		
		try {
			Files.write(Paths.get(filePath), fileContent.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException ioe) {
			String err = "Error when writing XML content to file: " + filePath;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
	}
	
	private void writeStreamToXML(StringWriter output, String filePath) {
		Transformer transformer = getTransformer();
		transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, ""); // for header
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		
		filePath = getFileNameAsXML(filePath);
		
		try {
			transformer.transform(new StreamSource(new StringReader(output.toString())), new StreamResult(new File(filePath)));
		} catch (TransformerException te) {
			String err = "Failed to generate XML at path: " + filePath;
			log.error(err);
			log.printStackTrace(te);
			throw new RuntimeException(err);
		}
	}
	
	private String getRelativePath(Path absolutePath) {
		int pathLength = absolutePath.getNameCount();
		return absolutePath.subpath(pathLength - 1, pathLength).toString();
	}
	
	private String getFileNameAsXML(String fileName) {
		if (!fileName.contains(".xml")) {
			fileName += ".xml";
		}
		
		return fileName;
	}
	
	private FileWriter getFileWriterForTarget(File target) {
		try {
			return new FileWriter(target);
		} catch (IOException ioe) {
			String err = "Could not open file at path: " + target.getAbsolutePath();
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(err);
		}
	}
	
	private JAXBContext getXMLContext(XMLData data) {
		try {
			return JAXBContext.newInstance(data.getClass());
			
		} catch (JAXBException jaxb){
			String err = "Failed to instantiate XML data class: " + data.toString();
			log.error(err);
			log.printStackTrace(jaxb);
			throw new RuntimeException(err);
		}
	}
	
	private Marshaller defineXMLFormat(JAXBContext context) {
		Marshaller xmlBuilder = null;
		String err;
		
		try {
			xmlBuilder = context.createMarshaller();
			xmlBuilder.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
			xmlBuilder.setProperty(Marshaller.JAXB_FRAGMENT, false); // remove XML header - defined by Transformer
		} catch (PropertyException pe) {
			err = "Failed to define XML properties for marshaller: " + xmlBuilder.toString();
			log.error(err);
			log.printStackTrace(pe);
			throw new RuntimeException(err);
		} catch (JAXBException je) {
			err = "Failed to create JAXB marshaller from context: " + context.toString();
			log.error(err);
			log.printStackTrace(je);
			throw new RuntimeException(err);
		}
		
		return xmlBuilder;
	}
	
	private StringWriter buildXMLFromData(Marshaller xmlBuilder, XMLData data) {
		StringWriter output = new StringWriter();
		
		try {
			xmlBuilder.marshal(data, output);
		} catch (JAXBException jaxb) {
			String err = "Failed to build XML object for class: " + data.toString();
			log.error(err);
			log.printStackTrace(jaxb);
			throw new RuntimeException(err);
		}
		
		return output;
	}
	
	private XMLData getXMLDataFromFile(String fileName, Class<? extends XMLData> responseXMLData) {
		JAXBContext context;
		Unmarshaller parser;
		
		try {
			context = JAXBContext.newInstance(responseXMLData);
			parser = context.createUnmarshaller();
			return responseXMLData.cast(parser.unmarshal(new File(fileName)));
		} catch (JAXBException jaxb) {
			String err = "Failed to parse XML data for class: " + responseXMLData.toString();
			log.error(err);
			log.printStackTrace(jaxb);
			throw new RuntimeException(err);
		}
	}
	
	private Transformer getTransformer() {
		try {
			return TransformerFactory.newInstance().newTransformer();
		} catch (TransformerConfigurationException tce) {
			String err = "Failed to configure XML transformer";
			log.error(err);
			log.printStackTrace(tce);
			throw new RuntimeException(err);
		}
	}
}
 