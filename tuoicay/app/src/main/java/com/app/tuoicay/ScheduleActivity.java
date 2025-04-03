package com.app.tuoicay;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ScheduleActivity extends AppCompatActivity {

    private TimePicker timePickerCommon;
    private Switch switchPump, switchLight;
    private EditText editTextPumpTime, editTextLightTime;
    private MaterialButton buttonSaveSchedule;
    private Spinner spinnerRepeatSchedule;
    private LinearLayout layoutCustomRepeat;
    private CheckBox checkMonday, checkTuesday, checkWednesday, checkThursday, checkFriday, checkSaturday, checkSunday;
    private TextView textViewStatus; // TextView hiển thị trạng thái bật/tắt

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        // Khởi tạo các view
        timePickerCommon = findViewById(R.id.timePickerCommon);
        switchPump = findViewById(R.id.switchPump);
        switchLight = findViewById(R.id.switchLight);
        editTextPumpTime = findViewById(R.id.editTextPumpTime);
        editTextLightTime = findViewById(R.id.editTextLightTime);
        buttonSaveSchedule = findViewById(R.id.buttonSaveSchedule);
        spinnerRepeatSchedule = findViewById(R.id.spinnerRepeatSchedule);
        layoutCustomRepeat = findViewById(R.id.layoutCustomRepeat);

        // Khởi tạo các CheckBox
        checkMonday = findViewById(R.id.checkMonday);
        checkTuesday = findViewById(R.id.checkTuesday);
        checkWednesday = findViewById(R.id.checkWednesday);
        checkThursday = findViewById(R.id.checkThursday);
        checkFriday = findViewById(R.id.checkFriday);
        checkSaturday = findViewById(R.id.checkSaturday);
        checkSunday = findViewById(R.id.checkSunday);



        // Ẩn layout chứa checkbox khi bắt đầu
        layoutCustomRepeat.setVisibility(View.GONE);

        // Thiết lập adapter cho Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.repeat_schedule_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatSchedule.setAdapter(adapter);

        // Lắng nghe sự kiện chọn "Tùy chỉnh" từ Spinner
        spinnerRepeatSchedule.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                String repeatOption = parentView.getItemAtPosition(position).toString();

                // Kiểm tra nếu lựa chọn là "Tùy chỉnh"
                if ("Tùy chỉnh".equals(repeatOption)) {
                    layoutCustomRepeat.setVisibility(View.VISIBLE); // Hiển thị form chọn ngày
                } else {
                    layoutCustomRepeat.setVisibility(View.GONE); // Ẩn form chọn ngày
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Xử lý nếu không có lựa chọn nào được chọn
            }
        });

        // Lắng nghe sự kiện bật/tắt của công tắc máy bơm
        switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchLight.setChecked(false);
                findViewById(R.id.layoutPumpTime).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutLightTime).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layoutPumpTime).setVisibility(View.GONE);
            }
        });

        // Lắng nghe sự kiện bật/tắt của công tắc đèn
        switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                switchPump.setChecked(false);
                findViewById(R.id.layoutLightTime).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutPumpTime).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layoutLightTime).setVisibility(View.GONE);
            }
        });

        // Xử lý sự kiện lưu lịch trình
        buttonSaveSchedule.setOnClickListener(v -> {
            int hour = timePickerCommon.getHour();
            int minute = timePickerCommon.getMinute();
            String formattedTime = String.format("%02d:%02d", hour, minute);

            String timeStart = formattedTime;

            String pumpTime = editTextPumpTime.getText().toString();
            String lightTime = editTextLightTime.getText().toString();

            String pumpEndTime = "";
            String lightEndTime = "";
            String deviceName = "";

            if (switchPump.isChecked()) {
                deviceName = "Máy bơm";
                pumpEndTime = pumpTime + " giây";
            }

            if (switchLight.isChecked()) {
                deviceName = "Đèn";
                lightEndTime = lightTime + " giờ";
            }
            // Mặc định trạng thái là tắt (false)
            boolean status = false;
            String repeatSchedule = spinnerRepeatSchedule.getSelectedItem().toString();
            String customDays = getCustomDays(); // Lấy các ngày đã chọn





            // Lưu lịch trình vào Firebase
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("schedule");

            String timeId = myRef.push().getKey();

            myRef.child(timeId).setValue(new Schedule(
                    timeId,
                    timeStart,
                    switchPump.isChecked() ? pumpEndTime : lightEndTime,
                    deviceName,
                    switchPump.isChecked(),
                    switchLight.isChecked(),
                    repeatSchedule,
                    customDays, // Lưu các ngày tùy chỉnh
                    status // Lưu trạng thái bật/tắt
            ));

            Toast.makeText(ScheduleActivity.this, "Lịch trình đã được lưu!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // Hàm lấy các ngày trong tuần đã chọn
    private String getCustomDays() {
        StringBuilder days = new StringBuilder();
        if (checkMonday.isChecked()) days.append("Thứ 2, ");
        if (checkTuesday.isChecked()) days.append("Thứ 3, ");
        if (checkWednesday.isChecked()) days.append("Thứ 4, ");
        if (checkThursday.isChecked()) days.append("Thứ 5, ");
        if (checkFriday.isChecked()) days.append("Thứ 6, ");
        if (checkSaturday.isChecked()) days.append("Thứ 7, ");
        if (checkSunday.isChecked()) days.append("Chủ nhật, ");

        if (days.length() > 0) {
            days.setLength(days.length() - 2); // Loại bỏ dấu phẩy thừa
        }
        return days.toString();
    }

    public static class Schedule {
        public String timeId;
        public String timeStart;
        public String timeEnd;
        public String deviceName;
        public boolean pumpStatus;
        public boolean lightStatus;
        public String repeatSchedule;
        public String customDays; // Lưu các ngày tùy chỉnh
        public boolean status; // Trạng thái bật/tắt

        public Schedule(String timeId, String timeStart, String timeEnd, String deviceName, boolean pumpStatus, boolean lightStatus, String repeatSchedule, String customDays, boolean isActive) {
            this.timeId = timeId;
            this.timeStart = timeStart;
            this.timeEnd = timeEnd;
            this.deviceName = deviceName;
            this.pumpStatus = pumpStatus;
            this.lightStatus = lightStatus;
            this.repeatSchedule = repeatSchedule;
            this.customDays = customDays; // Gán ngày tùy chỉnh
            this.status = status; // Gán trạng thái bật/tắt
        }
    }
}
