package vn.hcmute.videoshorts;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<VideoItem> list;
    private final Context context;

    public VideoAdapter(List<VideoItem> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = list.get(position);

        // Email người đăng
        holder.txtEmail.setText(item.getEmail());

        // Mô tả video ngắn gọn, không show full URL cho đỡ rối
        holder.txtVideoInfo.setText("Cloudinary short video");

        // Like / Dislike
        holder.btnLike.setText("❤️ " + item.getLikes());
        holder.btnDislike.setText("👎 " + item.getDislikes());

        // Play video bằng app video / browser
        holder.btnPlay.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getVideoUrl()));
            context.startActivity(intent);
        });

        // Tăng like
        holder.btnLike.setOnClickListener(v ->
                updateCounter(item, "likes", holder));

        // Tăng dislike
        holder.btnDislike.setOnClickListener(v ->
                updateCounter(item, "dislikes", holder));
    }

    private void updateCounter(VideoItem item, String field, VideoViewHolder holder) {
        FirebaseFirestore.getInstance()
                .collection("videos")
                .document(item.getId())
                .update(field, FieldValue.increment(1))
                .addOnSuccessListener(unused -> {
                    if ("likes".equals(field)) {
                        item.setLikes(item.getLikes() + 1);
                        holder.btnLike.setText("❤️ " + item.getLikes());
                    } else {
                        item.setDislikes(item.getDislikes() + 1);
                        holder.btnDislike.setText("👎 " + item.getDislikes());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context,
                                "Lỗi cập nhật: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        TextView txtEmail, txtVideoInfo;
        Button btnPlay, btnLike, btnDislike;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEmail = itemView.findViewById(R.id.txtEmail);
            txtVideoInfo = itemView.findViewById(R.id.txtVideoUrl); // id trong item_video.xml
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnDislike = itemView.findViewById(R.id.btnDislike);
        }
    }
}
