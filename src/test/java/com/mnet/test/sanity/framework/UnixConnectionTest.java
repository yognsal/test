package com.mnet.test.sanity.framework;

import java.io.File;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.framework.middleware.UnixCommand;
import com.mnet.framework.reporting.TestReporter.ReportLevel;

public class UnixConnectionTest extends MITETest {

	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.REMOTE_MACHINE);
		relativeDataDirectory = "sanity/framework";
		super.initialize(context);
	}
	
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void unixConnectionTest(String localPath, String remotePath, String fileName) {
		String modifiedFileName = "test_" + fileName;
		String modifiedLocalPath = localPath + File.separator + modifiedFileName;
		String modifiedRemotePath = remotePath + File.separator + modifiedFileName;
		String rename = "mv " + fileName + " " + modifiedFileName;
		
		remoteMachine.copyFileToRemote(localPath + File.separator + fileName, remotePath + File.separator + fileName);
		report.logStep(ReportLevel.PASS,"Copied " + fileName + " from local: " + localPath + " to remote: " + remotePath);
		
		remoteMachine.queueCommands(new UnixCommand(rename, remotePath));
		remoteMachine.executeShellCommands();
		report.logStep(ReportLevel.PASS, "Renamed " + fileName + " to " + modifiedFileName + " in remote");
		
		remoteMachine.copyFileToLocal(modifiedRemotePath, modifiedLocalPath);
		report.assertCondition(fileManager.fileExists(modifiedLocalPath), true,
				"Modified file was successfully copied to local at: " + modifiedLocalPath, 
				"Modified file could not be found at: " + modifiedLocalPath);
	}
}
