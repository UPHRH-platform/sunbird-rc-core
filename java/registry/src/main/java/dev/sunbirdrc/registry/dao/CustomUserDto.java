package dev.sunbirdrc.registry.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CustomUserDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String roleName;
}
