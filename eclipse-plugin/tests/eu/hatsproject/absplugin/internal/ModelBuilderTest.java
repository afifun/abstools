package eu.hatsproject.absplugin.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;

import abs.frontend.analyser.SemanticErrorList;
import abs.frontend.ast.CompilationUnit;
import abs.frontend.typechecker.locationtypes.infer.LocationTypeInferrerExtension.LocationTypingPrecision;
import eu.hatsproject.absplugin.internal.IncrementalModelBuilder;
import eu.hatsproject.absplugin.internal.NoModelException;
import eu.hatsproject.absplugin.internal.TypecheckInternalException;

public class ModelBuilderTest {
	private IncrementalModelBuilder modelbuilder;

	@Before
	public void setupModelBuilder(){
		this.modelbuilder  = new IncrementalModelBuilder();
	}
	
	@Test
	public void testTypecheck() throws IOException{
		String moduleText = 
			"//$id: Peertopeer.abs 5108 2010-07-17 20:14:12Z jschaefer $ \n"+
			"\n"+
			"module PeerToPeer;\n"+
			"import * from ImportTest;\n"+
			"\n"+
			"//type synonyms  \n"+
			"\n"+
			"type Filename = String ;\n"+
			"type Filenames = Set<String> ;\n"+
			"type Packet = String ;\n"+
			"type File = List<Packet> ;\n"+
			"type Catalog = List<Pair<Peer, Filenames> > ;\n"+
			"\n"+
			" \n"+
			" \n"+
			"// Application functions\n"+
			"def Peer findServer(Filename file, Catalog catalog) =\n"+
			"  case catalog {\n"+
			"    Nil => null;\n"+
			"    Cons(Pair(server, files), rest) =>\n"+
			"      case contains(files, file) { True => server;\n"+
			"                                   False => findServer(file, rest); };\n"+
			"  };\n"+
			"\n"+
			"interface Server {\n"+
			"  Filenames enquire();\n"+
			"  Int getLength(Filename fId);\n"+
			"  Packet getPack(Filename fId, Int pNbr);\n"+
			"}\n"+
			"\n"+
			"\n"+
			"interface Peer extends Client, Server { \n"+
			"   Unit setAdmin(Network admin);\n"+
			"   Unit run();\n"+
			"}\n"+
			"\n"+
			"interface Network {\n"+
			"  List<Peer> getNeighbors(Peer caller);\n"+
			"}\n"+
			"\n"+
			"interface DataBase {\n"+
			"  File getFile(Filename fId);\n"+
			"  Int getLength(Filename fId);\n"+
			"  Unit storeFile(Filename fId, File file);\n"+
			"  Filenames listFiles();\n"+
			"}\n"+
			"\n"+
			"interface Client {\n"+
			"  Unit reqFile(Server sId, Filename fId);\n"+
			"}\n"+
			"\n"+
			"class DataBaseImpl(Map<Filename, File> db) implements DataBase {\n"+
			"  File getFile(Filename fId) {\n"+
			"    return lookup(db, fId);\n"+
			"  }\n"+
			"\n"+
			"  Int getLength(Filename fId) {\n"+
			"    return length(lookup(db,fId));\n"+
			"  }\n"+
			"\n"+
			"  Unit storeFile(Filename fId, File file) {\n"+
			"    db = InsertAssoc(Pair(fId,file), db);\n"+
			"  } \n"+
			"\n"+
			"  Filenames listFiles() {\n"+
			"    return keys(db);\n"+
			"  }\n"+
			"}\n"+
			"\n"+
			"class Node(DataBase db, Filename file) implements Peer {\n"+
			"  Catalog catalog = Nil;\n"+
			"  List<Peer> myNeighbors = Nil;\n"+
			"  Network admin = null;\n"+
			"\n"+
			"  Unit run() {\n"+
			"    Fut<Catalog> c ; \n"+
			"    Fut<List<Peer>> f;\n"+
			"    Server server ; \n"+
			"\n"+
			"    await admin != null;\n"+
			"    f = admin!getNeighbors(this);   // Asynchronous call to admin\n"+
			"    await f?;\n"+
			"    myNeighbors = f.get;\n"+
			"    c = this!availFiles(myNeighbors); // Asynchronous call\n"+
			"    await c?;               // Allow other peers to call in the meantime\n"+
			"    catalog = c.get;        // Build the catalog\n"+
			"    server = findServer(file, catalog); // Find the server for the requested file\n"+
			"    if (server != null) {\n"+
			"      this.reqFile(server,file) ;        // Download file\n"+
			"    }\n"+
			"  }\n"+
			"\n"+
			"  Unit setAdmin(Network admin) {\n"+
			"    this.admin = admin;\n"+
			"  }\n"+
			"\n"+
			"  Filenames enquire() { \n"+
			"    Fut<Filenames> f ;  \n"+
			"    f = db!listFiles();\n"+
			"    await f?;\n"+
			"    return f.get;\n"+
			"  }\n"+
			"\n"+
			"  Int getLength(Filename fId) {\n"+
			"    Fut<Int> length ; \n"+
			"    length = db!getLength(fId);\n"+
			"    await length?;\n"+
			"    return length.get;\n"+
			"  }\n"+
			"\n"+
			"  Packet getPack(Filename fId, Int pNbr) {\n"+
			"    File f = Nil;\n"+
			"    Fut<File> ff;\n"+
			"    ff = db!getFile(fId);\n"+
			"    await ff?;\n"+
			"    f = ff.get;\n"+
			"    return nth(f, pNbr);\n"+
			"  }\n"+
			"\n"+
			"  Catalog availFiles (List<Peer> sList) {\n"+
			"    Catalog cat = Nil;\n"+
			"    Filenames fNames = EmptySet; \n"+
			"    Fut<Filenames> fN;\n"+
			"    Catalog catList = Nil; \n"+
			"    Fut<Catalog> cL;\n"+
			"\n"+
			"    if (sList != Nil) {\n"+
			"      fN = head(sList)!enquire();\n"+
			"      cL = this!availFiles(tail(sList));\n"+
			"      await fN? & cL?;\n"+
			"      catList = cL.get;\n"+
			"      fNames = fN.get;\n"+
			"      cat = appendright(catList, Pair(head(sList), fNames)); \n"+
			"    }\n"+
			"    return cat; \n"+
			"  }\n"+
			"\n"+
			"  Unit reqFile(Server sId, Filename fId) {\n"+
			"    File file = Nil;\n"+
			"    Packet pack = \"\";\n"+
			"    Int lth = 0;\n"+
			"    Fut<Int> l1;\n"+
			"    Fut<Packet> l2;\n"+
			"\n"+
			"    l1 = sId!getLength(fId);\n"+
			"    await l1?;\n"+
			"    lth = l1.get; \n"+
			"    while (lth > 0) {\n"+
			"      lth = lth - 1;            // indexing is zero-based\n"+
			"      l2 = sId!getPack(fId, lth);\n"+
			"      await l2?;\n"+
			"      pack = l2.get ;\n"+
			"      file = Cons(pack, file); \n"+
			"    } \n"+
			"    db!storeFile(fId, file);\n"+
			"  }\n"+
			"}\n"+
			"\n"+
			"class OurTopology(Peer node0,   Peer node1,   Peer node2,   Peer node3)\n"+
			"implements Network\n"+
			"{\n"+
			"  List<Peer> getNeighbors(Peer caller) {\n"+
			"    List<Peer> res = Nil;\n"+
			"    if (caller == node0) { res = list[node1, node2]; }\n"+
			"    if (caller == node1) { res = list[node3]; }\n"+
			"    if (caller == node2) { res = list[node0, node1, node3]; }\n"+
			"    if (caller == node3) { res = list[node0, node2]; }\n"+
			"    return res;\n"+
			"  }\n"+
			"}\n"+
			"\n"+
			"{ \n"+
			"  Peer node0;\n"+
			"  Peer node1;\n"+
			"  Peer node2;\n"+
			"  Peer node3;\n"+
			"  DataBase db0;\n"+
			"  DataBase db1;\n"+
			"  DataBase db2;\n"+
			"  Network admin;\n"+
			"  // Map<Filename, File>\n"+
			"  db0 = new cog DataBaseImpl(map[Pair(\"file0\", list[\"file\", \"from\", \"db0\"])]);\n"+
			"  db1 = new cog DataBaseImpl(map[Pair(\"file1\", list[\"file\", \"from\", \"db1\"])]);\n"+
			"  db2 = new cog DataBaseImpl(map[Pair(\"file2\", list[\"file\", \"from\", \"db2\"])]);\n"+
			"  node0 = new cog Node(db0, \"file2\");\n"+
			"  node1 = new cog Node(db1, \"file2\");\n"+
			"  node2 = new cog Node(db2, \"file1\");\n"+
			"  node3 = new cog Node(db2, \"file0\");\n"+
			"  admin = new cog OurTopology(node0, node1, node2, node3);\n"+
			"  node0!setAdmin(admin);\n"+
			"  node1!setAdmin(admin);\n"+
			"  node2!setAdmin(admin);\n"+
			"  node3!setAdmin(admin);\n"+
			"  \n"+
			"  node0!run();\n"+
			"  node1!run();\n"+
			"  node2!run();\n"+
			"  node3!run();\n"+
			"}\n";

		CompilationUnit testcu = abs.frontend.parser.Main.parseUnit(new File("PeerToPeer.abs"), moduleText, new StringReader(moduleText), true);
		assertEquals(0, testcu.getParserErrors().size());
		try{
			modelbuilder.setCompilationUnit(testcu);
			SemanticErrorList testel = modelbuilder.typeCheckModel(true, "Somewhere", LocationTypingPrecision.BASIC.toString());
			assertEquals(2, testel.size()); //Why 2? Module not resolved gets inserted 2 times.
		} catch(RuntimeException e){
			fail("Runtime Exception on typecheck: "+e.toString());
		} catch(NoModelException e){
			fail("No model while setting compilation unit.");
		} catch(TypecheckInternalException e){
			fail("Exception while typechecking!" + e.toString());
		}
		String importTestText = "module ImportTest;";
		CompilationUnit importTestCU = abs.frontend.parser.Main.parseUnit(new File("importtest.abs"), importTestText, new StringReader(importTestText), true);
		try{
			modelbuilder.setCompilationUnit(importTestCU);
			SemanticErrorList testel = modelbuilder.typeCheckModel(true, "Somewhere", LocationTypingPrecision.BASIC.toString());
			assertEquals(0, testel.size());
		} catch(RuntimeException e){
			fail("Runtime Exception on typecheck: "+e.toString());
		} catch(NoModelException e){
			fail("No model while setting compilation unit.");
		} catch(TypecheckInternalException e){
			fail("Exception while typechecking!" + e.toString());
		}
		try{
			modelbuilder.removeCompilationUnit(importTestCU);
			SemanticErrorList testel = modelbuilder.typeCheckModel(true, "Somewhere", LocationTypingPrecision.BASIC.toString());
			assertEquals(2, testel.size()); //Why 2? Module not resolved gets inserted 2 times.
		} catch(RuntimeException e){
			fail("Runtime Exception on typecheck: "+e.toString());
		} catch(NoModelException e){
			fail("No model while setting compilation unit.");
		} catch(TypecheckInternalException e){
			fail("Exception while typechecking!" + e.toString());
		}
	}
}