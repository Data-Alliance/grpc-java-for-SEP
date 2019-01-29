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

import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.transport.http.HttpProvider;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.examples.helloworld.data.CommonData;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class HelloWorldServer {
	private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

	private Server server;

	private void start() throws IOException {
		/* The port on which the server should run */
		int port = 50051;
		server = ServerBuilder.forPort(port).addService(new GreeterImpl()).build().start();
		logger.info("Server started, listening on " + port);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				// Use stderr here since the logger may have been reset by its JVM shutdown
				// hook.
				System.err.println("*** shutting down gRPC server since JVM is shutting down");
				HelloWorldServer.this.stop();
				System.err.println("*** server shut down");
			}
		});
	}

	private void stop() {
		if (server != null) {
			server.shutdown();
		}
	}

	/**
	 * Await termination on the main thread since the grpc library uses daemon
	 * threads.
	 */
	private void blockUntilShutdown() throws InterruptedException {
		if (server != null) {
			server.awaitTermination();
		}
	}

	/**
	 * Main launches the server from the command line.
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		final HelloWorldServer server = new HelloWorldServer();
		server.start();
		server.blockUntilShutdown();
	}

	static class GreeterImpl extends GreeterGrpc.GreeterImplBase {

		@Override
		public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
			HelloReply reply = HelloReply.newBuilder().setMessage("Hello " + req.getName()).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void createWallet(CreateWalletRequest req, StreamObserver<CreateWalletReply> responseObserver) {
			CreateWalletReply reply = CreateWalletReply.newBuilder().setPrivatekey("").build();

			// Create keyWallet and store it as a keyStorefile
			System.out.println("Create KeyWallet");
			KeyWallet createdWallet;
			try {
				createdWallet = KeyWallet.create();
				logger.info("address: " + createdWallet.getAddress());
				reply = CreateWalletReply.newBuilder().setPrivatekey(createdWallet.getPrivateKey().toHexString(false))
						.setPublickey(createdWallet.getPublicKey().toHexString(false))
						.setDid(createdWallet.getPublicKey().toHexString(false))
						.setAddress(createdWallet.getAddress().toString()).build();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void checkBalance(CheckBalanceRequest req, StreamObserver<CheckBalanceReply> responseObserver) {
			CheckBalanceReply reply = CheckBalanceReply.newBuilder().setBalance("").build();

			HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BODY);
			OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
			IconService iconService = new IconService(new HttpProvider(httpClient, CommonData.URI4testnet));

			Address address = new Address("hx11ff20b38b81f2c33ac61c3b9037f94cca167e7c");
			BigInteger balance;
			try {
				balance = iconService.getBalance(address).execute();
				System.out.println("Example_wallet balance:" + balance);
				reply = CheckBalanceReply.newBuilder().setBalance(balance.toString()).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void sendICX(SendIcxRequest req, StreamObserver<SendIcxReply> responseObserver) {
			SendIcxReply reply = SendIcxReply.newBuilder().setMessage("Send ICX result = ").build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
	}
}
