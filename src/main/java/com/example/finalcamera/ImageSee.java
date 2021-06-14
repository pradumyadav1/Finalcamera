package com.example.finalcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jetbrains.annotations.NotNull;

import java.io.FileDescriptor;
import java.io.IOException;

public class ImageSee extends AppCompatActivity {
  private ImageView imageView1;
  private  TextView textView1;
  private ImageView imageView2;
  private  TextView textView2;
  private ImageView imageView3;
  private  TextView textView3;
  private ImageView imageView4;
  private  TextView textView4;
  private ProgressDialog progressDialog;
 // private String string="initial";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
       // requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
       // getSupportActionBar().hide(); // hide the title bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_see);
        imageView1 = findViewById(R.id.image1);
        textView1 = findViewById(R.id.text1);
        imageView2 = findViewById(R.id.image2);
        textView2 = findViewById(R.id.text2);
        imageView3 = findViewById(R.id.image3);
        textView3 = findViewById(R.id.text3);
        imageView4 = findViewById(R.id.image4);
        textView4 = findViewById(R.id.text4);

        progressDialog=new ProgressDialog(this);

              getImgref();


    }


private void getImgref(){

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("message");
    myRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
           String string =snapshot.getValue(String.class);
            Toast.makeText(ImageSee.this, "scusses", Toast.LENGTH_SHORT).show();

            for(int i=0;i<4;i++){
                getImage(string,i);
            }

        }

        @Override
        public void onCancelled(@NonNull @NotNull DatabaseError error) {
            Toast.makeText(ImageSee.this, "not get information", Toast.LENGTH_SHORT).show();

        }

    });

}
private void getImage(String string,int i){
    progressDialog.setMessage("Downloading please wait....");
    progressDialog.show();
    //conversion
        int dec=Integer.parseInt(string);
        dec=dec-i;
        String fstring=String.valueOf(dec);
     //Firebase
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef = storage.getReferenceFromUrl("gs://finalcamera-31ffc.appspot.com")
            .child(fstring+".jpg");


    storageRef.getBytes(1024*1024*1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
        @Override
        public void onSuccess(byte[] bytes) {
            Toast.makeText(ImageSee.this, "Scusses", Toast.LENGTH_SHORT).show();
            Bitmap bitma=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
            //imageView.setImageBitmap(bitma);
            if(i==0){
                textView1.setText(fstring);
                imageView1.setImageBitmap(bitma);
            }
            if(i==1){
                textView2.setText(fstring);
                imageView2.setImageBitmap(bitma);
            }
            if(i==2){
                textView3.setText(fstring);
                imageView3.setImageBitmap(bitma);
            }
            if(i==3){
                textView4.setText(fstring);
                imageView4.setImageBitmap(bitma);
            }
        }
    }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull @NotNull Exception e) {
            Toast.makeText(ImageSee.this, "Error image", Toast.LENGTH_SHORT).show();
        }
    }).addOnCompleteListener(new OnCompleteListener<byte[]>() {
        @Override
        public void onComplete(@NonNull @NotNull Task<byte[]> task) {

            progressDialog.dismiss();
        }
    });




}

    }