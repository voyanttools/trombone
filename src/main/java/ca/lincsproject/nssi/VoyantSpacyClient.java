package ca.lincsproject.nssi;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.EntityType;

public class VoyantSpacyClient {

	public static enum JobStatus {
		IN_PROGRESS, READY, FAILED, CANCELLED
	}
	
	private static String accessToken;
	
	private static boolean debug = true;
	
	private static boolean convertToStanfordTypes = false;
	
	
	public static void main(String[] args) throws IOException {
		String testText = "I had seen little of Holmes lately. My marriage had drifted us away from each other. My own complete happiness, and the home-centred interests which rise up around the man who first finds himself master of his own establishment, were sufficient to absorb all my attention, while Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street, buried among his old books, and alternating from week to week between cocaine and ambition, the drowsiness of the drug, and the fierce energy of his own keen nature. He was still, as ever, deeply attracted by the study of crime, and occupied his immense faculties and extraordinary powers of observation in following out those clues, and clearing up those mysteries which had been abandoned as hopeless by the official police. From time to time I heard some vague account of his doings: of his summons to Odessa in the case of the Trepoff murder, of his clearing up of the singular tragedy of the Atkinson brothers at Trincomalee, and finally of the mission which he had accomplished so delicately and successfully for the reigning family of Holland. Beyond these signs of his activity, however, which I merely shared with all the readers of the daily press, I knew little of my former friend and companion.";
		String smallText = "Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street";
		
		long start = System.currentTimeMillis();
		debug = true;
		List<DocumentEntity> result = VoyantSpacyClient.submitJob(testText);
		long end = System.currentTimeMillis();
		long ellapsed = (end-start)/1000;
		System.out.println("ellapsed: "+ellapsed);
		VoyantSpacyClient.printResults(result);
	}
	
	private static void setAccessToken() throws IOException {
		VoyantSpacyClient.accessToken = "foo";
	}
	
