package com.example.buttontimerecorder;

import java.io.Serializable;

/**
 * 时间记录数据模型
 * 对应原Python代码中的记录字典
 */
public class TimeRecord implements Serializable {
    private int id;
    private String name;           // 按钮名称
    private long pressTimestamp;   // 按下时间戳（毫秒）
    private String pressTime;      // 按下时间字符串 (HH:mm)
    private String pressFullTime;  // 按下时间字符串 (yyyy-MM-dd HH:mm:ss)
    private String releaseTime;    // 弹起时间字符串 (HH:mm)，null表示未弹起
    private String releaseFullTime;// 弹起时间字符串 (yyyy-MM-dd HH:mm:ss)
    private long releaseTimestamp; // 弹起时间戳
    private String duration;       // 持续时长字符串

    public TimeRecord() {}

    public TimeRecord(int id, String name, long pressTimestamp, String pressTime, String pressFullTime) {
        this.id = id;
        this.name = name;
        this.pressTimestamp = pressTimestamp;
        this.pressTime = pressTime;
        this.pressFullTime = pressFullTime;
        this.releaseTime = null;
        this.duration = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getPressTimestamp() { return pressTimestamp; }
    public void setPressTimestamp(long pressTimestamp) { this.pressTimestamp = pressTimestamp; }

    public String getPressTime() { return pressTime; }
    public void setPressTime(String pressTime) { this.pressTime = pressTime; }

    public String getPressFullTime() { return pressFullTime; }
    public void setPressFullTime(String pressFullTime) { this.pressFullTime = pressFullTime; }

    public String getReleaseTime() { return releaseTime; }
    public void setReleaseTime(String releaseTime) { this.releaseTime = releaseTime; }

    public String getReleaseFullTime() { return releaseFullTime; }
    public void setReleaseFullTime(String releaseFullTime) { this.releaseFullTime = releaseFullTime; }

    public long getReleaseTimestamp() { return releaseTimestamp; }
    public void setReleaseTimestamp(long releaseTimestamp) { this.releaseTimestamp = releaseTimestamp; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public boolean isUnclosed() {
        return releaseTime == null || releaseTime.isEmpty();
    }
}
