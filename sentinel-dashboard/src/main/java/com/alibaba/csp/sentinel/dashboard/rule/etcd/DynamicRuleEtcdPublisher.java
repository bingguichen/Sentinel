package com.alibaba.csp.sentinel.dashboard.rule.etcd;

import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.datasource.etcd.EtcdConfig;
import com.alibaba.fastjson.JSONObject;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component("dynamicRuleEtcdPublisher")
public class DynamicRuleEtcdPublisher<T> implements DynamicRulePublisher<T> {
    private static Logger logger = LoggerFactory.getLogger(DynamicRuleEtcdPublisher.class);

    private Charset charset = Charset.forName(EtcdConfig.getCharset());

    @Override
    public <T> void publish(String appName, String ruleName, T flowRules) throws Exception {
        Client client;

        String rule_key = "/sentinel/".concat(appName).concat("/").concat(ruleName);
        if (!EtcdConfig.isAuthEnable()) {
            client = Client.builder().endpoints(EtcdConfig.getEndPoints().split(",")).build();
        } else {
            client = Client.builder().endpoints(EtcdConfig.getEndPoints().split(",")).user(ByteSequence.from(EtcdConfig.getUser(), this.charset)).password(ByteSequence.from(EtcdConfig.getPassword(), this.charset)).authority(EtcdConfig.getAuthority()).build();
        }

        client.getKVClient().put(ByteSequence.from(rule_key.getBytes()), ByteSequence.from(JSONObject.toJSONString(flowRules).getBytes()));
    }

}
