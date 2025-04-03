package com.app.tuoicay;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class ScheduleListActivity extends AppCompatActivity {

    private RecyclerView recyclerViewSchedules;
    private List<Schedule> scheduleList;
    private ScheduleAdapter scheduleAdapter;

    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.schedule_list_activity);

        // Khởi tạo RecyclerView và adapter
        recyclerViewSchedules = findViewById(R.id.recyclerViewSchedules);
        recyclerViewSchedules.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo danh sách lịch trình và adapter
        scheduleList = new ArrayList<>();
        scheduleAdapter = new ScheduleAdapter(scheduleList);
        recyclerViewSchedules.setAdapter(scheduleAdapter);
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        // Lấy dữ liệu lịch trình từ Firebase
        fetchScheduleDataFromFirebase();

        // Bắt đầu kiểm tra liên tục
        startPeriodicCheck();
        startPeriodicCheckLight();
        // Sự kiện click để chuyển đến ScheduleActivity (thêm lịch trình mới)
        FloatingActionButton fabAddSchedule = findViewById(R.id.fabAddSchedule);
        fabAddSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(ScheduleListActivity.this, ScheduleActivity.class);
            startActivity(intent);
        });
    }

    // Hàm lấy dữ liệu lịch trình từ Firebase
    private void fetchScheduleDataFromFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference scheduleRef = database.getReference("schedule");

        // Lắng nghe sự thay đổi dữ liệu
        scheduleRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                scheduleList.clear(); // Xóa danh sách cũ trước khi thêm mới

                // Duyệt qua tất cả các lịch trình từ Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Schedule schedule = snapshot.getValue(Schedule.class);
                    if (schedule != null) {
                        schedule.timeId = snapshot.getKey(); // Lấy timeId từ Firebase
                        scheduleList.add(schedule); // Thêm lịch trình vào danh sách
                    }
                }

                // Cập nhật RecyclerView sau khi dữ liệu thay đổi
                scheduleAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Xử lý lỗi khi tải dữ liệu từ Firebase
                Toast.makeText(ScheduleListActivity.this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public List<String> customDays; // Danh sách ngày trong tuần (ví dụ: ["Monday", "Wednesday"])
    public String repeatSchedule;  // "Hằng ngày" hoặc "Một lần"

    // Biến toàn cục để kiểm tra trạng thái máy bơm
    private boolean isPumpActivated = false;

    public void checkAndControlPump() {
        // Lấy trạng thái relay1 từ Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference relayRef = database.getReference("Relay2");

        relayRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayStatus = task.getResult().getValue(Integer.class);

                if (relayStatus != null && relayStatus == 1) {
                    // Máy bơm đang bật
                    System.out.println("Relay2 (máy bơm) đã bật, dừng kiểm tra trong 10 phút.");
                    isPumpActivated = true;

                    // Dừng kiểm tra trong 10 phút
                    new Handler().postDelayed(() -> {
                        isPumpActivated = false; // Reset trạng thái sau 10 phút
                        System.out.println("Tiếp tục kiểm tra sau 10 phút.");
                    }, 60000); // 600000 ms = 10 phút
                    return; // Không cần kiểm tra thêm
                }

                // Nếu máy bơm chưa bật, tiếp tục kiểm tra lịch trình
                String currentTimeOnly = getCurrentTimeOnly(); // Lấy giờ phút hiện tại
                String currentDay = getCurrentDay(); // Lấy thứ hiện tại (ví dụ: "Thứ 2")
                String formattedCurrentDay = getFormattedDay(currentDay); // Chuyển đổi thứ (nếu cần)

                Log.i("DEBUG", "Giờ phút hiện tại: " + currentTimeOnly);
                Log.i("DEBUG", "Thứ trong tuần (formattedCurrentDay): " + formattedCurrentDay);
                Log.i("DEBUG", "Thời gian hiện tại: " + getCurrentTime());

                for (Schedule schedule : scheduleList) {
                    if (schedule.status && schedule.pumpStatus) {
                        // Kiểm tra customDays
                        boolean shouldExecute = false;

                        if (schedule.customDays != null && !schedule.customDays.isEmpty()) {
                            // Tách chuỗi customDays thành danh sách các ngày
                            List<String> daysList = Arrays.asList(schedule.customDays.split(",\\s*")); // Phân tách bởi dấu phẩy và khoảng trắng
                            Log.i("DEBUG", "customDays: " + daysList);

                            // Kiểm tra xem formattedCurrentDay có nằm trong danh sách hay không
                            shouldExecute = daysList.stream()
                                    .map(String::trim) // Loại bỏ khoảng trắng
                                    .anyMatch(day -> day.equalsIgnoreCase(formattedCurrentDay));
                        } else if ("Hằng ngày".equals(schedule.repeatSchedule)) {
                            shouldExecute = true; // Lịch trình hằng ngày
                        } else if ("Một lần".equals(schedule.repeatSchedule)) {
                            shouldExecute = true; // Lịch trình một lần
                        }

                        Log.i("DEBUG", "shouldExecute: " + shouldExecute);

                        if (shouldExecute && currentTimeOnly.equals(schedule.timeStart)) {
                            // Thời gian hiện tại trùng với thời gian bắt đầu
                            Toast.makeText(ScheduleListActivity.this, "Đến lịch: " + schedule.timeStart, Toast.LENGTH_SHORT).show();
                            controlRelayInFirebase(2, 1); // Bật máy bơm
                            isPumpActivated = true;

                            // Gửi thông báo đến bảng thông báo ngoài ứng dụng
                            sendNotification("Đến lịch tưới", "Đến lịch tưới lúc " + schedule.timeStart);

                            // Tính thời gian trì hoãn để tắt máy bơm (dựa trên timeEnd)
                            int delayTime = calculateDelayTime(schedule.timeEnd);

                            if (delayTime > 0) {
                                new Handler().postDelayed(() -> {
                                    controlRelayInFirebase(1, 0); // Tắt máy bơm
                                    if ("Một lần".equals(schedule.repeatSchedule)) {
                                        schedule.status = false; // Cập nhật trạng thái lịch trình
                                        updateScheduleStatusInDatabase(schedule); // Cập nhật Firebase
                                    }
                                }, delayTime); // Tắt máy bơm sau thời gian trì hoãn
                            } else {
                                if ("Một lần".equals(schedule.repeatSchedule)) {
                                    schedule.status = false; // Cập nhật trạng thái lịch trình
                                    updateScheduleStatusInDatabase(schedule); // Cập nhật Firebase
                                }
                            }
                            break; // Thoát khỏi vòng lặp khi đã bật máy bơm
                        }
                    }
                }
            } else {
                System.out.println("Không thể lấy trạng thái relay1 từ Firebase: " + task.getException());
            }
        });
    }

    // Hàm gửi thông báo đến bảng thông báo ngoài ứng dụng
    private void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Nếu phiên bản Android lớn hơn Oreo, bạn cần tạo channel thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Default Channel";
            String description = "Channel for regular notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default_channel", name, importance);
            channel.setDescription(description);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Tạo thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.ic_notification) // Thay đổi icon nếu cần
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Hiển thị thông báo
        if (notificationManager != null) {
            notificationManager.notify(0, builder.build());
        }
    }
    // Hàm cập nhật trạng thái lịch trình trong Firebase
    private void updateScheduleStatusInDatabase(Schedule schedule) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference scheduleRef = database.getReference("schedule").child(schedule.timeId); // Giả sử `schedule.id` là khóa chính

        scheduleRef.child("status").setValue(false).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                System.out.println("Cập nhật trạng thái 'status' của lịch trình thành công.");
            } else {
                System.out.println("Lỗi khi cập nhật trạng thái 'status' của lịch trình: " + task.getException());
            }
        });
    }

    // Hàm điều khiển Relay trong Firebase
    private void controlRelayInFirebase(int relayId, int status) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference relayRef = database.getReference("Relay2");

        relayRef.setValue(status)
                .addOnSuccessListener(aVoid -> {
                    if (status == 1) {
                        System.out.println("Relay2 (máy bơm) đã bật.");
                    } else {
                        System.out.println("Relay2 (máy bơm) đã tắt.");
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("Lỗi khi điều khiển Relay1: " + e.getMessage());
                });
    }

    // Hàm lấy thời gian hiện tại đầy đủ (bao gồm thứ, ngày, giờ, phút)
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
        Date date = new Date();
        return sdf.format(date);
    }

    // Hàm chỉ lấy giờ và phút từ thời gian hiện tại
    private String getCurrentTimeOnly() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date();
        return sdf.format(date);
    }

    // Hàm chỉ lấy thứ trong tuần từ thời gian hiện tại
    private String getCurrentDay() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", new Locale("vi", "VN"));
        Date date = new Date();
        return sdf.format(date);
    }

    // Hàm chuyển đổi thứ trong tuần
    private String getFormattedDay(String currentDay) {
        switch (currentDay) {
            case "Thứ Hai": return "Thứ 2";
            case "Thứ Ba": return "Thứ 3";
            case "Thứ Tư": return "Thứ 4";
            case "Thứ Năm": return "Thứ 5";
            case "Thứ Sáu": return "Thứ 6";
            case "Thứ Bảy": return "Thứ 7";
            case "Chủ Nhật": return "Chủ Nhật";
            default: return currentDay; // Nếu không khớp, trả về nguyên bản
        }
    }

    // Hàm tính thời gian trì hoãn
    private int calculateDelayTime(String timeEnd) {
        int delayTimeInSeconds = 0;

        if (timeEnd != null && !timeEnd.isEmpty()) {
            String timeEndWithoutUnit = timeEnd.replace(" giây", "").trim();
            try {
                delayTimeInSeconds = Integer.parseInt(timeEndWithoutUnit);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return delayTimeInSeconds * 1000; // Chuyển đổi sang millisecond
    }

    // Bắt đầu kiểm tra liên tục mỗi phút
    private void startPeriodicCheck() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                checkAndControlPump();
                if (!isPumpActivated) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(runnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(runnable); // Dừng kiểm tra khi activity bị huỷ
        }
    }




    // Biến toàn cục để kiểm tra trạng thái máy bơm
    private boolean isLightActivated = false;
    public void checkAndControlLight() {
        // Lấy trạng thái relay2 từ Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference relayRef = database.getReference("Relay1"); // Relay điều khiển đèn

        relayRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Integer relayStatus = task.getResult().getValue(Integer.class);

                if (relayStatus != null && relayStatus == 1) {
                    // Đèn đang bật
                    System.out.println("Relay1 (đèn) đã bật, dừng kiểm tra trong 10 phút.");
                    isLightActivated = true;

                    // Dừng kiểm tra trong 10 phút
                    new Handler().postDelayed(() -> {
                        isLightActivated = false; // Reset trạng thái sau 10 phút
                        System.out.println("Tiếp tục kiểm tra sau 10 phút.");
                    }, 600000); // 600000 ms = 10 phút
                    return; // Không cần kiểm tra thêm
                }

                // Nếu đèn chưa bật, tiếp tục kiểm tra lịch trình
                String currentTimeOnly = getCurrentTimeOnly(); // Lấy giờ phút hiện tại
                String currentDay = getCurrentDay(); // Lấy thứ hiện tại (ví dụ: "Thứ 2")
                String formattedCurrentDay = getFormattedDay(currentDay); // Chuyển đổi thứ (nếu cần)

                Log.i("DEBUG", "Giờ phút hiện tại: " + currentTimeOnly);
                Log.i("DEBUG", "Thứ trong tuần (formattedCurrentDay): " + formattedCurrentDay);
                Log.i("DEBUG", "Thời gian hiện tại: " + getCurrentTime());

                for (Schedule schedule : scheduleList) {
                    if (schedule.status && schedule.lightStatus) { // Kiểm tra trạng thái của đèn
                        // Kiểm tra customDays
                        boolean shouldExecute = false;

                        if (schedule.customDays != null && !schedule.customDays.isEmpty()) {
                            // Tách chuỗi customDays thành danh sách các ngày
                            List<String> daysList = Arrays.asList(schedule.customDays.split(",\\s*")); // Phân tách bởi dấu phẩy và khoảng trắng
                            Log.i("DEBUG", "customDays: " + daysList);

                            // Kiểm tra xem formattedCurrentDay có nằm trong danh sách hay không
                            shouldExecute = daysList.stream()
                                    .map(String::trim) // Loại bỏ khoảng trắng
                                    .anyMatch(day -> day.equalsIgnoreCase(formattedCurrentDay));
                        } else if ("Hằng ngày".equals(schedule.repeatSchedule)) {
                            shouldExecute = true; // Lịch trình hằng ngày
                        } else if ("Một lần".equals(schedule.repeatSchedule)) {
                            shouldExecute = true; // Lịch trình một lần
                        }

                        Log.i("DEBUG", "shouldExecute: " + shouldExecute);

                        if (shouldExecute && currentTimeOnly.equals(schedule.timeStart)) {
                            // Thời gian hiện tại trùng với thời gian bắt đầu
                            Toast.makeText(ScheduleListActivity.this, "Đến lịch: " + schedule.timeStart, Toast.LENGTH_SHORT).show();
                            controlRelay2InFirebase(1, 1); // Bật đèn
                            isLightActivated = true;

                            // Gửi thông báo đến bảng thông báo ngoài ứng dụng
                            sendNotification("Đến lịch bật đèn", "Đến lịch bật đèn lúc " + schedule.timeStart);

                            // Tính thời gian trì hoãn để tắt đèn (dựa trên timeEnd)
                            int delayTime = calculate2DelayTime(schedule.timeEnd);

                            if (delayTime > 0) {
                                new Handler().postDelayed(() -> {
                                    controlRelay2InFirebase(1, 0); // Tắt đèn
                                    if ("Một lần".equals(schedule.repeatSchedule)) {
                                        schedule.status = false; // Cập nhật trạng thái lịch trình
                                        updateScheduleStatusInDatabase(schedule); // Cập nhật Firebase
                                    }
                                }, delayTime); // Tắt đèn sau thời gian trì hoãn
                            } else {
                                if ("Một lần".equals(schedule.repeatSchedule)) {
                                    schedule.status = false; // Cập nhật trạng thái lịch trình
                                    updateScheduleStatusInDatabase(schedule); // Cập nhật Firebase
                                }
                            }
                            break; // Thoát khỏi vòng lặp khi đã bật đèn
                        }
                    }
                }
            } else {
                System.out.println("Không thể lấy trạng thái relay2 từ Firebase: " + task.getException());
            }
        });
    }


    // Hàm điều khiển Relay trong Firebase (đèn - relay2)
    private void controlRelay2InFirebase(int relayId, int status) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference relayRef = database.getReference("Relay1");

        relayRef.setValue(status)
                .addOnSuccessListener(aVoid -> {
                    if (relayId == 1) {
                        System.out.println("Relay1 (đèn) đã bật.");
                    } else {
                        System.out.println("Relay1 (đèn) đã tắt.");
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("Lỗi khi điều khiển Relay1: " + e.getMessage());
                });
    }

    // Hàm tính thời gian trì hoãn (giờ)
    private int calculate2DelayTime(String timeEnd) {
        int delayTimeInHours = 0;

        if (timeEnd != null && !timeEnd.isEmpty()) {
            String timeEndWithoutUnit = timeEnd.replace(" giờ", "").trim();
            try {
                delayTimeInHours = Integer.parseInt(timeEndWithoutUnit);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return delayTimeInHours * 60 * 60 * 1000; // Chuyển đổi từ giờ sang millisecond
    }
    private void startPeriodicCheckLight() {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                checkAndControlLight();
                if (!isLightActivated) {
                    handler.postDelayed(this, 1000); // Lặp lại kiểm tra mỗi giây nếu đèn chưa bật
                }
            }
        };
        handler.post(runnable);
    }



}
