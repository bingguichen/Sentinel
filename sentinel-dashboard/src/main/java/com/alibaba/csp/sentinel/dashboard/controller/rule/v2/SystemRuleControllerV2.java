/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.csp.sentinel.dashboard.controller.rule.v2;

import com.alibaba.csp.sentinel.dashboard.auth.AuthAction;
import com.alibaba.csp.sentinel.dashboard.auth.AuthService.PrivilegeType;
import com.alibaba.csp.sentinel.dashboard.client.SentinelApiClient;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.DegradeRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.SystemRuleEntity;
import com.alibaba.csp.sentinel.dashboard.discovery.AppManagement;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.csp.sentinel.dashboard.domain.Result;
import com.alibaba.csp.sentinel.dashboard.repository.rule.RuleRepository;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRuleProvider;
import com.alibaba.csp.sentinel.dashboard.rule.DynamicRulePublisher;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author leyou(lihao)
 */
@RestController
@RequestMapping("/v2/system")
public class SystemRuleControllerV2 {

    private final Logger logger = LoggerFactory.getLogger(SystemRuleControllerV2.class);

    @Autowired
    private AppManagement appManagement;
    @Autowired
    private RuleRepository<SystemRuleEntity, Long> repository;
    @Autowired
    @Qualifier("dynamicRuleEtcdProvider")
    private DynamicRuleProvider<List<SystemRuleEntity>> ruleProvider;
    @Autowired
    @Qualifier("dynamicRuleEtcdPublisher")
    private DynamicRulePublisher<List<SystemRuleEntity>> rulePublisher;

    private void publishRules(String app) throws Exception {
        List<SystemRuleEntity> rules = repository.findAllByApp(app);
        List<SystemRule> systemRules = rules.stream().map(SystemRuleEntity::toRule).collect(Collectors.toList());
        rulePublisher.publish(app, SystemRuleEntity.SYSTEM_RULE_PREFIX, systemRules);
    }

    private <R> Result<R> checkBasicParams(SystemRuleEntity entity) {
        if (StringUtil.isEmpty(entity.getApp())) {
            return Result.ofFail(-1, "app can't be null or empty");
        }
        if (StringUtil.isEmpty(entity.getIp())) {
            return Result.ofFail(-1, "ip can't be null or empty");
        }
        if (entity.getPort() == null) {
            return Result.ofFail(-1, "port can't be null");
        }
        if (!appManagement.isValidMachineOfApp(entity.getApp(), entity.getIp())) {
            return Result.ofFail(-1, "given ip does not belong to given app");
        }
        if (entity.getPort() <= 0 || entity.getPort() > 65535) {
            return Result.ofFail(-1, "port should be in (0, 65535)");
        }


        int notNullCount = countNotNullAndNotNegative(entity.getHighestCpuUsage(), entity.getAvgRt(), entity.getMaxThread(), entity.getQps(), entity.getHighestSystemLoad());
        if (notNullCount != 1) {
            return Result.ofFail(-1, "only one of [highestSystemLoad, avgRt, maxThread, qps,highestCpuUsage] "
                    + "value must be set > 0, but " + notNullCount + " values get");
        }
        if (null != entity.getHighestCpuUsage() && entity.getHighestCpuUsage() > 1) {
            return Result.ofFail(-1, "highestCpuUsage must between [0.0, 1.0]");
        }
        return null;
    }

    private int countNotNullAndNotNegative(Number... values) {
        int notNullCount = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && values[i].doubleValue() >= 0) {
                notNullCount++;
            }
        }
        return notNullCount;
    }

    @GetMapping("/rules")
    @AuthAction(PrivilegeType.READ_RULE)
    public Result<List<SystemRuleEntity>> queryRules(String app) {
        if (StringUtil.isEmpty(app)) {
            return Result.ofFail(-1, "app cannot be null or empty");
        }

        try {
            List<SystemRuleEntity> rules = new ArrayList<>();
            List<SystemRule> systemRules = ruleProvider.getRules(app, SystemRuleEntity.SYSTEM_RULE_PREFIX, SystemRule.class);
            MachineInfo machine = appManagement.getFirstMachine(app);

            if(systemRules !=null && !systemRules.isEmpty() && machine.isHealthy()) {
                rules = systemRules.stream().map(v -> SystemRuleEntity.fromSystemRule(machine.getApp(), machine.getIp(), machine.getPort(), v)).collect(Collectors.toList());
                if (!rules.isEmpty()) {
                    for (SystemRuleEntity entity : rules) {
                        entity.setApp(app);
                    }
                    rules = repository.saveAll(rules);
                }
            }
            return Result.ofSuccess(rules);
        } catch (Throwable throwable) {
            logger.error("Query machine system rules error", throwable);
            return Result.ofThrowable(-1, throwable);
        }
    }


    @PostMapping("/rule")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<SystemRuleEntity> addRule(@RequestBody SystemRuleEntity entity) {
        Result<SystemRuleEntity> checkResult = checkBasicParams(entity);
        if (checkResult != null) {
            return checkResult;
        }
        Date date = new Date();
        entity.setGmtCreate(date);
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
            publishRules(entity.getApp());
        } catch (Throwable throwable) {
            logger.error("Add SystemRule error", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        return Result.ofSuccess(entity);
    }

    @PutMapping("/rule/{id}")
    @AuthAction(PrivilegeType.WRITE_RULE)
    public Result<SystemRuleEntity> updateRule(@PathVariable("id") Long id, @RequestBody SystemRuleEntity entity) {
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }
        SystemRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofFail(-1, "id " + id + " dose not exist");
        }
        if (entity == null) {
            return Result.ofFail(-1, "invalid body");
        }
        entity.setApp(oldEntity.getApp());
        entity.setIp(oldEntity.getIp());
        entity.setPort(oldEntity.getPort());
        entity.setId(oldEntity.getId());
        Result<SystemRuleEntity> checkResult = checkBasicParams(entity);
        if (checkResult != null) {
            return checkResult;
        }

        Date date = new Date();
        entity.setGmtCreate(oldEntity.getGmtCreate());
        entity.setGmtModified(date);
        try {
            entity = repository.save(entity);
            publishRules(entity.getApp());
        } catch (Throwable throwable) {
            logger.error("save error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        return Result.ofSuccess(entity);
    }

    @DeleteMapping("/rule/{id}")
    @AuthAction(PrivilegeType.DELETE_RULE)
    public Result<?> deleteRule(@PathVariable("id") Long id) {
        if (id == null) {
            return Result.ofFail(-1, "id can't be null");
        }
        SystemRuleEntity oldEntity = repository.findById(id);
        if (oldEntity == null) {
            return Result.ofSuccess(null);
        }
        try {
            repository.delete(id);
            publishRules(oldEntity.getApp());
        } catch (Throwable throwable) {
            logger.error("delete error:", throwable);
            return Result.ofThrowable(-1, throwable);
        }
        return Result.ofSuccess(id);
    }

}
