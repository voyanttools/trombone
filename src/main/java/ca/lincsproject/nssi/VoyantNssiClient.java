package ca.lincsproject.nssi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.voyanttools.trombone.model.DocumentEntity;
import org.voyanttools.trombone.model.EntityType;

public class VoyantNssiClient {

	public static enum JobStatus {
		IN_PROGRESS, READY, FAILED, CANCELLED
	}
	
	private static String accessToken;
	
	private final static String PROJECT_NAME = "Voyant Project";
	private final static String WORKFLOW = "stanford_ner";
	private static String clientSecret = null;
	
	private final static long DEFAULT_POLLING_INTERVAL = 30000;
	
	private static boolean debug = false;
	
	private final static String testResponse = "{\"processingDate\":\"2021-10-21T19:34:41.561559\",\"metadata\":{},\"data\":[{\"selections\":[{\"lemma\":\"Street\",\"selection\":{\"start\":106,\"end\":112}}],\"classification\":\"LOCATION\",\"entity\":\"Street\"},{\"selections\":[{\"lemma\":\"Holmes\",\"selection\":{\"start\":0,\"end\":6}}],\"classification\":\"PERSON\",\"entity\":\"Holmes\"}]}";
	
	
	public static void main(String[] args) throws IOException {
		String testText = "I had seen little of Holmes lately. My marriage had drifted us away from each other. My own complete happiness, and the home-centred interests which rise up around the man who first finds himself master of his own establishment, were sufficient to absorb all my attention, while Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street, buried among his old books, and alternating from week to week between cocaine and ambition, the drowsiness of the drug, and the fierce energy of his own keen nature. He was still, as ever, deeply attracted by the study of crime, and occupied his immense faculties and extraordinary powers of observation in following out those clues, and clearing up those mysteries which had been abandoned as hopeless by the official police. From time to time I heard some vague account of his doings: of his summons to Odessa in the case of the Trepoff murder, of his clearing up of the singular tragedy of the Atkinson brothers at Trincomalee, and finally of the mission which he had accomplished so delicately and successfully for the reigning family of Holland. Beyond these signs of his activity, however, which I merely shared with all the readers of the daily press, I knew little of my former friend and companion.";
		String smallText = "Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street";
		
		debug = true;
		int jobId = VoyantNssiClient.submitJob(testText);
		List<DocumentEntity> result = VoyantNssiClient.getResults(jobId);
		VoyantNssiClient.printResults(result);
	}
	
	private static void setAccessToken() throws IOException {
		if (debug) System.out.println("setting access token");
		if (clientSecret == null) {
			String[] nssiConfig = readConfiguration();
			clientSecret = nssiConfig[0];
		}
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpPost httpPost = new HttpPost("https://keycloak.lincsproject.ca/auth/realms/lincs/protocol/openid-connect/token");

			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("client_id", "voyant"));
			params.add(new BasicNameValuePair("client_secret", clientSecret));
			params.add(new BasicNameValuePair("grant_type", "client_credentials"));
			httpPost.setEntity(new UrlEncodedFormEntity(params));
			
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				JSONObject json = getJSONResponse(response);
				
