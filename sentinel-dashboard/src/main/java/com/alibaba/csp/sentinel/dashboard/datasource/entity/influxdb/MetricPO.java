package com.alibaba.csp.sentinel.dashboard.datasource.entity.influxdb;


import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

import java.time.Instant;

@Measurement(name = "sentinel_metric")
public class MetricPO {
    @Column(name = "time")
    private long time;

    @Column(name = "id")
    private long id;

    @Column(name = "gmtCreate")
    private long gmtCreate;

    @Column(name = "gmtModified")
    private long gmtModified;

    @Column(name = "app", tag = true)
    private String app;

    @Column(name = "resource", tag = true)
    private String resource;

    @Column(name = "passQps")
    private long passQps;

    @Column(name = "successQps")
    private long successQps;

    @Column(name = "blockQps")
    private long blockQps;

    @Column(name = "exceptionQps")
    private long exceptionQps;

    @Column(name = "rt")
    private double rt;

    @Column(name = "count")
    private int count;

    @Column(name = "resourceCode")
    private int resourceCode;

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(long gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public long getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(long gmtModified) {
        this.gmtModified = gmtModified;
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public long getPassQps() {
        return passQps;
    }

    public void setPassQps(long passQps) {
        this.passQps = passQps;
    }

    public long getSuccessQps() {
        return successQps;
    }

    public void setSuccessQps(long successQps) {
        this.successQps = successQps;
    }

    public long getBlockQps() {
        return blockQps;
    }

    public void setBlockQps(long blockQps) {
        this.blockQps = blockQps;
    }

    public long getExceptionQps() {
        return exceptionQps;
    }

    public void setExceptionQps(long exceptionQps) {
        this.exceptionQps = exceptionQps;
    }

    public double getRt() {
        return rt;
    }

    public void setRt(double rt) {
        this.rt = rt;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(int resourceCode) {
        this.resourceCode = resourceCode;
    }
}
