package com.universidad.messaging.server.servicios.api;

import com.universidad.messaging.server.shared.schema.commentSchema.Comment;
import com.universidad.messaging.server.shared.schema.commentSchema.CommentInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ICommentManager {

    Comment registrarComentario(Long documentId, String username, String content);

    Comment replicarComentario(Long id, Long documentId, String username, String content, String sentimentStr, BigDecimal confidence, LocalDateTime createdAt);

    List<CommentInfo> listarComentariosPorDocumento(Long documentId);


}
