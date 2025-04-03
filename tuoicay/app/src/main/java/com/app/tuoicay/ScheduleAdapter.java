package com.app.tuoicay;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    private List<Schedule> scheduleList;

    // Constructor nhận danh sách lịch trình
    public ScheduleAdapter(List<Schedule> scheduleList) {
        this.scheduleList = scheduleList;
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_schedule vào RecyclerView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        Schedule schedule = scheduleList.get(position);

        // Thiết lập trạng thái của Switch và các giá trị hiển thị
        holder.switchDevice.setChecked(schedule.status);
        holder.textViewDeviceName.setText(schedule.deviceName);
        holder.textViewTimeStartValue.setText(schedule.timeStart);
        holder.textViewTimeEndValue.setText(schedule.timeEnd);
        holder.textViewRepeatScheduleValue.setText(schedule.repeatSchedule);
        holder.textViewCustomDaysValue.setText(schedule.customDays);

        // Gán sự kiện khi người dùng thay đổi trạng thái của Switch
        holder.switchDevice.setOnCheckedChangeListener(null); // Gỡ bỏ listener cũ
        holder.switchDevice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (schedule.status != isChecked) {
                schedule.status = isChecked; // Cập nhật trạng thái trong danh sách cục bộ
                updateScheduleStatus(schedule, holder);
            }
        });

        // Xử lý sự kiện click vào toàn bộ item để chỉnh sửa lịch trình
        holder.itemView.setOnClickListener(v -> {
            // Chuyển đến EditScheduleActivity với đầy đủ thông tin của lịch trình
            Intent intent = new Intent(holder.itemView.getContext(), EditScheduleActivity.class);
            intent.putExtra("timeId", schedule.timeId);
            intent.putExtra("deviceName", schedule.deviceName);
            intent.putExtra("timeStart", schedule.timeStart);
            intent.putExtra("timeEnd", schedule.timeEnd);
            intent.putExtra("pumpStatus", schedule.pumpStatus);
            intent.putExtra("lightStatus", schedule.lightStatus);
            intent.putExtra("repeatSchedule", schedule.repeatSchedule);
            intent.putExtra("customDays", schedule.customDays);
            intent.putExtra("status", schedule.status);

            holder.itemView.getContext().startActivity(intent);
        });

        // Sự kiện long click để hiển thị tùy chọn xóa
        holder.itemView.setOnLongClickListener(v -> {
            // Hiển thị tùy chọn xóa
            showDeleteOptionMenu(holder.itemView.getContext(), schedule, position, holder);
            return true; // Trả về true để sự kiện không tiếp tục lan tỏa
        });
    }

    @Override
    public int getItemCount() {
        return scheduleList.size();
    }

    private void updateScheduleStatus(Schedule schedule, ScheduleViewHolder holder) {
        // Tham chiếu đến Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                .getReference("schedule")
                .child(schedule.timeId);

        // Cập nhật trạng thái trong Firebase
        databaseReference.child("status").setValue(schedule.status)
                .addOnSuccessListener(aVoid -> {
                    // Thông báo khi cập nhật thành công
                    String message = schedule.status ? "Thiết bị đã bật" : "Thiết bị đã tắt";
                    Toast.makeText(holder.itemView.getContext(), message, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Xử lý lỗi và khôi phục trạng thái Switch nếu cập nhật thất bại
                    schedule.status = !schedule.status; // Phục hồi trạng thái cũ
                    holder.switchDevice.setChecked(schedule.status); // Cập nhật lại trạng thái cho Switch
                    Toast.makeText(holder.itemView.getContext(),
                            "Lỗi: Không thể cập nhật trạng thái!",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Hàm hiển thị menu tùy chọn xóa
    private void showDeleteOptionMenu(Context context, Schedule schedule, int position, ScheduleViewHolder holder) {
        // Tạo tùy chọn xóa trong menu
        new AlertDialog.Builder(context)
                .setTitle("Lựa chọn xóa")
                .setMessage("Bạn có muốn xóa lịch trình này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Xóa lịch trình khi xác nhận
                    deleteScheduleFromFirebase(schedule, holder);
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Hàm xóa lịch trình khỏi Firebase và danh sách
    private void deleteScheduleFromFirebase(Schedule schedule, ScheduleViewHolder holder) {
        // Kiểm tra xem timeId có hợp lệ không
        if (schedule.timeId != null && !schedule.timeId.isEmpty()) {
            // Tham chiếu đến Firebase
            DatabaseReference databaseReference = FirebaseDatabase.getInstance()
                    .getReference("schedule")
                    .child(schedule.timeId);

            // Xóa lịch trình khỏi Firebase
            databaseReference.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Xóa item khỏi danh sách cục bộ
                        scheduleList.remove(schedule);  // Thay vì dùng position, chúng ta dùng đối tượng schedule
                        notifyDataSetChanged();  // Cập nhật RecyclerView
                        Toast.makeText(holder.itemView.getContext(), "Đã xóa lịch trình", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Xử lý lỗi khi xóa
                        Toast.makeText(holder.itemView.getContext(), "Lỗi: Không thể xóa lịch trình", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Nếu không có timeId hợp lệ, thông báo lỗi
            Toast.makeText(holder.itemView.getContext(), "Lỗi: timeId không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    // ViewHolder để chứa các thành phần trong một item
    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDeviceName, textViewTimeStartValue, textViewTimeEndValue, textViewRepeatScheduleValue, textViewCustomDaysValue;
        Switch switchDevice;

        public ScheduleViewHolder(View itemView) {
            super(itemView);
            textViewDeviceName = itemView.findViewById(R.id.textViewDeviceName);
            textViewTimeStartValue = itemView.findViewById(R.id.textViewTimeStartValue);
            textViewTimeEndValue = itemView.findViewById(R.id.textViewTimeEndValue);
            textViewRepeatScheduleValue = itemView.findViewById(R.id.textViewRepeatScheduleValue); // Khai báo mới
            textViewCustomDaysValue = itemView.findViewById(R.id.textViewCustomDaysValue);
            switchDevice = itemView.findViewById(R.id.switchDevice);
        }
    }
}
