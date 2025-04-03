package com.app.tuoicay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView textViewHumidity, textViewSoilMoisture, textViewTemperature, textViewScheduleCount;
    private Button buttonRelay1, buttonRelay2, buttonAutoMode, buttonHeatLamp;
    private DatabaseReference databaseReference;
    private boolean autoMode = false;
    private Handler handler = new Handler();
    private Runnable updateDataRunnable;
    private Button buttonSchedule;

    private static final String CHANNEL_ID = "TuoicayNotifications";
    private boolean isNotificationSent = false; // Biến để kiểm soát thông báo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo các thành phần giao diện
        initializeUIComponents();

        // Kết nối với Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Tạo NotificationChannel
        createNotificationChannel();

        // Bắt đầu cập nhật dữ liệu từ Firebase
        startUpdatingData();

        // Đọc trạng thái relay để hiển thị và cập nhật màu nút
        loadRelayState("Relay1", buttonRelay1);
        loadRelayState("Relay2", buttonRelay2);
        loadRelayState("Relay3", buttonHeatLamp);  // Load trạng thái relay cho đèn sưởi
        // Lấy số lượng lịch trình bật
        updateScheduleCount();
        // Thiết lập sự kiện cho các nút
        setupButtonListeners();
        // Thay đổi màu nút tự động khi mở ứng dụng
        updateAutoModeButtonState();


        buttonSchedule = findViewById(R.id.buttonSchedule);
        // Xử lý sự kiện click
        buttonSchedule.setOnClickListener(v -> {
            // Chuyển hướng đến ScheduleActivity
            Intent intent = new Intent(MainActivity.this, ScheduleListActivity.class);
            startActivity(intent);
        });
    }

    private void initializeUIComponents() {
        textViewHumidity = findViewById(R.id.textViewHumidity);
        textViewSoilMoisture = findViewById(R.id.textViewSoilMoisture);
        textViewTemperature = findViewById(R.id.textViewTemperature);
        buttonRelay1 = findViewById(R.id.buttonRelay1);
        buttonRelay2 = findViewById(R.id.buttonRelay2);
        buttonAutoMode = findViewById(R.id.buttonAutoMode);
        buttonHeatLamp = findViewById(R.id.buttonHeatLamp);  // Khởi tạo nút đèn sưởi
        textViewScheduleCount = findViewById(R.id.textViewScheduleCount);

    }

    private void setupButtonListeners() {
        buttonRelay1.setOnClickListener(v -> toggleRelayState("Relay1", buttonRelay1));
        buttonRelay2.setOnClickListener(v -> toggleRelayState("Relay2", buttonRelay2));
        buttonAutoMode.setOnClickListener(v -> toggleAutoMode());
        buttonHeatLamp.setOnClickListener(v -> toggleRelayState("Relay3", buttonHeatLamp));  // Thêm sự kiện cho nút đèn sưởi
    }

    private void updateAutoModeButtonState() {
        if (autoMode) {
            // Chế độ tự động đang bật, cập nhật nút
            buttonAutoMode.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
            buttonAutoMode.setText("Tắt chế độ tự động");
        } else {
            // Chế độ tự động không bật, cập nhật nút
            buttonAutoMode.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            buttonAutoMode.setText("Bật chế độ tự động");
        }
    }

    private void toggleAutoMode() {
        if (autoMode) {
            autoMode = false;
            buttonAutoMode.setText("Bật chế độ tự động");
            // Thay đổi màu sắc của nút khi tắt chế độ tự động
            buttonAutoMode.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            setRelayState("Relay2", 0); // Tắt Relay2 khi chế độ tự động tắt
            setRelayState("Relay3", 0); // Tắt Relay2 khi chế độ tự động tắt
            Toast.makeText(this, "Chế độ tự động đã tắt", Toast.LENGTH_SHORT).show();
        } else {
            showAutoModeDialog();
        }
    }

    // Biến toàn cục để lưu độ ẩm và nhiệt độ mong muốn
    private int targetMoisture = -1;  // -1 chỉ ra rằng chưa nhập độ ẩm
    private int targetTemperature = -1;  // -1 chỉ ra rằng chưa nhập nhiệt độ


    private void showAutoModeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chế độ tự động");

        // Tạo EditText để nhập độ ẩm mong muốn
        final EditText moistureInput = new EditText(this);
        moistureInput.setHint("Nhập độ ẩm mong muốn (%)");

        // Tạo EditText để nhập nhiệt độ mong muốn
        final EditText temperatureInput = new EditText(this);
        temperatureInput.setHint("Nhập nhiệt độ mong muốn (°C)");

        // Thiết lập bố cục cho các EditText
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(moistureInput);
        layout.addView(temperatureInput);
        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String targetMoistureString = moistureInput.getText().toString();
            String targetTemperatureString = temperatureInput.getText().toString();

            // Kiểm tra nếu có giá trị độ ẩm được nhập, nếu không thì gán về -1
            if (!targetMoistureString.isEmpty()) {
                targetMoisture = Integer.parseInt(targetMoistureString);  // Lưu giá trị độ ẩm mong muốn vào biến toàn cục
            } else {
                targetMoisture = -1;  // Nếu không nhập, gán về -1
            }

            // Kiểm tra nếu có giá trị nhiệt độ được nhập, nếu không thì gán về -1
            if (!targetTemperatureString.isEmpty()) {
                targetTemperature = Integer.parseInt(targetTemperatureString);  // Lưu giá trị nhiệt độ mong muốn vào biến toàn cục
            } else {
                targetTemperature = -1;  // Nếu không nhập, gán về -1
            }

            // In giá trị độ ẩm và nhiệt độ nhập vào ra Log
            Log.d("AutoMode", "Độ ẩm nhập vào: " + targetMoisture);
            Log.d("AutoMode", "Nhiệt độ nhập vào: " + targetTemperature);

            autoMode = true;

            // Thay đổi màu nền và văn bản của nút khi chế độ tự động được bật
            buttonAutoMode.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
            buttonAutoMode.setText("Tắt chế độ tự động");

            String message = "Chế độ tự động đã bật";

            // Thêm thông tin vào thông báo nếu có độ ẩm và nhiệt độ
            if (targetMoisture != -1) {
                message += " với độ ẩm mong muốn: " + targetMoisture + "%";
            }

            if (targetTemperature != -1) {
                message += " và nhiệt độ mong muốn: " + targetTemperature + "°C";
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.setNeutralButton("Tắt tự động", (dialog, which) -> {
            // Đặt lại giá trị của targetMoisture và targetTemperature về -1 khi tắt chế độ tự động
            targetMoisture = -1;
            targetTemperature = -1;
            autoMode = false;

            // In ra giá trị khi tắt tự động
            Log.d("AutoMode", "Đã tắt chế độ tự động. Giá trị độ ẩm: " + targetMoisture + ", Nhiệt độ: " + targetTemperature);

            // Tắt cả Relay2 và Relay3 khi tắt chế độ tự động
            setRelayState("Relay2", 0);  // Tắt Relay2
            setRelayState("Relay3", 0);  // Tắt Relay3

            // Thay đổi giao diện khi tắt chế độ tự động
            buttonAutoMode.setBackgroundColor(ContextCompat.getColor(this, R.color.red)); // Màu đỏ khi tắt chế độ tự động
            buttonAutoMode.setText("Bật chế độ tự động");

            // Hiển thị thông báo về việc tắt chế độ tự động
            Toast.makeText(this, "Chế độ tự động đã tắt", Toast.LENGTH_SHORT).show();
        });

        builder.show();
    }

    private void checkAutoMode() {
        // Kiểm tra nếu chế độ tự động đang bật
        if (autoMode) {
            // Trường hợp cả độ ẩm và nhiệt độ đều có giá trị
            if (targetMoisture != -1 && targetTemperature != -1) {
                // Kiểm tra độ ẩm
                databaseReference.child("DoAmDat").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Long currentMoisture = task.getResult().getValue(Long.class);
                        if (currentMoisture != null) {
                            // Kiểm tra nếu độ ẩm đất nhỏ hơn độ ẩm mong muốn
                            if (currentMoisture < targetMoisture) {
                                setRelayState("Relay2", 1); // Bật máy bơm
                            } else {
                                setRelayState("Relay2", 0); // Tắt máy bơm
                            }
                        }
                    }
                });

                // Kiểm tra nhiệt độ
                databaseReference.child("NhietDo").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Long currentTemperature = task.getResult().getValue(Long.class);
                        if (currentTemperature != null) {
                            // Kiểm tra nếu nhiệt độ thấp hơn nhiệt độ mong muốn
                            if (currentTemperature < targetTemperature) {
                                setRelayState("Relay3", 1); // Bật Relay3
                            } else {
                                setRelayState("Relay3", 0); // Tắt Relay3
                            }
                        }
                    }
                });

            } else {
                // Trường hợp chỉ có độ ẩm hoặc nhiệt độ
                if (targetMoisture != -1) {
                    // Kiểm tra độ ẩm
                    databaseReference.child("DoAmDat").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Long currentMoisture = task.getResult().getValue(Long.class);
                            if (currentMoisture != null) {
                                if (currentMoisture < targetMoisture) {
                                    setRelayState("Relay2", 1); // Bật máy bơm
                                } else {
                                    setRelayState("Relay2", 0); // Tắt máy bơm
                                }
                            }
                        }
                    });
                } else {
                    setRelayState("Relay2", 0); // Tắt Relay2 nếu không có giá trị độ ẩm
                }

                if (targetTemperature != -1) {
                    // Kiểm tra nhiệt độ
                    databaseReference.child("NhietDo").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Long currentTemperature = task.getResult().getValue(Long.class);
                            if (currentTemperature != null) {
                                if (currentTemperature < targetTemperature) {
                                    setRelayState("Relay3", 1); // Bật Relay3
                                } else {
                                    setRelayState("Relay3", 0); // Tắt Relay3
                                }
                            }
                        }
                    });
                } else {
                    setRelayState("Relay3", 0); // Tắt Relay3 nếu không có giá trị nhiệt độ
                }
            }
        } else {
            // Tắt tất cả Relay nếu chế độ tự động không bật
            setRelayState("Relay2", 0); // Tắt Relay2
            setRelayState("Relay3", 0); // Tắt Relay3
        }
    }


    private void startUpdatingData() {
        updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                loadDataFromFirebase();  // Cập nhật các giá trị từ Firebase (nhiệt độ, độ ẩm, v.v.)

                // Kiểm tra chế độ tự động và cập nhật trạng thái relay liên tục
                if (autoMode) {
                    checkAutoMode(); // Kiểm tra chế độ tự động và bật/tắt các relay theo độ ẩm và nhiệt độ
                }

                // Lặp lại mỗi 1 giây
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateDataRunnable); // Bắt đầu cập nhật liên tục
    }

    private void loadDataFromFirebase() {
        // Đọc độ ẩm đất
        databaseReference.child("DoAmDat").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Long soilMoistureValue = task.getResult().getValue(Long.class);
                textViewSoilMoisture.setText("Độ ẩm đất: " + (soilMoistureValue != null ? soilMoistureValue : 0) + "%");

                // Gửi thông báo nếu độ ẩm đất dưới 10% và chưa gửi trong vòng 60 giây
                if (soilMoistureValue != null && soilMoistureValue < 10) {
                    if (!isNotificationSent) {
                        sendLowMoistureNotification();
                        isNotificationSent = true; // Đánh dấu là đã gửi thông báo

                        // Đặt lại cờ isNotificationSent sau 60 giây để có thể gửi thông báo mới
                        handler.postDelayed(() -> isNotificationSent = false, 60000);
                    }
                } else {
                    isNotificationSent = false; // Đặt lại trạng thái nếu độ ẩm đất > 10%
                }
            }
        });

        // Cập nhật dữ liệu khác như nhiệt độ, độ ẩm không khí, relay, v.v.
        databaseReference.child("DoAm").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Long humidityValue = task.getResult().getValue(Long.class);
                textViewHumidity.setText("Độ ẩm: " + (humidityValue != null ? humidityValue : 0) + "%");
            }
        });

        databaseReference.child("NhietDo").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Long temperatureValue = task.getResult().getValue(Long.class);
                textViewTemperature.setText("Nhiệt độ: " + (temperatureValue != null ? temperatureValue : 0) + "°C");
            }
        });

        databaseReference.child("Relay1").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayState1 = task.getResult().getValue(Integer.class);
                if (relayState1 != null) {
                    updateButtonState(buttonRelay1, relayState1);
                }
            }
        });

        databaseReference.child("Relay2").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayState2 = task.getResult().getValue(Integer.class);
                if (relayState2 != null) {
                    updateButtonState(buttonRelay2, relayState2);
                }
            }
        });

        // Đọc trạng thái Relay3 (Đèn sưởi)
        databaseReference.child("Relay3").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayState3 = task.getResult().getValue(Integer.class);
                if (relayState3 != null) {
                    updateButtonState(buttonHeatLamp, relayState3);
                }
            }
        });
    }

    private void loadRelayState(String relay, Button button) {
        databaseReference.child(relay).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayState = task.getResult().getValue(Integer.class);
                if (relayState != null) {
                    updateButtonState(button, relayState);
                }
            }
        });
    }

    private void toggleRelayState(String relay, Button button) {
        databaseReference.child(relay).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer currentState = task.getResult().getValue(Integer.class);
                if (currentState != null) {
                    int newState = (currentState == 0) ? 1 : 0;
                    setRelayState(relay, newState);
                    updateButtonState(button, newState);

                    // Thêm thông báo khi bật/tắt relay
                    String message = relay.equals("Relay1") ? (newState == 1 ? "Đèn đã được bật" : "Đèn đã được tắt") :
                            (relay.equals("Relay2") ? (newState == 1 ? "Máy bơm đã được bật" : "Máy bơm đã được tắt") :
                                    (newState == 1 ? "Đèn sưởi đã được bật" : "Đèn sưởi đã được tắt"));
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setRelayState(String relay, int state) {
        databaseReference.child(relay).setValue(state);
    }

    private void updateButtonState(Button button, int state) {
        if (button == buttonRelay1) {
            if (state == 1) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                button.setText("Tắt đèn");  // Văn bản cho nút Relay1
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                button.setText("Bật đèn");  // Văn bản cho nút Relay1
            }
        } else if (button == buttonRelay2) {
            if (state == 1) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                button.setText("Tắt máy bơm");  // Văn bản cho nút Relay2
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                button.setText("Bật máy bơm");  // Văn bản cho nút Relay2
            }
        } else if (button == buttonHeatLamp) {
            if (state == 1) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                button.setText("Tắt đèn sưởi");  // Văn bản cho nút Relay3 (Đèn sưởi)
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                button.setText("Bật đèn sưởi");  // Văn bản cho nút Relay3 (Đèn sưởi)
            }
        } else if (button == buttonAutoMode) {
            if (state == 1) {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                button.setText("Tắt chế độ tự động");  // Văn bản cho nút chế độ tự động
            } else {
                button.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
                button.setText("Bật chế độ tự động");  // Văn bản cho nút chế độ tự động
            }
        }
    }

    private void sendLowMoistureNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Cảnh báo độ ẩm đất")
                .setContentText("Độ ẩm đất quá thấp! Cần bổ sung nước cho cây!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "TuoicayNotifications";
            String description = "Thông báo liên quan đến hệ thống tưới cây";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateScheduleCount() {
        // Giả sử bạn có một bảng "Schedules" trong Firebase chứa các lịch trình
        databaseReference.child("schedule").orderByChild("status").equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int scheduleCount = (int) dataSnapshot.getChildrenCount();
                        textViewScheduleCount.setText("Số lượng lịch trình đang bật: " + scheduleCount);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Xử lý khi có lỗi
                    }
                });
    }
}
