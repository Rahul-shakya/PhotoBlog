package com.example.asus.photoblog;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NewPostActivity extends AppCompatActivity {

    private Toolbar newPostToolbar;

    private ImageView newPostImage;
    private EditText newPostDesc;
    private Button newPostBtn;

    private Uri postImageUri=null;

    private StorageReference storageReference;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;

    private String current_user_id;
    private String downloadUri;
    private StorageReference filepath;
    Map<String,Object> map=new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        storageReference=FirebaseStorage.getInstance().getReference();
        firebaseFirestore=FirebaseFirestore.getInstance();
        firebaseAuth=FirebaseAuth.getInstance();

        current_user_id=firebaseAuth.getCurrentUser().getUid();

        newPostToolbar=findViewById(R.id.new_post_toolbar);
        setSupportActionBar(newPostToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  //for enabling back button on the toolbar

        newPostImage=findViewById(R.id.new_post_image);
        newPostDesc=findViewById(R.id.new_post_desc);
        newPostBtn=findViewById(R.id.post_btn);

        newPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setMinCropResultSize(512,512)
                        .setAspectRatio(2,1)
                        .start(NewPostActivity.this);

            }
        });

        newPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final String desc=newPostDesc.getText().toString();

                if(!TextUtils.isEmpty(desc) && postImageUri != null){

                    String randomName=UUID.randomUUID().toString();

                    filepath=storageReference.child("post_images").child(randomName + ".jpg");

                    filepath.putFile(postImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {



                            if(task.isSuccessful()){

                                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        downloadUri=uri.toString();
                                        map.put("image_url",downloadUri);
                                        map.put("desc",desc);
                                        map.put("user_id",current_user_id);
                                        map.put("timestamp",FieldValue.serverTimestamp());

                                        firebaseFirestore.collection("Posts").add(map).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {

                                                if(task.isSuccessful()){

                                                    Toast.makeText(NewPostActivity.this,"Post was added",Toast.LENGTH_SHORT).show();
                                                    Intent mainIntent= new Intent(NewPostActivity.this,MainActivity.class);
                                                    startActivity(mainIntent);
                                                    finish();

                                                }else{

                                                }

                                            }
                                        });
                                    }
                                });


                            }else {

                            }
                        }
                    });

                }


            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                postImageUri=result.getUri();
                newPostImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                newPostImage.setImageURI(postImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }


    }


}
