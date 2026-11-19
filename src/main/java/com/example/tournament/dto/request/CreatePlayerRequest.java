package com.example.tournament.dto.request;

import jakarta.validation.constraints.*;

public class CreatePlayerRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    private String position;

    @Min(value = 1, message = "Le numéro doit être supérieur à 0")
    private Integer number;

    public CreatePlayerRequest() {
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
}
