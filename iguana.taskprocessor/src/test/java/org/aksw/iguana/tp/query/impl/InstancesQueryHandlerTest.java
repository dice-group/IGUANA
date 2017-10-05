package org.aksw.iguana.tp.query.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.Worker;
import org.aksw.iguana.tp.tasks.impl.stresstest.worker.impl.UPDATEWorker;
import org.aksw.iguana.tp.utils.FileUtils;
import org.junit.After;
import org.junit.Test;

/**
 * Unit test for InstancesQueryHandler
 * 
 * @author f.conrads
 *
 */
public class InstancesQueryHandlerTest {

	/**
	 * Clean up the directory
	 */
	@After
	public void clean() {
		File dir = new File("queryInstances");
		dir.delete();
	}
	
	/**
	 * Tests the id handling
	 * 
	 * @throws IOException
	 */
	@Test
	public void testIdHandling() throws IOException {
		InstancesQueryHandler handler = new InstancesQueryHandler(new ArrayList<Worker>());
		File tmpDir = new File("tmp");
		tmpDir.mkdir();
		File tmp = handler.createFileWithID(tmpDir, "test");
		assertEquals("test0", tmp.getName());
		tmp.delete();
		tmp = handler.createFileWithID(tmpDir, "test");
		assertEquals("test1", tmp.getName());
		tmp.delete();
		tmp = handler.createFileWithID(tmpDir, "test");
		assertEquals("test2", tmp.getName());
		tmp.delete();
		tmpDir.delete();
		
	}

	/**
	 * Test if the generated queries will be inserted into the Worker
	 */
	@Test
	public void testWorkerInjection() {
		List<Worker> workers =new ArrayList<Worker>();
		UPDATEWorker  worker = new UPDATEWorker("1", 1, null, "",  null, 
				"src/test/resources/queryhandler/updates/", 0, 0, "NONE",null);
		workers.add(worker);
		InstancesQueryHandler handler = new InstancesQueryHandler(workers);
		
		handler.generateQueries();
		File[] files = worker.getUpdateFiles();
		List<String> names = new LinkedList<String>();
		assertTrue(files.length == 3);
		File remove = new File(files[0].getParent());
		for (File file : files) {
			names.add(file.getName());
		}
		assertTrue(names.contains("update1.sparql"));
		assertTrue(names.contains("update2.sparql"));
		assertTrue(names.contains("update3.sparql"));
		remove.delete();
	}

	/**
	 * Checks the caching of the QueryHandler
	 */
	@Test
	public void checkCaching() {
		InstancesQueryHandler handler = new InstancesQueryHandler(new ArrayList<Worker>());
		File[] files = handler.generateSPARQL("src/test/resources/queryhandler/queries.sparql");
		assertTrue(files.length==2);
		File remove = files[0].getParentFile();
		for(File f : files) {
			f.delete();
		}
		InstancesQueryHandler handler2 = new InstancesQueryHandler(new ArrayList<Worker>());
		files = handler2.generateSPARQL("src/test/resources/queryhandler/queries.sparql");
		assertTrue(files.length==0);
		remove.delete();
	}

	/**
	 * Tests the sparql generation
	 * @throws IOException 
	 */
	@Test
	public void testSPARQL() throws IOException {
		InstancesQueryHandler handler = new InstancesQueryHandler(new ArrayList<Worker>());
		File[] files = handler.generateSPARQL("src/test/resources/queryhandler/queries.sparql");
		
		assertTrue(files.length==2);
		File remove = new File(files[0].getParent());
		for (File file : files) {
			assertTrue(file.getParentFile().getParentFile().getName().equals("queryInstances"));
			if (file.getName().equals("sparql0")) {
				assertEquals("SELECT bla0;", FileUtils.readLineAt(0, file));
			}
			else if (file.getName().equals("sparql1")) {
				assertEquals("SELECT bla1;", FileUtils.readLineAt(0, file));
			}
			file.delete();
		}
		remove.delete();
	}

	/**
	 * Test the Update as path generation
	 */
	@Test
	public void testUPDATEAsPath() {
		InstancesQueryHandler handler = new InstancesQueryHandler(new ArrayList<Worker>());
		File[] files = handler.generateUPDATE("src/test/resources/queryhandler/updates");
		List<String> names = new LinkedList<String>();
		assertTrue(files.length == 3);
		File remove = new File(files[0].getParent());
		for (File file : files) {
			names.add(file.getName());
		}
		assertTrue(names.contains("update1.sparql"));
		assertTrue(names.contains("update2.sparql"));
		assertTrue(names.contains("update3.sparql"));
		remove.delete();
	}

	/**
	 * Test the Update as file generation
	 * @throws IOException 
	 */
	@Test
	public void testUPDATEAsFile() throws IOException {
		InstancesQueryHandler handler = new InstancesQueryHandler(new ArrayList<Worker>());
		File[] files = handler.generateUPDATE("src/test/resources/queryhandler/updates.sparql");
		assertTrue(files.length==2);
		File remove = new File(files[0].getParent());
		for (File file : files) {
			assertTrue(file.getParentFile().getParentFile().getName().equals("queryInstances"));
			if (file.getName().equals("update0")) {
				assertEquals("INSERT bla1;", FileUtils.readLineAt(0, file));
			}
			else if (file.getName().equals("update1")) {
				assertEquals("INSERT bla2;", FileUtils.readLineAt(0, file));
			}
			file.delete();
		}
		remove.delete();
	}
}
