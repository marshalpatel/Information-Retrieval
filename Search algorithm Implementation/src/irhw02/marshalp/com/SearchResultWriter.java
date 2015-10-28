package irhw02.marshalp.com;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;
import java.util.Stack;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

public class SearchResultWriter {

	BufferedWriter bw = null;

	public SearchResultWriter(String filename) throws IOException {

		FileWriter fw = new FileWriter(filename, true);
		bw = new BufferedWriter(fw);
	}

	public void writeResultsToFile(int queryId, PriorityQueue<RelevanceScore> pq)
			throws IOException {

		System.out.println("Writing Results...");

		Stack<RelevanceScore> stack = new Stack<RelevanceScore>();

		while (!pq.isEmpty()) {

			stack.push(pq.poll());

		}

		writer(queryId, stack);

	}

	private void writer(int queryId, Stack<RelevanceScore> s)
			throws IOException {

		int rank = 1;
		while (!s.isEmpty()) {
			RelevanceScore rs = s.pop();
			bw.write(rs.getQueryId() + "\t0\t" + rs.getDocName() + "\t\t\t"
					+ rank + "\t\t\t" + rs.getScore() + "\t\t\trun-1");
			bw.newLine();
			bw.flush();
			rank++;
		}

		bw.close();

	}

	public void writeAlgorithmResults(int queryId, ScoreDoc[] hits, IndexSearcher searcher, String runId) throws IOException {
		System.out.println("Writing Results...");

		int rank = 1;
		for (int i = 0; i < hits.length; i++) {

			Document doc = searcher.doc(hits[i].doc);
			float score = hits[i].score;
			
			bw.write(queryId + "\t0\t" + doc.get("DOCNO") + "\t\t\t"
					+ rank + "\t\t\t" + score + "\t\t\t"+runId);
			bw.newLine();
			bw.flush();
			
			rank++;
		}
		
		bw.close();

	}

}
