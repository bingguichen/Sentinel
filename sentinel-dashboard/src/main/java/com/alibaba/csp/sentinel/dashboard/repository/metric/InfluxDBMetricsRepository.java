package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.influxdb.MetricPO;
import com.alibaba.csp.sentinel.dashboard.service.InfluxDB2Service;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Repository("influxDBMetricsRepository")
public class InfluxDBMetricsRepository implements MetricsRepository<MetricEntity>{

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBMetricsRepository.class);
    /**时间格式*/
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    /**数据库名称*/
    private static final String SENTINEL_DATABASE = "sentinel";

    /**数据表名称*/
    private static final String METRIC_MEASUREMENT = "sentinel_metric";

    /**北京时间领先UTC时间8小时 UTC: Universal Time Coordinated,世界统一时间*/
    private static final Integer UTC_8 = 8;

    @Autowired
    private InfluxDB2Service influxDB2Service;

    @Override
    public void save(MetricEntity metric) {
        if (metric == null || StringUtil.isBlank(metric.getApp())) {
            return;
        }

        if (metric.getId() == null) {
            metric.setId(System.currentTimeMillis());
        }

        influxDB2Service.insertDataByPO(convertToMetricPO(metric));
    }

    @Override
    public void saveAll(Iterable<MetricEntity> metrics) {
        List<MetricPO> points = new ArrayList<MetricPO>();
        for (MetricEntity metric : metrics) {
            if (metric.getId() == null) {
                metric.setId(System.currentTimeMillis());
            }
            points.add(convertToMetricPO(metric));
        }
        influxDB2Service.batchInsertDataByPO(points);
    }


    @Override
    public List<MetricEntity> queryByAppAndResourceBetween(String app, String resource, long startTime, long endTime) {
        List<MetricEntity> results = new ArrayList<MetricEntity>();
        if (StringUtil.isBlank(app)) {
            return results;
        }

        if (StringUtil.isBlank(resource)) {
            return results;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM " + METRIC_MEASUREMENT);
        sql.append(" WHERE app='").append(app).append("'");
        sql.append(" AND resource='").append(resource).append("'");
        sql.append(" AND time>='").append(DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN)).append("'");
        sql.append(" AND time<='").append(DateFormatUtils.format(new Date(endTime), DATE_FORMAT_PATTERN)).append("'");

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("app", app);
        paramMap.put("resource", resource);
        paramMap.put("startTime", DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN));
        paramMap.put("endTime", DateFormatUtils.format(new Date(endTime), DATE_FORMAT_PATTERN));

        List<MetricPO> metricPOS = influxDB2Service.selectWithInfluxQL(SENTINEL_DATABASE, sql.toString(), paramMap, MetricPO.class);

        if (CollectionUtils.isEmpty(metricPOS)) {
            return results;
        }

        for (MetricPO metricPO : metricPOS) {
            results.add(convertToMetricEntity(metricPO));
        }

        return results;
    }

    @Override
    public List<String> listResourcesOfApp(String app, String period) {
        List<String> results = new ArrayList<>();
        if (StringUtil.isBlank(app)) {
            return results;
        }
        long startTime = System.currentTimeMillis() - 1000 * 60;
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM " + METRIC_MEASUREMENT);
        sql.append(" WHERE app='").append(app).append("'");
        sql.append(" AND time>='").append(DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN)).append("'");

        Map<String, Object> paramMap = new HashMap<String, Object>();

        paramMap.put("app", app);
        paramMap.put("startTime", DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN));

        List<MetricPO> metricPOS = influxDB2Service.selectWithInfluxQL(SENTINEL_DATABASE, sql.toString(), paramMap, MetricPO.class);

        if (CollectionUtils.isEmpty(metricPOS)) {
            return results;
        }

        List<MetricEntity> metricEntities = new ArrayList<MetricEntity>();
        for (MetricPO metricPO : metricPOS) {
            metricEntities.add(convertToMetricEntity(metricPO));
        }

        Map<String, MetricEntity> resourceCount = new HashMap<>(32);

        for (MetricEntity metricEntity : metricEntities) {
            String resource = metricEntity.getResource();
            if (resourceCount.containsKey(resource)) {
                MetricEntity oldEntity = resourceCount.get(resource);
                oldEntity.addPassQps(metricEntity.getPassQps());
                oldEntity.addRtAndSuccessQps(metricEntity.getRt(), metricEntity.getSuccessQps());
                oldEntity.addBlockQps(metricEntity.getBlockQps());
                oldEntity.addExceptionQps(metricEntity.getExceptionQps());
                oldEntity.addCount(1);
            } else {
                resourceCount.put(resource, MetricEntity.copyOf(metricEntity));
            }
        }

        // Order by last minute b_qps DESC.
        return resourceCount.entrySet()
                .stream()
                .sorted((o1, o2) -> {
                    MetricEntity e1 = o1.getValue();
                    MetricEntity e2 = o2.getValue();
                    int t = e2.getBlockQps().compareTo(e1.getBlockQps());
                    if (t != 0) {
                        return t;
                    }
                    return e2.getPassQps().compareTo(e1.getPassQps());
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private MetricEntity convertToMetricEntity(MetricPO metricPO) {
        MetricEntity metricEntity = new MetricEntity();

        metricEntity.setId(metricPO.getId());
        metricEntity.setGmtCreate(new Date(metricPO.getGmtCreate()));
        metricEntity.setGmtModified(new Date(metricPO.getGmtModified()));
        metricEntity.setApp(metricPO.getApp());
        metricEntity.setTimestamp(new Date(metricPO.getGmtCreate()));
        metricEntity.setResource(metricPO.getResource());
        metricEntity.setPassQps(metricPO.getPassQps());
        metricEntity.setSuccessQps(metricPO.getSuccessQps());
        metricEntity.setBlockQps(metricPO.getBlockQps());
        metricEntity.setExceptionQps(metricPO.getExceptionQps());
        metricEntity.setRt(metricPO.getRt());
        metricEntity.setCount(metricPO.getCount());

        return metricEntity;
    }

    private MetricPO convertToMetricPO(MetricEntity metric) {
        MetricPO metricPO = new MetricPO();

        metricPO.setId(metric.getId());
        metricPO.setGmtCreate(metric.getGmtCreate().getTime());
        metricPO.setGmtModified(metric.getGmtModified().getTime());
        metricPO.setApp(metric.getApp());
        metricPO.setResource(metric.getResource());
        metricPO.setPassQps(metric.getPassQps());
        metricPO.setSuccessQps(metric.getSuccessQps());
        metricPO.setBlockQps(metric.getBlockQps());
        metricPO.setExceptionQps(metric.getExceptionQps());
        metricPO.setRt(metric.getRt());
        metricPO.setCount(metric.getCount());

        return metricPO;
    }

}
