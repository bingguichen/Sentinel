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
package com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.config;

import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.extractor.ApacheHttpAsyncClientResourceExtractor;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.extractor.DefaultApacheHttpAsyncClientResourceExtractor;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.fallback.ApacheHttpAsyncClientFallback;
import com.alibaba.csp.sentinel.adapter.apache.httpasyncclient.fallback.DefaultApacheHttpAsyncClientFallback;
import com.alibaba.csp.sentinel.util.AssertUtil;

/**
 * @author zhaoyuguang
 */
public class SentinelApacheHttpAsyncClientConfig {

    private String prefix = "httpasyncclient:";
    private ApacheHttpAsyncClientResourceExtractor extractor = new DefaultApacheHttpAsyncClientResourceExtractor();
    private ApacheHttpAsyncClientFallback fallback = new DefaultApacheHttpAsyncClientFallback();

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        AssertUtil.notNull(prefix, "prefix cannot be null");
        this.prefix = prefix;
    }

    public ApacheHttpAsyncClientResourceExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(ApacheHttpAsyncClientResourceExtractor extractor) {
        AssertUtil.notNull(extractor, "extractor cannot be null");
        this.extractor = extractor;
    }

    public ApacheHttpAsyncClientFallback getFallback() {
        return fallback;
    }

    public void setFallback(ApacheHttpAsyncClientFallback fallback) {
        AssertUtil.notNull(fallback, "fallback cannot be null");
        this.fallback = fallback;
    }
}
