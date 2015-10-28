package irhw02.marshalp.com;

import java.util.*;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

public class EasySearch {

	private IndexReader reader;
	private IndexSearcher searcher;
	private HashMap<Term, Integer> documentFrequencyMap;
	private int numberOfDocs;
	private float idf;
	private HashMap<Integer, Float> normalizedDocLength;
	private HashMap<Integer, HashMap<Term, Integer>> termCountMap;
	private HashMap<Integer, String> docIdNameMap;
	private Set<Term> queryTerms;
	private int queryId;
	
	
	public int getQueryId(){
		
		return this.queryId;
		
	}

	public EasySearch(String path) throws IOException {

		this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(path)));

		this.searcher = new IndexSearcher(reader);

		this.queryTerms = new LinkedHashSet<Term>();

		this.documentFrequencyMap = new HashMap<Term, Integer>();

		this.docIdNameMap = new HashMap<Integer, String>();

		this.normalizedDocLength = new HashMap<Integer, Float>();

		this.termCountMap = new HashMap<Integer, HashMap<Term, Integer>>();

		numberOfDocs = this.reader.maxDoc();

		this.queryId = 0;

	}

	public EasySearch(String path, int queryId) throws IOException {

		this.reader = DirectoryReader.open(FSDirectory.open(Paths.get(path)));

		this.searcher = new IndexSearcher(reader);

		this.queryTerms = new LinkedHashSet<Term>();

		this.documentFrequencyMap = new HashMap<Term, Integer>();

		this.docIdNameMap = new HashMap<Integer, String>();

		this.normalizedDocLength = new HashMap<Integer, Float>();

		this.termCountMap = new HashMap<Integer, HashMap<Term, Integer>>();

		numberOfDocs = this.reader.maxDoc();

		this.queryId = queryId;

	}

	public void parseQuery(String queryString) throws ParseException,
			IOException {

		Analyzer analyzer = new StandardAnalyzer();
		QueryParser q_parser = new QueryParser("TEXT", analyzer);
		
		Query query = q_parser.parse(QueryParser.escape(queryString));

		this.queryTerms = new LinkedHashSet<Term>();
		searcher.createNormalizedWeight(query, false).extractTerms(queryTerms);

	}

	/*
	 * Gets documents frequency of each term in the query.
	 */
	private void setDoumentFreq() throws IOException {

		// document frequency

		for (Term t : this.queryTerms) {

			this.documentFrequencyMap.put(t, 0);
		}

		for (Term t : this.queryTerms) {
			int df = this.reader.docFreq(new Term("TEXT", t.text()));
			this.documentFrequencyMap.put(t, df);
		}

		// return this.documentFrequencyMap;

	}

	private void setIDF() {

		for (Term t : this.queryTerms) {

			this.idf += Math
					.log10(1 + ((float) this.numberOfDocs / this.documentFrequencyMap
							.get(t)));

		}

	}

	private void setTermCountInDocumentHelper(Term t) throws IOException {

		DefaultSimilarity dSimi = new DefaultSimilarity();
		// Get the segments of the index
		List<LeafReaderContext> leafContexts = reader.getContext().reader()
				.leaves();

		for (int i = 0; i < leafContexts.size(); i++) {
			// Get document length
			LeafReaderContext leafContext = leafContexts.get(i);
			int startDocNo = leafContext.docBase;

			PostingsEnum de = MultiFields.getTermDocsEnum(leafContext.reader(),
					"TEXT", new BytesRef(t.text()));
			int doc = 0;
			if (de != null) {
				while ((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {

					// System.out.println("\"police\" occurs " + de.freq()+
					// " time(s) in doc(" +(de.docID() + startDocNo)+ ")");
					HashMap<Term, Integer> termFreqMap = this.termCountMap
							.get(de.docID() + startDocNo);

					if (termFreqMap != null) {
						termFreqMap.put(t, de.freq());
						this.termCountMap.put(de.docID() + startDocNo,
								termFreqMap);
					}

					else {
						termFreqMap = new HashMap<Term, Integer>();

						termFreqMap.put(t, de.freq());
						this.termCountMap.put(de.docID() + startDocNo,
								termFreqMap);

					}

				}
			}

		}

	}

	private void setTermCountInDoc() throws IOException {

		for (Term t : this.queryTerms) {

			setTermCountInDocumentHelper(t);

		}
	}

	public void setNormalizedDocLengthMap() throws IOException {

		DefaultSimilarity dSimi = new DefaultSimilarity();
		List<LeafReaderContext> leafContexts = reader.getContext().reader()
				.leaves();

		for (int i = 0; i < leafContexts.size(); i++) {
			// Get document length
			LeafReaderContext leafContext = leafContexts.get(i);
			int startDocNo = leafContext.docBase;
			int numberOfDoc = leafContext.reader().maxDoc();

			for (int docId = 0; docId < numberOfDoc; docId++) {
				// Get normalized length (1/sqrt(numOfTokens)) of the document
				float normDocLeng = dSimi.decodeNormValue(leafContext.reader()
						.getNormValues("TEXT").get(docId));
				// Get length of the document
				float docLeng = 1 / (normDocLeng * normDocLeng);
				// System.out.println("Length of doc(" + (docId +startDocNo)+
				// ", " + searcher.doc(docId +startDocNo).get("DOCNO")+ ") is "
				// + docLeng);
				// System.exit(0);
				this.docIdNameMap.put(docId + startDocNo,
						searcher.doc(docId + startDocNo).get("DOCNO"));
				this.normalizedDocLength.put(docId + startDocNo, docLeng);
			}

		}
	}

	public void init() throws IOException {

		setNormalizedDocLengthMap();
		setDoumentFreq();
		setIDF();
		setTermCountInDoc();

	}

	public PriorityQueue<RelevanceScore> getRelevanceScore() throws IOException {

		float termRelevance = 0;
		HashMap<Integer, Float> relevanceMap = new HashMap<Integer, Float>();
		PriorityQueue<RelevanceScore> pq = new PriorityQueue<RelevanceScore>();

		// float max_rel = Float.MIN_VALUE;
		// int docId = 0;

		List<RelevanceScore> relList = new ArrayList<RelevanceScore>();

		DefaultSimilarity dSimi = new DefaultSimilarity();
		// Get the segments of the index
		List<LeafReaderContext> leafContexts = reader.getContext().reader()
				.leaves();
		for (Term t : this.queryTerms) {
			System.out.println("=========== Computing relevance for "
					+ t.text() + " =======");
			for (int i = 0; i < leafContexts.size(); i++) {
				// Get document length
				LeafReaderContext leafContext = leafContexts.get(i);
				int startDocNo = leafContext.docBase;
				int numberOfDoc = leafContext.reader().maxDoc();
				// Get frequency of the term "police" from its postings
				PostingsEnum de = MultiFields.getTermDocsEnum(
						leafContext.reader(), "TEXT", new BytesRef(t.text()));
				int doc;
				if (de != null) {
					while ((doc = de.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
						float c = this.termCountMap
								.get(de.docID() + startDocNo).get(t);
						float normLength = this.normalizedDocLength.get(de
								.docID() + startDocNo);
						float tf = c / normLength;
						float idf = (float) Math
								.log10(1 + ((double) this.numberOfDocs / this.documentFrequencyMap
										.get(t)));
						termRelevance = tf * idf;
//						System.out
//								.println("Relevance for query term: "
//										+ t.text()
//										+ " for document ("
//										+ (de.docID() + startDocNo)
//										+ ") "
//										+ this.docIdNameMap.get(de.docID()
//												+ startDocNo) + " is "
//										+ termRelevance);
						if (relevanceMap.containsKey(de.docID() + startDocNo)) {
							float rel = relevanceMap.get(de.docID()
									+ startDocNo)
									+ termRelevance;
							relevanceMap.put(de.docID() + startDocNo, rel);

							setTop1000Results(
									pq,
									de.docID() + startDocNo,
									this.queryId,
									rel,
									this.docIdNameMap.get(de.docID()
											+ startDocNo));

						} else {
							relevanceMap.put(de.docID() + startDocNo,
									termRelevance);
							relList.add(new RelevanceScore(de.docID()
									+ startDocNo, this.queryId, termRelevance,
									this.docIdNameMap.get(de.docID()
											+ startDocNo)));
							setTop1000Results(
									pq,
									de.docID() + startDocNo,
									this.queryId,
									termRelevance,
									this.docIdNameMap.get(de.docID()
											+ startDocNo));

						}
						// System.out.println("\"police\" occurs " + de.freq()+
						// " time(s) in doc(" +(de.docID() + startDocNo)+ ")");
					}
				}
			}

		}

		// Collections.sort(relList);

		// System.out.println(max_rel);
		// System.out.println(docId);
		// System.out.println(relList.get(relList.size()-1));

		/*
		 * for(RelevanceScore s : relList){ System.out.println(s.score + " :" +
		 * s.docName); }
		 */

		// setTop1000Results(pq, relevanceMap);

		/*while (!pq.isEmpty()) {

			RelevanceScore rs = pq.poll();

			System.out.println(rs.getScore() + ": " + rs.getDocId() + ": "
					+ rs.getDocName());
		}*/

		return pq;

	}

		private void setTop1000Results(PriorityQueue<RelevanceScore> pq, int docId,
			int queryId, float scr, String docName) {

		RelevanceScore rs = new RelevanceScore(docId, queryId, scr, docName);

		//pq.offer(rs);

		if (pq.size() < 1000) {
			pq.offer(rs);
		} else {

			float tempScore = pq.peek().getScore();
			if (tempScore < scr) {
				pq.poll();
				if (pq.contains(rs)) {

					pq.remove(rs);
					pq.offer(rs);
				} else {

					pq.offer(rs);
				}
			}
		}

	}

	public void printRelevanceValues(HashMap<Integer, Float> rel) {

		System.out.print("Total relavance score for the entire query ");
		for (Term t : this.queryTerms) {

			System.out.print(t.text() + " ");
		}
		System.out.println();

		Iterator<Map.Entry<Integer, Float>> itr = rel.entrySet().iterator();

		while (itr.hasNext()) {

			Map.Entry<Integer, Float> entry = itr.next();

			System.out.println(this.docIdNameMap.get(entry.getKey()) + ": "
					+ entry.getValue());
		}

	}

	public void printQueryTerms() {

		System.out.println("Query terms:");

		for (Term t : this.queryTerms) {

			System.out.println(t.text());
		}

	}

}

class RelevanceScore implements Comparable<RelevanceScore> {

	private int docId;
	private int queryId;
	private float score;
	private String docName;

	public int getDocId() {
		return this.docId;
	}

	public int getQueryId() {
		return this.queryId;
	}

	public float getScore() {
		return this.score;
	}

	public String getDocName() {
		return this.docName;
	}

	public RelevanceScore(int dId, int qId, float s, String docName) {

		this.docId = dId;
		this.queryId = qId;
		this.score = s;
		this.docName = docName;
	}

	@Override
	public boolean equals(Object o) {

		if (o == this)
			return true;

		if (!(o instanceof RelevanceScore)) {
			return false;
		}

		RelevanceScore r = (RelevanceScore) o;

		if (r.docId == this.docId)
			return true;
		else
			return false;
	}

	@Override
	public int hashCode() {

		return new HashCodeBuilder(19, 31).append(this.docId)
				.append(this.queryId).hashCode();

	}

	@Override
	public int compareTo(RelevanceScore obj) {

		RelevanceScore rs = (RelevanceScore) obj;

		if (this.score > rs.score)
			return 1;
		else if (this.score < rs.score)
			return -1;

		else
			return 0;
	}
}