				VoyantNssiClient.accessToken = json.getString("access_token");
			}
		}
	}
	
	public static int submitJob(String text) throws IOException {
		if (VoyantNssiClient.accessToken == null) {
			setAccessToken();
		}
		
		if (debug) System.out.println("submitting job");
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpPost httpPost = new HttpPost("https://api.nssi.lincsproject.ca/api/jobs");
			httpPost.setHeader("Authorization", "Bearer "+VoyantNssiClient.accessToken);
			
			JSONObject jsonBody = new JSONObject();
			jsonBody.put("projectName", PROJECT_NAME);
			jsonBody.put("format", "text/html");
			jsonBody.put("workflow", WORKFLOW);
			jsonBody.put("document", text);
			String input = jsonBody.toString();
			
			StringEntity body = new StringEntity(input, "UTF-8");
			httpPost.setEntity(body);
			httpPost.setHeader("Accept", "application/json");
			httpPost.setHeader("Content-type", "application/json; charset=UTF-8");
			
			try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
				JSONObject json = getJSONResponse(response);
			
				int jobId = json.getInt("jobId");
				
				float estimated = json.getFloat("estimatedMinUntilRun");
				int millisecondsUntilRun = Math.round(estimated * 60 * 1000);
				
				return jobId;
			} catch (HttpResponseException e) {
				if (e.getStatusCode() == 401) {
					setAccessToken();
					return submitJob(text);
				} else {
					throw new IOException("Error submitting job. Status code: "+e.getStatusCode());
				}
			}
		}
	}
	
	public static int submitJobTest(String text) throws IOException {
		return 999;
	}
	
	public static List<DocumentEntity> getResults(int jobId) throws IOException {
		return getResults(jobId, DEFAULT_POLLING_INTERVAL);
	}
	
	public static List<DocumentEntity> getResults(int jobId, long pollingInterval) throws IOException {
		JobStatus status = getJobStatus(jobId);
		while (status.equals(JobStatus.IN_PROGRESS) && !Thread.interrupted()) {
			try {
				Thread.sleep(pollingInterval);
				status = getJobStatus(jobId);
				System.out.println("jobId: "+jobId+", status: "+status+", thread: "+Thread.currentThread().getId());
			} catch (InterruptedException e) {
				System.out.println("INTERRUPTED, jobId: "+jobId+", status: "+status);
			}
		}
		
		if (status.equals(JobStatus.READY)) {
			return getJobResults(jobId);
		} else {
			throw new IOException("Error get NSSI results");
		}
	}
	
	public static List<DocumentEntity> getResultsTest(int jobId) throws IOException, InterruptedException {
		Thread.sleep(DEFAULT_POLLING_INTERVAL/10);
		JSONObject json = new JSONObject(testResponse);
		List<DocumentEntity> nssiResult = jsonToDocumentEntities(json);
		return nssiResult;
	}
	
	public static JobStatus getJobStatus(int jobId) throws IOException {
		if (debug) System.out.println("checking job: "+jobId);
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpGet httpGet = new HttpGet("https://api.nssi.lincsproject.ca/api/jobs/"+jobId);
			httpGet.setHeader("Authorization", "Bearer "+VoyantNssiClient.accessToken);
			
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				JSONObject json = getJSONResponse(response);
				
				String status = json.getString("status");
				if (debug) System.out.println(status);
				
				for (JobStatus js : JobStatus.values()) {
					if (status.equals(js.name())) {
						return js;
					}
				}
				return JobStatus.FAILED;
			} catch (HttpResponseException e) {
				if (e.getStatusCode() == 401) {
					setAccessToken();
					return getJobStatus(jobId);
				} else {
					throw new IOException("Error getting job status. Status code: "+e.getStatusCode());
				}
			}
		}
	}
	
	public static boolean cancelJob(int jobId) throws IOException {
		if (debug) System.out.println("cancelling job: "+jobId);
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpPut httpPut = new HttpPut("https://api.nssi.lincsproject.ca/api/jobs/"+jobId+"/actions/cancel");
			httpPut.setHeader("Authorization", "Bearer "+VoyantNssiClient.accessToken);
			
			try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode == 204) {
					if (debug) System.out.println("job cancelled: "+jobId);
					return true;
				} else if (statusCode == 401) {
					setAccessToken();
					return cancelJob(jobId);
				} else {
					if (debug) System.out.println("error cancelling job: "+jobId+", statusCode: "+statusCode);
					return false;
				}
			}
		}
	}
	
	public static List<DocumentEntity> getJobResults(int jobId) throws IOException {
		if (debug) System.out.println("getting results: "+jobId);
		try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpGet httpGet = new HttpGet("https://api.nssi.lincsproject.ca/api/results/"+WORKFLOW+"/"+jobId);
			httpGet.setHeader("Authorization", "Bearer "+VoyantNssiClient.accessToken);
			
			try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
				JSONObject json = getJSONResponse(response);
				
				return jsonToDocumentEntities(json);
			} catch (HttpResponseException e) {
				if (e.getStatusCode() == 401) {
					setAccessToken();
					return getJobResults(jobId);
				} else {
					throw new IOException("Error getting job results. Status code: "+e.getStatusCode());
				}
			}
		}
	}
	
	private static NssiResult jsonToNssiResult(JSONObject json) {
		NssiResult nssiResult = new NssiResult();
		
		JSONArray data = json.getJSONArray("data");
		
		data.forEach(item -> {
			JSONObject obj = (JSONObject) item;
			
			String entity = obj.getString("entity");
			String classification = obj.getString("classification");
			
			JSONArray selections = obj.getJSONArray("selections");
			selections.forEach(item2 -> {
				JSONObject obj2 = (JSONObject) item2;
				String lemma = obj2.getString("lemma");
				
				JSONObject selection = obj2.getJSONObject("selection");
				int start = selection.getInt("start");
				int end = selection.getInt("end");
				
				nssiResult.add(entity, classification, lemma, start, end);
			});
		});
		
		return nssiResult;
	}

	
	private static List<DocumentEntity> jsonToDocumentEntities(JSONObject json) {
		List<DocumentEntity> entities = new ArrayList<DocumentEntity>();
		
		Map<String, DocumentEntity> entitiesMap = new HashMap<>();
		
		JSONArray data = json.getJSONArray("data");
		
		data.forEach(item -> {
			JSONObject obj = (JSONObject) item;
			
			String entity = obj.getString("entity");
			String classification = obj.getString("classification");
			
			JSONArray selections = obj.getJSONArray("selections");
			int rawFreq = selections.length();
			selections.forEach(item2 -> {
				JSONObject obj2 = (JSONObject) item2;
				String lemma = obj2.getString("lemma");
				
				JSONObject selection = obj2.getJSONObject("selection");
				int start = selection.getInt("start");
				int end = selection.getInt("end");
				
				DocumentEntity currEntity;
				if (entitiesMap.containsKey(entity)) {
					currEntity = entitiesMap.get(entity);
				} else {
					currEntity = new DocumentEntity(-1, entity, lemma, EntityType.getForgivingly(classification), rawFreq);
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
			if (debug) System.out.println("response: "+jsonString);
			
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
	
	private static String[] readConfiguration() {
		Properties prop = new Properties();
		InputStream input = null;
		String[] nssiConfig = new String[1];

		try {
			input = VoyantNssiClient.class.getResourceAsStream("/ca/lincsproject/nssi/config.properties");
			prop.load(input);
			nssiConfig[0] = prop.getProperty("client_secret");

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return nssiConfig;
	}
	
	public static void printResults(List<DocumentEntity> nssiResult) {
		for (DocumentEntity ent : nssiResult) {
			System.out.println(ent.getType()+": "+ent.getTerm()+" / "+ent.getNormalized()+", pos: ["+ent.getOffsets()+"]");
		}
	}
}

