package com.example.secundariaaapp;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScannerExternoActivity extends AppCompatActivity {

    private TextInputLayout tilBusqueda;
    private TextInputEditText etBusqueda;
    private LinearLayout resultadosLayout;
    private EditText etScanInput; // Cambiado a EditText

    private List<Estudiante> listaEstudiantes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner_externo);

        // Inicializar vistas
        tilBusqueda = findViewById(R.id.tilBusqueda);
        etBusqueda = findViewById(R.id.etBusqueda);
        resultadosLayout = findViewById(R.id.resultados_layout);
        etScanInput = findViewById(R.id.etScanInput);  // Ahora EditText

        // Cargar estudiantes desde JSON
        cargarEstudiantesDesdeJson();

        // Configurar botón de búsqueda
        findViewById(R.id.btnBuscar).setOnClickListener(v -> registrarAsistencia());

        // Configurar búsqueda al presionar Enter en el campo de búsqueda
        etBusqueda.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                registrarAsistencia();
                return true;
            }
            return false;
        });

        // Configurar el EditText invisible para capturar input del escáner
        etScanInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {

                String codigoEscaneado = etScanInput.getText().toString().trim();
                if (!codigoEscaneado.isEmpty()) {
                    // Poner el código escaneado en el campo de búsqueda visible
                    etBusqueda.setText(codigoEscaneado);
                    // Realizar el registro automáticamente
                    registrarAsistencia();
                    // Limpiar el campo invisible
                    etScanInput.setText("");
                }
                return true;
            }
            return false;
        });

        // Configurar botón volver
        findViewById(R.id.btnVolver).setOnClickListener(v -> finish());

        // Dar foco al campo de búsqueda visible
        etBusqueda.requestFocus();
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
            // Si no se puede cargar el JSON, usar datos de ejemplo
            listaEstudiantes = new ArrayList<>();
            listaEstudiantes.add(new Estudiante("12345", "Juan Pérez", false));
            listaEstudiantes.add(new Estudiante("67890", "Ana López", false));
            listaEstudiantes.add(new Estudiante("54321", "Carlos Ramírez", false));
        }
    }

    private void registrarAsistencia() {
        String codigoEstudiante = etBusqueda.getText().toString().trim();

        if (codigoEstudiante.isEmpty()) {
            // Mostrar error simple
            tilBusqueda.setError("Por favor ingresa un código");
            return;
        }

        // Limpiar error si existe
        tilBusqueda.setError(null);

        // Limpiar resultados anteriores
        resultadosLayout.removeAllViews();
        resultadosLayout.setVisibility(View.VISIBLE);

        // Buscar estudiante por ID
        Estudiante estudiante = buscarEstudiantePorId(codigoEstudiante);

        if (estudiante == null) {
            // Estudiante no encontrado
            mostrarEstudianteNoEncontrado();
        } else {
            // Estudiante encontrado - registrar asistencia
            if (estudiante.isYaRegistrado()) {
                // Ya se registró anteriormente
                mostrarAsistenciaRegistrada();
            } else {
                // Primer registro
                mostrarPrimerRegistro(estudiante);
                // Marcar como registrado
                estudiante.setYaRegistrado(true);
            }
        }

        // Limpiar campo de búsqueda
        etBusqueda.setText("");
        etBusqueda.requestFocus();
    }

    private Estudiante buscarEstudiantePorId(String id) {
        for (Estudiante estudiante : listaEstudiantes) {
            if (estudiante.getId().equals(id)) {
                return estudiante;
            }
        }
        return null;
    }

    private void mostrarEstudianteNoEncontrado() {
        LinearLayout resultadoItem = new LinearLayout(this);
        resultadoItem.setOrientation(LinearLayout.HORIZONTAL);
        resultadoItem.setPadding(16, 16, 16, 16);
        resultadoItem.setBackgroundResource(R.drawable.item_resultado_background);
        resultadoItem.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView iconoX = new ImageView(this);
        iconoX.setImageResource(R.drawable.ic_x_red);
        iconoX.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        iconoX.setPadding(0, 0, 16, 0);

        TextView tvError = new TextView(this);
        tvError.setText(getString(R.string.codigo_no_existe));
        tvError.setTextSize(18);
        tvError.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        tvError.setGravity(Gravity.CENTER_VERTICAL);

        resultadoItem.addView(iconoX);
        resultadoItem.addView(tvError);

        resultadosLayout.addView(resultadoItem);
    }

    private void mostrarAsistenciaRegistrada() {
        LinearLayout resultadoItem = new LinearLayout(this);
        resultadoItem.setOrientation(LinearLayout.HORIZONTAL);
        resultadoItem.setPadding(16, 16, 16, 16);
        resultadoItem.setBackgroundResource(R.drawable.item_resultado_background);
        resultadoItem.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView iconoCheck = new ImageView(this);
        iconoCheck.setImageResource(R.drawable.ic_check_green);
        iconoCheck.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        iconoCheck.setPadding(0, 0, 16, 0);

        TextView tvAsistencia = new TextView(this);
        tvAsistencia.setText(getString(R.string.asistencia_registrada));
        tvAsistencia.setTextSize(18);
        tvAsistencia.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tvAsistencia.setGravity(Gravity.CENTER_VERTICAL);

        resultadoItem.addView(iconoCheck);
        resultadoItem.addView(tvAsistencia);

        resultadosLayout.addView(resultadoItem);
    }

    private void mostrarPrimerRegistro(Estudiante estudiante) {
        LinearLayout resultadoItem = new LinearLayout(this);
        resultadoItem.setOrientation(LinearLayout.HORIZONTAL);
        resultadoItem.setPadding(16, 16, 16, 16);
        resultadoItem.setBackgroundResource(R.drawable.item_resultado_background);
        resultadoItem.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        ImageView iconoCheck = new ImageView(this);
        iconoCheck.setImageResource(R.drawable.ic_check_green);
        iconoCheck.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
        iconoCheck.setPadding(0, 0, 16, 0);

        TextView tvPrimerRegistro = new TextView(this);
        tvPrimerRegistro.setText(getString(R.string.primer_registro, estudiante.getPrimerNombre()));
        tvPrimerRegistro.setTextSize(18);
        tvPrimerRegistro.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        tvPrimerRegistro.setGravity(Gravity.CENTER_VERTICAL);

        resultadoItem.addView(iconoCheck);
        resultadoItem.addView(tvPrimerRegistro);

        resultadosLayout.addView(resultadoItem);
    }
}

