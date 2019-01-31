/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.grpc.examples.helloworld;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * A simple client that requests a greeting from the {@link HelloWorldServer}.
 */
public class HelloWorldClient {
	private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

	private final ManagedChannel channel;
	private final GreeterGrpc.GreeterBlockingStub blockingStub;

	/** Construct client connecting to HelloWorld server at {@code host:port}. */
	public HelloWorldClient(String host, int port) {
		this(ManagedChannelBuilder.forAddress(host, port)
				// Channels are secure by default (via SSL/TLS). For the example we disable TLS
				// to avoid
				// needing certificates.
				.usePlaintext().build());
	}

	/**
	 * Construct client for accessing HelloWorld server using the existing channel.
	 */
	HelloWorldClient(ManagedChannel channel) {
		this.channel = channel;
		blockingStub = GreeterGrpc.newBlockingStub(channel);
	}

	public void shutdown() throws InterruptedException {
		channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
	}

	/** Say hello to server. */
	public void greet(String name) {
		logger.info("Will try to greet " + name + " ...");

		HelloRequest request;
		HelloReply response;
		try {
			request = HelloRequest.newBuilder().setName(name).build();
			response = blockingStub.sayHello(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("Greeting: " + response.getMessage());

		CreateKeyWalletRequest createKeyWalletRequest;
		CreateKeyWalletReply createKeyWalletResponse;
		try {
			createKeyWalletRequest = CreateKeyWalletRequest.newBuilder().setPassword("").build();
			createKeyWalletResponse = blockingStub.createKeyWallet(createKeyWalletRequest);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("pv key: " + createKeyWalletResponse.getPrivatekey());
		logger.info("pb key: " + createKeyWalletResponse.getPublickey());
		logger.info("did: " + createKeyWalletResponse.getDid());
		logger.info("address: " + createKeyWalletResponse.getAddress());

		CreateKeystoreFileRequest createKeystorefileRequest;
		CreateKeystoreFileReply createKeystorefileResponse;
		try {
			createKeystorefileRequest = CreateKeystoreFileRequest.newBuilder()
					.setPrivatekey(createKeyWalletResponse.getPrivatekey()).setPassword("Pa55w0rd").build();
			createKeystorefileResponse = blockingStub.createKeystoreFile(createKeystorefileRequest);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("keystorefile: " + createKeystorefileResponse.getKeystorefile());

		CheckBalanceRequest checkBalanceRequest;
		CheckBalanceReply checkBalanceReply;
		try {
			checkBalanceRequest = CheckBalanceRequest.newBuilder().setAddress(createKeyWalletResponse.getAddress())
					.build();
			checkBalanceReply = blockingStub.checkBalance(checkBalanceRequest);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("check balance: " + checkBalanceReply.getBalance());

		SendIcxRequest sendIcxRequest;
		SendIcxReply sendIcxReply;
		try {
			sendIcxRequest = SendIcxRequest.newBuilder().setPrivatekey(createKeyWalletResponse.getPrivatekey()).build();
			sendIcxReply = blockingStub.sendICX(sendIcxRequest);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return;
		}
		logger.info("send ICX: " + sendIcxReply.getMessage());
	}

	/**
	 * Greet server. If provided, the first element of {@code args} is the name to
	 * use in the greeting.
	 */
	public static void main(String[] args) throws Exception {
		HelloWorldClient client = new HelloWorldClient("localhost", 50051);
		try {
			/* Access a service running on the local machine on port 50051 */
			String user = "world";
			if (args.length > 0) {
				user = args[0]; /* Use the arg as the name to greet if provided */
			}
			client.greet(user);
		} finally {
			client.shutdown();
		}
	}
}
