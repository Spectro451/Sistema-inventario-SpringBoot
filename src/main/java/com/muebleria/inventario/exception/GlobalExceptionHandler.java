package com.muebleria.inventario.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("Error 415 - Media Type no soportado\n");
        sb.append("Mensaje: ").append(ex.getMessage()).append("\n");
        sb.append("Tipo recibido: ").append(ex.getContentType()).append("\n");
        sb.append("Tipos soportados: ").append(ex.getSupportedMediaTypes());

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(sb.toString());
    }
}
