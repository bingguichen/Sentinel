package com.alibaba.csp.sentinel.dashboard.repository.metric;

import com.alibaba.csp.sentinel.dashboard.datasource.entity.MetricEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.influxdb.MetricPO;
import com.alibaba.csp.sentinel.dashboard.service.InfluxDB2Service;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository("influxDBFluxMetricsRepository")
public class InfluxDBFluxMetricsRepository implements MetricsRepository<MetricEntity>{

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBFluxMetricsRepository.class);
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
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("app", app);
        tags.put("resource", resource);
        List<MetricPO> metrics = influxDB2Service.selectBetweenWithFlux(METRIC_MEASUREMENT, tags, null, startTime, endTime, MetricPO.class);
        List<MetricEntity> results = new ArrayList<MetricEntity>();
        for (MetricPO metricPO: metrics){
            logger.debug("Metric PO: {}", JSONObject.toJSON(metricPO));
            results.add(convertToMetricEntity(metricPO));
        }
        return results;
    }

    @Override
    public List<String> listResourcesOfApp(String app, String period) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("app", app);
        List<MetricPO> metrics = influxDB2Service.selectBetweenWithFlux(METRIC_MEASUREMENT, tags, null, period, MetricPO.class);
        List<String> results = new ArrayList<String>();
        if (CollectionUtils.isEmpty(metrics)) {
            return results;
        }

        List<MetricEntity> metricEntities = new ArrayList<MetricEntity>();
        for (MetricPO metricPO : metrics) {
            logger.debug("Metric PO: {}", JSONObject.toJSON(metricPO));
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

    private Point convertMetricToPoint(MetricEntity metric){

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("app", metric.getApp());
        tags.put("resource", metric.getResource());

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("id", metric.getId());
        fields.put("gmtCreate", metric.getGmtCreate().getTime());
        fields.put("gmtModified", metric.getGmtModified().getTime());
        fields.put("passQps", metric.getPassQps());
        fields.put("successQps", metric.getSuccessQps());
        fields.put("blockQps", metric.getBlockQps());
        fields.put("exceptionQps", metric.getExceptionQps());
        fields.put("rt", metric.getRt());
        fields.put("count", metric.getCount());
        fields.put("resourceCode", metric.getResourceCode());

        return Point
                .measurement(METRIC_MEASUREMENT)
                .addTags(tags)
                .addFields(fields)
                .time(Instant.now(), WritePrecision.NS);
    }

    private MetricPO convertToMetricPO(MetricEntity metric) {
        MetricPO metricPO = new MetricPO();

        metricPO.setId(metric.getId());
        metricPO.setGmtCreate(metric.getGmtCreate().getTime());
        metricPO.setGmtModified(metric.getGmtModified().getTime());
        metricPO.setApp(metric.getApp());
//        metricPO.setTimestamp(Date.from(metric.getTime().minusMillis(TimeUnit.HOURS.toMillis(UTC_8))));// 查询数据减8小时
        metricPO.setResource(metric.getResource());
        metricPO.setPassQps(metric.getPassQps());
        metricPO.setSuccessQps(metric.getSuccessQps());
        metricPO.setBlockQps(metric.getBlockQps());
        metricPO.setExceptionQps(metric.getExceptionQps());
        metricPO.setRt(metric.getRt());
        metricPO.setCount(metric.getCount());

        return metricPO;
    }

    private MetricEntity convertToMetricEntity(MetricPO metricPO) {
        MetricEntity metricEntity = new MetricEntity();

        metricEntity.setId(metricPO.getId());
//        metricEntity.setGmtCreate(new Date(metricPO.getGmtCreate().toEpochMilli()));
//        metricEntity.setGmtModified(new Date(metricPO.getGmtModified().toEpochMilli()));
        metricEntity.setApp(metricPO.getApp());
        metricEntity.setTimestamp(new Date(metricPO.getTime()));
        metricEntity.setResource(metricPO.getResource());
        metricEntity.setPassQps(metricPO.getPassQps());
        metricEntity.setSuccessQps(metricPO.getSuccessQps());
        metricEntity.setBlockQps(metricPO.getBlockQps());
        metricEntity.setExceptionQps(metricPO.getExceptionQps());
        metricEntity.setRt(metricPO.getRt());
        metricEntity.setCount(metricPO.getCount());

        return metricEntity;
    }

    private MetricEntity convertToMetric(String app, Map<String, Object> result){
        MetricEntity metric = new MetricEntity();
        metric.setApp(app);
        metric.setId(Long.parseLong(result.get("id").toString()));
        metric.setGmtCreate(Date.valueOf(result.get("gmtCreate").toString()));
        metric.setGmtModified(Date.valueOf(result.get("gmtModified").toString()));
        metric.setTimestamp(new java.util.Date());
        metric.setResource(result.get("resource").toString());
        metric.setPassQps(Long.parseLong(result.get("passQps").toString()));
        metric.setSuccessQps(Long.parseLong(result.get("successQps").toString()));
        metric.setBlockQps(Long.parseLong(result.get("blockQps").toString()));
        metric.setExceptionQps(Long.parseLong(result.get("exceptionQps").toString()));
        metric.setRt(Double.parseDouble(result.get("rt").toString()));
        metric.setCount(Integer.parseInt(result.get("count").toString()));
        return metric;
    }
}
