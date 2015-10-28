package irhw02.marshalp.com;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;

public class CompareAlgorithms {

	String indexPath;
	private IndexReader reader;
	private IndexSearcher searcher;
	private String[] algorithms = { "VSM", "BM25", "LMDS", "LMJM" };

	public CompareAlgorithms(String indexPath) throws IOException {

		this.indexPath = indexPath;
		this.reader = DirectoryReader.open(FSDirectory.open(Paths
				.get(indexPath)));

		this.searcher = new IndexSearcher(reader);
	}

	public void compareAlgorithms() throws ParseException, IOException {

		// String queryString = "police";

		TrecParser tp = new TrecParser("topics.51-100");
		tp.parseTopics();

		HashMap<Integer, String> shortQueries = tp.getShortQueryMap();
		HashMap<Integer, String> longQueries = tp.getLongQueryMap();

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser q_parser = new QueryParser("TEXT", analyzer);

		for (int i = 0; i < this.algorithms.length; i++) {

			String algo = this.algorithms[i];

			if (algo.equals("BM25")) {
				this.searcher.setSimilarity(new BM25Similarity());
			} else if (algo.equals("VSM")) {
				this.searcher.setSimilarity(new DefaultSimilarity());
			} else if (algo.equals("LMDS")) {
				this.searcher.setSimilarity(new LMDirichletSimilarity());

			}

			else {
				this.searcher
						.setSimilarity(new LMJelinekMercerSimilarity(0.7f));

			}

			for (int j = 51; j <= 100; j++) {

				String queryString = shortQueries.get(j);
				Query query = q_parser.parse(QueryParser.escape(queryString));
				TopDocs results = this.searcher.search(query, 1000);
				ScoreDoc[] hits = results.scoreDocs;
				SearchResultWriter srw = new SearchResultWriter(algo
						+ "shortQuery.txt");

				srw.writeAlgorithmResults(j, hits, this.searcher, algo
						+ "_short");
			}

			for (int j = 51; j <= 100; j++) {

				String queryString = longQueries.get(j);
				Query query = q_parser.parse(QueryParser.escape(queryString));
				TopDocs results = this.searcher.search(query, 1000);
				ScoreDoc[] hits = results.scoreDocs;
				SearchResultWriter srw = new SearchResultWriter(algo
						+ "longQuery.txt");
				srw.writeAlgorithmResults(j, hits, this.searcher, algo
						+ "_long");
			}

		}

		// System.out.println("Searching for: " + query.toString("TEXT"));

		// int numTotalHits = results.totalHits;
		// System.out.println(numTotalHits + " total matching documents");

		// print retrieved results

	}

}
