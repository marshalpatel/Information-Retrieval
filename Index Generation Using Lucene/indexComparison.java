package ir.IRHW01.com;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class indexComparison {
	
	
	
	Index index_keyword;
	Index stop_analyzer;
	Index simple_analyzer;
	
	
	public indexComparison() throws IOException{
		
		 index_keyword = new Index("KeywordAnalyzer");
		 stop_analyzer = new Index("StopAnalyzer");
		 simple_analyzer = new Index("SimpleAnalyzer");
	}
	
	public void compareIndex() throws IOException{
		
		 index_keyword.createIndex("E:\\Marshal\\Masters\\Search\\Assignment1\\corpus\\corpus");
		 stop_analyzer.createIndex("E:\\Marshal\\Masters\\Search\\Assignment1\\corpus\\corpus");
		 simple_analyzer.createIndex("E:\\Marshal\\Masters\\Search\\Assignment1\\corpus\\corpus");
	}

}


/*
 * Same as generateIndex Class
 */

class Index {

	/*
	 * Class variables to store index writer, path of the index directory, and
	 * type of analyzer.
	 */
	private IndexWriter idxWriter = null;
	IndexReader idxReader;
	private String analyzer;
	private String indexPath;

	/*
	 * Constants to store String literals.
	 */
	public static final String DOCNO = "DOCNO";
	public static final String HEAD = "HEAD";
	public static final String BYLINE = "BYLINE";
	public static final String DATELINE = "DATELINE";
	public static final String TEXT = "TEXT";

	public static final String[] fieldNames = { DOCNO, HEAD, BYLINE, DATELINE,
			TEXT };

	/*
	 * By default assign the analyzer type as Standard analyzer.
	 */
	public Index() {

		this.analyzer = "StandardAnalyzer";
	}

	public Index(String analyzer) {

		this.analyzer = analyzer;
	}

	public String getAnalyzerType() {

		return this.analyzer;
	}

	/*
	 * Create an analyzer object based on the type of analyzer.
	 */

	private Analyzer getAnaLyzer() {

		if (this.analyzer.equals("KeywordAnalyzer")) {

			return new KeywordAnalyzer();
		}

		else if (this.analyzer.equals("SimpleAnalyzer")) {

			return new SimpleAnalyzer();
		}

		else if (this.analyzer.equals("StopAnalyzer")) {

			return new StopAnalyzer();
		}

		else
			return new StandardAnalyzer();

	}

	/*
	 * Get the index writer object.
	 */

	private IndexWriter getIndexWriter() throws IOException {

		Analyzer a = getAnaLyzer();

		if (idxWriter == null) {

			indexPath = this.analyzer + " index-directory";
			Directory indexDir = FSDirectory.open(Paths.get(indexPath));
			IndexWriterConfig config = new IndexWriterConfig(a);

			config.setOpenMode(OpenMode.CREATE);

			idxWriter = new IndexWriter(indexDir, config);
		}

		return idxWriter;
	}

	/*
	 * Close the index writer.
	 */

	private void close() throws CorruptIndexException, IOException {

		idxWriter.forceMerge(1);
		idxWriter.commit();
		idxWriter.close();
	}

	/*
	 * Start creating index by accessing the corpus file by file.
	 */

	public void createIndex(String dirPath) throws IOException {

		File[] files = new File(dirPath).listFiles();

		for (File f : files) {
			indexDocument(f);
		}

		close();
		printStatistics();
	}
	
	/*
	 * Read the file line by line, extract the content between each <DOC>...</DOC>
	 */

	private void indexDocument(File f) throws IOException {

		HashMap<String, StringBuilder> fm;
		if (!f.getName().equals(".DS_Store")) {

			BufferedReader br = new BufferedReader(new FileReader(f));

			String s = "";
			while ((s = br.readLine()) != null) {

				String temp = "";

				if (s.contains("<DOC>")) {

					s = br.readLine();

					while (!s.contains("</DOC>")) {

						temp += s + " ";

						s = br.readLine();
					}

					fm = parseTags(temp);
					indexDocumentHelper(fm);
				}

			}
			br.close();
		}

	}

	/*
	 * Index the fields. DOCNO will be indexed as StringField.
	 * Other fields will be indexed as TextField.
	 */
	private void indexDocumentHelper(HashMap<String, StringBuilder> fm)
			throws IOException {

		IndexWriter writer = getIndexWriter();

		Document doc = new Document();

		for (int i = 0; i < fieldNames.length; i++) {

			if (fieldNames[i].equals("DOCNO")) {

				doc.add(new StringField(fieldNames[i], fm.get(fieldNames[i])
						.toString(), Field.Store.YES));
			}

			else {
				doc.add(new TextField(fieldNames[i], fm.get(fieldNames[i])
						.toString(), Field.Store.YES));
			}
		}

		writer.addDocument(doc);

	}
	
	
	/*
	 * Read the content between each <DOCNO>, <HEAD>, <TEXT>, <BYLINE> and <DATELINE> 
	 * and put in the hashmap.
	 */

	private HashMap<String, StringBuilder> parseTags(String str) {

		HashMap<String, StringBuilder> fieldMap = new HashMap<String, StringBuilder>();

		for (int i = 0; i < fieldNames.length; i++) {

			fieldMap.put(fieldNames[i], new StringBuilder(""));

		}

		String[][] content = new String[fieldNames.length][];

		for (int i = 0; i < fieldNames.length; i++) {

			content[i] = StringUtils.substringsBetween(str, "<" + fieldNames[i]
					+ ">", "</" + fieldNames[i] + ">");
		}

		for (int i = 0; i < content.length; i++) {

			StringBuilder temp = new StringBuilder(" ");
			if (content[i] != null) {

				for (int j = 0; j < content[i].length; j++) {

					temp.append(content[i][j]);

					if (content[i].length - 1 > j)
						temp.append(" ");
				}

				fieldMap.put(fieldNames[i], temp);

			}

		}

		return fieldMap;
	}
	
	/*
	 * Utility method to print the statistics about the index
	 */

	private void printStatistics() throws IOException {

		System.out.println();
		
		System.out.println("*********************** " + analyzer
				+ " *********************************");

		idxReader = DirectoryReader
				.open(FSDirectory.open(Paths.get(indexPath)));

		System.out.println("Total no. of document in the corpus: "
				+ idxReader.maxDoc());

		Terms vocabulary = MultiFields.getTerms(idxReader, "TEXT");

		System.out.println("Number of terms in dictionary for TEXT field:"
				+ vocabulary.size());

		System.out.println("Number of tokens for TEXT field:"
				+ vocabulary.getSumTotalTermFreq());

		idxReader.close();
	}

}

