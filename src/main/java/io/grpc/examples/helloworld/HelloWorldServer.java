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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

import foundation.icon.icx.Call;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.SignedTransaction;
import foundation.icon.icx.Transaction;
import foundation.icon.icx.TransactionBuilder;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.data.IconAmount;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class HelloWorldServer {
	private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());
	private static IconService iconService;

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
		public void createKeyWallet(CreateKeyWalletRequest req, StreamObserver<CreateKeyWalletReply> responseObserver) {
			CreateKeyWalletReply reply = CreateKeyWalletReply.newBuilder().setPrivatekey("").build();

			// Create keyWallet and store it as a keyStorefile
			System.out.println("Create KeyWallet");
			KeyWallet createdWallet;
			try {
				createdWallet = KeyWallet.create();
				logger.info("address: " + createdWallet.getAddress());
				reply = CreateKeyWalletReply.newBuilder()
						.setPrivatekey(createdWallet.getPrivateKey().toHexString(false))
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
		public void createKeystoreFile(CreateKeystoreFileRequest req,
				StreamObserver<CreateKeystoreFileReply> responseObserver) {
			CreateKeystoreFileReply reply = CreateKeystoreFileReply.newBuilder().setKeystorefile("").build();

			// Create keyWallet and store it as a keyStorefile
			System.out.println("Create KeyWallet");

			String dirPath = "./";
			// File of directory for keystorfile.
			File destinationDirectory = new File(dirPath);

			try {
				KeyWallet wallet = KeyWallet.load(new Bytes(req.getPrivatekey()));
				String fileName = KeyWallet.store(wallet, req.getPassword(), destinationDirectory);
				BufferedReader br;
				String keyStorefile = "";

				File file = new File(destinationDirectory, fileName);
				if (file.exists() && file.isFile() && file.canRead()) {
					// ObjectMapper mapper = new ObjectMapper();
					// KeystoreFile keystoreFile = mapper.readValue(file, KeystoreFile.class);
					br = new BufferedReader(new FileReader(file));
					keyStorefile = br.readLine();
				}
				logger.info("keyStorefile: " + keyStorefile);
				reply = CreateKeystoreFileReply.newBuilder().setKeystorefile(keyStorefile).build();
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

			Address address = new Address(req.getAddress());
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

			final Address scoreAddress = new Address("cx6775fe9c32444a917f854f4a53fa08d763127c79");

			HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BODY);
			OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
			IconService iconService = new IconService(new HttpProvider(httpClient, CommonData.URI4testnet));

			Call<RpcItem> call = new Call.Builder().to(scoreAddress).method("create_did").build();

			try {
				RpcItem result = iconService.call(call).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void createDID(CreateDIDRequest req, StreamObserver<CreateDIDReply> responseObserver) {
			CreateDIDReply reply = CreateDIDReply.newBuilder().setMessage("Send ICX result = ").build();

			KeyWallet wallet = KeyWallet
					.load(new Bytes("1915704752ee6e926e15e938d2d8153957fb8d43724a5f61a129ddd1fbb9884d"));
			Address fromAddress = wallet.getAddress();
			Address scoreAddress = new Address("cxd6bdebfbba1b35141fdade2a5f806d618c713369");

			BigInteger networkId = new BigInteger("3");
			BigInteger value = IconAmount.of("0", IconAmount.Unit.ICX).toLoop();
			BigInteger stepLimit = new BigInteger("1000000");
			long timestamp = System.currentTimeMillis() * 1000L;
			BigInteger nonce = new BigInteger("1");

			RpcObject params = new RpcObject.Builder().put("publickey", new RpcValue(req.getPublickey())).build();

			Transaction transaction = TransactionBuilder.newBuilder()
					.nid(networkId)
					.from(fromAddress)
					.to(scoreAddress)
					.value(value)
					.stepLimit(stepLimit)
					.timestamp(new BigInteger(Long.toString(timestamp)))
					.nonce(nonce)
					.call("create_did_from_pubkey")
					.params(params)
					.build();

			SignedTransaction signedTransaction = new SignedTransaction(transaction, wallet);

			HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
			logging.setLevel(HttpLoggingInterceptor.Level.BODY);
			OkHttpClient httpClient = new OkHttpClient.Builder().addInterceptor(logging).build();
			iconService = new IconService(new HttpProvider(httpClient, CommonData.URI4testnet));
			try {
				Bytes hash = iconService.sendTransaction(signedTransaction).execute();
				System.out.println("txHash:" + hash);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}
	}
}
