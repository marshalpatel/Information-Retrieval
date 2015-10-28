package irhw02.marshalp.com;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class TrecParser {

	private String path;
	private HashMap<Integer, String> longQueryMap;
	private HashMap<Integer, String> shortQueryMap;

	public TrecParser(String path) {

		this.path = path;
		this.longQueryMap = new HashMap<Integer, String>();
		this.shortQueryMap = new HashMap<Integer, String>();
	}

	public HashMap<Integer, String> getShortQueryMap() {

		return this.shortQueryMap;
	}

	public HashMap<Integer, String> getLongQueryMap() {

		return this.longQueryMap;
	}

	public void parseTopics() throws IOException {

		FileReader fr = new FileReader(this.path);

		BufferedReader br = new BufferedReader(fr);
		String s = "";
		String t = "";
		while ((s = br.readLine()) != null) {

			t = "";
			if (s.equals("<top>")) {
				while ((s = br.readLine()) != null && !s.equals("</top>")) {
					if (!s.equals("")) {
						t += s;
						t.trim();
					}
				}

				setLongQueryMap(t);
				setShortQueryMap(t);

			}
		}
		
		br.close();

	}

	private void setLongQueryMap(String t) {

		String longQuery = StringUtils.substringsBetween(t, "<desc>", "<")[0]
				.replaceAll("Description:", "").trim();

		String queryId = StringUtils.substringsBetween(t, "<num>", "<")[0]
				.replaceAll("Number:", "").trim();

		this.longQueryMap.put(Integer.parseInt(queryId), longQuery);
	}

	private void setShortQueryMap(String t) {

		String shortQuery = StringUtils.substringsBetween(t, "<title>", "<")[0]
				.replaceAll("Topic:", "").trim();

		String queryId = StringUtils.substringsBetween(t, "<num>", "<")[0]
				.replaceAll("Number:", "").trim();

		this.shortQueryMap.put(Integer.parseInt(queryId), shortQuery);

	}

}
