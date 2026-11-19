package com.example.tournament.dto.request;

import com.example.tournament.entity.Country;
import jakarta.validation.constraints.*;

public class CreateTeamRequest {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    private String logo;

    private String city;

    private Country country;

    @NotBlank(message = "L'entraîneur est obligatoire")
    private String coach;

    public CreateTeamRequest() {
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public String getCoach() { return coach; }
    public void setCoach(String coach) { this.coach = coach; }
}
