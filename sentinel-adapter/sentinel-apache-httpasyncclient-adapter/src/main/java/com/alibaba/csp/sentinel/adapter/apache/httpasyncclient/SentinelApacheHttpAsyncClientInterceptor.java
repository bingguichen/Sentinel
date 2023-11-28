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

import com.alibaba.csp.sentinel.*;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.config.SentinelApacheHttpAsyncClientConfig;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.hc.client5.http.async.AsyncExecCallback;
import org.apache.hc.client5.http.async.AsyncExecChain;
import org.apache.hc.client5.http.async.AsyncExecChainHandler;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;

import java.io.IOException;

/**
 * @author zhaoyuguang
 */
public class SentinelApacheHttpAsyncClientInterceptor implements AsyncExecChainHandler {

    private final SentinelApacheHttpAsyncClientConfig config;

    public SentinelApacheHttpAsyncClientInterceptor(){
        this.config = new SentinelApacheHttpAsyncClientConfig();
    }

    public SentinelApacheHttpAsyncClientInterceptor(SentinelApacheHttpAsyncClientConfig config){
        this.config = config;
    }

    @Override
    public void execute(HttpRequest request, AsyncEntityProducer asyncEntityProducer, AsyncExecChain.Scope scope, AsyncExecChain asyncExecChain, AsyncExecCallback asyncExecCallback) throws HttpException, IOException {

        Entry entry = null;
        try {
            String name = config.getExtractor().extractor(request);
            if (!StringUtil.isEmpty(config.getPrefix())) {
                name = config.getPrefix() + name;
            }
            entry = SphU.entry(name, ResourceTypeConstants.COMMON_WEB, EntryType.OUT);
            asyncExecChain.proceed(request, asyncEntityProducer, scope, asyncExecCallback);
        } catch (BlockException e) {
            config.getFallback().handle(request, e);
//        }  catch (IOException ex) {
//            Tracer.traceEntry(ex, entry);
//            throw ex;
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
