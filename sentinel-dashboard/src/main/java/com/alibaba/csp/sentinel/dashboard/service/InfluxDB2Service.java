package com.alibaba.csp.sentinel.dashboard.service;

import com.alibaba.fastjson.JSONObject;
import com.influxdb.annotations.Column;
import com.influxdb.client.*;
import com.influxdb.client.domain.InfluxQLQuery;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxColumn;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.influxdb.query.InfluxQLQueryResult;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class InfluxDB2Service {
    private static final Logger logger = LoggerFactory.getLogger(InfluxDB2Service.class);
    //    /**时间格式*/
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final Pattern CAMEL_CASE_TO_SNAKE_CASE_REPLACE_PATTERN = Pattern.compile("[A-Z]");
    @Resource
    private InfluxDBClient influxDBClient;

    @Value("${spring.influxdb2.url}")
    private String url;
    @Value("${spring.influxdb2.token}")
    private String token;
    @Value("${spring.influxdb2.org}")
    private String org;
    @Value("${spring.influxdb2.bucket}")
    private String bucket;

    public void insertData(String measurement, Map<String, String> tags, Map<String, Object> fields) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            Point point = Point
                    .measurement(measurement)
                    .addTags(tags)
                    .addFields(fields)
                    .time(Instant.now(), WritePrecision.MS);
            writeApi.writePoint(bucket, org, point);
        }catch (Exception e){
            logger.error(e.getMessage());
        }

    }

    public void insertData(Point point) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            writeApi.writePoint(bucket, org, point);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    public <M> void insertDataByPO(M po) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            writeApi.writeMeasurement(bucket, org, WritePrecision.MS, po);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    public <M> void batchInsertDataByPO(List<M> pos) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            writeApi.writeMeasurements(bucket, org, WritePrecision.MS, pos);
        }catch (Exception e){
            logger.error(e.getMessage());
        }
    }

    public void batchInsertData(String measurement, List<Point> points) {
        try (WriteApi writeApi = influxDBClient.makeWriteApi()) {
            writeApi.writePoints(bucket, org, points);
        }catch (Exception e){
            logger.error(e.getMessage());
        }

    }

    public Map<String, List<Map<String, Object>>> selectDataWithFlux(String measurement, List<String> stringList) {
        StringBuilder sql1 = new StringBuilder();
        for (int i = 0; i < stringList.size(); i++) {
            if (i == stringList.size() - 1) {
                sql1.append('"').append(stringList.get(i)).append('"');
            } else {
                sql1.append('"').append(stringList.get(i)).append('"').append(" or r[\"_field\"] == ");
            }
        }
        String sql2 = "|> range(start: -1m)";

        String sql = "from(bucket: \"%s\")\n" + sql2 +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == " + sql1 + ")\n" +
//                "  |> sort(columns:[\"valueTime\"])  " +
                "  |> yield()";

        return selectWithFlux(measurement, sql);
    }

    public Map<String, List<Map<String, Object>>> selectHistoryDataWithFlux(String measurement, List<String> stringList, String period) {
        StringBuilder sql1 = new StringBuilder();
        for (int i = 0; i < stringList.size(); i++) {
            if (i == stringList.size() - 1) {
                sql1.append('"').append(stringList.get(i)).append('"');
            } else {
                sql1.append('"').append(stringList.get(i)).append('"').append(" or r[\"_field\"] == ");
            }
        }
        String sql2 = "|> range(start: "+period+")";
        String sql = "  from(bucket: \"%s\")\n" + sql2 +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == " + sql1 + ")\n" +
//                "  |> sort(columns:[\"valueTime\"])  " +
//                "  |> aggregateWindow(every: 1h, fn: mean, createEmpty: true)\n" +
                "  |> yield(name: \"mean\")";

        return selectWithFlux(measurement, sql);
    }

    public Map<String, List<Map<String, Object>>> selectDataBetweenWithFlux(String measurement, Map<String, String> tags, List<String> fields, long startTime, long endTime) {

        String rangeSql = "|> range(start: " + DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN) +
                ", stop: " + DateFormatUtils.format(new Date(endTime), DATE_FORMAT_PATTERN) +
                ")";

        String sql = "from(bucket: \"%s\")\n" + rangeSql +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                convertTags(tags) +
                convertFields(fields) +
