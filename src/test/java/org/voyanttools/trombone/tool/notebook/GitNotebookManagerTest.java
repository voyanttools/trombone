package org.voyanttools.trombone.tool.notebook;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.voyanttools.trombone.storage.Storage;
import org.voyanttools.trombone.storage.file.FileStorage;
import org.voyanttools.trombone.util.FlexibleParameters;
import org.voyanttools.trombone.util.TestHelper;


public class GitNotebookManagerTest {

	@Test
	public void test() throws IOException {
		FileStorage storage = new FileStorage(TestHelper.getTemporaryTestStorageDirectory());
		
		assertFalse(exists(storage, "ajmacdonald@gh_notebook"));
		save(storage);
		assertTrue(exists(storage, "ajmacdonald@gh_notebook"));
		load(storage);
		delete(storage);
		assertFalse(exists(storage, "ajmacdonald@gh_notebook"));
		reindex(storage);
		assertTrue(exists(storage, "notebook")); // reindexing uses filename as notebookId so the ID to check also needs to be different
		
		storage.destroy();
	}
	
	private void save(FileStorage storage) throws IOException {
		File notebookFile = TestHelper.getResource("json/notebook.json");
		String fileContents = FileUtils.readFileToString(notebookFile, "UTF-8");
		String metadata = fileContents.substring(fileContents.indexOf("{\"title\""), fileContents.indexOf(",\"cells\""));
		FlexibleParameters parameters = new FlexibleParameters(new String[] {"action=save","key=foobar","spyral-id=ajmacdonald@gh","data="+fileContents,"metadata="+metadata,"name=notebook"});
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
	}
	
	private void load(FileStorage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[] {"action=load","id=ajmacdonald@gh_notebook"});
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
		String data = gnm.getData();
		assertTrue(data.indexOf("ajmacdonald@gh") != -1);
	}
	
	private void delete(FileStorage storage) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[] {"action=delete","key=foobar","spyral-id=ajmacdonald@gh","id=ajmacdonald@gh_notebook"});
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
	}
	
	private boolean exists(FileStorage storage, String notebookId) throws IOException {
		FlexibleParameters parameters = new FlexibleParameters(new String[] {"action=exists","id="+notebookId});
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
		return gnm.getData().equals("true");
	}

	static void reindex(FileStorage storage) throws IOException {
		File notebookFile = TestHelper.getResource("json/notebook.json");
		File notebookStorage = new File(storage.storageLocation, Storage.Location.notebook.toString());
		FileUtils.copyFileToDirectory(notebookFile, notebookStorage);
		
		FlexibleParameters parameters = new FlexibleParameters(new String[] {"action=reindex"});
		GitNotebookManager gnm = new GitNotebookManager(storage, parameters);
		gnm.run();
	}


}
