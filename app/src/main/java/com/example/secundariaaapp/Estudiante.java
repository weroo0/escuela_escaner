package com.example.secundariaaapp;

public class Estudiante {
    private String id;
    private String nombre;
    private boolean yaRegistrado;

    // Constructor por defecto
    public Estudiante() {
        this.yaRegistrado = false;
    }

    // Constructor con parámetros
    public Estudiante(String id, String nombre) {
        this.id = id;
        this.nombre = nombre;
        this.yaRegistrado = false;
    }

    // Constructor con parámetros incluyendo estado de registro
    public Estudiante(String id, String nombre, boolean yaRegistrado) {
        this.id = id;
        this.nombre = nombre;
        this.yaRegistrado = yaRegistrado;
    }

    public String getId() { 
        return id; 
    }
    
    public String getNombre() { 
        return nombre; 
    }

    public boolean isYaRegistrado() {
        return yaRegistrado;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setYaRegistrado(boolean yaRegistrado) {
        this.yaRegistrado = yaRegistrado;
    }

    // Método para obtener solo el primer nombre
    public String getPrimerNombre() {
        if (nombre != null && !nombre.trim().isEmpty()) {
            return nombre.split(" ")[0];
        }
        return nombre;
    }
}