	public static List<DocumentEntity> submitJob(String text) throws IOException {
		if (VoyantSpacyClient.accessToken == null) {
			setAccessToken();
		}
		
		if (debug) System.out.println("submitting job, size: "+text.getBytes().length);
		long start = System.currentTimeMillis();
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpPost httpPost = new HttpPost("https://lincs-api.lincsproject.ca/api/ner");
//			httpPost.setHeader("Authorization", "Bearer "+VoyantSpacyClient.accessToken);
			
			JSONObject jsonBody = new JSONObject();
			jsonBody.put("language", "en"); // TODO
			jsonBody.put("text", text);
			String input = jsonBody.toString();
			
			StringEntity body = new StringEntity(input, "UTF-8");
			httpPost.setEntity(body);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
			
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				long end = System.currentTimeMillis();
				long ellapsed = end-start;
				if (debug) System.out.println("job time: "+ellapsed);
				JSONObject json = getJSONResponse(response);
				return jsonToDocumentEntities(json);
			} catch (HttpResponseException e) {
//				if (e.getStatusCode() == 401) {
//					setAccessToken();
//					return submitJob(text);
//				} else {
					throw new IOException("Error submitting job. Status code: "+e.getStatusCode());
//				}
			}
		}
	}

	
	private static List<DocumentEntity> jsonToDocumentEntities(JSONObject json) {
		List<DocumentEntity> entities = new ArrayList<DocumentEntity>();
		
		Map<String, DocumentEntity> entitiesMap = new HashMap<>();
		
		JSONArray data = json.getJSONArray("entities");
		
		data.forEach(item -> {
			JSONObject obj = (JSONObject) item;
			
			String entity = obj.getString("name");
			String spacyType = obj.getString("label");
			String stanfordType = VoyantSpacyClient.getStanfordTypeFromSpacyType(spacyType);
			
			JSONArray selections = obj.getJSONArray("matches");
			int rawFreq = selections.length();
			selections.forEach(item2 -> {
				JSONObject obj2 = (JSONObject) item2;
				String lemma = obj2.getString("text");
				int start = obj2.getInt("start");
				int end = obj2.getInt("end");
				
				DocumentEntity currEntity;
				if (entitiesMap.containsKey(entity)) {
					currEntity = entitiesMap.get(entity);
				} else {
					if (VoyantSpacyClient.convertToStanfordTypes) {
						currEntity = new DocumentEntity(-1, entity, lemma, EntityType.getForgivingly(stanfordType), rawFreq);
					} else {
						currEntity = new DocumentEntity(-1, entity, lemma, EntityType.getForgivingly(spacyType), rawFreq);
					}
					entitiesMap.put(entity, currEntity);
				}
				int[][] offsets = currEntity.getOffsets();
				if (offsets == null) {
					currEntity.setOffsets(new int[][] {{start, end}});
				} else {
					int[][] newOffsets = new int[offsets.length+1][2];
					for (int i = 0; i < offsets.length; i++) {
						newOffsets[i] = offsets[i];
					}
					newOffsets[newOffsets.length-1] = new int[] {start, end};
					currEntity.setOffsets(newOffsets);
				}
			});
		});
		
		for (DocumentEntity ent : entitiesMap.values()) {
			entities.add(ent);
		}
		
		return entities;
	}
	
	private static String getStanfordTypeFromSpacyType(String spacyType) {
//	DATE - Absolute or relative dates or periods
//	PERSON - People, including fictional
//	GPE - Countries, cities, states
//	LOC - Non-GPE locations, mountain ranges, bodies of water
//	MONEY - Monetary values, including unit
//	TIME - Times smaller than a day
//	PRODUCT - Objects, vehicles, foods, etc. (not services)
//	CARDINAL - Numerals that do not fall under another type
//	ORDINAL - "first", "second", etc.
//	QUANTITY - Measurements, as of weight or distance
//	EVENT - Named hurricanes, battles, wars, sports events, etc.
//	FAC - Buildings, airports, highways, bridges, etc.
//	LANGUAGE - Any named language
//	LAW - Named documents made into laws.
//	NORP - Nationalities or religious or political groups
//	PERCENT - Percentage, including "%"
//	WORK_OF_ART - Titles of books, songs, etc.
		switch (spacyType.toLowerCase()) {
		case "gpe":
		case "loc":
			return "location";
		case "norp":
			return "organization";
		case "product":
		case "cardinal":
		case "ordinal":
		case "quantity":
		case "event":
		case "fac":
		case "language":
		case "law":
		case "work_of_art":
			return "misc";
		default:
			return spacyType.toLowerCase();
		}
	}
	
	
	private static JSONObject getJSONResponse(CloseableHttpResponse response) throws HttpResponseException, IOException {
		int statusCode = response.getStatusLine().getStatusCode();
		if (debug) System.out.println("status: "+statusCode);
		if (statusCode >= 400) {
			throw new HttpResponseException(statusCode, "Error getting response.");
		} else if (statusCode == 204) {
			if (debug) System.out.println("no response");
			return null;
		} else {
			HttpEntity entity = response.getEntity();
			Header encodingHeader = entity.getContentEncoding();
			
			Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());
			
			String jsonString = EntityUtils.toString(entity, encoding);
//			if (debug) System.out.println("response: "+jsonString);
			
			try {
				JSONObject json = new JSONObject(jsonString);
				return json;
			} catch (JSONException e) {
				if (statusCode == 200 && jsonString.equals("")) {
					// temporary handling of https://gitlab.com/calincs/conversion/NSSI/-/issues/259
					throw new HttpResponseException(401, "Access token expired");
				} else {
					throw new IOException("Error parsing JSON: "+jsonString);
				}
			}
		}
	}
	
	public static void printResults(List<DocumentEntity> spacyResult) {
		for (DocumentEntity ent : spacyResult) {
			System.out.println(ent.getType()+": "+ent.getTerm()+" / "+ent.getNormalized()+", pos: ["+ent.getOffsets()+"]");
		}
	}
}

