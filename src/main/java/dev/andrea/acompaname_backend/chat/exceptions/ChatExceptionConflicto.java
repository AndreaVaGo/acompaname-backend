package dev.andrea.acompaname_backend.chat.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Conflict")
public class ChatExceptionConflicto extends ChatException {
    public ChatExceptionConflicto(String message) {
        super(message);
    }
}
