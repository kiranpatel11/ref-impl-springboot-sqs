package com.example.eventapp;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.elasticmq.rest.sqs.SQSRestServer;
import org.elasticmq.rest.sqs.SQSRestServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.ResourceUtils;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@ActiveProfiles("local")
class ApplicationTests {

	private static final Logger logger = LoggerFactory.getLogger(ApplicationTests.class);

	private static SQSRestServer sqsMockServer;

	private static String queueUrl;

	@Value("${sqs.my-queue.name}")
	private String queueName;

	//Injects the same client being used by the application, see testMessage2 for alternative option
	@Autowired
	private SqsAsyncClient sqsAsyncClient;

	@Test
	void contextLoads() {
	}

	@BeforeAll
	static void setup() {
		sqsMockServer = SQSRestServerBuilder.start();
		InetSocketAddress host = sqsMockServer.waitUntilStarted().localAddress();		
		queueUrl = "http://" + host.getHostName() + ":" + host.getPort() + "/000000000000/";   //accountId
		logger.info("ElasticMQ started on {}:{}", host.getHostName(), host.getPort());
	}

	@Test
	void testMessage(CapturedOutput output) throws IOException, URISyntaxException, InterruptedException, ExecutionException {

		String exampleRequest = FileUtils.readFileToString(ResourceUtils.getFile("classpath:testdata/my-event.json"), StandardCharsets.UTF_8);		

		//String messageBody = "Hello SQS";
		// Send message
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
				.queueUrl(queueUrl + queueName)
                .messageBody(exampleRequest)				
                .build();

		CompletableFuture<SendMessageResponse> sendMessageFuture = sqsAsyncClient.sendMessage(sendMessageRequest);

		assertNotNull(sendMessageFuture);
		
		SendMessageResponse sendMessageResponse = sendMessageFuture.get();
		String messageId = sendMessageResponse.messageId();
		logger.info("(SqsAsyncClient) Message sent successfully with MessageId: {}" , messageId);

		//Assert that message is received by the listener using the log message	
		//Alternatively, you can verify the state in your database, cache or DLQ 
		await().atMost(5, SECONDS).until(() -> output.getOut().contains("Received message: " + messageId));
	}

	@Test
	void testMessage2(CapturedOutput output) throws URISyntaxException, InterruptedException, FileNotFoundException, IOException {

		//Example of using the dedicated SqsClient for tests,
		//This is useful to reuse the same tests against remote SUT, and not dependent on injecting spring bean 
		SqsClient sqsClient = SqsClient.builder()
		.region(Region.of("elastic-mq"))   //should be parameterised
		.endpointOverride(new URI("http://localhost:9324")) //should be parameterised
		.build();

		String exampleRequest = FileUtils.readFileToString(ResourceUtils.getFile("classpath:testdata/my-event-2.json"), StandardCharsets.UTF_8);		
		// Send message
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
				.queueUrl(queueUrl + queueName)
                .messageBody(exampleRequest)
                .build();

		SendMessageResponse sendMessage = sqsClient.sendMessage(sendMessageRequest);
		assertNotNull(sendMessage);
		String messageId = sendMessage.messageId();
		assertNotNull(messageId);
		logger.info("(SqsClient) Message sent successfully with MessageId: {}", messageId);

		//Assert that message is received by the listener using the log message	
		//Alternatively, you can verify the state in your database, cache or DLQ 
		await().atMost(5, SECONDS).until(() -> output.getOut().contains("Received message: " + messageId));
	}

	@Test
	void testGetQueueUrl() throws InterruptedException, ExecutionException {
        CompletableFuture<GetQueueUrlResponse> getQueueUrlResponseFuture =
                sqsAsyncClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
	
		GetQueueUrlResponse getQueueUrlResponse = getQueueUrlResponseFuture.get();		
		assertEquals(queueUrl+queueName, getQueueUrlResponse.queueUrl());
	}

	@AfterAll
	static void tearDown() throws InterruptedException {
		sqsMockServer.stopAndWait();
	}
}
