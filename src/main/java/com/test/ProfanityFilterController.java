package com.test;

import io.grpc.ManagedChannel;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLException;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@RequestMapping("/filter")
@RestController
@Slf4j
public class ProfanityFilterController {

    private static ProfanityFilterGrpc.ProfanityFilterBlockingStub stub;

    @RequestMapping(method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ResponseEntity filter(@RequestParam("input") String text) {
        return ResponseEntity.ok(filterTextByCallingServiceWithRetries(text));
    }

    private static String filterTextByCallingServiceWithRetries(String text) {
        try {
            return filterTextByCallingService(text);
        } catch (Exception e) {
            stub = null; // The exception could have been caused by a closed connection, so we kill the stub.
            return filterTextByCallingService(text);
        }
    }

    private static String filterTextByCallingService(String text) {
        if (stub == null) {
            log.info("Initialize stub");
            buildStub();
        }
        BatchFilterResponse batchFilterResponse = stub.batchFilterContent(buildRequest(text));
        return extractFilteredText(batchFilterResponse);
    }

    private static void buildStub() {
        ManagedChannel channel;
        try {
            channel = NettyChannelBuilder.forAddress("profanity-service", 8835)
                    .sslContext(GrpcSslContexts.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build())
                    .build();
        } catch (SSLException e) {
            log.error("Stub build exception", e);
            throw new IllegalStateException(e);
        }
        stub = ProfanityFilterGrpc.newBlockingStub(channel);
    }

    private static BatchFilterRequest buildRequest(String text) {
        return BatchFilterRequest.newBuilder()
                .setFilterConfig(FilterConfig.newBuilder()
                        .setFilterId("")
                        .build())
                .addContent(Content.newBuilder()
                        .setOriginalText(text)
                        .build())
                .build();
    }

    private static String extractFilteredText(BatchFilterResponse batchFilterResponse) {
        return batchFilterResponse
                .getFilteredContentList()
                .stream()
                .map(FilteredContent::getFilteredText)
                .collect(Collectors.joining(" "));
    }
}
