package com.example.secundariaaapp;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class ScannerExternoActivity extends AppCompatActivity {

    private TextView tvScanStatus;
    private EditText etScanInput;
    private Button btnVolver;

    private Map<String, String> estudiantesMap;
    private boolean modoEscanerActivo = true; // siempre activo en esta pantalla

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_externo);

        tvScanStatus = findViewById(R.id.tvScanStatus);
        etScanInput = findViewById(R.id.etScanInput);
        btnVolver = findViewById(R.id.btnVolver);

        cargarEstudiantesDesdeJson();

        etScanInput.requestFocus();

        etScanInput.setOnEditorActionListener((v, actionId, event) -> {
            if ((actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN))
                    && modoEscanerActivo) {

                String codigoEscaneado = etScanInput.getText().toString().trim();
                procesarEscaneo(codigoEscaneado);
                etScanInput.setText("");
                return true;
            }
            return false;
        });

        btnVolver.setOnClickListener(v -> finish());
    }

    private void cargarEstudiantesDesdeJson() {
        estudiantesMap = new HashMap<>();
        estudiantesMap.put("12345", "Juan Perez");
        estudiantesMap.put("54321", "Maria Lopez");
        estudiantesMap.put("56789", "Carlos Sánchez");
    }

    private void procesarEscaneo(String idEscaneado) {
        String nombre = estudiantesMap.get(idEscaneado);
        if (nombre != null) {
            tvScanStatus.setText("Bienvenido: " + nombre + "\nID: " + idEscaneado);
        } else {
            tvScanStatus.setText("ID (matrícula) " + idEscaneado + " no se encontró en los registros.");
        }
    }
}