//                "  |> sort(columns:[\"valueTime\"])  " +
                "  |> yield()";

        return selectWithFlux(measurement, sql);
    }

    public Map<String, List<Map<String, Object>>> selectDataBetweenWithFlux(String measurement, Map<String, String> tags, List<String> fields, String period) {

        String rangeSql = "|> range(start: "+period+")";

        String sql = "from(bucket: \"%s\")\n" + rangeSql +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                convertTags(tags) +
                convertFields(fields) +
//                "  |> sort(columns:[\"valueTime\"])  " +
                "  |> yield()";

        return selectWithFlux(measurement, sql);
    }

    public <M> List<M> selectBetweenWithFlux(String measurement, Map<String, String> tags, List<String> fields, long startTime, long endTime, Class<M> mClass) {

        String rangeSql = "|> range(start: " + DateFormatUtils.format(new Date(startTime), DATE_FORMAT_PATTERN) +
                ", stop: " + DateFormatUtils.format(new Date(endTime), DATE_FORMAT_PATTERN) +
                ")";

        String sql = "from(bucket: \"%s\")\n" + rangeSql +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                convertTags(tags) +
                convertFields(fields);
//                "  |> sort(columns:[\"valueTime\"])  " +
//                "  |> yield()";

        return selectWithFlux(measurement, sql, mClass);
    }

    public <M> List<M> selectBetweenWithFlux(String measurement, Map<String, String> tags, List<String> fields, String period, Class<M> mClass) {

        String rangeSql = "|> range(start: "+period+")";

        String sql = "from(bucket: \"%s\")\n" + rangeSql +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"%s\")\n" +
                convertTags(tags) +
                convertFields(fields);
//                "  |> sort(columns:[\"valueTime\"])  " +
//                "  |> yield()";

        return selectWithFlux(measurement, sql, mClass);
    }

    private String convertTags(Map<String, String> tags){
        StringBuilder tagSql = new StringBuilder();
        if(tags!=null && tags.size()>0) {
            tagSql.append(" |> filter(fn: (r) => ");
            for (String tag : tags.keySet()) {
                tagSql.append("r[\"").append(tag).append("\"] == \"").append(tags.get(tag)).append("\" or ");
            }
            tagSql.delete(tagSql.length()-3, tagSql.length());
            tagSql.append(")\n");
        }
        return tagSql.toString();
    }

    private String convertFields(List<String> fields){
        StringBuilder fieldSql = new StringBuilder();
        if(fields!=null && fields.size()>0) {
            fieldSql.append(" |> filter(fn: (r) => r[\"_field\"] == ");
            for (int i = 0; i < fields.size(); i++) {
                if (i == fields.size() - 1) {
                    fieldSql.append('"').append(fields.get(i)).append('"');
                } else {
                    fieldSql.append('"').append(fields.get(i)).append('"').append(" or r[\"_field\"] == ");
                }
            }
            fieldSql.append(")\n");
        }
        return fieldSql.toString();
    }

    private Map<String, List<Map<String, Object>>> selectWithFlux(String measurement, String sql){
        String flux = String.format(sql, bucket, measurement);
        QueryApi queryApi = influxDBClient.getQueryApi();

        List<FluxTable> tables = queryApi.query(flux, org);

        Map<String, List<Map<String, Object>>> resulMap = new HashMap<>();

        if (!CollectionUtils.isEmpty(tables)) {
            for (FluxTable table : tables) {
                List<FluxColumn> columns = table.getColumns();
                for (FluxColumn column: columns){

                }
                List<FluxRecord> records = table.getRecords();
                List<Map<String, Object>> mapList = new ArrayList<>();
                for (FluxRecord fluxRecord : records) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("value", fluxRecord.getValue());
                    map.put("valueTime", fluxRecord.getTime());//Instant
                    map.put("field", fluxRecord.getField());
                    mapList.add(map);
                }
                String name = (String) mapList.get(0).get("field");
                resulMap.put(name, mapList);
            }
        }
        return resulMap;
    }


    private <M> List<M> selectWithFlux(String measurement, String sql, Class<M> mClass){
        List<M> results = new ArrayList<>();
        String flux = String.format(sql, bucket, measurement);
        List<FluxTable>  tables = influxDBClient.getQueryApi().query(flux, org);
        logger.debug("----Source FluxTable: {}", JSONObject.toJSONString(tables));
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable.getRecords();
            List<FluxColumn> columns = fluxTable.getColumns();
            logger.debug("----Source FluxColumn: {}", JSONObject.toJSONString(columns));
            logger.debug("----Source FluxRecord: {}", JSONObject.toJSONString(records));

            for (FluxRecord fluxRecord : records) {
                logger.debug(fluxRecord.getTime() + ": " + fluxRecord.getField() + ":"
                        + fluxRecord.getValue());
            }

        }
        return results;
    }

    public <T> List<T> selectWithInfluxQL(String database, String sql, Map<String, Object> paramMap, Class<T> tClass) {
        List<T> resp = new ArrayList<>();
//        try (InfluxDBClient influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), org)) {
            InfluxQLQueryApi queryApi = influxDBClient.getInfluxQLQueryApi();

            // send request
            InfluxQLQueryResult result = queryApi.query(new InfluxQLQuery(sql, database));
            logger.debug("----Source InfluxQLQueryResult: {}", JSONObject.toJSONString(result));
            if(result!=null && result.getResults()!=null && !result.getResults().isEmpty()) {
                for (InfluxQLQueryResult.Result resultResult : result.getResults()) {
                    for (InfluxQLQueryResult.Series series : resultResult.getSeries()) {
                        for (InfluxQLQueryResult.Series.Record record : series.getValues()) {
                            logger.debug(JSONObject.toJSONString(record));
                            Map<String, Object> re = new HashMap<>();
                            for (String col : series.getColumns().keySet()) {
                                re.put(col, record.getValueByKey(col));
                            }
                            logger.debug("----Parse POJO: {}", JSONObject.toJSONString(toPOJO(re, tClass)));

                            if (!re.isEmpty()) {
                                resp.add(toPOJO(re, tClass));
                            }
                        }
                    }
                }
            }
//        }
        return resp;
    }

    private <T> T toPOJO(@Nonnull final Map<String, Object> recordValues, @Nonnull final Class<T> tClass) {

        Objects.requireNonNull(recordValues, "Record is required");
        Objects.requireNonNull(tClass, "Class type is required");

        try {
            T pojo = tClass.newInstance();

            Class<?> currentClazz = tClass;
            while (currentClazz != null) {

                Field[] fields = currentClazz.getDeclaredFields();
                for (Field field : fields) {
                    Column anno = field.getAnnotation(Column.class);
                    String columnName = field.getName();
                    if (anno != null && !anno.name().isEmpty()) {
                        columnName = anno.name();
                    }

                    String col = null;

                    if (recordValues.containsKey(columnName)) {
                        col = columnName;
                    } else if (recordValues.containsKey("_" + columnName)) {
                        col = "_" + columnName;
                    } else if (anno != null && anno.measurement()) {
                        col = "_measurement";
                    } else {
                        String columnNameInSnakeCase = camelCaseToSnakeCase(columnName);
                        if (recordValues.containsKey(columnNameInSnakeCase)) {
                            col = columnNameInSnakeCase;
                        }
                    }

                    if (col != null) {
                        Object value = recordValues.get(col);

                        setFieldValue(pojo, field, value);
                    }
                }

                currentClazz = currentClazz.getSuperclass();
            }
            return pojo;
        } catch (Exception e) {
            throw new InfluxException(e);
        }
    }
    private String camelCaseToSnakeCase(final String str) {
        return CAMEL_CASE_TO_SNAKE_CASE_REPLACE_PATTERN.matcher(str)
                .replaceAll("_$0")
                .toLowerCase();
    }

    private void setFieldValue(@Nonnull final Object object,
                               @Nullable final Field field,
                               @Nullable final Object value) {

        if (field == null || value == null) {
            return;
        }
        String msg =
                "Class '%s' field '%s' was defined with a different field type and caused a ClassCastException. "
                        + "The correct type is '%s' (current field value: '%s').";

        try {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            Class<?> fieldType = field.getType();

            //the same type
            if (fieldType.equals(value.getClass())) {
                field.set(object, value);
                return;
            }

            //convert primitives
            if (double.class.isAssignableFrom(fieldType) || Double.class.isAssignableFrom(fieldType)) {
                field.set(object, toDoubleValue(value));
                return;
            }
            if (long.class.isAssignableFrom(fieldType) || Long.class.isAssignableFrom(fieldType)) {
                field.set(object, toLongValue(value));
                return;
            }
            if (int.class.isAssignableFrom(fieldType) || Integer.class.isAssignableFrom(fieldType)) {
                field.set(object, toIntValue(value));
                return;
            }
            if (float.class.isAssignableFrom(fieldType) || Float.class.isAssignableFrom(fieldType)) {
                field.set(object, toFloatValue(value));
                return;
            }
            if (short.class.isAssignableFrom(fieldType) || Short.class.isAssignableFrom(fieldType)) {
                field.set(object, toShortValue(value));
                return;
            }
            if (boolean.class.isAssignableFrom(fieldType)) {
                field.setBoolean(object, Boolean.valueOf(String.valueOf(value)));
                return;
            }
            if (BigDecimal.class.isAssignableFrom(fieldType)) {
                field.set(object, toBigDecimalValue(value));
                return;
            }

            //enum
            if (fieldType.isEnum()) {
                //noinspection unchecked, rawtypes
                field.set(object, Enum.valueOf((Class<Enum>) fieldType, String.valueOf(value)));
                return;
            }

            field.set(object, value);

        } catch (ClassCastException | IllegalAccessException e) {

            throw new InfluxException(String.format(msg, object.getClass().getName(), field.getName(),
                    value.getClass().getName(), value));
        }
    }

    private double toDoubleValue(final Object value) {

        if (double.class.isAssignableFrom(value.getClass()) || Double.class.isAssignableFrom(value.getClass())) {
            return (double) value;
        }

        return Double.parseDouble(value.toString());
    }

    private long toLongValue(final Object value) {

        if (long.class.isAssignableFrom(value.getClass()) || Long.class.isAssignableFrom(value.getClass())) {
            return (long) value;
        }

        return Long.parseLong(value.toString());
    }

    private int toIntValue(final Object value) {

        if (int.class.isAssignableFrom(value.getClass()) || Integer.class.isAssignableFrom(value.getClass())) {
            return (int) value;
        }

        return Integer.parseInt(value.toString());
    }

    private float toFloatValue(final Object value) {

        if (float.class.isAssignableFrom(value.getClass()) || Float.class.isAssignableFrom(value.getClass())) {
            return (float) value;
        }

        return Float.parseFloat(value.toString());
    }

    private short toShortValue(final Object value) {

        if (short.class.isAssignableFrom(value.getClass()) || Short.class.isAssignableFrom(value.getClass())) {
            return (short) value;
        }

        return Short.parseShort(value.toString());
    }

    private BigDecimal toBigDecimalValue(final Object value) {
        if (String.class.isAssignableFrom(value.getClass())) {
            return new BigDecimal((String) value);
        }

        if (double.class.isAssignableFrom(value.getClass()) || Double.class.isAssignableFrom(value.getClass())) {
            return BigDecimal.valueOf((double) value);
        }

        if (int.class.isAssignableFrom(value.getClass()) || Integer.class.isAssignableFrom(value.getClass())) {
            return BigDecimal.valueOf((int) value);
        }

        if (long.class.isAssignableFrom(value.getClass()) || Long.class.isAssignableFrom(value.getClass())) {
            return BigDecimal.valueOf((long) value);
        }

        if (float.class.isAssignableFrom(value.getClass()) || Float.class.isAssignableFrom(value.getClass())) {
            return BigDecimal.valueOf((float) value);
        }

        if (short.class.isAssignableFrom(value.getClass()) || Short.class.isAssignableFrom(value.getClass())) {
            return BigDecimal.valueOf((short) value);
        }

        String message = String.format("Cannot cast %s [%s] to %s.",
                value.getClass().getName(), value, BigDecimal.class);

        throw new ClassCastException(message);
    }

}
