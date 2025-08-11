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

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText inputBusqueda;
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

        inputBusqueda = findViewById(R.id.inputBusqueda);
        btnCamara = findViewById(R.id.btnCamara);
        btnEscaner = findViewById(R.id.btnEscaner);
        tvResultado = findViewById(R.id.tvResultado);

        estudiantesMap = new HashMap<>();
        cargarEstudiantesDesdeJson();

        // Buscar cuando el usuario presiona Enter en el input
        inputBusqueda.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                buscarEstudiante(inputBusqueda.getText().toString().trim());
                return true;
            }
            return false;
        });

        btnCamara.setOnClickListener(v -> {
            modoEscanerActivo = false;  // desactivar modo escáner externo
            Intent intent = new Intent(this, ScannerCamaraActivity.class);
            startActivity(intent);
        });

        btnEscaner.setOnClickListener(v -> {
            modoEscanerActivo = true;
            tvResultado.setText("Modo escáner externo activado, esperando datos...");
            activarModoEscanerExterno();
        });
    }

    private void cargarEstudiantesDesdeJson() {
        // TODO: reemplaza con carga real desde JSON
        estudiantesMap.put("12345", "Juan Perez");
        estudiantesMap.put("54321", "Maria Lopez");
        estudiantesMap.put("56789", "Carlos Sánchez");
        // Agrega todos tus estudiantes aquí
    }

    private void buscarEstudiante(String texto) {
        if (texto.isEmpty()) {
            tvResultado.setText("Por favor ingresa un ID o nombre para buscar.");
            return;
        }

        // Busca por ID o nombre (ignorando mayúsculas)
        String nombreEncontrado = null;
        String idEncontrado = null;

        for (Map.Entry<String, String> entry : estudiantesMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(texto) || entry.getValue().equalsIgnoreCase(texto)) {
                nombreEncontrado = entry.getValue();
                idEncontrado = entry.getKey();
                break;
            }
        }

        if (nombreEncontrado != null) {
            tvResultado.setText("Estudiante encontrado: " + nombreEncontrado + " (ID: " + idEncontrado + ")");
        } else {
            tvResultado.setText("No se encontró estudiante con ese ID o nombre.");
        }
    }

    private void activarModoEscanerExterno() {
        // Simulación de recepción de datos desde escáner externo después de 5 segundos
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (modoEscanerActivo) {
                String idEscaneado = "12345";  // Cambia este valor para simular diferentes IDs
                procesarEscaneoExterno(idEscaneado);
            }
        }, 5000);
    }

    private void procesarEscaneoExterno(String idEscaneado) {
        if (!modoEscanerActivo) return;

        String nombre = estudiantesMap.get(idEscaneado);
        if (nombre != null) {
            tvResultado.setText("Bienvenido: " + nombre + "\nID: " + idEscaneado);
        } else {
            tvResultado.setText("ID (matrícula) " + idEscaneado + " no se encontró en los registros.");
        }

        // Puedes desactivar el modo escáner si quieres que solo escanee una vez:
        // modoEscanerActivo = false;
    }
}

