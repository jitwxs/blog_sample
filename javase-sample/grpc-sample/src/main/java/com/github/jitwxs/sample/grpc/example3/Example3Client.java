package com.github.jitwxs.sample.grpc.example3;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import com.github.jitwxs.sample.grpc.common.Constant;
import com.github.jitwxs.sample.grpc.common.ProtoUtils;
import com.github.jitwxs.sample.grpc.UserRpcProto;
import com.github.jitwxs.sample.grpc.UserRpcServiceGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author jitwxs
 * @date 2020年04月05日 19:42
 */
public class Example3Client {
    private static final Logger logger = Logger.getLogger(Example3Client.class.getName());

    public static void main(String[] args) throws Exception {
        // STEP1 构造 Channel 和 BlockingStub
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", Constant.RUNNING_PORT)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid needing certificates.
                .usePlaintext()
                .build();

        // 如果客户端请求是流式请求，那么就不能使用阻塞式 stub，必须使用异步的 stub
        UserRpcServiceGrpc.UserRpcServiceStub asyncStub = UserRpcServiceGrpc.newStub(channel);

        // STEP2 准备请求响应的回调方法
        StreamObserver<UserRpcProto.UserListResponse> responseStreamObserver = new StreamObserver<UserRpcProto.UserListResponse>() {
            @Override
            public void onNext(UserRpcProto.UserListResponse response) {
                logger.info("Rec response: " + ProtoUtils.toStr(response));
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onCompleted() {
                logger.info("Rec Complete!");
            }
        };

        // STEP3 准备多次请求，流式通信
        StreamObserver<UserRpcProto.AgeRequest> requestStreamObserver = asyncStub.streamListByAge(responseStreamObserver);
        requestStreamObserver.onNext(UserRpcProto.AgeRequest.newBuilder().setAge(18).build());
        requestStreamObserver.onNext(UserRpcProto.AgeRequest.newBuilder().setAge(20).build());
        requestStreamObserver.onNext(UserRpcProto.AgeRequest.newBuilder().setAge(22).build());
        requestStreamObserver.onNext(UserRpcProto.AgeRequest.newBuilder().setAge(24).build());
        // 请求完毕
        requestStreamObserver.onCompleted();

        // 由于请求是异步的，所以此处sleep，等待接受到响应
        Thread.sleep(5000);

        // STEP4 关闭 Channel
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
}
