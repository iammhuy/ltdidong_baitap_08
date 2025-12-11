package vn.hcmute.videoshorts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView txtEmail;
    private Button btnChooseVideo, btnLogout, btnVideoList, btnWebview;

    private Uri selectedVideoUri;
    private ActivityResultLauncher<String> pickVideoLauncher;

    // ====== Cloudinary config ======
    private static final String CLOUDINARY_CLOUD_NAME = "diwqjjdoq";
    private static final String CLOUDINARY_UPLOAD_PRESET = "android_unsigned";

    private final OkHttpClient okHttpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtEmail       = findViewById(R.id.txtEmail);
        btnChooseVideo = findViewById(R.id.btnChooseVideo);
        btnLogout      = findViewById(R.id.btnLogout);
        btnVideoList   = findViewById(R.id.btnVideoList);
        btnWebview     = findViewById(R.id.btnWebview);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // chưa login thì đá về LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        txtEmail.setText("Logged in: " + user.getEmail());

        // Chọn video từ máy
        pickVideoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedVideoUri = uri;
                        uploadVideoToCloudinary(uri);   // ⬅ dùng Cloudinary
                    }
                });

        btnChooseVideo.setOnClickListener(v ->
                pickVideoLauncher.launch("video/*"));

        btnVideoList.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, VideoListActivity.class)));

        btnWebview.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, WebViewActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    // ================== UPLOAD LÊN CLOUDINARY ==================

    private void uploadVideoToCloudinary(Uri videoUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = user.getUid();

        try {
            InputStream inputStream = getContentResolver().openInputStream(videoUri);
            if (inputStream == null) {
                Toast.makeText(this, "Không đọc được file video", Toast.LENGTH_SHORT).show();
                return;
            }

            byte[] videoBytes = readAllBytes(inputStream);

            MediaType mediaType = MediaType.parse("video/*");
            RequestBody videoBody = RequestBody.create(videoBytes, mediaType);

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "video.mp4", videoBody)
                    .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                    .build();

            String url = "https://api.cloudinary.com/v1_1/"
                    + CLOUDINARY_CLOUD_NAME + "/video/upload";

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            Toast.makeText(this, "Đang upload lên Cloudinary...", Toast.LENGTH_SHORT).show();

            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Upload lỗi: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String body = response.body() != null ? response.body().string() : "";
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Upload thất bại: " + response.code(),
                                        Toast.LENGTH_LONG).show());
                        return;
                    }

                    String body = response.body() != null ? response.body().string() : "";
                    try {
                        JSONObject json = new JSONObject(body);
                        String secureUrl = json.getString("secure_url");

                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this,
                                    "Upload thành công!",
                                    Toast.LENGTH_SHORT).show();
                            // lưu dữ liệu video vào Firestore
                            saveVideoInfoToFirestore(uid, secureUrl);
                        });
                    } catch (Exception ex) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Parse JSON lỗi: " + ex.getMessage(),
                                        Toast.LENGTH_LONG).show());
                    }
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi đọc file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    // ================== FIRESTORE ==================

    private void saveVideoInfoToFirestore(String uid, String videoUrl) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String email = user != null ? user.getEmail() : "";

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> video = new HashMap<>();
        video.put("userId", uid);
        video.put("email", email);
        video.put("videoUrl", videoUrl);
        video.put("likes", 0);
        video.put("dislikes", 0);
        video.put("createdAt", FieldValue.serverTimestamp());

        db.collection("videos")
                .add(video)
                .addOnSuccessListener(doc ->
                        Toast.makeText(this, "Đã lưu video vào Firestore!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lưu DB lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
