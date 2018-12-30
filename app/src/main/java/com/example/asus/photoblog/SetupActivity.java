package com.example.asus.photoblog;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private CircleImageView setupImage;
    private Uri mainImageUri=null;

    private EditText setupName;
    private Button  setupBtn;

    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;

    private String user_name;
    private String user_id;

    private Boolean isChanged = false;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        Toolbar setupToolbar = findViewById(R.id.setuoToolbar);
        setSupportActionBar(setupToolbar);
        getSupportActionBar().setTitle("Account Setup");

        firebaseAuth=FirebaseAuth.getInstance();
        storageReference=FirebaseStorage.getInstance().getReference();
        firebaseFirestore=FirebaseFirestore.getInstance();

        setupImage=findViewById(R.id.setup_image);
        setupName=findViewById(R.id.setup_name);
        setupBtn=findViewById(R.id.setup_btn);

        user_id=firebaseAuth.getCurrentUser().getUid();


        //retrieve data from storage and FireStore
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if(task.isSuccessful()){

                    if(task.getResult().exists()){

                       String name=task.getResult().getString("name");
                       String image=task.getResult().getString("image");
                       mainImageUri=Uri.parse(image);

                       setupName.setText(name);
                        RequestOptions placeholderRequest = new RequestOptions();
                        placeholderRequest.placeholder(R.drawable.defaultuser);

                       Glide.with(SetupActivity.this).setDefaultRequestOptions(placeholderRequest).load(image).into(setupImage);

                    }

                }else{

                    String error= task.getException().getMessage();
                    Toast.makeText(SetupActivity.this,"Firestore Retrieve Error :" + error,Toast.LENGTH_LONG).show();

                }

            }
        });




        //upload the data to storage and firestore database
        setupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProgressDialog=new ProgressDialog(SetupActivity.this);

                mProgressDialog.setTitle("Uploading Details.");
                mProgressDialog.setMessage("Please wait...");
                mProgressDialog.setCanceledOnTouchOutside(false);
                mProgressDialog.show();

                user_name=setupName.getText().toString();

                Toast.makeText(SetupActivity.this,"huihui",Toast.LENGTH_SHORT).show();



                if( !(TextUtils.isEmpty(user_name)) && mainImageUri != null){


                    StorageReference image_path= storageReference.child("profile_images").child(user_id + ".jpg");
                    image_path.putFile(mainImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                            if(task.isSuccessful()){


                                storeFirestore();

                            }else {
                                mProgressDialog.dismiss();
                                String error=task.getException().getMessage();
                                Toast.makeText(SetupActivity.this,"Image Upload Error : "+error,Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }

            }
        });


        setupImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

                    if(ContextCompat.checkSelfPermission(SetupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

                        Toast.makeText(SetupActivity.this,"Permission Denied",Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(SetupActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    }else{

                        Toast.makeText(SetupActivity.this,"Permission given already",Toast.LENGTH_SHORT).show();

                        CropImage.activity()
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .setAspectRatio(1,1)
                                .start(SetupActivity.this);
                    }
                }else {

                    CropImage.activity()
                            .setGuidelines(CropImageView.Guidelines.ON)
                            .setAspectRatio(1,1)
                            .start(SetupActivity.this);

                }


            }
        });


    }

    private void storeFirestore() {

        Toast.makeText(SetupActivity.this,"Image Uploaded",Toast.LENGTH_SHORT).show();

        storageReference.child("profile_images").child(user_id + ".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {

                String download_url=uri.toString();
                Map<String,String> userMap=new HashMap<>();
                userMap.put("name",user_name);
                userMap.put("image",download_url);


                firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if(task.isSuccessful()){

                            mProgressDialog.dismiss();
                            Intent mainIntent = new Intent(SetupActivity.this,MainActivity.class);
                            startActivity(mainIntent);
                            finish();


                        }else{
                            mProgressDialog.dismiss();
                            String error= task.getException().getMessage();
                            Toast.makeText(SetupActivity.this,"Firestore Error :" + error,Toast.LENGTH_LONG).show();

                        }


                    }
                });

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                mainImageUri = result.getUri();

                setupImage.setImageURI(mainImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }


    }
}
