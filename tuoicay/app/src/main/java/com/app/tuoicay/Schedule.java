package com.app.tuoicay;

import java.util.HashMap;
import java.util.Map;

public class Schedule {
    public String timeId;
    public String timeStart;
    public String timeEnd;
    public String deviceName;
    public boolean pumpStatus;
    public boolean lightStatus;
    public String repeatSchedule;
    public String customDays;
    public boolean status;
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("timeId", timeId);
        result.put("timeStart", timeStart);
        result.put("timeEnd", timeEnd);
        result.put("deviceName", deviceName);
        result.put("pumpStatus", pumpStatus);
        result.put("lightStatus", lightStatus);
        result.put("repeatSchedule", repeatSchedule);
        result.put("customDays", customDays);
        result.put("status", status);
        return result;
    }
    // Constructor không tham số (no-argument constructor)
    public Schedule() {
        // Constructor rỗng để Firebase có thể tạo đối tượng
    }

    // Các getter và setter (nếu bạn cần)
    public String getTimeId() {
        return timeId;
    }

    public void setTimeId(String timeId) {
        this.timeId = timeId;
    }

    public String getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public String getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public boolean isPumpStatus() {
        return pumpStatus;
    }

    public void setPumpStatus(boolean pumpStatus) {
        this.pumpStatus = pumpStatus;
    }

    public boolean isLightStatus() {
        return lightStatus;
    }

    public void setLightStatus(boolean lightStatus) {
        this.lightStatus = lightStatus;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCustomDays() {
        return customDays;
    }

    public void setCustomDays(String customDays) {
        this.customDays = customDays;
    }

    public String getRepeatSchedule() {
        return repeatSchedule;
    }

    public void setRepeatSchedule(String repeatSchedule) {
        this.repeatSchedule = repeatSchedule;
    }
}
