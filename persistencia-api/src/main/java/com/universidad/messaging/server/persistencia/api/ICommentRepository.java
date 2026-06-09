package com.universidad.messaging.server.persistencia.api;


import com.universidad.messaging.server.shared.schema.commentSchema.Comment;
import com.universidad.messaging.server.shared.schema.commentSchema.CommentInfo;

import java.util.List;

public interface ICommentRepository {
    /**
     * Inserta un nuevo comentario en la base de datos.
     * * @param comment El objeto que contiene documentId, userId, content, sentiment y confidence.
     * @return El comentario registrado, incluyendo el ID autogenerado y la fecha (created_at).
     */
    Comment registrarComentario(Comment comment);

    Comment replicarComentario(Comment comment);
    /**
     * Lista todos los comentarios asociados a un documento específico.
     * * @param documentId El ID del documento padre.
     * @return Una lista de comentarios ordenados (usualmente por fecha de creación).
     */
    List<CommentInfo> listarComentariosPorDocumento(Long documentId);

}
