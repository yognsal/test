package com.mnet.test.sanity.mobility;

import java.text.ParseException;

import org.testng.ITestContext;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mnet.framework.core.FrameworkProperties;
import com.mnet.framework.core.MITETest;
import com.mnet.framework.core.TestDataProvider;
import com.mnet.mobility.utilities.MobilityUtilities.MobilityOS;
import com.mnet.mobility.utilities.NGQPatientApp;

/**
 * Sanity test for sneakernet encrypted payload API call and validation
 * @author NAIKKX12
 *
 */
public class EncryptedSneakernetPayloadTest extends MITETest {
	
	@Override
	@BeforeClass
	public void initialize(ITestContext context) {
		attributes.add(TestAttribute.DATABASE);
		attributes.add(TestAttribute.AZURE);
		relativeDataDirectory = "sanity/mobility";
		super.initialize(context);
	}
	
	// Last Activity API call and validation
	@Test(dataProvider = "TestData", dataProviderClass = TestDataProvider.class)
	public void encryptedSneakernetPayloadTest(String payloadFile) throws ParseException {
		NGQPatientApp ngqPatientApp = new NGQPatientApp(this, MobilityOS.ANDROID);
		ngqPatientApp.setApiValidation(true);
		
		String payloadPath = FrameworkProperties.getProperty("TRANSMISSION_PAYLOAD_PATH_NGQ") + payloadFile;
		ngqPatientApp.encryptedSnearkernetTransmission(payloadPath);
	}
}
