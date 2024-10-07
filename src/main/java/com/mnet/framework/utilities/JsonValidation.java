package com.mnet.framework.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.reporting.FrameworkLog;
import com.mnet.framework.reporting.TestReporter;
import com.mnet.framework.reporting.TestStep;
import com.mnet.framework.reporting.TestStep.ReportLevel;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * Utility to validate JSON schema
 * @author kotwarx2
 *
 */
public class JsonValidation {
	
	private static String JSON_PATH = FrameworkProperties.getSystemProperty("user.dir") + File.separator
			+ FrameworkProperties.getProperty("JSON_PATH");
	private static TestReporter report;
	private static FrameworkLog log;
	
	/**
	 * 
	 * @param log
	 * @param schemaFile - Pass the name of the schema file
	 * @param jsonFileName - Pass complete / partial filename which you want to compare against the schema
	 * @implNote It will validate the latest file containing the filename in case multiple files with same partial names exist.
	 */
	public static boolean validateJsonSchema(MITETest currentTest, String schemaFile, String jsonFileName) {
		
		log = currentTest.getLog();
		report = currentTest.getReport();
		FileUtilities fileUtils = new FileUtilities(log);
		
		ObjectMapper objectMapper = new ObjectMapper();
		JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
		String localPath = log.getLogDirectory() + File.separator;

		try {
			String fileName = fileUtils.getLatestFile(localPath, jsonFileName);
			InputStream jsonStream = new FileInputStream(localPath+fileName);
			InputStream schemaStream = new FileInputStream(JSON_PATH+schemaFile);
			JsonNode json = objectMapper.readTree(jsonStream);
			JsonSchema schema = schemaFactory.getSchema(schemaStream);
			Set<ValidationMessage> validationResult = schema.validate(json);

			if (validationResult.isEmpty()) {
				report.logStep(TestStep.builder().message("No validation errors were found. Schema validation successful for "+fileName+" against schema "+schemaFile).build());
				return true;
			}
			validationResult.forEach(vm -> report.logStep(TestStep.builder().reportLevel(ReportLevel.ERROR).message(vm.getMessage()).build()));
		} catch (IOException ioe) {
			String err = "Failed to find file at path = "+localPath+jsonFileName;
			log.error(err);
			log.printStackTrace(ioe);
			throw new RuntimeException(ioe);
		}
		
		return false;
		
	}

}
