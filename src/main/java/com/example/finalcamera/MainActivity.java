package com.example.finalcamera;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final String[] CAMERA_PERMISSION = new String[]{Manifest.permission.CAMERA,Manifest.permission.INTERNET};
    private static final int CAMERA_REQUEST_CODE = 10;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Button button;
    private ImageCapture imageCapture ;
    private int i;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE); //will hide the title
        getSupportActionBar().hide(); // hide the title bar
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);
        button=findViewById(R.id.button);
       progressDialog=new ProgressDialog(this);

        if (hasCameraPermission()) {
        } else {
            requestPermission();
        }

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        // runable inside
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    //prvew and anylize image
                    bindImageAnalysis(cameraProvider);

                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

    }

    // camera permission

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE
        );
    }


// preview,anylize,capture implementation

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
// camera function start
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });

        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                //to see orientation
                //  textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.enable();

        //preview
        Preview preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(previewView.createSurfaceProvider());

        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetRotation(Surface.ROTATION_0)
                .setFlashMode(ImageCapture.FLASH_MODE_ON)
                .build();
        //   Toast.makeText(this, "All right", Toast.LENGTH_SHORT).show();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture,
                imageAnalysis, preview);
//camera function end

 //to take picture and up load
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                        Toast.makeText(MainActivity.this, "first pass", Toast.LENGTH_LONG).show();
                        imageCapture.takePicture(ContextCompat.getMainExecutor(MainActivity.this),
                                new  ImageCapture.OnImageCapturedCallback() {
                                    @RequiresApi(api = Build.VERSION_CODES.N)
                                    @SuppressLint("UnsafeExperimentalUsageError")
                                    @Override
                                    public void onCaptureSuccess(@NonNull @NotNull ImageProxy image) {
                                        //   super.onCaptureSuccess(image);

                                        try{
                                            Toast.makeText(MainActivity.this, "Image captured", Toast.LENGTH_SHORT).show();


                                            upLoad(imageProxyToBitmap(image));



                                        }
                                        catch (Exception e){
                                            Toast.makeText(MainActivity.this, "Image not showing", Toast.LENGTH_SHORT).show();
                                        }
                                        image.close();
                                    }

                                    @Override
                                    public void onError(@NonNull @NotNull ImageCaptureException exception) {
                                        super.onError(exception);
                                        Toast.makeText(MainActivity.this, "Image not clicked", Toast.LENGTH_LONG).show();
                                    }
                                 });

            }
        });
    }


    // ImageProxy to Bitmap
    private Bitmap imageProxyToBitmap(ImageProxy image)
    {
        ImageProxy.PlaneProxy planeProxy = image.getPlanes()[0];
        ByteBuffer buffer = planeProxy.getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // change activity see images
    public void onclick(View view){

        //Toast.makeText(this, "Image show", Toast.LENGTH_SHORT).show();
        Intent intent=new Intent(MainActivity.this,ImageSee.class);
        startActivity(intent);
    }

    //image upload
   @RequiresApi(api = Build.VERSION_CODES.N)
   private void upLoad(Bitmap bitmap){

        //bitmap to bytes

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
       byte[] data = baos.toByteArray();
//bitmap to bytes end

     /* SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
       String format = s.format(new Date());
      String str="first"+format;

      */
     getImgref();
      if(i==0){
         Toast.makeText(this, "Fail to get refrence "+i, Toast.LENGTH_SHORT).show();
      }
      else {
          progressDialog.setMessage("Uploading plz wait.....");
          progressDialog.show();
          String str = String.valueOf(i);
          FirebaseStorage storage = FirebaseStorage.getInstance();
          StorageReference storageRef = storage.getReferenceFromUrl("gs://finalcamera-31ffc.appspot.com");

          StorageReference imagesRef = storageRef.child(str + ".jpg");

          UploadTask uploadTask = imagesRef.putBytes(data);
          // String finalStr = str;
          uploadTask.addOnFailureListener(new OnFailureListener() {
              @Override
              public void onFailure(@NonNull Exception exception) {
                  // Handle unsuccessful uploads
                  Toast.makeText(MainActivity.this, "not up loaded", Toast.LENGTH_SHORT).show();
              }
          }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
              @Override
              public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                  Toast.makeText(MainActivity.this, "uploaded", Toast.LENGTH_SHORT).show();
                  uploadImgref(str);
              }
          }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
              @Override
              public void onComplete(@NonNull @NotNull Task<UploadTask.TaskSnapshot> task) {
                  progressDialog.dismiss();
              }
          });
      }
   }
    //upload image ref
   private  void uploadImgref(String string){
       FirebaseDatabase database = FirebaseDatabase.getInstance();
       DatabaseReference myRef = database.getReference("message");

       myRef.setValue(string);
   }
   //get image ref
   void getImgref(){
       FirebaseDatabase database = FirebaseDatabase.getInstance();
       DatabaseReference myRef = database.getReference("message");
       myRef.addValueEventListener(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
               String string =snapshot.getValue(String.class);

               //textView.setText(string);
               i=Integer.parseInt(string);
               ++i;
               Toast.makeText(MainActivity.this, "Scusses to get image refrence"+i, Toast.LENGTH_SHORT).show();
             //  getImage(string);
           }

           @Override
           public void onCancelled(@NonNull @NotNull DatabaseError error) {
               Toast.makeText(MainActivity.this, "Error to get image refrence", Toast.LENGTH_SHORT).show();
              i=0;
           }

       });

   }
}