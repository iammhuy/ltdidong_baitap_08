package vn.hcmute.videoshorts;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends AppCompatActivity {

    private RecyclerView recyclerVideos;
    private VideoAdapter adapter;
    private final List<VideoItem> videoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        recyclerVideos = findViewById(R.id.recyclerVideos);
        recyclerVideos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VideoAdapter(videoList, this);
        recyclerVideos.setAdapter(adapter);

        loadVideos();
    }

    private void loadVideos() {
        FirebaseFirestore.getInstance()
                .collection("videos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    videoList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        VideoItem item = doc.toObject(VideoItem.class);
                        item.setId(doc.getId());
                        videoList.add(item);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải danh sách: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
