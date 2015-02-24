package nl.base.crypto.gpg;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import junit.framework.TestCase;
import nl.base.crypto.gpg.GPG.GPGException;

import org.apache.commons.io.IOUtils;

public class TestGPG extends TestCase {

	private static final String SECKEY_ASC_RESOURCE_FILENAME = "seckey.asc";
	private static final String PUBKEY_ASC_RESOURCE_FILENAME = "pubkey.asc";
	private static final String JUNIT_PASSPHRASE = "JUnitPassphrase";
	private static final String JUNIT_KEYPAIR_FINGERPRINT = "AB98FD9C260FD9F4E323BB8E1084E2961A0D3FC6";

	public void testHaveKey() throws IOException {
		GPG tool = getNewJUnitGPGTool();
		try (InputStream is = JUnitUtil.getResourceInputStream(PUBKEY_ASC_RESOURCE_FILENAME)) {
			assertTrue("Key was not successfully imported / haveKey doesn't see it",
					tool.havePublicKey(tool.getFingerPrint(is)));
		}
		assertFalse("haveKey() sees a key that it shouldn't.", tool.havePublicKey("foobar"));
	}

	public void testEncrypt() throws IOException {
		GPG tool = getNewJUnitGPGTool();
		File cipherTmp = File.createTempFile("JUnit", ".gpg");
		tool.encrypt(new ByteArrayInputStream("this is cleartext".getBytes()), cipherTmp,
				getJunitKeyringFingerPrint(tool));
		String actual = new String(Files.readAllBytes(cipherTmp.toPath()));
		assertNotSame("cleartext not encrypted", "this is cleartext", actual);
	}

	public void testDecrypt() throws IOException {
		GPG tool = getNewJUnitGPGTool();
		InputStream decryptStream = tool.decrypt(
				JUnitUtil.getResourceInputStream("gpgencrypted.gpg"), JUNIT_PASSPHRASE);
		assertEquals("Decrypt failed.", "this is a statically encrypted resource for JUnit testing\n",
				IOUtils.toString(decryptStream));
	}

	public void testDeleteKey() throws IOException {
		GPG tool = getNewJUnitGPGTool();
		try {
			tool.deletePublicKey(getJunitKeyringFingerPrint(tool));
			throw new IllegalStateException("deletePublicKey should have thrown an error, but it didn't");
		} catch (GPGException e) {
			if(!e.getMessage().contains("gpg: there is a secret key for public key")) {
				throw new IllegalStateException("Wrong exception", e);
			}
		}
		tool.deleteSecretKey(getJunitKeyringFingerPrint(tool));
		// Now we should be allowed to delete public key
		tool.deletePublicKey(getJunitKeyringFingerPrint(tool));
	}

	/**
	 *
	 * Utility method to get a GPG instance with clean keyrings.
	 *
	 * 	@return GPG instance with default JUnit keyrings imported
	 */
	private GPG getNewJUnitGPGTool() {
		try (InputStream pkis = JUnitUtil.getResourceInputStream(PUBKEY_ASC_RESOURCE_FILENAME);
				InputStream skis = JUnitUtil.getResourceInputStream(SECKEY_ASC_RESOURCE_FILENAME)) {
			GPG tool = new GPG(File.createTempFile("JUnit", "pkr"), File.createTempFile("JUnit", "skr"));
			tool.importKey(pkis);
			tool.importKey(skis);
			return tool;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getJunitKeyringFingerPrint(GPG gpg) {
		try (InputStream pkis = JUnitUtil.getResourceInputStream(PUBKEY_ASC_RESOURCE_FILENAME)) {
			return gpg.getFingerPrint(pkis);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}