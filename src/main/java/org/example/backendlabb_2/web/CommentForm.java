package org.example.backendlabb_2.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentForm {
    @NotBlank(message = "Skriv en kommentar.")
    @Size(max = 2000, message = "Kommentaren får vara högst 2000 tecken.")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
