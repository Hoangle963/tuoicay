package com.app.tuoicay;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class EditScheduleActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private EditText pumpTimeEditText, lightTimeEditText;
    private Spinner repeatScheduleSpinner;
    private LinearLayout customRepeatLayout;
    private CheckBox mondayCheckBox, tuesdayCheckBox, wednesdayCheckBox, thursdayCheckBox, fridayCheckBox, saturdayCheckBox, sundayCheckBox;
    private Switch pumpSwitch, lightSwitch;
    private Button saveScheduleButton;

    private String timeId;
    private DatabaseReference scheduleDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_edit_schedule);

        // Ánh xạ các view
        initializeViews();

        // Lấy timeId từ Intent
        timeId = getIntent().getStringExtra("timeId");
        if (timeId == null || timeId.isEmpty()) {
            Toast.makeText(this, "ID không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khởi tạo Firebase database reference
        scheduleDatabaseRef = FirebaseDatabase.getInstance().getReference("schedule");

        // Tải dữ liệu lịch trình từ Firebase
        loadScheduleData();

        // Xử lý lưu lịch trình
        saveScheduleButton.setOnClickListener(v -> saveSchedule());
    }

    private void initializeViews() {
        timePicker = findViewById(R.id.timePickerCommon);
        pumpTimeEditText = findViewById(R.id.editTextPumpTime);
        lightTimeEditText = findViewById(R.id.editTextLightTime);
        repeatScheduleSpinner = findViewById(R.id.spinnerRepeatSchedule);
        customRepeatLayout = findViewById(R.id.layoutCustomRepeat);
        mondayCheckBox = findViewById(R.id.checkMonday);
        tuesdayCheckBox = findViewById(R.id.checkTuesday);
        wednesdayCheckBox = findViewById(R.id.checkWednesday);
        thursdayCheckBox = findViewById(R.id.checkThursday);
        fridayCheckBox = findViewById(R.id.checkFriday);
        saturdayCheckBox = findViewById(R.id.checkSaturday);
        sundayCheckBox = findViewById(R.id.checkSunday);
        pumpSwitch = findViewById(R.id.switchPump);
        lightSwitch = findViewById(R.id.switchLight);
        saveScheduleButton = findViewById(R.id.buttonSaveSchedule);

        // Thiết lập adapter cho Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.repeat_schedule_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatScheduleSpinner.setAdapter(adapter);

        // Lắng nghe sự kiện chọn "Tùy chỉnh" từ Spinner
        repeatScheduleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
                String repeatOption = parentView.getItemAtPosition(position).toString();
                customRepeatLayout.setVisibility("Tùy chỉnh".equals(repeatOption) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Xử lý nếu không có lựa chọn nào được chọn
            }
        });

        // Lắng nghe sự kiện bật/tắt của công tắc máy bơm
        pumpSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                lightSwitch.setChecked(false);
                findViewById(R.id.layoutPumpTime).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutLightTime).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layoutPumpTime).setVisibility(View.GONE);
            }
        });

        // Lắng nghe sự kiện bật/tắt của công tắc đèn
        lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                pumpSwitch.setChecked(false);
                findViewById(R.id.layoutLightTime).setVisibility(View.VISIBLE);
                findViewById(R.id.layoutPumpTime).setVisibility(View.GONE);
            } else {
                findViewById(R.id.layoutLightTime).setVisibility(View.GONE);
            }
        });
    }

    // Hàm tải dữ liệu lịch trình từ Firebase
    private void loadScheduleData() {
        scheduleDatabaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean isFound = false;
                for (DataSnapshot scheduleSnapshot : snapshot.getChildren()) {
                    Schedule schedule = scheduleSnapshot.getValue(Schedule.class);
                    if (schedule != null && schedule.timeId != null && schedule.timeId.equals(timeId)) {
                        populateForm(schedule);
                        isFound = true;
                        break;
                    }
                }

                if (!isFound) {
                    Toast.makeText(EditScheduleActivity.this, "Không tìm thấy lịch trình với mã: " + timeId, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditScheduleActivity.this, "Lỗi truy vấn: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Điền dữ liệu vào form
    private void populateForm(Schedule schedule) {
        // Cập nhật thời gian bắt đầu


        // Cập nhật trạng thái của bơm và đèn
        pumpSwitch.setChecked(schedule.pumpStatus);
        lightSwitch.setChecked(schedule.lightStatus);

        // Nếu là lặp lại tuỳ chỉnh, hiển thị các checkbox ngày
        if ("Custom".equals(schedule.repeatSchedule)) {
            customRepeatLayout.setVisibility(View.VISIBLE);
            setCustomDays(schedule.customDays);
        } else {
            customRepeatLayout.setVisibility(View.GONE);
        }
    }

    // Cập nhật các ngày trong tuần cho lịch lặp lại tuỳ chỉnh
    private void setCustomDays(String customDays) {
        String[] daysArray = customDays.split(",");
        mondayCheckBox.setChecked(contains(daysArray, "Thứ 2"));
        tuesdayCheckBox.setChecked(contains(daysArray, "Thứ 3"));
        wednesdayCheckBox.setChecked(contains(daysArray, "Thứ 4"));
        thursdayCheckBox.setChecked(contains(daysArray, "Thứ 5"));
        fridayCheckBox.setChecked(contains(daysArray, "Thứ 6"));
        saturdayCheckBox.setChecked(contains(daysArray, "Thứ 7"));
        sundayCheckBox.setChecked(contains(daysArray, "Chủ nhật"));
    }

    // Kiểm tra mảng có chứa giá trị hay không
    private boolean contains(String[] array, String value) {
        for (String day : array) {
            if (day.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    private void saveSchedule() {
        // Kiểm tra các trường nhập liệu
        if (pumpTimeEditText.getText().toString().isEmpty() && lightTimeEditText.getText().toString().isEmpty()) {
            Toast.makeText(EditScheduleActivity.this, "Vui lòng nhập thời gian bơm hoặc đèn", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy thông tin từ các view
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String pumpTime = pumpTimeEditText.getText().toString();
        String lightTime = lightTimeEditText.getText().toString();
        boolean pumpStatus = pumpSwitch.isChecked();
        boolean lightStatus = lightSwitch.isChecked();

        // Xử lý lịch lặp lại
        String repeatSchedule = repeatScheduleSpinner.getSelectedItem().toString();
        String customDays = "";
        if ("Custom".equals(repeatSchedule)) {
            customDays = getSelectedDays();

            // Debug: Kiểm tra giá trị customDays trước khi gửi
            Log.d("CustomDays", "Custom Days: " + customDays);

            // Kiểm tra nếu không có ngày nào được chọn
            if (customDays.isEmpty()) {
                Toast.makeText(EditScheduleActivity.this, "Vui lòng chọn ít nhất một ngày để lặp lại!", Toast.LENGTH_SHORT).show();
                return; // Dừng lại không lưu lên Firebase
            }
        }

        // Khởi tạo scheduleId từ Intent nếu có
        String scheduleId = getIntent().getStringExtra("timeId"); // Lấy ID từ Intent (nếu có)

        // Tạo đối tượng Schedule mới
        Schedule schedule = new Schedule();
        schedule.timeId = scheduleId != null ? scheduleId : ""; // Nếu có timeId, gán vào đối tượng
        schedule.timeStart = String.format("%02d:%02d", hour, minute);

        // Tính toán timeEnd dựa vào thiết bị nào được bật
        if (pumpStatus) {
            schedule.timeEnd = pumpTime + " giây"; // Nếu là bơm, thời gian là pumpTime
            schedule.deviceName = "Máy bơm";
        } else if (lightStatus) {
            schedule.timeEnd = lightTime + " giờ"; // Nếu là đèn, thời gian là lightTime
            schedule.deviceName = "Đèn";
        }

        schedule.pumpStatus = pumpStatus;
        schedule.lightStatus = lightStatus;
        schedule.repeatSchedule = repeatSchedule;
        schedule.customDays = customDays;
        schedule.status = false; // Mặc định trạng thái là tắt

        // Kiểm tra nếu có scheduleId, nghĩa là đang sửa lịch trình
        if (scheduleId != null && !scheduleId.isEmpty()) {
            // Cập nhật lịch trình trong Firebase nếu có timeId
            scheduleDatabaseRef.child(scheduleId).updateChildren(schedule.toMap()).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditScheduleActivity.this, "Lịch trình đã được cập nhật", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditScheduleActivity.this, "Lỗi khi cập nhật lịch trình", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Nếu không có timeId (ví dụ thêm mới), thêm lịch trình mới
            scheduleId = scheduleDatabaseRef.push().getKey(); // Tạo ID mới nếu không có scheduleId
            schedule.timeId = scheduleId;  // Gán lại scheduleId cho đối tượng Schedule
            scheduleDatabaseRef.child(scheduleId).setValue(schedule).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditScheduleActivity.this, "Lịch trình đã được lưu", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditScheduleActivity.this, "Lỗi khi lưu lịch trình", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private String getSelectedDays() {
        List<String> selectedDays = new ArrayList<>();
        if (mondayCheckBox.isChecked()) selectedDays.add("Thứ 2");
        if (tuesdayCheckBox.isChecked()) selectedDays.add("Thứ 3");
        if (wednesdayCheckBox.isChecked()) selectedDays.add("Thứ 4");
        if (thursdayCheckBox.isChecked()) selectedDays.add("Thứ 5");
        if (fridayCheckBox.isChecked()) selectedDays.add("Thứ 6");
        if (saturdayCheckBox.isChecked()) selectedDays.add("Thứ 7");
        if (sundayCheckBox.isChecked()) selectedDays.add("Chủ nhật");

        // Debug: Kiểm tra xem các ngày được chọn có đúng không
        Log.d("SelectedDays", "Selected Days: " + selectedDays.toString());

        // Nếu không có ngày nào được chọn, trả về chuỗi rỗng
        return selectedDays.isEmpty() ? "" : String.join(",", selectedDays);
    }

}
