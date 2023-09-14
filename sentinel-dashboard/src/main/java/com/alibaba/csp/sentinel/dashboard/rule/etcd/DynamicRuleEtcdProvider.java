package com.alibaba.csp.sentinel.dashboard.rule.etcd;

import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.etcd.EtcdConfig;
import com.alibaba.csp.sentinel.datasource.etcd.EtcdDataSource;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Component("dynamicRuleEtcdProvider")
public class DynamicRuleEtcdProvider<T> implements DynamicRuleProvider<T>  {
    private static Logger logger = LoggerFactory.getLogger(DynamicRuleEtcdProvider.class);
    private Charset charset = Charset.forName(EtcdConfig.getCharset());

    @Override
    public <T> List<T> getRules(String appName, String ruleName, Class<T> tClass) throws Exception {
        if (StringUtil.isBlank(appName)) {
            return new ArrayList<>();
        }
        String rule_key = "/sentinel/".concat(appName).concat("/").concat(ruleName);
        ReadableDataSource<String, List<T>> flowRuleEtcdDataSource = new EtcdDataSource<>(rule_key, (rule) -> JSON.parseArray(rule, tClass));
        return flowRuleEtcdDataSource.loadConfig();

    }

}
