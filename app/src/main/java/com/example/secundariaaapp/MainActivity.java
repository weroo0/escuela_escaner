package com.example.secundariaaapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private Button btnCamara, btnEscaner;
    private TextView tvResultado;

    // Mapa para búsqueda rápida: id -> nombre
    private Map<String, String> estudiantesMap;

    // Flag modo escáner externo
    private boolean modoEscanerActivo = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnCamara = findViewById(R.id.btnCamara);
        btnEscaner = findViewById(R.id.btnEscaner);


        estudiantesMap = new HashMap<>();
        cargarEstudiantesDesdeJson();



        btnCamara.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerCamaraActivity.class);
            startActivity(intent);
        });

        btnEscaner.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerExternoActivity.class);
            startActivity(intent);
        });
    }

    private void cargarEstudiantesDesdeJson() {
        // TODO: reemplaza con carga real desde JSON
        estudiantesMap.put("12345", "Juan Perez");
        estudiantesMap.put("54321", "Maria Lopez");
        estudiantesMap.put("56789", "Carlos Sánchez");
        // Agrega todos tus estudiantes aquí
    }

}

