package com.example.secundariaaapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.mlkit.vision.barcode.common.Barcode;
import androidx.camera.core.ExperimentalGetImage;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;



@ExperimentalGetImage
public class ScannerCamaraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;
    private PreviewView previewView;
    private TextView tvResult;

    private List<Estudiante> listaEstudiantes = new ArrayList<>();

    // para evitar lecturas repetidas inmediatas
    private String lastScannedId = "";
    private long lastScanTime = 0L;
    private static final long SCAN_COOLDOWN_MS = 2000; // 2s

    private MediaPlayer soundError;
    private MediaPlayer soundSuccess;

    private final Handler handler = new Handler();
    private Runnable resetTextRunnable;

    private boolean yaRegistrado;

    public boolean isYaRegistrado() {
        return yaRegistrado;
    }

    public void setYaRegistrado(boolean yaRegistrado) {
        this.yaRegistrado = yaRegistrado;
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_camara);
        soundError = MediaPlayer.create(this, R.raw.error);
        soundSuccess = MediaPlayer.create(this, R.raw.success);



        previewView = findViewById(R.id.previewView);
        tvResult = findViewById(R.id.tvResult);

        ImageView btnHome = findViewById(R.id.btnHome);
        btnHome.setOnClickListener(v -> finish());



        // pedir permiso si hace falta
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cargarEstudiantesDesdeJson();
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void cargarEstudiantesDesdeJson() {
        try {
            InputStream is = getAssets().open("estudiantes.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Estudiante>>(){}.getType();
            listaEstudiantes = gson.fromJson(json, listType);

            if (listaEstudiantes == null) listaEstudiantes = new ArrayList<>();

        } catch (IOException e) {
            Log.e("MainActivity", "Error leyendo estudiantes.json", e);
            tvResult.setText("No se pudo cargar estudiantes.json");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();

                Executor executor = ContextCompat.getMainExecutor(this);

                imageAnalysis.setAnalyzer(executor, image -> {
                    Image mediaImage = image.getImage();
                    if (mediaImage != null) {
                        InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> processBarcodes(barcodes))
                                .addOnFailureListener(e -> Log.e("MainActivity", "Error escaneando", e))
                                .addOnCompleteListener(task -> image.close());
                    } else {
                        image.close();
                    }
                });

                // Bind
                cameraProvider.unbindAll();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

            } catch (ExecutionException | InterruptedException e) {
                Log.e("MainActivity", "Error al iniciar la cámara", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processBarcodes(List<Barcode> barcodes) {
        if (barcodes == null || barcodes.isEmpty()) return;

        for (Barcode barcode : barcodes) {
            String id = barcode.getRawValue();
            if (id == null) continue;

            long now = System.currentTimeMillis();
            if (id.equals(lastScannedId) && (now - lastScanTime) < SCAN_COOLDOWN_MS) {
                continue; // Ignora lectura repetida en cooldown
            }

            lastScannedId = id;
            lastScanTime = now;

            Estudiante estudiante = buscarEstudiantePorId(id);

            runOnUiThread(() -> {
                if (resetTextRunnable != null) {
                    handler.removeCallbacks(resetTextRunnable);
                }

                if (estudiante == null) {
                    tvResult.setText("❌ No existe el código del alumno.");
                    playSound(soundError, R.raw.error);
                } else {
                    if (estudiante.isYaRegistrado()) {
                        tvResult.setText("⚠️ Asistencia ya registrada");
                        playSound(soundSuccess, R.raw.success);
                    } else {
                        estudiante.setYaRegistrado(true);
                        tvResult.setText("✅ Asistencia registrada: " + estudiante.getNombre());
                        playSound(soundSuccess, R.raw.success);
                    }
                }

                resetTextRunnable = () -> tvResult.setText("Esperando escaneo...");
                handler.postDelayed(resetTextRunnable, 1500);
            });
        }
    }

    private Estudiante buscarEstudiantePorId(String id) {
        for (Estudiante estudiante : listaEstudiantes) {
            if (estudiante.getId().equals(id)) {
                return estudiante;
            }
        }
        return null;
    }


    private void playSound(MediaPlayer sound, int resId) {
        if (sound != null) {
            if (sound.isPlaying()) {
                sound.stop();
                sound.release();
            }
        }
        sound = MediaPlayer.create(this, resId);
        sound.start();
    }


    // manejar respuesta del permiso
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cargarEstudiantesDesdeJson();
                startCamera();
            } else {
                tvResult.setText("Permiso de cámara denegado.");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundSuccess != null) {
            soundSuccess.release();
            soundSuccess = null;
        }
        if (soundError != null) {
            soundError.release();
            soundError = null;
        }
    }

}
