package irhw02.marshalp.com;

import java.io.IOException;
import java.util.HashMap;
import java.util.PriorityQueue;

import org.apache.lucene.queryparser.classic.ParseException;

public class SearchTrecTopics {
	
	
	
	public void searchTrecTopics() throws IOException, ParseException{
		
		TrecParser tp = new TrecParser("topics.51-100");
		tp.parseTopics();
		
		HashMap<Integer,String> shortQueries = tp.getShortQueryMap();
		HashMap<Integer,String> longQueries = tp.getLongQueryMap();
		
		
		for(int i = 51;i<=100;i++){
			
			System.out.println("############## Processing: " + i+" #######");
			EasySearch esObj = new EasySearch(
				"E:\\Marshal\\Masters\\Search\\Assignment2\\index\\index",i);
			
			esObj.parseQuery(shortQueries.get(i));
			esObj.init();
			PriorityQueue<RelevanceScore> pq = esObj.getRelevanceScore();
			SearchResultWriter srw = new SearchResultWriter("title.txt");
			srw.writeResultsToFile(esObj.getQueryId(), pq);
		}
		
		System.out.println("============= Processing Long Queries =============");
		
			for(int i = 51;i<=100;i++){
			
			System.out.println("############## Processing: " + i+" #######");
			EasySearch esObj = new EasySearch(
				"E:\\Marshal\\Masters\\Search\\Assignment2\\index\\index",i);
			
			esObj.parseQuery(longQueries.get(i));
			esObj.init();
			PriorityQueue<RelevanceScore> pq = esObj.getRelevanceScore();
			SearchResultWriter srw = new SearchResultWriter("desc.txt");
			srw.writeResultsToFile(esObj.getQueryId(), pq);
		}
		
	}

}
