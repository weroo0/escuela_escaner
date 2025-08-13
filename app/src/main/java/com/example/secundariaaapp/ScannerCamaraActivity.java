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
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_camara);


        previewView = findViewById(R.id.previewView);
        tvResult = findViewById(R.id.tvResult);

        Button btnVolver = findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> finish());


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
                // ignora lectura repetida en el cooldown
                continue;
            }

            lastScannedId = id;
            lastScanTime = now;

            boolean encontrado = false;
            String nombre = null;
            for (Estudiante e : listaEstudiantes) {
                if (id.equals(e.getId())) {
                    encontrado = true;
                    nombre = e.getNombre();
                    break;
                }
            }

            final boolean finalEncontrado = encontrado;
            final String finalNombre = nombre;
            final String finalId = id;

            runOnUiThread(() -> {
                if (finalEncontrado) {
                    tvResult.setText("Bienvenido: " + finalNombre);
                } else {
                    tvResult.setText("ID no encontrado: " + finalId);
                }
            });
        }
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
}
