package dev.andrea.acompaname_backend.chat.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Access denied")
public class ChatExceptionAccesoDenegado extends ChatException {
    public ChatExceptionAccesoDenegado(String message) {
        super(message);
    }
}
