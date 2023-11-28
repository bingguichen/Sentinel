/*
 * Copyright 1999-2020 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.adapter.apache.httpasyncclient;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.app.TestApplication;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.config.SentinelApacheHttpAsyncClientConfig;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.extractor.ApacheHttpAsyncClientResourceExtractor;
import com.alibaba.csp.sentinel.node.ClusterNode;
import com.alibaba.csp.sentinel.slots.clusterbuilder.ClusterBuilderSlot;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.ChainElement;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.impl.BasicEntityDetails;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.AsyncDataConsumer;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;

/**
 * @author zhaoyuguang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8185"
        })
public class SentinelApacheHttpAsyncClientTest {

    @Value("${server.port}")
    private Integer port;

    @Test
    public void testSentinelOkHttpInterceptor0() throws Exception {

        final IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

        final SentinelApacheHttpAsyncClientConfig config = new SentinelApacheHttpAsyncClientConfig();
        config.setExtractor(new ApacheHttpAsyncClientResourceExtractor() {
            @Override
            public String extractor(HttpRequest request) {
                String contains = "/httpasyncclient/back/";
                String uri = request.getPath();
                if (uri.startsWith(contains)) {
                    uri = uri.substring(0, uri.indexOf(contains) + contains.length()) + "{id}";
                }
                return request.getMethod() + ":" + uri;
            }
        });

        final SentinelApacheHttpAsyncClientInterceptor interceptor = new SentinelApacheHttpAsyncClientInterceptor(config);

        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .addExecInterceptorFirst(ChainElement.PROTOCOL.name(), interceptor)
                .build();

        client.start();

        final String requestUri = "http://localhost:" + port + "/httpasyncclient/back";
        for (int i = 0; i < 20; i++) {
            final SimpleHttpRequest request = SimpleRequestBuilder.get(requestUri).build();
            System.out.println("Executing request " + request);
            final Future<SimpleHttpResponse> future = client.execute(
                    request,
                    new FutureCallback<SimpleHttpResponse>() {

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            System.out.println(request + "->" + new StatusLine(response));
                            System.out.println(response.getBody());
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.out.println(request + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            System.out.println(request + " cancelled");
                        }

                    });
            future.get();
        }
        System.out.println("Shutting down");
        ClusterNode cn = ClusterBuilderSlot.getClusterNode("httpasyncclient:GET:/httpasyncclient/back");
        assertNotNull(cn);
        Constants.ROOT.removeChildList();
        ClusterBuilderSlot.getClusterNodeMap().clear();
        client.close(CloseMode.GRACEFUL);
    }


}
