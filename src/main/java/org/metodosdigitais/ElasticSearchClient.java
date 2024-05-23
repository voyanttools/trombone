package org.metodosdigitais;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;

public class ElasticSearchClient {

	public static enum JobStatus {
		IN_PROGRESS, READY, FAILED, CANCELLED
	}
	
	private static String hostName = "";
	private static int port = 9200;
	private static String username = "";
	private static String password = "";
	
	
	//"_id","_index","_score","_type","all_text","all_text.keyword","chat_id","chat_title",
	// "forward_chat_id","forward_user_id","from_user_id",
	// "from_user_is_bot","from_user_is_deleted","from_user_is_fake","from_user_is_verified"
	// "image_hash","image_hash.keyword","message_caption","message_caption.keyword","message_id","sender_chat_id",
	// "strict_date","strict_edit_date",text,type,views,"word_count"
	
	public static void main(String[] args) throws IOException {
		ElasticSearchClient.query("freedom");
	}
	
	public static ElasticsearchClient getElasticSearchClient() {
		final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
	
		// https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/8.11/_encrypted_communication.html
		RestClient restClient = RestClient.builder(new HttpHost(hostName, port, "http"))
			.setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
				.setConnectTimeout(5000)
				.setSocketTimeout(60000))
			.setHttpClientConfigCallback(httpClientBuilder ->
				httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
			.build();

		ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
		return new ElasticsearchClient(transport);
	}
	
	public static void query(String query) throws IOException {
		ElasticsearchClient esClient = getElasticSearchClient();
		
//		Query ssq = SimpleQueryStringQuery.of(q -> q.query(query))._toQuery();
		
		try {
			SearchResponse<ChatMessage> response = esClient.search(s -> s
				.index("chat_index")
				.source(SourceConfig.of(sc -> sc
					.filter(f -> f
						.includes(List.of("chat_id","chat_title","all_text","strict_date","forward_user_id","from_user_id"))
					)
				))
				.query(q -> q
					.bool(b -> b
						.must(m -> m
							.term(t -> t
								.field("all_text")
								.value(v -> v.stringValue(query))
							)
						)
						.must(m -> m
							.term(t -> t
								.field("from_user_is_bot")
								.value(v -> v.booleanValue(false))
							)
						)
					)
					
				),
				ChatMessage.class
			);

			int count = 0;
			for (Hit<ChatMessage> hit: response.hits().hits()) {
				System.out.println(hit.source());
				count++;
				if (count > 3) break;
			}
		} catch (Exception e) {
			System.out.println(e);
			System.out.println(e.getCause());
		}
		
		esClient._transport().close();
	}

}

