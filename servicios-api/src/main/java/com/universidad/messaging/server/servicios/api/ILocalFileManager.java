package com.universidad.messaging.server.servicios.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public interface ILocalFileManager {

    String guardarOriginal(InputStream inputStream, String extension, long expectedSize) throws IOException;

    String guardarCifrado(InputStream inputStream, String extension) throws IOException;

    InputStream leerArchivo(String filePath) throws FileNotFoundException;

}
